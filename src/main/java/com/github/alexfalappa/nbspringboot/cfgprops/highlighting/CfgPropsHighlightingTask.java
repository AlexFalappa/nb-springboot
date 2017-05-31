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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

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
 * Highlighting task for syntax errors, duplicate properties and ... in configuration properties editor.
 *
 * @author Alessandro Falappa
 */
public class CfgPropsHighlightingTask extends ParserResultTask<CfgPropsParser.CfgPropsParserResult> {

    private final Formatter<InvalidInputError> formatter = new DefaultInvalidInputErrorFormatter();

    @Override
    public void run(CfgPropsParser.CfgPropsParserResult cfgResult, SchedulerEvent se) {
        try {
            System.out.println("--- Highlighting errors/warnings...");
            List<ParseError> parseErrors = cfgResult.getParbResult().parseErrors;
            Document document = cfgResult.getSnapshot().getSource().getDocument(false);
            List<ErrorDescription> errors = new ArrayList<>();
            // syntax errors
            for (ParseError error : parseErrors) {
//                System.out.println(ErrorUtils.printParseError(error));
//                System.out.printf("start %d stop %d%n", error.getStartIndex(), error.getEndIndex());
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
            // duplicate props
            Map<String, SortedSet<Integer>> propsLines = cfgResult.getPropLines();
            for (Map.Entry<String, SortedSet<Integer>> entry : propsLines.entrySet()) {
                final SortedSet<Integer> lines = entry.getValue();
                if (lines.size() > 1) {
                    Iterator<Integer> it = lines.iterator();
                    Integer firstLine = it.next();
                    while (it.hasNext()) {
                        ErrorDescription errDesc = ErrorDescriptionFactory.createErrorDescription(
                                Severity.WARNING,
                                String.format("Duplicate of property at line %d", firstLine),
                                document,
                                it.next()
                        );
                        errors.add(errDesc);
                    }
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
