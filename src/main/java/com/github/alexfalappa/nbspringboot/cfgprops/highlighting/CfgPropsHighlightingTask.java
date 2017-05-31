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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.parboiled.errors.ErrorUtils;
import org.parboiled.errors.InvalidInputError;
import org.parboiled.errors.ParseError;

import com.github.alexfalappa.nbspringboot.cfgprops.parser.CfgPropLine;
import com.github.alexfalappa.nbspringboot.cfgprops.parser.CfgPropsParser;

/**
 * Highlighting task for syntax errors, duplicate properties and ... in configuration properties editor.
 *
 * @author Alessandro Falappa
 */
public class CfgPropsHighlightingTask extends ParserResultTask<CfgPropsParser.CfgPropsParserResult> {

    private Formatter<InvalidInputError> formatter = new DefaultInvalidInputErrorFormatter();

    @Override
    public void run(CfgPropsParser.CfgPropsParserResult cfgResult, SchedulerEvent se) {
        try {
            System.out.println("Running SyntaxErrorHighlightingTask");
            List<ParseError> parseErrors = cfgResult.getResult().parseErrors;
            Document document = cfgResult.getSnapshot().getSource().getDocument(false);
            List<ErrorDescription> errors = new ArrayList<>();
            // syntax errors
            System.out.println("... syntax errors");
            for (ParseError error : parseErrors) {
                System.out.println(ErrorUtils.printParseError(error));
                String message = error.getErrorMessage() != null
                        ? error.getErrorMessage()
                        : error instanceof InvalidInputError
                                ? formatter.format((InvalidInputError) error)
                                : error.getClass().getSimpleName();
//                int start = NbDocument.findLineOffset((StyledDocument) document, token.beginLine - 1) + token.beginColumn - 1;
//                int end = NbDocument.findLineOffset((StyledDocument) document, token.endLine - 1) + token.endColumn;
                ErrorDescription errDesc = ErrorDescriptionFactory.createErrorDescription(
                        Severity.ERROR,
                        message,
                        document,
                        document.createPosition(error.getStartIndex()),
                        document.createPosition(error.getEndIndex())
                );
                errors.add(errDesc);
            }
            // duplicate props
            System.out.println("... duplicate props");
            List<CfgPropLine> propsList = cfgResult.getParser().getPropsList();
            Set<String> keys = new HashSet<>();
            for (CfgPropLine propLine : propsList) {
                final String key = propLine.getKey();
                if (keys.contains(key)) {
                    ErrorDescription errDesc = ErrorDescriptionFactory.createErrorDescription(
                            Severity.WARNING,
                            "Duplicate property",
                            document,
                            propLine.getLine()
                    );
                    errors.add(errDesc);
                } else {
                    keys.add(key);
                }
            }
            HintsController.setErrors(document, "simple-java", errors);
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
