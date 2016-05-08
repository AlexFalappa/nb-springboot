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
public class SpringBootConfigurationCompletionProvider implements CompletionProvider {

    private static final String METADATA_JSON = "META-INF/spring-configuration-metadata.json";
    private static final Logger logger = Logger.getLogger(SpringBootConfigurationCompletionProvider.class.getName());
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
                String filter = null;
                int startOffset = caretOffset - 1;
                try {
                    final StyledDocument bDoc = (StyledDocument) document;
                    final int lineStartOffset = getRowFirstNonWhite(bDoc, caretOffset);
                    final char[] line = bDoc.getText(lineStartOffset, caretOffset - lineStartOffset).toCharArray();
                    final int whiteOffset = indexOfWhite(line);
                    filter = new String(line, whiteOffset + 1, line.length - whiteOffset - 1);
                    if (whiteOffset > 0) {
                        startOffset = lineStartOffset + whiteOffset + 1;
                    } else {
                        startOffset = lineStartOffset;
                    }
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }
                complete(completionResultSet, filter, cp, startOffset, caretOffset);
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

    static int getRowFirstNonWhite(StyledDocument doc, int offset) throws BadLocationException {
        Element lineElement = doc.getParagraphElement(offset);
        int start = lineElement.getStartOffset();
        while (start + 1 < lineElement.getEndOffset()) {
            try {
                if (doc.getText(start, 1).charAt(0) != ' ') {
                    break;
                }
            } catch (BadLocationException ex) {
                throw (BadLocationException) new BadLocationException(
                        "calling getText(" + start + ", " + (start + 1) + ") on doc of length: " + doc.getLength(), start).initCause(ex);
            }
            start++;
        }
        return start;
    }

    static int indexOfWhite(char[] line) {
        int i = line.length;
        while (--i > -1) {
            final char c = line[i];
            if (Character.isWhitespace(c)) {
                return i;
            }
        }
        return -1;
    }

    // Create a completion result list based on a filter string, classpath and document offsets.
    private void complete(CompletionResultSet completionResultSet, String filter, ClassPath cp, int startOffset, int caretOffset) {
        long mark = System.currentTimeMillis();
        updateCachesMaps(cp);
        for (String propName : properties.keySet()) {
            if (propName.contains(filter)) {
                for (ItemMetadata item : properties.get(propName)) {
                    completionResultSet.addItem(new SpringBootConfigurationCompletionItem(item, hints.get(propName), cp, startOffset,
                            caretOffset));
                }
            }
        }
        logger.log(INFO, "Completion of ''{0}'' took: {1} msecs", new Object[]{filter, System.currentTimeMillis() - mark});
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
