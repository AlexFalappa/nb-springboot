/*
 * Copyright 2019 Alessandro Falappa.
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
package com.github.alexfalappa.nbspringboot.cfgprops.completion;

import java.util.Objects;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;

import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ValueHint;

import com.github.alexfalappa.nbspringboot.PrefConstants;
import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;

import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_DEPR_ERROR_SHOW;
import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_DEPR_SORT_LAST;
import static java.util.logging.Level.FINER;

/**
 * Completion query used in {@link CfgPropsCompletionProvider}.
 *
 * @author Alessandro Falappa
 */
public class CfgPropsCompletionQuery extends AsyncCompletionQuery {

    private static final Logger logger = Logger.getLogger(CfgPropsCompletionQuery.class.getName());
    private static final Pattern PATTERN_PROP_NAME = Pattern.compile("[^=\\s]+");
    private final SpringBootService sbs;

    public CfgPropsCompletionQuery(SpringBootService sbs) {
        this.sbs = Objects.requireNonNull(sbs);
    }

    @Override
    protected void query(CompletionResultSet completionResultSet, Document document, int caretOffset) {
        final StyledDocument styDoc = (StyledDocument) document;
        Element lineElement = styDoc.getParagraphElement(caretOffset);
        int lineStartOffset = lineElement.getStartOffset();
        try {
            String lineToCaret = styDoc.getText(lineStartOffset, caretOffset - lineStartOffset);
            logger.log(FINER, "Completion on: {0}", lineToCaret);
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
                    completePropValue(sbs, completionResultSet, propPrefix, valPrefix, lineStartOffset
                            + lineToCaret.indexOf(valPrefix, equalSignOffset), caretOffset);
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

    // Create a completion result list of config properties based on a filter string, classpath and document offsets.
    private void completePropName(SpringBootService sbs, CompletionResultSet completionResultSet, String filter, int startOffset,
            int caretOffset) {
        final Preferences prefs = NbPreferences.forModule(PrefConstants.class);
        final boolean bDeprLast = prefs.getBoolean(PREF_DEPR_SORT_LAST, true);
        final boolean bErrorShow = prefs.getBoolean(PREF_DEPR_ERROR_SHOW, true);
        long mark = System.currentTimeMillis();
        logger.log(FINER, "Completing property name: {0}", filter);
        for (ConfigurationMetadataProperty propMeta : sbs.queryPropertyMetadata(filter)) {
            if (Utils.isErrorDeprecated(propMeta)) {
                // show error level deprecated props based on pref
                if (bErrorShow) {
                    completionResultSet.addItem(new CfgPropCompletionItem(propMeta, sbs, startOffset, caretOffset, bDeprLast));
                }
            } else {
                completionResultSet.addItem(new CfgPropCompletionItem(propMeta, sbs, startOffset, caretOffset, bDeprLast));
            }
        }
        final long elapsed = System.currentTimeMillis() - mark;
        logger.log(FINER, "Property completion of ''{0}'' took: {1} msecs", new Object[]{filter, elapsed});
    }

    // Create a completion result list of properties values based on a property name, filter string, classpath and document offsets.
    public void completePropValue(SpringBootService sbs, CompletionResultSet completionResultSet, String propName, String filter,
            int startOffset, int caretOffset) {
        long mark = System.currentTimeMillis();
        logger.log(FINER, "Completing property value: {0}", filter);
        for (ValueHint valueHint : sbs.queryHintMetadata(propName, filter)) {
            completionResultSet.addItem(new CfgPropValueCompletionItem(valueHint, startOffset, caretOffset));
        }
        final long elapsed = System.currentTimeMillis() - mark;
        logger.log(FINER, "Value completion of ''{0}'' on ''{1}'' took: {2} msecs", new Object[]{filter, propName, elapsed});
    }

}
