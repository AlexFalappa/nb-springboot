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
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.Document;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.util.ClassUtils;

import com.github.alexfalappa.nbspringboot.cfgprops.parser.CfgPropsParser;
import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;
import com.github.drapostolos.typeparser.TypeParser;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.regex.Pattern.compile;

/**
 * Highlighting task for data type mismatch in configuration properties values.
 *
 * @author Alessandro Falappa
 */
public class DataTypeMismatchHighlightingTask extends ParserResultTask<CfgPropsParser.CfgPropsParserResult> {

    private static final Logger logger = Logger.getLogger(DataTypeMismatchHighlightingTask.class.getName());
    private static final String ERROR_LAYER_NAME = "boot-cfgprops-typemismatches";
    private final Pattern pOneGenTypeArg = compile("([^<>]+)<(.+)>");
    private final Pattern pTwoGenTypeArgs = compile("([^<>]+)<(.+),(.+)>");
    private final Pattern pArrayNotation = compile("(.+)\\[\\d+\\]");
    private final TypeParser parser = TypeParser.newBuilder().build();

    @Override
    public void run(CfgPropsParser.CfgPropsParserResult cfgResult, SchedulerEvent se) {
        logger.fine("Highlighting data type mismatches");
        List<ErrorDescription> errors = new ArrayList<>();
        final Map<String, SortedSet<Integer>> propsLines = cfgResult.getPropLines();
        final Document document = cfgResult.getSnapshot().getSource().getDocument(false);
        final Project prj = Utilities.actionsGlobalContext().lookup(Project.class);
        if (prj != null) {
            logger.log(FINER, "Context project: {0}", FileUtil.getFileDisplayName(prj.getProjectDirectory()));
            final SpringBootService sbs = prj.getLookup().lookup(SpringBootService.class);
            final ClassPath cp = getProjectClasspath(prj);
            if (sbs != null && cp != null) {
                final Properties parsedProps = cfgResult.getParsedProps();
                final Set<String> pNames = new TreeSet<>(parsedProps.stringPropertyNames());
                for (String pName : pNames) {
                    StringBuilder sb = new StringBuilder("Property ").append(pName).append(" -> ")
                            .append(parsedProps.getProperty(pName));
                    ConfigurationMetadataProperty cfgMeta = sbs.getPropertyMetadata(pName);
                    if (cfgMeta == null) {
                        // try to interpret array notation (strip '[index]' from pName)
                        Matcher mArrNot = pArrayNotation.matcher(pName);
                        if (mArrNot.matches()) {
                            cfgMeta = sbs.getPropertyMetadata(mArrNot.group(1));
                            sb.append("    - property with array notation");
                        } else {
                            // try to interpret map notation (see if pName starts with a set of known map props)
                            for (String mapPropertyName : sbs.getMapPropertyNames()) {
                                if (pName.startsWith(mapPropertyName)) {
                                    cfgMeta = sbs.getPropertyMetadata(mapPropertyName);
                                    sb.append("    - property with map notation");
                                    break;
                                }
                            }
                        }
                    } else {
                        sb.append("    - direct property");
                    }
                    logger.log(FINER, sb.toString());
                    if (cfgMeta != null) {
                        final String type = cfgMeta.getType();
                        final String pValue = parsedProps.getProperty(pName);
                        final Integer line = propsLines.get(pName).first();
                        final ClassLoader cl = cp.getClassLoader(true);
                        if (type.contains("<")) {
                            // maps
                            Matcher mMap = pTwoGenTypeArgs.matcher(type);
                            if (mMap.matches() && mMap.groupCount() == 3) {
                                String keyType = mMap.group(2);
                                check(keyType, pName.substring(pName.lastIndexOf('.') + 1), document, line, errors, cl);
                                String valueType = mMap.group(3);
                                check(valueType, pValue, document, line, errors, cl);
                            }
                            // collections
                            Matcher mColl = pOneGenTypeArg.matcher(type);
                            if (mColl.matches() && mColl.groupCount() == 2) {
                                String genericType = mColl.group(2);
                                if (pValue.contains(",")) {
                                    for (String val : pValue.split("\\s*,\\s*")) {
                                        check(genericType, val, document, line, errors, cl);
                                    }
                                } else {
                                    check(genericType, pValue, document, line, errors, cl);
                                }
                            }
                        } else {
                            if (pValue.contains(",") && type.endsWith("[]")) {
                                for (String val : pValue.split("\\s*,\\s*")) {
                                    check(type.substring(0, type.length() - 2), val, document, line, errors, cl);
                                }
                            } else {
                                if (type.endsWith("[]")) {
                                    check(type.substring(0, type.length() - 2), pValue, document, line, errors, cl);
                                } else {
                                    check(type, pValue, document, line, errors, cl);
                                }
                            }
                        }
                    } else {
                        logger.log(FINE, "No metadata for {0}   ", pName);
                    }
                }
            }
        }
        HintsController.setErrors(document, ERROR_LAYER_NAME, errors);
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

    @Override
    public int getPriority() {
        return 200;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    @Override
    public void cancel() {
    }

    private void check(String type, String text, Document document, int line, List<ErrorDescription> errors, ClassLoader cl) {
        // non generic types
        try {
            if (!checkType(type, text, cl)) {
                ErrorDescription errDesc = ErrorDescriptionFactory.createErrorDescription(
                        Severity.ERROR,
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
