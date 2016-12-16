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
package com.github.alexfalappa.nbspringboot.spi;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.spi.editor.completion.CompletionResultSet;
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

import com.github.alexfalappa.nbspringboot.api.SpringBootService;
import com.github.alexfalappa.nbspringboot.cfgeditor.ConfigPropertyCompletionItem;
import com.github.alexfalappa.nbspringboot.cfgeditor.ConfigValueCompletionItem;

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
    public static final int LOG_COMPLETION_TRESH = 50;
    private final Project prj;
    private ClassPath cpExec = null;
    private final JsonMarshaller jsonMarsaller = new JsonMarshaller();
    private final Map<String, ConfigurationMetadata> cfgMetasInJars = new HashMap<>();
    private final MultiValueMap<String, ItemMetadata> properties = new LinkedMultiValueMap<>();
    private final MultiValueMap<String, ItemMetadata> groups = new LinkedMultiValueMap<>();
    private final Map<String, ItemHint> hints = new HashMap<>();

    public SpringBootServiceImpl(Project p) {
        this.prj = p;
    }

    @Override
    public void init() {
        logger.log(INFO, "Initializing SpringBootService for project {0}", new Object[]{prj.toString()});
        // set up a reference to the execute classpath object
        Sources srcs = ProjectUtils.getSources(prj);
        SourceGroup[] srcGroups = srcs.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        for (SourceGroup group : srcGroups) {
            if (group.getName().toLowerCase().contains("source")) {
                cpExec = ClassPath.getClassPath(group.getRootFolder(), ClassPath.EXECUTE);
                break;
            }
        }
        // listen for classpath changes
        cpExec.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                System.out.println(evt.toString());
            }
        });
        // build configuration properties maps
        updateCacheMaps();
    }

    @Override
    public boolean cfgPropsCompletionEnabled() {
        if (cpExec == null) {
            return false;
        }
        try {
            cpExec.getClassLoader(false).loadClass("org.springframework.boot.context.properties.ConfigurationProperties");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    @Override
    public void completePropName(CompletionResultSet completionResultSet, String filter, int startOffset, int caretOffset) {
        long mark = System.currentTimeMillis();
        updateCacheMaps();
        for (String propName : properties.keySet()) {
            if (filter == null || propName.contains(filter)) {
                for (ItemMetadata item : properties.get(propName)) {
                    completionResultSet.addItem(new ConfigPropertyCompletionItem(item, hints.get(propName), cpExec, startOffset,
                            caretOffset));
                }
            }
        }
        final long elapsed = System.currentTimeMillis() - mark;
        if (elapsed > LOG_COMPLETION_TRESH) {
            logger.log(INFO, "Property completion of ''{0}'' took: {1} msecs", new Object[]{filter, elapsed});
        }
    }

    @Override
    public void completePropValue(CompletionResultSet completionResultSet, String propName, String filter, int startOffset, int caretOffset) {
        long mark = System.currentTimeMillis();
        updateCacheMaps();
        if (hints.containsKey(propName)) {
            ItemHint hint = hints.get(propName);
            final List<ItemHint.ValueHint> values = hint.getValues();
            if (values != null) {
                for (ItemHint.ValueHint valHint : values) {
                    if (filter == null || valHint.getValue().toString().startsWith(filter)) {
                        completionResultSet.addItem(new ConfigValueCompletionItem(valHint, cpExec, startOffset, caretOffset));
                    }
                }
            }
        }
        final long elapsed = System.currentTimeMillis() - mark;
        if (elapsed > LOG_COMPLETION_TRESH) {
            logger.log(INFO, "Value completion of ''{0}'' on ''{1}'' took: {2} msecs", new Object[]{filter, propName, elapsed});
        }
    }

    // Update internal caches and maps from the given classpath.
    private void updateCacheMaps() {
        this.properties.clear();
        this.hints.clear();
        this.groups.clear();
        final List<FileObject> cfgMetaFiles = cpExec.findAllResources(METADATA_JSON);
        for (FileObject fo : cfgMetaFiles) {
            try {
                ConfigurationMetadata meta;
                FileObject archiveFo = FileUtil.getArchiveFile(fo);
                if (archiveFo != null) {
                    // parse and cache configuration metadata from JSON file in jar
                    String archivePath = archiveFo.getPath();
                    if (!cfgMetasInJars.containsKey(archivePath)) {
                        logger.log(INFO, "Unmarshalling configuration metadata from {0}", FileUtil.getFileDisplayName(fo));
                        cfgMetasInJars.put(archivePath, jsonMarsaller.read(fo.getInputStream()));
                    }
                    meta = cfgMetasInJars.get(archivePath);
                } else {
                    // parse configuration metadata from JSON file (usually produced by spring configuration processor)
                    logger.log(INFO, "Unmarshalling configuration metadata from {0}", FileUtil.getFileDisplayName(fo));
                    meta = jsonMarsaller.read(fo.getInputStream());
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

}
