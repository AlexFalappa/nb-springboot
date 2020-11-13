/*
 * Copyright 2017 the original author or authors.
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
import java.util.logging.Level;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import org.netbeans.api.project.Project;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.Exceptions;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation;

import com.github.alexfalappa.nbspringboot.PrefConstants;
import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.CfgElement;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.PairElement;
import com.github.alexfalappa.nbspringboot.cfgprops.fixes.DeletePropFix;
import com.github.alexfalappa.nbspringboot.cfgprops.fixes.ReplacePropFix;
import com.github.alexfalappa.nbspringboot.cfgprops.parser.CfgPropsParser;
import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;

import static org.springframework.boot.configurationmetadata.Deprecation.Level.ERROR;

/**
 * Highlighting task for deprecated configuration properties names.
 *
 * @author Alessandro Falappa
 */
public class DeprecatedPropsHighlightingTask extends BaseHighlightingTask {

    @Override
    protected String getHighlightPrefName() {
        return PrefConstants.PREF_HLIGHT_LEV_DEPRECATED;
    }

    @Override
    protected int getHighlightDefaultValue() {
        return 1;
    }

    @Override
    protected String getErrorLayerName() {
        return "boot-cfgprops-deprecatedprops";
    }

    @Override
    public int getPriority() {
        return 400;
    }

    @Override
    protected void internalRun(CfgPropsParser.CfgPropsParserResult cfgResult, SchedulerEvent se, BaseDocument document,
            List<ErrorDescription> errors, Severity unused) {
        logger.fine("Highlighting deprecated properties");
        final Project prj = Utils.getActiveProject();
        if (prj != null) {
            final SpringBootService sbs = prj.getLookup().lookup(SpringBootService.class);
            if (sbs != null) {
                for (PairElement pair : cfgResult.getCfgFile().getElements()) {
                    final CfgElement key = pair.getKey();
                    final CfgElement value = pair.getValue();
                    final String pName = key.getText();
                    ConfigurationMetadataProperty cfgMeta = sbs.getPropertyMetadata(pName);
                    if (cfgMeta != null && cfgMeta.getDeprecation() != null) {
                        try {
                            final Deprecation deprecation = cfgMeta.getDeprecation();
                            List<Fix> fixes = new ArrayList<>();
                            final int start = key.getIdxStart();
                            int end = value != null ? value.getIdxEnd() : key.getIdxEnd();
                            fixes.add(new DeletePropFix((StyledDocument) document, key.getText(), key.getIdxStart(), end));
                            if (deprecation.getReplacement() != null) {
                                end = key.getIdxEnd();
                                fixes.add(new ReplacePropFix((StyledDocument) document, start, end, deprecation.getReplacement()));
                            }
                            Deprecation.Level deprLevel = deprecation.getLevel();
                            ErrorDescription errDesc;
                            if (deprLevel == ERROR) {
                                errDesc = ErrorDescriptionFactory.createErrorDescription(
                                        Severity.ERROR,
                                        String.format("No more supported Spring Boot property '%s'", pName),
                                        fixes,
                                        document,
                                        document.createPosition(start),
                                        document.createPosition(end)
                                );
                            } else {
                                errDesc = ErrorDescriptionFactory.createErrorDescription(
                                        Severity.WARNING,
                                        String.format("Deprecated Spring Boot property '%s'", pName),
                                        fixes,
                                        document,
                                        document.createPosition(start),
                                        document.createPosition(end)
                                );
                            }
                            errors.add(errDesc);
                        } catch (BadLocationException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                    if (canceled) {
                        break;
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            logger.log(Level.FINE, "Found {0} deprecated properties", errors.size());
        }
    }

}
