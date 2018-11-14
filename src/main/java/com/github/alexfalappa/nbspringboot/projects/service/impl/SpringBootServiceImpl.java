/*
 * Copyright 2016 Alessandro Falappa.
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
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
import org.springframework.boot.configurationmetadata.ValueHint;

import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static java.util.regex.Pattern.compile;
import org.apache.maven.artifact.Artifact;

/**
 * Project wide {@link SpringBootService} implementation.
 * <p>
 * It reads Spring Boot configuration properties metadata and maintains indexed structures extracted out of it.
 * <p>
 * Registered for maven projects.
 *
 * @author Alessandro Falappa
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
    private final Pattern pArrayNotation = compile("(.+)\\[\\d+\\]");
    private SimpleConfigurationMetadataRepository repo = new SimpleConfigurationMetadataRepository();
    private final Map<String, ConfigurationMetadataRepository> reposInJars = new HashMap<>();
    private NbMavenProjectImpl mvnPrj;
    private ClassPath cpExec;
    private Map<String, ConfigurationMetadataProperty> cachedProperties;
    private Map<String, Boolean> cachedDepsPresence = new HashMap<>();
    private final Set<String> collectionProperties = new HashSet<>();
    private final Set<String> mapProperties = new HashSet<>();

    public SpringBootServiceImpl(Project p) {
        if (p instanceof NbMavenProjectImpl) {
            this.mvnPrj = (NbMavenProjectImpl) p;
        }
        logger.log(Level.INFO, "Creating Spring Boot service for project {0}",
                FileUtil.getFileDisplayName(p.getProjectDirectory()));
    }

    @Override
    public void refresh() {
        logger.info("Refreshing Spring Boot service");
        // check maven project has a dependency starting with 'spring-boot'
        logger.fine("Checking maven project has a spring boot dependency");
        boolean springBootAvailable = dependencyArtifactIdContains(mvnPrj.getProjectWatcher(), "spring-boot");
        // clear and exit if no spring boot dependency detected
        if (!springBootAvailable) {
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
                    Matcher mArrNot = pArrayNotation.matcher(relaxedName);
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
    public List<ValueHint> queryHintMetadata(String propertyName, String filter) {
        if (cpExec == null) {
            init();
        }
        List<ValueHint> ret = new LinkedList<>();
        ConfigurationMetadataProperty cfgMeta = getPropertyMetadata(propertyName);
        if (cfgMeta != null) {
            for (ValueHint valueHint : cfgMeta.getHints().getValueHints()) {
                if (filter == null || valueHint.getValue().toString().contains(filter)) {
                    ret.add(valueHint);
                }
            }
        }
        return ret;
    }

    @Override
    public boolean hasPomDependency(String artifactId) {
        if (cpExec == null) {
            init();
        }
        if (cachedDepsPresence != null) {
            if (!cachedDepsPresence.containsKey(artifactId)) {
                cachedDepsPresence.put(artifactId, dependencyArtifactIdContains(mvnPrj.getProjectWatcher(), artifactId));
            }
            return cachedDepsPresence.get(artifactId);
        }
        return false;
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
        if (mvnPrj == null) {
            return;
        }
        logger.info("Initializing Spring Boot service");
        // check maven project has a dependency starting with 'spring-boot'
        boolean springBootAvailable = dependencyArtifactIdContains(mvnPrj.getProjectWatcher(), "spring-boot");
        logger.fine("Checking maven project has a spring boot dependency");
        // early exit if no spring boot dependency detected
        if (!springBootAvailable) {
            return;
        }
        logger.log(INFO, "Initializing SpringBootService for project {0}", new Object[]{mvnPrj.toString()});
        cachedDepsPresence.clear();
        // set up a reference to the execute classpath object
        Sources srcs = ProjectUtils.getSources(mvnPrj);
        SourceGroup[] srcGroups = srcs.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        boolean srcGroupFound = false;
        for (SourceGroup group : srcGroups) {
            if (group.getName().toLowerCase().contains("source")) {
                srcGroupFound = true;
                cpExec = ClassPath.getClassPath(group.getRootFolder(), ClassPath.EXECUTE);
                break;
            }
        }
        if (!srcGroupFound) {
            logger.log(WARNING, "No sources found for project: {0}", new Object[]{mvnPrj.toString()});
        }
        if (cpExec != null) {
            // listen for pom changes
            logger.info("Adding maven pom listener...");
            mvnPrj.getProjectWatcher().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    final String propertyName = String.valueOf(evt.getPropertyName());
                    logger.log(FINE, "Maven pom change ({0})", propertyName);
                    if (propertyName.equals("MavenProject")) {
                        refresh();
                    }
                }
            });
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
                if (type.contains("Map<")) {
                    mapProperties.add(key);
                }
                if (type.contains("List<") || type.contains("Set<") || type.contains("Collection<")) {
                    collectionProperties.add(key);
                }
            }
        }
    }

    // check if any of the project dependencies artifact ids contains the given string
    private boolean dependencyArtifactIdContains(NbMavenProject nbMvn, String artifactId) {
        MavenProject mPrj = nbMvn.getMavenProject();
        for (Object o : mPrj.getDependencies()) {
            Dependency d = (Dependency) o;
            if (d.getArtifactId().contains(artifactId)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasDependencyArtifactIdStartsWithAndVersionStartsWith(NbMavenProject mavenProject, String artifactId, String version) {
        MavenProject mPrj = mavenProject.getMavenProject();
        for (Object o : mPrj.getDependencies()) {
            Dependency d = (Dependency) o;
            if (d.getArtifactId().startsWith(artifactId) && d.getVersion().startsWith(version)) {
                return true;
            }
        }
        return false;
    }

    // tell if the project currently uses Spring Boot 2.x
    private boolean isBoot2() {
        if (mvnPrj != null) {
            // retrieve boot version from parent pom declaration if present
            String springBootParentVersion = lookupSpringBootParentVersion(mvnPrj.getProjectWatcher().getMavenProject());
            if(springBootParentVersion != null) {
                return springBootParentVersion.startsWith("2");
            }
            // look in dependency management section (inclusion of spring boot BOM)
            String springBootBOMVersion = lookupSpringBootBOMVersion(mvnPrj.getProjectWatcher().getMavenProject());
            if(springBootBOMVersion != null) {
                return springBootBOMVersion.startsWith("2");
            }            
            // consider spring-boot 2.x if not 1.x dependencies found
            return !hasDependencyArtifactIdStartsWithAndVersionStartsWith(mvnPrj.getProjectWatcher(), "spring-boot", "1");
        }        
        // consider spring-boot 2.x as default
        return true;
    }

    // retrieve boot version from parent hierarchy
    private String lookupSpringBootParentVersion(MavenProject mavenProject) {
        if(mavenProject.hasParent()){
            Artifact parent = mavenProject.getParentArtifact();
            if("org.springframework.boot".equals(parent.getGroupId()) && "spring-boot-starter-parent".equals(parent.getArtifactId())){
                return parent.getVersion();
            } else {
                return lookupSpringBootParentVersion(mavenProject.getParent());
            }
        } else {
            return null;
        }
    }
    
    // retrieve boot version from dependency management (spring boot BOM)
    private String lookupSpringBootBOMVersion(MavenProject mavenProject) {
        for (Dependency d : mavenProject.getDependencyManagement().getDependencies()) {
            if("org.springframework.boot".equals(d.getGroupId()) && "spring-boot-dependencies".equals(d.getArtifactId())){
                return d.getVersion();
            }
        }
        return null;
    }
    
    private void adjustNbActions() {
        final FileObject foPrjDir = mvnPrj.getProjectDirectory();
        FileObject foNbAct = foPrjDir.getFileObject("nbactions.xml");
        if (foNbAct != null) {
            logger.fine("Adjusting nbactions.xml file");
            try (FileLock lock = foNbAct.lock()) {
                try (PrintWriter pw = new PrintWriter(foPrjDir.createAndOpen("nbactions.tmp"))) {
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
            try (FileLock lock = foTmp.lock()) {
                foTmp.move(lock, foPrjDir, "nbactions", "xml");
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

}
