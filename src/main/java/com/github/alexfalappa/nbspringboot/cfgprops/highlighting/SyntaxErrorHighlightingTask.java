/*
 * Copyright 2017 Alessandro Falappa.
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
package com.github.alexfalappa.nbspringboot.cfgprops.highlighting;

import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.Exceptions;
import org.parboiled.buffers.InputBuffer;
import org.parboiled.common.Formatter;
import org.parboiled.errors.DefaultInvalidInputErrorFormatter;
import org.parboiled.errors.InvalidInputError;
import org.parboiled.errors.ParseError;

import com.github.alexfalappa.nbspringboot.PrefConstants;
import com.github.alexfalappa.nbspringboot.cfgprops.parser.CfgPropsParser;

/**
 * Highlighting task for syntax errors.
 *
 * @author Alessandro Falappa
 */
public class SyntaxErrorHighlightingTask extends BaseHighlightingTask {

    private final Formatter<InvalidInputError> formatter = new DefaultInvalidInputErrorFormatter();

    @Override
    protected String getHighlightPrefName() {
        return PrefConstants.PREF_HLIGHT_LEV_SYNERR;
    }

    @Override
    protected int getHighlightDefaultValue() {
        return 2;
    }

    @Override
    protected String getErrorLayerName() {
        return "boot-props-syntax-errors";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    protected void internalRun(CfgPropsParser.CfgPropsParserResult cfgResult, SchedulerEvent se, Document document, List<ErrorDescription> errors, Severity severity) {
        logger.fine("Highlighting syntax errors");
        try {
            final InputBuffer ibuf = cfgResult.getParbResult().inputBuffer;
            final List<ParseError> parseErrors = cfgResult.getParbResult().parseErrors;
            for (ParseError error : parseErrors) {
                String message = error.getErrorMessage() != null
                        ? error.getErrorMessage()
                        : error instanceof InvalidInputError
                                ? formatter.format((InvalidInputError) error)
                                : error.getClass().getSimpleName();
                ErrorDescription errDesc = ErrorDescriptionFactory.createErrorDescription(
                        severity,
                        message,
                        document,
                        document.createPosition(ibuf.getOriginalIndex(error.getStartIndex())),
                        document.createPosition(ibuf.getOriginalIndex(error.getEndIndex()))
                );
                errors.add(errDesc);
            }
        } catch (BadLocationException | ParseException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
