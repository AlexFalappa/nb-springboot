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
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

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
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;

import static java.util.logging.Level.FINER;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static org.springframework.boot.configurationprocessor.metadata.ItemMetadata.ItemType.GROUP;
import static org.springframework.boot.configurationprocessor.metadata.ItemMetadata.ItemType.PROPERTY;

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
    private final JsonMarshaller jsonMarshaller = new JsonMarshaller();
    private final Map<String, ConfigurationMetadata> cfgMetasInJars = new HashMap<>();
    private final MultiValueMap<String, ItemMetadata> properties = new LinkedMultiValueMap<>();
    private final MultiValueMap<String, ItemMetadata> groups = new LinkedMultiValueMap<>();
    private final Map<String, ItemHint> hints = new HashMap<>();
    private boolean springBootAvailable = false;
    private boolean cfgPropsCompletionAvailable = false;
    private NbMavenProjectImpl mvnPrj;
    private ClassPath cpExec;

    public SpringBootServiceImpl(Project p) {
        if (p instanceof NbMavenProjectImpl) {
            this.mvnPrj = (NbMavenProjectImpl) p;
        }
    }

    @Override
    public void init() {
        logger.info("Initializing SpringBoot service");
        if (mvnPrj == null) {
            return;
        }
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
        for (SourceGroup group : srcGroups) {
            if (group.getName().toLowerCase().contains("source")) {
                cpExec = ClassPath.getClassPath(group.getRootFolder(), ClassPath.EXECUTE);
                logger.info("Adding classpath listener...");
                cpExec.addPropertyChangeListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        logger.fine("Execution classpath change");
                        logger.finer(evt.toString());
                        if (evt.getPropertyName().equalsIgnoreCase("entries")) {
                            refresh();
                        }
                    }
                });
                // listen for pom changes
                mvnPrj.getProjectWatcher().addPropertyChangeListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        logger.fine("Maven pom change");
                        logger.finer(evt.toString());
                    }
                });
                break;
            }
        }
        // check if completion of configuration properties is possible
        try {
            logger.fine("Checking spring boot ConfigurationProperties class is on the project execution classpath");
            cpExec.getClassLoader(false).loadClass("org.springframework.boot.context.properties.ConfigurationProperties");
            cfgPropsCompletionAvailable = true;
        } catch (ClassNotFoundException ex) {
            cfgPropsCompletionAvailable = false;
        }
        // build configuration properties maps
        updateCacheMaps();
    }

    @Override
    public void refresh() {
        logger.info("Refreshing SpringBoot service");
        // check maven project has a dependency starting with 'spring-boot'
        logger.fine("Checking maven project has a spring boot dependency");
        springBootAvailable = dependencyArtifactIdContains(mvnPrj.getProjectWatcher(), "spring-boot");
        // clear and exit if no spring boot dependency detected
        if (!springBootAvailable) {
            cfgPropsCompletionAvailable = false;
            cfgMetasInJars.clear();
            properties.clear();
            hints.clear();
            groups.clear();
            return;
        }
        // check if completion of configuration properties is possible
        try {
            logger.fine("Checking spring boot ConfigurationProperties class is on the project execution classpath");
            cpExec.getClassLoader(false).loadClass("org.springframework.boot.context.properties.ConfigurationProperties");
            cfgPropsCompletionAvailable = true;
        } catch (ClassNotFoundException ex) {
            cfgPropsCompletionAvailable = false;
        }
        // build configuration properties maps
        updateCacheMaps();
    }

    @Override
    public ClassPath getManagedClassPath() {
        return cpExec;
    }

    @Override
    public boolean cfgPropsCompletionEnabled() {
        return cfgPropsCompletionAvailable;
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public List<ItemMetadata> getPropertyMetadata(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public List<ItemMetadata> queryPropertyMetadata(String filter) {
        List<ItemMetadata> ret = new LinkedList<>();
        updateCacheMaps();
        for (String propName : properties.keySet()) {
            if (filter == null || propName.contains(filter)) {
                ret.addAll(properties.get(propName));
            }
        }
        return ret;
    }

    @Override
    public ItemHint getHintMetadata(String propertyName) {
        return hints.get(propertyName);
    }

    @Override
    public List<ItemHint.ValueHint> queryHintMetadata(String propertyName, String filter) {
        List<ItemHint.ValueHint> ret = new LinkedList<>();
        updateCacheMaps();
        if (hints.containsKey(propertyName)) {
            ItemHint hint = hints.get(propertyName);
            final List<ItemHint.ValueHint> values = hint.getValues();
            if (values != null) {
                for (ItemHint.ValueHint valHint : values) {
                    if (filter == null || valHint.getValue().toString().contains(filter)) {
                        ret.add(valHint);
                    }
                }
            }
        }
        return ret;
    }

    // Update internal caches and maps from the given classpath.
    private void updateCacheMaps() {
        logger.fine("Updating cache maps");
        properties.clear();
        hints.clear();
        groups.clear();
        final List<FileObject> cfgMetaFiles = cpExec.findAllResources(METADATA_JSON);
        for (FileObject fo : cfgMetaFiles) {
            try {
                ConfigurationMetadata meta;
                FileObject archiveFo = FileUtil.getArchiveFile(fo);
                if (archiveFo != null) {
                    logger.log(FINER, "Considering metadata file in archive: {0}", FileUtil.getFileDisplayName(archiveFo));
                    // parse and cache configuration metadata from JSON file in jar
                    String archivePath = archiveFo.getPath();
                    if (!cfgMetasInJars.containsKey(archivePath)) {
                        logger.log(INFO, "Unmarshalling configuration metadata from {0}", FileUtil.getFileDisplayName(fo));
                        cfgMetasInJars.put(archivePath, jsonMarshaller.read(fo.getInputStream()));
                    }
                    meta = cfgMetasInJars.get(archivePath);
                } else {
                    logger.log(FINER, "Considering metadata file: {0}", FileUtil.getFileDisplayName(fo));
                    // parse configuration metadata from JSON file (usually produced by spring configuration processor)
                    logger.log(INFO, "Unmarshalling configuration metadata from {0}", FileUtil.getFileDisplayName(fo));
                    meta = jsonMarshaller.read(fo.getInputStream());
                }
                // update property and groups maps
                for (ItemMetadata item : meta.getItems()) {
                    final String itemName = item.getName();
                    if (item.isOfItemType(PROPERTY)) {
                        properties.add(itemName, item);
                    }
                    if (item.isOfItemType(GROUP)) {
                        groups.add(itemName, item);
                    }
                }
                // update hints maps
                for (ItemHint hint : meta.getHints()) {
                    ItemHint old = hints.put(hint.getName(), hint);
                    if (old != null) {
                        logger.log(WARNING, "Overwritten hint for property ''{0}''", old.toString());
                    }
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
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
}
