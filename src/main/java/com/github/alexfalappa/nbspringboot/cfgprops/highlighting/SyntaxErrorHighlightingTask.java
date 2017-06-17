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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.Exceptions;
import org.parboiled.common.Formatter;
import org.parboiled.errors.DefaultInvalidInputErrorFormatter;
import org.parboiled.errors.InvalidInputError;
import org.parboiled.errors.ParseError;

import com.github.alexfalappa.nbspringboot.cfgprops.parser.CfgPropsParser;

/**
 * Highlighting task for syntax errors.
 *
 * @author Alessandro Falappa
 */
public class SyntaxErrorHighlightingTask extends ParserResultTask<CfgPropsParser.CfgPropsParserResult> {

    private static final Logger logger = Logger.getLogger(SyntaxErrorHighlightingTask.class.getName());
    private static final String ERROR_LAYER_NAME = "boot-props-syntax-errors";
    private final Formatter<InvalidInputError> formatter = new DefaultInvalidInputErrorFormatter();

    @Override
    public void run(CfgPropsParser.CfgPropsParserResult cfgResult, SchedulerEvent se) {
        logger.fine("Highlighting syntax errors");
        try {
            List<ErrorDescription> errors = new ArrayList<>();
            final List<ParseError> parseErrors = cfgResult.getParbResult().parseErrors;
            final Document document = cfgResult.getSnapshot().getSource().getDocument(false);
            for (ParseError error : parseErrors) {
                String message = error.getErrorMessage() != null
                        ? error.getErrorMessage()
                        : error instanceof InvalidInputError
                                ? formatter.format((InvalidInputError) error)
                                : error.getClass().getSimpleName();
                ErrorDescription errDesc = ErrorDescriptionFactory.createErrorDescription(
                        Severity.ERROR,
                        message,
                        document,
                        document.createPosition(Math.min(error.getStartIndex(), document.getLength() + 1)),
                        document.createPosition(Math.min(error.getEndIndex(), document.getLength() + 1))
                );
                errors.add(errDesc);
            }
            HintsController.setErrors(document, ERROR_LAYER_NAME, errors);
        } catch (BadLocationException | ParseException ex1) {
            Exceptions.printStackTrace(ex1);
        }
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    @Override
    public void cancel() {
    }
}
