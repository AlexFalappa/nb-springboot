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
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.Document;

import org.netbeans.api.project.Project;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.Pair;
import org.openide.util.Utilities;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

import com.github.alexfalappa.nbspringboot.PrefConstants;
import com.github.alexfalappa.nbspringboot.cfgprops.parser.CfgPropsParser;
import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;

import static java.util.regex.Pattern.compile;

/**
 * Highlighting task for unknown configuration properties names.
 *
 * @author Alessandro Falappa
 */
public class UnknownPropsHighlightingTask extends BaseHighlightingTask {

    private final Pattern pArrayNotation = compile("(.+)\\[\\d+\\]");

    @Override
    protected String getHighlightPrefName() {
        return PrefConstants.PREF_HLIGHT_LEV_DTMISMATCH;
    }

    @Override
    protected int getHighlightDefaultValue() {
        return 2;
    }

    @Override
    protected String getErrorLayerName() {
        return "boot-cfgprops-unknownprops";
    }

    @Override
    public int getPriority() {
        return 400;
    }

    @Override
    protected void internalRun(CfgPropsParser.CfgPropsParserResult cfgResult, SchedulerEvent se, Document document, List<ErrorDescription> errors, Severity severity) {
        logger.fine("Highlighting unknown properties");
        final Project prj = Utilities.actionsGlobalContext().lookup(Project.class);
        if (prj != null) {
            final SpringBootService sbs = prj.getLookup().lookup(SpringBootService.class);
            if (sbs != null) {
                final Map<Integer, Pair<String, String>> propLines = cfgResult.getPropLines();
                for (Map.Entry<Integer, Pair<String, String>> entry : propLines.entrySet()) {
                    int line = entry.getKey();
                    String pName = entry.getValue().first();
                    ConfigurationMetadataProperty cfgMeta = sbs.getPropertyMetadata(pName);
                    if (cfgMeta == null) {
                        // try to interpret array notation (strip '[index]' from pName)
                        Matcher mArrNot = pArrayNotation.matcher(pName);
                        if (mArrNot.matches()) {
                            cfgMeta = sbs.getPropertyMetadata(mArrNot.group(1));
                        } else {
                            // try to interpret map notation (see if pName starts with a set of known map props)
                            for (String mapPropertyName : sbs.getMapPropertyNames()) {
                                if (pName.startsWith(mapPropertyName)) {
                                    cfgMeta = sbs.getPropertyMetadata(mapPropertyName);
                                    break;
                                }
                            }
                        }
                    }
                    if (cfgMeta == null) {
                        ErrorDescription errDesc = ErrorDescriptionFactory.createErrorDescription(
                                severity,
                                String.format("Unknown Spring Boot property '%s'", pName),
                                document,
                                line
                        );
                        errors.add(errDesc);
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            logger.log(Level.FINE, "Found {0} unknown properties", errors.size());
        }
    }

}
