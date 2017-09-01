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
package com.github.alexfalappa.nbspringboot.projects.service.spi;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

/**
 * Project wide {@link SpringBootService} implementation.
 * <p>
 * It reads Spring Boot configuration properties metadata and maintaining indexed structures extracted out of it.
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
    private boolean springBootAvailable = false;
    private NbMavenProjectImpl mvnPrj;
    private ClassPath cpExec;
    private Map<String, ConfigurationMetadataProperty> cachedProperties;
    private final Set<String> collectionProperties = new HashSet<>();
    private final Set<String> mapProperties = new HashSet<>();

    public SpringBootServiceImpl(Project p) {
        if (p instanceof NbMavenProjectImpl) {
            this.mvnPrj = (NbMavenProjectImpl) p;
        }
        logger.log(Level.INFO, "Creating Spring Boot service for project {0}", FileUtil.getFileDisplayName(p.getProjectDirectory()));
    }

    private void init() {
        if (mvnPrj == null) {
            return;
        }
        logger.info("Initializing SpringBoot service");
        // check maven project has a dependency starting with 'spring-boot'
        logger.fine("Checking maven project has a spring boot dependency");
        springBootAvailable = dependencyArtifactIdContains(mvnPrj.getProjectWatcher(), "spring-boot");
        // early exit if no spring boot dependency detected
        if (!springBootAvailable) {
            return;
        }
        logger.log(INFO, "Initializing SpringBootService for project {0}", new Object[]{mvnPrj.toString()});
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
            // check if completion of configuration properties is possible
            try {
                logger.fine("Checking spring boot ConfigurationProperties class is on the project execution classpath");
                cpExec.getClassLoader(false).loadClass("org.springframework.boot.context.properties.ConfigurationProperties");
            } catch (ClassNotFoundException ex) {
                // no completion
            }
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

    @Override
    public void refresh() {
        logger.info("Refreshing SpringBoot service");
        // check maven project has a dependency starting with 'spring-boot'
        logger.fine("Checking maven project has a spring boot dependency");
        springBootAvailable = dependencyArtifactIdContains(mvnPrj.getProjectWatcher(), "spring-boot");
        // clear and exit if no spring boot dependency detected
        if (!springBootAvailable) {
            reposInJars.clear();
            collectionProperties.clear();
            mapProperties.clear();
            return;
        }
        if (cpExec == null) {
            init();
        } else {
            // check if completion of configuration properties is possible
            try {
                logger.fine("Checking spring boot ConfigurationProperties class is on the project execution classpath");
                cpExec.getClassLoader(false).loadClass("org.springframework.boot.context.properties.ConfigurationProperties");
            } catch (ClassNotFoundException ex) {
                // no completion
            }
            // build configuration metadata repository
            updateConfigRepo();
        }
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
    public ConfigurationMetadataProperty getPropertyMetadata(String propertyName) {
        if (cpExec == null) {
            init();
        }
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
        return null;
    }

    @Override
    public List<ConfigurationMetadataProperty> queryPropertyMetadata(String filter) {
        if (cpExec == null) {
            init();
        }
        List<ConfigurationMetadataProperty> ret = new LinkedList<>();
        for (String propName : getPropertyNames()) {
            if (filter == null || propName.contains(filter)) {
                ret.add(cachedProperties.get(propName));
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

    @Override
    public Set<String> getCollectionPropertyNames() {
        return collectionProperties;
    }

    @Override
    public Set<String> getMapPropertyNames() {
        return mapProperties;
    }
}
