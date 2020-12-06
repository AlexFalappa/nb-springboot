/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.alexfalappa.nbspringboot.projects.service.impl;

import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.projects.service.api.HintProvider;
import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.regex.Pattern.compile;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.NbMavenProjectImpl;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.springframework.boot.bind.RelaxedNames;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.configurationmetadata.SimpleConfigurationMetadataRepository;

/**
 * Project wide {@link SpringBootService} implementation.
 * <p>
 * It scans the classpath for {@code META-INF/spring-configuration-metadata.json} files, then unmarshals the files into the
 * corresponding {@code ConfigurationMetadata} classes and maintains indexed structures extracted out of it.
 * <p>
 * Registered for maven projects with jar and war packaging.
 *
 * @author Alessandro Falappa
 * @author Diego Díez Ricondo
 */
@ProjectServiceProvider(
        service = SpringBootService.class,
        projectType = {
            "org-netbeans-modules-maven/" + NbMavenProject.TYPE_JAR,
            "org-netbeans-modules-maven/" + NbMavenProject.TYPE_WAR
        }
)
public class SpringBootServiceImpl implements SpringBootService {

    private static final Logger logger = Logger.getLogger(SpringBootServiceImpl.class.getName());
    private static final String METADATA_JSON = "META-INF/spring-configuration-metadata.json";
    private static final Pattern PATTERN_ARRAY_NOTATION = compile("(.+)\\[\\d+\\]");
    private static final NoopHintProvider NOOP_HINT_PROVIDER = new NoopHintProvider();
    private SimpleConfigurationMetadataRepository repo = new SimpleConfigurationMetadataRepository();
    private final Map<String, ConfigurationMetadataRepository> reposInJars = new HashMap<>();
    private NbMavenProjectImpl mvnPrj;
    private String springBootVersion;
    private ClassPath cpExec;
    private Map<String, ConfigurationMetadataProperty> cachedProperties;
    private final Map<String, Boolean> cachedDepsPresence = new HashMap<>();
    private final Set<String> collectionProperties = new HashSet<>();
    private final Set<String> mapProperties = new HashSet<>();
    private final Map<String, HintProvider> providerMap = new HashMap<>();

    public SpringBootServiceImpl(Project p) {
        final FileObject projectDirectory = p.getProjectDirectory();
        if (p instanceof NbMavenProjectImpl) {
            logger.log(INFO, "Creating Spring Boot service for project {0}", FileUtil.getFileDisplayName(projectDirectory));
            this.mvnPrj = (NbMavenProjectImpl) p;
            // check maven project is a spring-boot project
            logger.fine("Checking maven project has a spring boot dependency");
            springBootVersion = Utils.getSpringBootVersion(mvnPrj).orElse(null);
            // early exit if no spring boot dependency detected
            if (springBootVersion == null) {
                return;
            }
            // listen for pom changes
            logger.info("Adding maven pom listener...");
            this.mvnPrj.getProjectWatcher().addPropertyChangeListener((PropertyChangeEvent evt) -> {
                if (NbMavenProject.PROP_PROJECT.equals(evt.getPropertyName())) {
                    logger.log(FINE, "Maven pom change ({0})", evt.getPropertyName());
                    long start = System.currentTimeMillis();
                    refresh();
                    long elapsedMs = System.currentTimeMillis() - start;
                    logger.log(FINE, "Spring Boot service refresh took {0}ms", elapsedMs);
                }
            });
        } else {
            logger.log(SEVERE, "Error creating Spring Boot service for project {0}", FileUtil.getFileDisplayName(projectDirectory));
        }
    }

    @Override
    public void refresh() {
        logger.info("Refreshing Spring Boot service");
        // re-check maven project is a spring-boot project
        logger.fine("Checking maven project has a spring boot dependency");
        springBootVersion = Utils.getSpringBootVersion(mvnPrj).orElse(null);
        // clear and exit if no spring boot dependency detected
        if (springBootVersion == null) {
            reposInJars.clear();
            collectionProperties.clear();
            mapProperties.clear();
            // TODO delete nbactions.xml file from project dir ?
            return;
        }
        cachedDepsPresence.clear();
        if (cpExec == null) {
            init();
        } else {
            // build configuration metadata repository
            updateConfigRepo();
        }
        // adjust the nbactions.xml file depending on boot version
        adjustNbActions();
    }

    @Override
    public ClassPath getManagedClassPath() {
        return cpExec;
    }

    @Override
    public Set<String> getPropertyNames() {
        if (cpExec == null) {
            init();
        }
        return cachedProperties.keySet();
    }

    @Override
    public Set<String> getCollectionPropertyNames() {
        return collectionProperties;
    }

    @Override
    public Set<String> getMapPropertyNames() {
        return mapProperties;
    }

    @Override
    public HintProvider getHintProvider(String name) {
        return providerMap.getOrDefault(name, NOOP_HINT_PROVIDER);
    }

    @Override
    public ConfigurationMetadataProperty getPropertyMetadata(String propertyName) {
        if (cpExec == null) {
            init();
        }
        if (cachedProperties != null) {
            // generate and try relaxed variants
            for (String relaxedName : new RelaxedNames(propertyName)) {
                if (cachedProperties.containsKey(relaxedName)) {
                    return cachedProperties.get(relaxedName);
                } else {
                    // try to interpret array notation (strip '[index]' from pName)
                    Matcher mArrNot = PATTERN_ARRAY_NOTATION.matcher(relaxedName);
                    if (mArrNot.matches()) {
                        return cachedProperties.get(mArrNot.group(1));
                    } else {
                        // try to interpret map notation (see if pName starts with a set of known map props)
                        for (String mapPropertyName : getMapPropertyNames()) {
                            if (relaxedName.startsWith(mapPropertyName)) {
                                return cachedProperties.get(mapPropertyName);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<ConfigurationMetadataProperty> queryPropertyMetadata(String filter) {
        if (cpExec == null) {
            init();
        }
        List<ConfigurationMetadataProperty> ret = new LinkedList<>();
        if (cachedProperties != null) {
            for (String propName : getPropertyNames()) {
                if (filter == null || propName.contains(filter)) {
                    ret.add(cachedProperties.get(propName));
                }
            }
        }
        return ret;
    }

    @Override
    public boolean hasPomDependency(String artifactId) {
        if (!cachedDepsPresence.containsKey(artifactId)) {
            cachedDepsPresence.put(artifactId, Utils.dependencyArtifactIdContains(mvnPrj.getProjectWatcher(), artifactId));
        }
        return cachedDepsPresence.get(artifactId);
    }

    @Override
    public String getRestartEnvVarName() {
        return isBoot2() ? ENV_RESTART_20 : ENV_RESTART_15;
    }

    @Override
    public String getPluginPropsPrefix() {
        return isBoot2() ? "spring-boot.run" : "run";
    }

    private void init() {
        // set up a reference to the execute classpath object
        cpExec = Utils.execClasspathForProj(mvnPrj);
        if (cpExec != null) {
            // populate hint providers map
            FileObject resourcesFolder = Utils.resourcesFolderForProj(mvnPrj);
            providerMap.put("logger-name", new LoggerNameHintProvider(resourcesFolder));
            providerMap.put("class-reference", new ClassReferenceHintProvider(mvnPrj.getProjectDirectory(), cpExec));
            providerMap.put("handle-as", new HandleAsHintProvider(resourcesFolder, cpExec));
            // build configuration properties maps
            updateConfigRepo();
        }
    }

    // Update internal configuration metadata repository
    private void updateConfigRepo() {
        logger.fine("Updating config metadata repo");
        repo = new SimpleConfigurationMetadataRepository();
        final List<FileObject> cfgMetaFiles = cpExec.findAllResources(METADATA_JSON);
        for (FileObject fo : cfgMetaFiles) {
            try {
                ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();
                ConfigurationMetadataRepository currRepo;
                FileObject archiveFo = FileUtil.getArchiveFile(fo);
                if (archiveFo != null) {
                    // parse and cache configuration metadata from JSON file in jar
                    String archivePath = archiveFo.getPath();
                    if (!reposInJars.containsKey(archivePath)) {
                        logger.log(INFO, "Unmarshalling configuration metadata from {0}", FileUtil.getFileDisplayName(fo));
                        ConfigurationMetadataRepository jarRepo = builder.withJsonResource(fo.getInputStream()).build();
                        reposInJars.put(archivePath, jarRepo);
                    }
                    currRepo = reposInJars.get(archivePath);
                } else {
                    // parse configuration metadata from standalone JSON file (usually produced by spring configuration processor)
                    logger.log(INFO, "Unmarshalling configuration metadata from {0}", FileUtil.getFileDisplayName(fo));
                    currRepo = builder.withJsonResource(fo.getInputStream()).build();
                }
                repo.include(currRepo);
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        // update cached values
        cachedProperties = repo.getAllProperties();
        // extract collection/map properties names based on heuristics
        for (Map.Entry<String, ConfigurationMetadataProperty> entry : cachedProperties.entrySet()) {
            final String type = entry.getValue().getType();
            if (type != null) {
                final String key = entry.getKey();
                if (type.startsWith("java.util.Map<")) {
                    mapProperties.add(key);
                }
                if (type.startsWith("java.util.List<")
                        || type.startsWith("java.util.Set<")
                        || type.startsWith("java.util.Collection<")) {
                    collectionProperties.add(key);
                }
            }
        }
    }

    // tell if the project currently uses Spring Boot 2.x
    private boolean isBoot2() {
        return springBootVersion != null && springBootVersion.startsWith("2");
    }

    private void adjustNbActions() {
        final FileObject foPrjDir = mvnPrj.getProjectDirectory();
        FileObject foNbAct = foPrjDir.getFileObject("nbactions.xml");
        // only adjust nbactions.xml if necessary
        if (foNbAct != null && isAdjustingNeeded(foNbAct)) {
            logger.fine("Adjusting nbactions.xml file");
            try ( FileLock lock = foNbAct.lock()) {
                try ( PrintWriter pw = new PrintWriter(foPrjDir.createAndOpen("nbactions.tmp"))) {
                    if (isBoot2()) {
                        for (String line : foNbAct.asLines()) {
                            line = line.replace(ENV_RESTART_15, ENV_RESTART_20);
                            line = line.replace("<run.", "<spring-boot.run.");
                            line = line.replace("</run.", "</spring-boot.run.");
                            pw.println(line);
                        }
                    } else {
                        for (String line : foNbAct.asLines()) {
                            line = line.replace(ENV_RESTART_20, ENV_RESTART_15);
                            line = line.replace("<spring-boot.run.", "<run.");
                            line = line.replace("</spring-boot.run.", "</run.");
                            pw.println(line);
                        }
                    }
                }
                foNbAct.delete(lock);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            FileObject foTmp = foPrjDir.getFileObject("nbactions.tmp");
            try ( FileLock lock = foTmp.lock()) {
                foTmp.move(lock, foPrjDir, "nbactions", "xml");
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private boolean isAdjustingNeeded(FileObject nbActions) {
        try {
            String wrongPluginPropsPrefix = isBoot2() ? "<run." : "<spring-boot.run.";
            String wrongRestartTriggerFile = isBoot2() ? ENV_RESTART_15 : ENV_RESTART_20;
            return nbActions.asLines().stream()
                    .anyMatch(line -> line.contains(wrongPluginPropsPrefix) || line.contains(wrongRestartTriggerFile));
        } catch (IOException ex) {
            logger.log(WARNING, "IO problem examining nbactions.xml file", ex);
            return false;
        }
    }

}
