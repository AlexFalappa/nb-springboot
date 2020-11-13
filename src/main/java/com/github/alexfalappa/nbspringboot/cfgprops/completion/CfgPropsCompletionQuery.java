/*
 * Copyright 2019 the original author or authors.
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
package com.github.alexfalappa.nbspringboot.cfgprops.completion;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.Project;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Hints;
import org.springframework.boot.configurationmetadata.ValueHint;
import org.springframework.boot.configurationmetadata.ValueProvider;

import com.github.alexfalappa.nbspringboot.PrefConstants;
import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.cfgprops.completion.items.CfgPropCompletionItem;
import com.github.alexfalappa.nbspringboot.cfgprops.completion.items.KeyCompletionItem;
import com.github.alexfalappa.nbspringboot.cfgprops.completion.items.ValueCompletionItem;
import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;
import com.github.alexfalappa.nbspringboot.projects.service.impl.HintSupport;

import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_DEPR_ERROR_SHOW;
import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_DEPR_SORT_LAST;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;

/**
 * Completion query for normal (i.e. Ctrl+Space) completion used in {@link CfgPropsCompletionProvider}.
 *
 * @author Alessandro Falappa
 */
public class CfgPropsCompletionQuery extends AsyncCompletionQuery {

    private static final Logger logger = Logger.getLogger(CfgPropsCompletionQuery.class.getName());
    private static final Pattern PATTERN_PROP_NAME = Pattern.compile("[^=\\s]+");
    private static final Pattern PATTERN_MAPKEY_DATATYPE = Pattern.compile("java.util.Map<([^,]+),.*>");
    private static final Pattern PATTERN_MAPVALUE_DATATYPE = Pattern.compile("java.util.Map<.*,(.*)>");
    private static final Pattern PATTERN_NUMBER_UNIT = Pattern.compile("\\d+(\\w*)");
    private static final Map<String, String> DURATION_SUFFIXES = new HashMap<>();
    private static final Map<String, String> DATASIZE_SUFFIXES = new HashMap<>();
    private final SpringBootService sbs;
    private final Project proj;
    private final FileObject resourcesFolder;

    static {
        DURATION_SUFFIXES.put("ns", "nanoseconds");
        DURATION_SUFFIXES.put("us", "microseconds");
        DURATION_SUFFIXES.put("ms", "milliseconds");
        DURATION_SUFFIXES.put("s", "seconds");
        DURATION_SUFFIXES.put("m", "minutes");
        DURATION_SUFFIXES.put("h", "hours");
        DURATION_SUFFIXES.put("d", "days");
        DATASIZE_SUFFIXES.put("B", "bytes");
        DATASIZE_SUFFIXES.put("KB", "kilobytes");
        DATASIZE_SUFFIXES.put("MB", "megabytes");
        DATASIZE_SUFFIXES.put("GB", "gigabytes");
        DATASIZE_SUFFIXES.put("TB", "terabytes");
    }

    public CfgPropsCompletionQuery(SpringBootService sbs, Project proj) {
        this.sbs = Objects.requireNonNull(sbs);
        this.proj = proj;
        this.resourcesFolder = Utils.resourcesFolderForProj(proj);
    }

    @Override
    protected void query(CompletionResultSet completionResultSet, Document document, int caretOffset) {
        logger.finer("Starting completion");
        final StyledDocument styDoc = (StyledDocument) document;
        Element lineElement = styDoc.getParagraphElement(caretOffset);
        int lineStartOffset = lineElement.getStartOffset();
        try {
            String lineToCaret = styDoc.getText(lineStartOffset, caretOffset - lineStartOffset);
            if (!lineToCaret.contains("#")) {
                String[] parts = lineToCaret.split("=");
                //property name extraction from part before =
                Matcher matcher = PATTERN_PROP_NAME.matcher(parts[0]);
                String propPrefix = null;
                int propPrefixOffset = 0;
                while (matcher.find()) {
                    propPrefix = matcher.group();
                    propPrefixOffset = matcher.start();
                }
                // check which kind of completion
                final int equalSignOffset = lineToCaret.indexOf('=');
                if (parts.length > 1) {
                    //value completion
                    String valPrefix = parts[1].trim();
                    completePropValue(completionResultSet, propPrefix, valPrefix, lineStartOffset
                            + lineToCaret.indexOf(valPrefix, equalSignOffset), caretOffset);
                } else if (equalSignOffset >= 0) {
                    //value completion with empty filter
                    completePropValue(completionResultSet, propPrefix, "", lineStartOffset + equalSignOffset + 1, caretOffset);
                } else {
                    // property completion
                    completePropName(completionResultSet, propPrefix, lineStartOffset + propPrefixOffset, caretOffset);
                }
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        completionResultSet.finish();
    }

    // Create a completion result list of config properties based on a filter string and document offsets.
    private void completePropName(CompletionResultSet completionResultSet, String filter, int startOffset, int caretOffset) {
        final Preferences prefs = NbPreferences.forModule(PrefConstants.class);
        final boolean bDeprLast = prefs.getBoolean(PREF_DEPR_SORT_LAST, true);
        final boolean bErrorShow = prefs.getBoolean(PREF_DEPR_ERROR_SHOW, true);
        long mark = System.currentTimeMillis();
        // check if completing a property map key
        if (filter != null) {
            ClassPath cpExec = Utils.execClasspathForProj(proj);
            for (String mapProp : sbs.getMapPropertyNames()) {
                if (filter.length() > mapProp.length() && filter.contains(mapProp)) {
                    String key = filter.substring(mapProp.length() + 1);
                    logger.log(FINER, "Completing key for map property {0} from: ''{1}''", new Object[]{mapProp, key});
                    final ConfigurationMetadataProperty propMetadata = sbs.getPropertyMetadata(mapProp);
                    // if key data type is an enum complete with enum values
                    final String keyDataType = extractMapKeyType(propMetadata);
                    if (!keyDataType.contains("<")) {
                        Utils.completeEnum(cpExec, keyDataType, key, hint -> {
                            completionResultSet.addItem(new KeyCompletionItem(hint, startOffset + mapProp.length() + 1,
                                    caretOffset));
                        });
                    }
                    // check if key data type is boolean
                    if (keyDataType.equals("java.lang.Boolean")) {
                        Utils.completeBoolean(key, hint -> {
                            completionResultSet.addItem(new KeyCompletionItem(hint, startOffset + mapProp.length() + 1,
                                    caretOffset));
                        });
                    }
                    // check if key data type is Charset
                    if (keyDataType.equals("java.nio.charset.Charset")) {
                        Utils.completeCharset(key, hint -> {
                            completionResultSet.addItem(new KeyCompletionItem(hint, startOffset + mapProp.length() + 1,
                                    caretOffset));
                        });
                    }
                    // add metadata defined key hints to completion list
                    final Hints hints = propMetadata.getHints();
                    if (!hints.getKeyHints().isEmpty()) {
                        String keyLowcase = key.toLowerCase();
                        for (ValueHint keyHint : hints.getKeyHints()) {
                            if (keyHint.getValue().toString().toLowerCase().contains(keyLowcase)) {
                                completionResultSet.addItem(new KeyCompletionItem(keyHint, startOffset + mapProp.length() + 1,
                                        caretOffset));
                            }
                        }
                    }
                    // invoke key providers
                    if (!hints.getKeyProviders().isEmpty()) {
                        logger.log(FINER, "Key providers for {0}:", mapProp);
                        for (ValueProvider vp : hints.getKeyProviders()) {
                            logger.log(FINER, "  {0} - params: {1}", new Object[]{vp.getName(), vp.getParameters()});
                            sbs.getHintProvider(vp.getName()).provide(vp.getParameters(), propMetadata, key, true,
                                    completionResultSet, startOffset + mapProp.length() + 1, caretOffset);
                        }
                    }
                }
            }
        }
        for (ConfigurationMetadataProperty propMeta : sbs.queryPropertyMetadata(filter)) {
            if (Utils.isErrorDeprecated(propMeta)) {
                // show error level deprecated props based on pref
                if (bErrorShow) {
                    completionResultSet.addItem(new CfgPropCompletionItem(propMeta, startOffset, caretOffset, bDeprLast));
                }
            } else {
                completionResultSet.addItem(new CfgPropCompletionItem(propMeta, startOffset, caretOffset, bDeprLast));
            }
        }
        final long elapsed = System.currentTimeMillis() - mark;
        logger.log(FINE, "Name completion of ''{0}'' took: {1} msecs", new Object[]{filter, elapsed});
    }

    // Create a completion result list of properties values based on a property name, filter string and document offsets.
    public void completePropValue(CompletionResultSet completionResultSet, String propName, String filter, int startOffset,
            int caretOffset) {
        long mark = System.currentTimeMillis();
        String filterLowcase = filter.toLowerCase();
        logger.log(FINER, "Completing property value from: ''{0}''", filter);
        ConfigurationMetadataProperty propMeta = sbs.getPropertyMetadata(propName);
        if (propMeta != null) {
            final String propType = propMeta.getType();
            final String mapValueType = extractMapValueType(propMeta);
            // if data type is collection or array adjust filter and startOffset to part after last comma
            if (propType.contains("List<") || propType.contains("Set<") || propType.contains("[]")) {
                int idx = filter.lastIndexOf(',');
                if (idx > 0) {
                    startOffset = startOffset + idx + 1;
                    filter = filter.substring(idx + 1);
                    filterLowcase = filter.toLowerCase();
                }
            }
            // check if data type or map value type is boolean
            if (propType.equals("java.lang.Boolean") || mapValueType.equals("java.lang.Boolean")) {
                if ("true".contains(filterLowcase)) {
                    completionResultSet.addItem(new ValueCompletionItem(Utils.createHint("true"), startOffset, caretOffset));
                }
                if ("false".contains(filterLowcase)) {
                    completionResultSet.addItem(new ValueCompletionItem(Utils.createHint("false"), startOffset, caretOffset));
                }
            }
            // check if data type or map value type is CharSet
            if (propType.equals("java.nio.charset.Charset") || mapValueType.equals("java.nio.charset.Charset")) {
                for (String chrsName : HintSupport.getAllCharsets()) {
                    if (chrsName.toLowerCase().contains(filterLowcase)) {
                        completionResultSet.addItem(new ValueCompletionItem(Utils.createHint(chrsName), startOffset, caretOffset));
                    }
                }
            }
            // check if data type or map value type is Locale
            if (propType.equals("java.util.Locale") || mapValueType.equals("java.util.Locale")) {
                for (String lclName : HintSupport.getAllLocales()) {
                    if (lclName.toLowerCase().contains(filterLowcase)) {
                        completionResultSet.addItem(new ValueCompletionItem(Utils.createHint(lclName), startOffset, caretOffset));
                    }
                }
            }
            // check if data type or map value type is MimeType
            if (propType.equals("org.springframework.util.MimeType") || mapValueType.equals("org.springframework.util.MimeType")) {
                for (String mime : HintSupport.MIMETYPES) {
                    if (mime.toLowerCase().contains(filterLowcase)) {
                        completionResultSet.addItem(new ValueCompletionItem(Utils.createHint(mime), startOffset, caretOffset));
                    }
                }
            }
            // check if data type or map value type is a Spring Resource
            if (propType.equals("org.springframework.core.io.Resource")
                    || mapValueType.equals("org.springframework.core.io.Resource")) {
                Utils.completeSpringResource(resourcesFolder, filter, completionResultSet, startOffset, caretOffset);
            }
            // check if data type is an enum
            completeValueEnum(propType, filterLowcase, completionResultSet, startOffset, caretOffset);
            // check if map value data type is an enum (not for collections)
            if (!mapValueType.contains("<")) {
                completeValueEnum(mapValueType, filterLowcase, completionResultSet, startOffset, caretOffset);
            }
            // check if filter is a number with unit
            Matcher m = PATTERN_NUMBER_UNIT.matcher(filter);
            if (m.matches()) {
                String unitPart = m.group(1).toLowerCase();
                final int newStartOffset = startOffset + filter.length() - unitPart.length();
                // if data type is java.time.Duration offer simple form suffixes
                if (propType.equals("java.time.Duration")) {
                    for (Map.Entry<String, String> entry : DURATION_SUFFIXES.entrySet()) {
                        if (entry.getKey().toLowerCase().startsWith(unitPart)) {
                            completionResultSet.addItem(new ValueCompletionItem(
                                    Utils.createHint(entry.getKey(), entry.getValue()), newStartOffset, caretOffset));
                        }
                    }
                }
                // if data type is org.springframework.util.unit.DataSize offer size suffixes
                if (propType.equals("org.springframework.util.unit.DataSize")) {
                    for (Map.Entry<String, String> entry : DATASIZE_SUFFIXES.entrySet()) {
                        if (entry.getKey().toLowerCase().startsWith(unitPart)) {
                            completionResultSet.addItem(new ValueCompletionItem(
                                    Utils.createHint(entry.getKey(), entry.getValue()), newStartOffset, caretOffset));
                        }
                    }
                }
            }
            // add metadata defined value hints to completion list
            final Hints hints = propMeta.getHints();
            for (ValueHint valueHint : hints.getValueHints()) {
                if (valueHint.getValue().toString().toLowerCase().contains(filterLowcase)) {
                    completionResultSet.addItem(new ValueCompletionItem(valueHint, startOffset, caretOffset));
                }
            }
            // invoke value providers
            if (!hints.getValueProviders().isEmpty()) {
                logger.log(FINER, "Value providers for {0}:", propName);
                for (ValueProvider vp : hints.getValueProviders()) {
                    logger.log(FINER, "  {0} - params: {1}", new Object[]{vp.getName(), vp.getParameters()});
                    sbs.getHintProvider(vp.getName()).provide(vp.getParameters(), propMeta, filter, false,
                            completionResultSet, startOffset, caretOffset);
                }
            }
        }
        final long elapsed = System.currentTimeMillis() - mark;
        logger.log(FINE, "Value completion of ''{0}'' on ''{1}'' took: {2} msecs", new Object[]{filter, propName, elapsed});
    }

    private void completeValueEnum(String dataType, String filter, CompletionResultSet completionResultSet, int startOffset,
            int caretOffset) {
        try {
            ClassPath cpExec = Utils.execClasspathForProj(proj);
            Object[] enumvals = cpExec.getClassLoader(true).loadClass(dataType).getEnumConstants();
            if (enumvals != null) {
                for (Object val : enumvals) {
                    final String valName = val.toString().toLowerCase();
                    if (valName.contains(filter)) {
                        completionResultSet.addItem(new ValueCompletionItem(Utils.createEnumHint(valName), startOffset,
                                caretOffset));
                    }
                }
            }
        } catch (ClassNotFoundException ex) {
            // enum not available in project classpath, no completion possible
        }
    }

    private String extractMapKeyType(ConfigurationMetadataProperty propMeta) {
        Matcher matcher = PATTERN_MAPKEY_DATATYPE.matcher(propMeta.getType());
        if (matcher.matches()) {
            final String dataType = matcher.group(1);
            logger.log(FINER, "Map key data type: {0}", dataType);
            return dataType;
        }
        return "";
    }

    private String extractMapValueType(ConfigurationMetadataProperty propMeta) {
        Matcher matcher = PATTERN_MAPVALUE_DATATYPE.matcher(propMeta.getType());
        if (matcher.matches()) {
            final String dataType = matcher.group(1);
            logger.log(FINER, "Map value data type: {0}", dataType);
            return dataType;
        }
        return "";
    }

}
