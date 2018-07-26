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
import java.util.logging.Level;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;

import org.netbeans.api.project.Project;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.Exceptions;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

import com.github.alexfalappa.nbspringboot.PrefConstants;
import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.CfgElement;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.PairElement;
import com.github.alexfalappa.nbspringboot.cfgprops.fixes.DeletePropFix;
import com.github.alexfalappa.nbspringboot.cfgprops.parser.CfgPropsParser;
import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;

/**
 * Highlighting task for unknown configuration properties names.
 *
 * @author Alessandro Falappa
 */
public class UnknownPropsHighlightingTask extends BaseHighlightingTask {

    @Override
    protected String getHighlightPrefName() {
        return PrefConstants.PREF_HLIGHT_LEV_UNKNOWN;
    }

    @Override
    protected int getHighlightDefaultValue() {
        return 1;
    }

    @Override
    protected String getErrorLayerName() {
        return "boot-cfgprops-unknownprops";
    }

    @Override
    public int getPriority() {
        return 500;
    }

    @Override
    protected void internalRun(CfgPropsParser.CfgPropsParserResult cfgResult, SchedulerEvent se, Document document,
            List<ErrorDescription> errors, Severity severity) {
        logger.fine("Highlighting unknown properties");
        final Project prj = Utils.getActiveProject();
        if (prj != null) {
            final SpringBootService sbs = prj.getLookup().lookup(SpringBootService.class);
            if (sbs != null) {
                for (PairElement pair : cfgResult.getCfgFile().getElements()) {
                    final CfgElement key = pair.getKey();
                    final CfgElement value = pair.getValue();
                    final String pName = key.getText();
                    ConfigurationMetadataProperty cfgMeta = sbs.getPropertyMetadata(pName);
                    if (cfgMeta == null) {
                        try {
                            List<Fix> fixes = new ArrayList<>();
                            int end = value != null ? value.getIdxEnd() : key.getIdxEnd();
                            fixes.add(new DeletePropFix((StyledDocument) document, key.getText(), key.getIdxStart(), end));
                            ErrorDescription errDesc = ErrorDescriptionFactory.createErrorDescription(
                                    severity,
                                    String.format("Unknown Spring Boot property '%s'", pName),
                                    fixes,
                                    document,
                                    document.createPosition(key.getIdxStart()),
                                    document.createPosition(key.getIdxEnd())
                            );
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
            logger.log(Level.FINE, "Found {0} unknown properties", errors.size());
        }
    }

}
