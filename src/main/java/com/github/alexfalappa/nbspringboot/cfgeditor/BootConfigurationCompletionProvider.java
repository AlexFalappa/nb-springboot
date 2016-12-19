/*
 * Copyright 2015 Keevosh ULP.
 * Modifications copyright 2016 Alessandro Falappa.
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

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.project.Project;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;

import static java.util.logging.Level.FINER;

/**
 * The Spring Boot Configuration implementation of {@code CompletionProvider}.
 * <p>
 * The entry point of completion support. This provider is registered for text/x-properties files and is enabled if spring-boot is available
 * on the classpath.
 * <p>
 * It scans the classpath for {@code META-INF/spring-configuration-metadata.json} files, then unmarshals the files into the corresponding {@link
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

    private static final Logger logger = Logger.getLogger(BootConfigurationCompletionProvider.class.getName());
    private static final Pattern PATTERN_PROP_NAME = Pattern.compile("[^=\\s]+");

    @Override
    public CompletionTask createTask(int queryType, JTextComponent jtc) {
        if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE) {
            return null;
        }
        Project prj = Utilities.actionsGlobalContext().lookup(Project.class);
        if (prj == null) {
            return null;
        }
        final SpringBootService sbs = prj.getLookup().lookup(SpringBootService.class);
        if (sbs == null) {
            return null;
        }
        if (!sbs.cfgPropsCompletionEnabled()) {
            return null;
        }
        logger.fine("Creating completion task");
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int caretOffset) {
                final StyledDocument styDoc = (StyledDocument) document;
                Element lineElement = styDoc.getParagraphElement(caretOffset);
                int lineStartOffset = lineElement.getStartOffset();
                try {
                    logger.log(FINER, "Completion on line: {0}", styDoc.getText(lineStartOffset,
                            lineElement.getEndOffset() - lineStartOffset));
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
                            completePropValue(sbs, completionResultSet, propPrefix, valPrefix, lineStartOffset + lineToCaret.indexOf(
                                    valPrefix), caretOffset);
                        } else if (equalSignOffset >= 0) {
                            //value completion with empty filter
                            completePropValue(sbs, completionResultSet, propPrefix, null, lineStartOffset + equalSignOffset + 1,
                                    caretOffset);
                        } else {
                            // property completion
                            completePropName(sbs, completionResultSet, propPrefix, lineStartOffset + propPrefixOffset, caretOffset);
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

    // Create a completion result list of config properties based on a filter string, classpath and document offsets.
    private void completePropName(SpringBootService sbs, CompletionResultSet completionResultSet, String filter, int startOffset, int caretOffset) {
        long mark = System.currentTimeMillis();
        logger.log(FINER, "Completing property name: {0}", filter);
        for (ItemMetadata item : sbs.queryPropertyMetadata(filter)) {
            completionResultSet.addItem(new ConfigPropertyCompletionItem(item, sbs, startOffset, caretOffset));
        }
        final long elapsed = System.currentTimeMillis() - mark;
        logger.log(FINER, "Property completion of ''{0}'' took: {1} msecs", new Object[]{filter, elapsed});
    }

    // Create a completion result list of properties values based on a property name, filter string, classpath and document offsets.
    public void completePropValue(SpringBootService sbs, CompletionResultSet completionResultSet, String propName, String filter, int startOffset, int caretOffset) {
        long mark = System.currentTimeMillis();
        logger.log(FINER, "Completing property value: {0}", filter);
        for (ItemHint.ValueHint hint : sbs.queryHintMetadata(propName, filter)) {
            completionResultSet.addItem(new ConfigValueCompletionItem(hint, startOffset, caretOffset));
        }
        final long elapsed = System.currentTimeMillis() - mark;
        logger.log(FINER, "Value completion of ''{0}'' on ''{1}'' took: {2} msecs", new Object[]{filter, propName, elapsed});
    }

}
