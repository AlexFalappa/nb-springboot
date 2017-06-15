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
import java.util.logging.Logger;

import javax.swing.text.Document;

import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;

import com.github.alexfalappa.nbspringboot.cfgprops.parser.CfgPropsParser;

/**
 * Highlighting task for duplicate properties.
 *
 * @author Alessandro Falappa
 */
public class DuplicatesHighlightingTask extends ParserResultTask<CfgPropsParser.CfgPropsParserResult> {

    private static final Logger logger = Logger.getLogger(DuplicatesHighlightingTask.class.getName());
    private static final String ERROR_LAYER_NAME = "boot-cfgprops-duplicates";

    @Override
    public void run(CfgPropsParser.CfgPropsParserResult cfgResult, SchedulerEvent se) {
        logger.fine("Highlighting duplicate props");
        List<ErrorDescription> errors = new ArrayList<>();
        final Document document = cfgResult.getSnapshot().getSource().getDocument(false);
        final Map<String, SortedSet<Integer>> propsLines = cfgResult.getPropLines();
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
        HintsController.setErrors(document, ERROR_LAYER_NAME, errors);
    }

    @Override
    public int getPriority() {
        return 300;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    @Override
    public void cancel() {
    }
}
