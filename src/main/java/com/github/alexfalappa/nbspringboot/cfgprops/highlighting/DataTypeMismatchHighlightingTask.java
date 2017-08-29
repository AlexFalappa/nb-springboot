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

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.Pair;
import org.openide.util.Utilities;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.util.ClassUtils;

import com.github.alexfalappa.nbspringboot.PrefConstants;
import com.github.alexfalappa.nbspringboot.cfgprops.parser.CfgPropsParser;
import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;
import com.github.drapostolos.typeparser.TypeParser;

import static java.util.regex.Pattern.compile;

/**
 * Highlighting task for data type mismatch in configuration properties values.
 *
 * @author Alessandro Falappa
 */
public class DataTypeMismatchHighlightingTask extends BaseHighlightingTask {

    private final Pattern pOneGenTypeArg = compile("([^<>]+)<(.+)>");
    private final Pattern pTwoGenTypeArgs = compile("([^<>]+)<(.+),(.+)>");
    private final TypeParser parser = TypeParser.newBuilder().enablePropertyEditor().build();

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
    protected void internalRun(CfgPropsParser.CfgPropsParserResult cfgResult, SchedulerEvent se, Document document, List<ErrorDescription> errors, Severity severity) {
        logger.fine("Highlighting data type mismatches");
        final Project prj = Utilities.actionsGlobalContext().lookup(Project.class);
        if (prj != null) {
            final SpringBootService sbs = prj.getLookup().lookup(SpringBootService.class);
            final ClassPath cp = getProjectClasspath(prj);
            if (sbs != null && cp != null) {
                final Map<Integer, Pair<String, String>> propLines = cfgResult.getPropLines();
                for (Map.Entry<Integer, Pair<String, String>> entry : propLines.entrySet()) {
                    int line = entry.getKey();
                    String pName = entry.getValue().first();
                    String pValue = entry.getValue().second();
                    ConfigurationMetadataProperty cfgMeta = sbs.getPropertyMetadata(pName);
                    if (cfgMeta != null) {
                        final String type = cfgMeta.getType();
                        final ClassLoader cl = cp.getClassLoader(true);
                        if (type.contains("<")) {
                            // maps
                            Matcher mMap = pTwoGenTypeArgs.matcher(type);
                            if (mMap.matches() && mMap.groupCount() == 3) {
                                String keyType = mMap.group(2);
                                check(keyType, pName.substring(pName.lastIndexOf('.') + 1), document, line, errors, cl, severity);
                                String valueType = mMap.group(3);
                                check(valueType, pValue, document, line, errors, cl, severity);
                            }
                            // collections
                            Matcher mColl = pOneGenTypeArg.matcher(type);
                            if (mColl.matches() && mColl.groupCount() == 2) {
                                String genericType = mColl.group(2);
                                if (pValue.contains(",")) {
                                    for (String val : pValue.split("\\s*,\\s*")) {
                                        check(genericType, val, document, line, errors, cl, severity);
                                    }
                                } else {
                                    check(genericType, pValue, document, line, errors, cl, severity);
                                }
                            }
                        } else {
                            if (pValue.contains(",") && type.endsWith("[]")) {
                                for (String val : pValue.split("\\s*,\\s*")) {
                                    check(type.substring(0, type.length() - 2), val, document, line, errors, cl, severity);
                                }
                            } else {
                                if (type.endsWith("[]")) {
                                    check(type.substring(0, type.length() - 2), pValue, document, line, errors, cl, severity);
                                } else {
                                    check(type, pValue, document, line, errors, cl, severity);
                                }
                            }
                        }
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

    private ClassPath getProjectClasspath(Project prj) {
        Sources srcs = ProjectUtils.getSources(prj);
        SourceGroup[] srcGroups = srcs.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        for (SourceGroup group : srcGroups) {
            if (group.getName().toLowerCase().contains("source")) {
                return ClassPath.getClassPath(group.getRootFolder(), ClassPath.EXECUTE);
            }
        }
        return null;
    }

    private void check(String type, String text, Document document, int line, List<ErrorDescription> errors, ClassLoader cl, Severity severity) {
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
                        line
                );
                errors.add(errDesc);
            }
        } catch (IllegalArgumentException ex) {
            // problems instantiating type class, cannot decide, ignore
        }
    }

    private boolean checkType(String type, String text, ClassLoader cl) throws IllegalArgumentException {
        Class<?> clazz = ClassUtils.resolveClassName(type, cl);
        if (clazz != null) {
            try {
                Object parsed = parser.parseType(text, clazz);
            } catch (Exception e1) {
                if (clazz.isEnum()) {
                    try {
                        Object parsed = parser.parseType(text.toUpperCase(), clazz);
                    } catch (Exception e2) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}
