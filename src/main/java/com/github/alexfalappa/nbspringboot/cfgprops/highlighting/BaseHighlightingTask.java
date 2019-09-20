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
import java.util.prefs.Preferences;

import javax.swing.text.Document;

import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.NbPreferences;

import com.github.alexfalappa.nbspringboot.PrefConstants;
import com.github.alexfalappa.nbspringboot.cfgprops.parser.CfgPropsParser;

/**
 * Highlighting task for duplicate properties.
 *
 * @author Alessandro Falappa
 */
public abstract class BaseHighlightingTask extends ParserResultTask<CfgPropsParser.CfgPropsParserResult> {

    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected volatile boolean canceled = false;

    @Override
    public void run(CfgPropsParser.CfgPropsParserResult cfgResult, SchedulerEvent se) {
        canceled = false;
        final Preferences prefs = NbPreferences.forModule(PrefConstants.class);
        final int sevLevel = prefs.getInt(getHighlightPrefName(), getHighlightDefaultValue());
        List<ErrorDescription> errors = new ArrayList<>();
        final Document document = cfgResult.getSnapshot().getSource().getDocument(false);
        if (document != null) {
            // skip error calculation if preference set to "None"
            if (sevLevel > 0) {
                Severity severity = decodeSeverity(sevLevel);
                internalRun(cfgResult, se, document, errors, severity);
            }
            HintsController.setErrors(document, getErrorLayerName(), errors);
        }
    }

    @Override
    public abstract int getPriority();

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    @Override
    public void cancel() {
        canceled = true;
    }

    protected abstract String getHighlightPrefName();

    protected abstract int getHighlightDefaultValue();

    protected abstract String getErrorLayerName();

    protected abstract void internalRun(CfgPropsParser.CfgPropsParserResult cfgResult, SchedulerEvent se, Document document,
            List<ErrorDescription> errors, Severity severity);

    private Severity decodeSeverity(int level) {
        switch (level) {
            case 1:
                return Severity.WARNING;
            case 2:
                return Severity.ERROR;
            default:
                throw new AssertionError();
        }
    }
}
