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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import org.openide.util.Exceptions;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;

/**
 * The Spring Boot Configuration implementation of CompletionProvider.
 *
 * The entry point of completion support. This provider is registered for text/x-properties files and is enabled if spring-boot is available
 * on the classpath.
 *
 * It scans the classpath for {@code META-INF/spring-configuration-metadata.json} files, Then demarshalls the files into the corresponding {@link
 * ConfigurationMetadata} classes and later in the query task scans for items and fills the {@link CompletionResultSet}.
 *
 * @author Aggelos Karalias
 */
@MimeRegistration(mimeType = "text/x-properties", service = CompletionProvider.class)
public class SpringBootConfigurationCompletionProvider implements CompletionProvider {

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
        final List<FileObject> configurationMetaFiles = cp.findAllResources("META-INF/spring-configuration-metadata.json");
        final List<ConfigurationMetadata> configurationMetas = new ArrayList<>(configurationMetaFiles.size());
        final JsonMarshaller jsonMarsaller = new JsonMarshaller();
        for (FileObject configurationMetaFile : configurationMetaFiles) {
            try {
                configurationMetas.add(jsonMarsaller.read(configurationMetaFile.getInputStream()));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
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
                for (ConfigurationMetadata configurationMeta : configurationMetas) {
                    for (ItemMetadata item : configurationMeta.getItems()) {
                        if (item.isOfItemType(ItemMetadata.ItemType.PROPERTY)
                                && !item.getName().equals("")
                                && item.getName().startsWith(filter)) {
                            completionResultSet.addItem(new SpringBootConfigurationCompletionItem(item, cp, startOffset, caretOffset));
                        }
                    }
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

    static int getRowFirstNonWhite(StyledDocument doc, int offset) throws BadLocationException {
        Element lineElement = doc.getParagraphElement(offset);
        int start = lineElement.getStartOffset();
        while (start + 1 < lineElement.getEndOffset()) {
            try {
                if (doc.getText(start, 1).charAt(0) != ' ') {
                    break;
                }
            } catch (BadLocationException ex) {
                throw (BadLocationException) new BadLocationException("calling getText(" + start + ", " + (start + 1) + ") on doc of length: " + doc
                        .getLength(), start).initCause(ex);
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
}
