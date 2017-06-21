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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.text.Document;

import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.Pair;

import com.github.alexfalappa.nbspringboot.PrefConstants;
import com.github.alexfalappa.nbspringboot.cfgprops.parser.CfgPropsParser;

/**
 * Highlighting task for duplicate properties.
 *
 * @author Alessandro Falappa
 */
public class DuplicatesHighlightingTask extends BaseHighlightingTask {

    @Override
    protected String getHighlightPrefName() {
        return PrefConstants.PREF_HLIGHT_LEV_DUPLICATES;
    }

    @Override
    protected int getHighlightDefaultValue() {
        return 1;
    }

    @Override
    protected String getErrorLayerName() {
        return "boot-cfgprops-duplicates";
    }

    @Override
    public int getPriority() {
        return 300;
    }

    @Override
    protected void internalRun(CfgPropsParser.CfgPropsParserResult cfgResult, SchedulerEvent se, Document document, List<ErrorDescription> errors, Severity severity) {
        logger.fine("Highlighting duplicate props");
        Map<String, Integer> firstOccur = new HashMap<>();
        final Map<Integer, Pair<String, String>> propLines = cfgResult.getPropLines();
        for (Map.Entry<Integer, Pair<String, String>> entry : propLines.entrySet()) {
            final String propName = entry.getValue().first();
            final Integer line = entry.getKey();
            if (firstOccur.containsKey(propName)) {
                ErrorDescription errDesc = ErrorDescriptionFactory.createErrorDescription(
                        severity,
                        String.format("Duplicate of property at line %d", firstOccur.get(propName)),
                        document,
                        line);
                errors.add(errDesc);
            } else {
                firstOccur.put(propName, line);
            }
            if (canceled) {
                break;
            }
        }
    }
}
