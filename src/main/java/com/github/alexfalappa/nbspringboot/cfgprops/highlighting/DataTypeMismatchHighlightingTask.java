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

import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.Project;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.Exceptions;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.util.ClassUtils;

import com.github.alexfalappa.nbspringboot.PrefConstants;
import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.CfgElement;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.PairElement;
import com.github.alexfalappa.nbspringboot.cfgprops.parser.CfgPropsParser;
import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;

import static java.util.regex.Pattern.compile;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Highlighting task for data type mismatch in configuration properties values.
 *
 * @author Alessandro Falappa
 */
public class DataTypeMismatchHighlightingTask extends BaseHighlightingTask {

    private final Pattern pOneGenTypeArg = compile("([^<>]+)<(.+)>");
    private final Pattern pTwoGenTypeArgs = compile("([^<>]+)<(.+),(.+)>");
    private final ApplicationConversionService conversionService = new ApplicationConversionService();

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
        return "boot-cfgprops-typemismatches";
    }

    @Override
    public int getPriority() {
        return 200;
    }

    @Override
    protected void internalRun(CfgPropsParser.CfgPropsParserResult cfgResult, SchedulerEvent se, BaseDocument document,
            List<ErrorDescription> errors, Severity severity) {
        logger.fine("Highlighting data type mismatches");
        final Project prj = Utils.getActiveProject();
        if (prj != null) {
            final SpringBootService sbs = prj.getLookup().lookup(SpringBootService.class);
            final ClassPath cp = Utils.execClasspathForProj(prj);
            if (sbs != null && cp != null) {
                final ClassLoader cl = cp.getClassLoader(true);
                for (PairElement pair : cfgResult.getCfgFile().getElements()) {
                    final CfgElement key = pair.getKey();
                    final CfgElement value = pair.getValue();
                    final String pName = key.getText();
                    final String pValue = value != null ? value.getText() : "";
                    ConfigurationMetadataProperty cfgMeta = sbs.getPropertyMetadata(pName);
                    if (cfgMeta == null) {
                        continue;
                    }
                    try {
                        final String type = cfgMeta.getType();
                        // type is null for deprecated configuration properties
                        if (type == null) {
                            continue;
                        }
                        if (type.contains("<")) {
                            // maps
                            Matcher mMap = pTwoGenTypeArgs.matcher(type);
                            if (mMap.matches() && mMap.groupCount() == 3) {
                                String keyType = mMap.group(2);
                                check(keyType, pName.substring(pName.lastIndexOf('.') + 1), document, key, errors, cl,
                                        severity);
                                String valueType = mMap.group(3);
                                check(valueType, pValue, document, value, errors, cl, severity);
                            }
                            // collections
                            Matcher mColl = pOneGenTypeArg.matcher(type);
                            if (mColl.matches() && mColl.groupCount() == 2) {
                                String genericType = mColl.group(2);
                                if (pValue.contains(",")) {
                                    for (String val : pValue.split("\\s*,\\s*")) {
                                        check(genericType, val, document, value, errors, cl, severity);
                                    }
                                } else {
                                    check(genericType, pValue, document, value, errors, cl, severity);
                                }
                            }
                        } else {
                            if (pValue.contains(",") && type.endsWith("[]")) {
                                for (String val : pValue.split("\\s*,\\s*")) {
                                    check(type.substring(0, type.length() - 2), val, document, value, errors, cl, severity);
                                }
                            } else {
                                if (type.endsWith("[]")) {
                                    check(type.substring(0, type.length() - 2), pValue, document, value, errors, cl, severity);
                                } else {
                                    check(type, pValue, document, value, errors, cl, severity);
                                }
                            }
                        }
                    } catch (BadLocationException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    if (canceled) {
                        break;
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            logger.log(Level.FINE, "Found {0} data type mismatches", errors.size());
        }
    }

    private void check(String type, String text, Document document, CfgElement elem, List<ErrorDescription> errors, ClassLoader cl,
            Severity severity) throws BadLocationException {
        if (canceled) {
            return;
        }
        if (text == null || text.isEmpty()) {
            return;
        }
        // non generic types
        try {
            if (!checkType(type, text, cl)) {
                ErrorDescription errDesc = ErrorDescriptionFactory.createErrorDescription(
                        severity,
                        String.format("Cannot parse '%s' as %s", text, type),
                        document,
                        document.createPosition(elem.getIdxStart()),
                        document.createPosition(elem.getIdxEnd())
                );
                errors.add(errDesc);
            }
        } catch (IllegalArgumentException ex) {
            // problems instantiating type class, cannot decide, ignore
        }
    }

    private boolean checkType(String type, String text, ClassLoader cl) throws IllegalArgumentException {
        Class<?> clazz;
        try {
            clazz = Class.forName(type);
        } catch (ClassNotFoundException ex) {
            clazz = ClassUtils.resolveClassName(type, cl);
        }
        if (clazz != null) {
            try {
                Object obj = conversionService.convert(text, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(clazz));
                return obj != null;
            } catch (Exception ex) {
                return false;
            }
        }
        // unresolvable/unknown class, assume user knows what is doing
        return true;
    }
}
