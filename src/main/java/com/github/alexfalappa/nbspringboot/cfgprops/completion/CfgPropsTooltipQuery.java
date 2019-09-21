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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JToolTip;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;

import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.openide.util.Exceptions;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;

import static java.util.logging.Level.FINER;

/**
 * Completion query for tooltip completion used in {@link CfgPropsCompletionProvider}.
 *
 * @author Alessandro Falappa
 */
public class CfgPropsTooltipQuery extends AsyncCompletionQuery {

    private static final Logger logger = Logger.getLogger(CfgPropsTooltipQuery.class.getName());
    private static final Pattern PATTERN_PROP_NAME = Pattern.compile("\\s*([^=\\s]+)\\s*[=:]?.*");
    private final SpringBootService sbs;

    public CfgPropsTooltipQuery(SpringBootService sbs) {
        this.sbs = Objects.requireNonNull(sbs);
    }

    @Override
    protected void query(CompletionResultSet completionResultSet, Document document, int caretOffset) {
        final StyledDocument styDoc = (StyledDocument) document;
        Element lineElement = styDoc.getParagraphElement(caretOffset);
        int lineStartOffset = lineElement.getStartOffset();
        int lineEndOffset = lineElement.getEndOffset();
        try {
            String line = styDoc.getText(lineStartOffset, lineEndOffset - lineStartOffset);
            if (line.endsWith("\n")) {
                line = line.substring(0, line.length() - 1);
            }
            logger.log(FINER, "Tooltip on: {0}", line);
            if (!line.contains("#")) {
                //property name extraction from line
                Matcher matcher = PATTERN_PROP_NAME.matcher(line);
                if (matcher.matches()) {
                    String propPrefix = matcher.group(1);
                    ConfigurationMetadataProperty propMeta = sbs.getPropertyMetadata(propPrefix);
                    if (propMeta != null) {
                        final JToolTip toolTip = new JToolTip();
                        toolTip.setTipText(Utils.shortenJavaType(propMeta.getType()));
                        completionResultSet.setToolTip(toolTip);
                    }
                }
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        completionResultSet.finish();
    }

}
