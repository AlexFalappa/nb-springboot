/*
 * Copyright 2015 Keevosh ULP.
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
package com.github.alexfalappa.nbspringboot.cfgeditor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static org.springframework.boot.configurationprocessor.metadata.ItemMetadata.ItemType.GROUP;
import static org.springframework.boot.configurationprocessor.metadata.ItemMetadata.ItemType.PROPERTY;

/**
 * The Spring Boot Configuration implementation of CompletionProvider.
 * <p>
 * The entry point of completion support. This provider is registered for text/x-properties files and is enabled if spring-boot is available
 * on the classpath.
 * <p>
 * It scans the classpath for {@code META-INF/spring-configuration-metadata.json} files, then unmarshalls the files into the corresponding {@link
 * ConfigurationMetadata} classes and later in the query task scans for items and fills the {@link CompletionResultSet}.
 * <p>
 * The provider organizes properties, groups and hints in maps indexed by name. It also maintains a cache of configuration metadata parsed
 * from JSON files in jars to speed up completion.
 *
 * @author Aggelos Karalias
 * @author Alessandro Falappa
 */
@MimeRegistration(mimeType = "text/x-properties", service = CompletionProvider.class)
public class BootConfigurationCompletionProvider implements CompletionProvider {

    private static final String METADATA_JSON = "META-INF/spring-configuration-metadata.json";
    private static final Logger logger = Logger.getLogger(BootConfigurationCompletionProvider.class.getName());
    private static final Pattern PATTERN_PROP_NAME = Pattern.compile("[^=\\s]+");
    private final JsonMarshaller jsonMarsaller = new JsonMarshaller();
    private final Map<String, ConfigurationMetadata> cfgMetasInJars = new HashMap<>();
    private final MultiValueMap<String, ItemMetadata> properties = new LinkedMultiValueMap<>();
    private final MultiValueMap<String, ItemMetadata> groups = new LinkedMultiValueMap<>();
    private final Map<String, ItemHint> hints = new HashMap<>();

    @Override
    public CompletionTask createTask(int queryType, JTextComponent jtc) {
        if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE) {
            return null;
        }
        final TopComponent activeTC = TopComponent.getRegistry().getActivated();
        if (activeTC == null) {
            return null;
        }
        final FileObject fileObject = activeTC.getLookup().lookup(FileObject.class);
        if (fileObject == null) {
            return null;
        }
        final ClassPath cp = ClassPath.getClassPath(fileObject, ClassPath.EXECUTE);
        if (cp == null) {
            return null;
        }
        try {
            cp.getClassLoader(false).loadClass("org.springframework.boot.context.properties.ConfigurationProperties");
        } catch (ClassNotFoundException ex) {
            return null;
        }
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int caretOffset) {
                final StyledDocument styDoc = (StyledDocument) document;
                Element lineElement = styDoc.getParagraphElement(caretOffset);
                int lineStartOffset = lineElement.getStartOffset();
                try {
                    String lineToCaret = styDoc.getText(lineStartOffset, caretOffset - lineStartOffset);
                    if (!lineToCaret.contains("#")) {
                        String[] parts = lineToCaret.split("=");
                        //property name extraction from part before =
                        Matcher matcher = PATTERN_PROP_NAME.matcher(parts[0]);
                        String propPrefix = null;
                        int propPrefixOffset = 0;
                        while (matcher.find()) {
                            propPrefix = matcher.group();
                            propPrefixOffset = matcher.start();
                        }
                        // check which kind of completion
                        final int equalSignOffset = lineToCaret.indexOf('=');
                        if (parts.length > 1) {
                            //value completion
                            String valPrefix = parts[1].trim();
                            completePropValue(completionResultSet, propPrefix, valPrefix, cp, lineStartOffset + lineToCaret.indexOf(
                                    valPrefix), caretOffset);
                        } else if (equalSignOffset >= 0) {
                            //value completion with empty filter
                            completePropValue(completionResultSet, propPrefix, null, cp, lineStartOffset + equalSignOffset + 1, caretOffset);
                        } else {
                            // property completion
                            completePropName(completionResultSet, propPrefix, cp, lineStartOffset + propPrefixOffset, caretOffset);
                        }
                    }
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }
                completionResultSet.finish();
            }
        }, jtc);
    }

    @Override
    public int getAutoQueryTypes(JTextComponent jtc, String string) {
        return 0;
    }

    static TopComponent getCurrentEditor() {
        Set<? extends Mode> modes = WindowManager.getDefault().getModes();
        for (Mode mode : modes) {
            if ("editor".equals(mode.getName())) {
                return mode.getSelectedTopComponent();
            }
        }
        return null;
    }

    // Create a completion result list of config properties based on a filter string, classpath and document offsets.
    private void completePropName(CompletionResultSet completionResultSet, String filter, ClassPath cp, int startOffset, int caretOffset) {
        long mark = System.currentTimeMillis();
        updateCachesMaps(cp);
        for (String propName : properties.keySet()) {
            if (filter == null || propName.contains(filter)) {
                for (ItemMetadata item : properties.get(propName)) {
                    completionResultSet.addItem(new BootConfigurationCompletionItem(item, hints.get(propName), cp, startOffset,
                            caretOffset));
                }
            }
        }
        logger.log(INFO, "Property completion of ''{0}'' took: {1} msecs", new Object[]{filter, System.currentTimeMillis() - mark});
    }

    // Create a completion result list of properties values based on a property name, filter string, classpath and document offsets.
    private void completePropValue(CompletionResultSet completionResultSet, String propName, String filter, ClassPath cp, int startOffset,
            int caretOffset) {
        long mark = System.currentTimeMillis();
        updateCachesMaps(cp);
        if (hints.containsKey(propName)) {
            ItemHint hint = hints.get(propName);
            final List<ItemHint.ValueHint> values = hint.getValues();
            if (values != null) {
                for (ItemHint.ValueHint valHint : values) {
                    if (filter == null || valHint.getValue().toString().startsWith(filter)) {
                        completionResultSet.addItem(new ConfigValueCompletionItem(valHint, cp, startOffset, caretOffset));
                    }
                }
            }
        }
        logger.log(INFO, "Value completion of ''{0}'' on ''{1}'' took: {2} msecs",
                new Object[]{filter, propName, System.currentTimeMillis() - mark});
    }

    // Update internal caches and maps from the given classpath.
    private void updateCachesMaps(ClassPath cp) {
        this.properties.clear();
        this.hints.clear();
        this.groups.clear();
        final List<FileObject> cfgMetaFiles = cp.findAllResources(METADATA_JSON);
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
