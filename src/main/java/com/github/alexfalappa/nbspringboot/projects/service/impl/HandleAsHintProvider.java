/*
 * Copyright 2019 Alessandro Falappa.
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
package com.github.alexfalappa.nbspringboot.projects.service.impl;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.cfgprops.completion.items.FileObjectCompletionItem;
import com.github.alexfalappa.nbspringboot.cfgprops.completion.items.ValueCompletionItem;
import com.github.alexfalappa.nbspringboot.projects.service.api.HintProvider;

/**
 * Implementation of {@link HintProvider} for 'handle-as' clauses.
 *
 * @author Alessandro Falappa
 */
public class HandleAsHintProvider implements HintProvider {

    private static final String PREFIX_CLASSPATH = "classpath:";
    private static final String PREFIX_FILE = "file://";
//    private static final Pattern PATTERN_WINDRIVES = Pattern.compile("[A-Z]:/");
    private final Set<String> resourcePrefixes = new HashSet<>();
    private ClassPath cpExec = null;
    private FileObject resourcesFolder = null;

    public HandleAsHintProvider(Project prj) {
        Sources srcs = ProjectUtils.getSources(prj);
        SourceGroup[] srcGroups = srcs.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        if (srcGroups.length > 0) {
            // the first sourcegroup is src/main/java (the second is src/test/java)
            this.cpExec = ClassPath.getClassPath(srcGroups[0].getRootFolder(), ClassPath.EXECUTE);
        }
        srcGroups = srcs.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_RESOURCES);
        if (srcGroups.length > 0) {
            // the first sourcegroup is src/main/resources (the second is src/test/resources)
            this.resourcesFolder = srcGroups[0].getRootFolder();
        }
        resourcePrefixes.add(PREFIX_CLASSPATH);
        resourcePrefixes.add(PREFIX_FILE);
        resourcePrefixes.add("http://");
        resourcePrefixes.add("https://");
    }

    @Override
    public void provide(Map<String, Object> params, ConfigurationMetadataProperty propMetadata, String filter,
            CompletionResultSet completionResultSet, int dotOffset, int caretOffset) {
        // target parameter is mandatory
        if (!params.containsKey("target")) {
            return;
        }
        if (filter == null) {
            filter = "";
        }
        String targetType = params.get("target").toString();
        switch (targetType) {
            case "org.springframework.core.io.Resource":
                if (filter.startsWith(PREFIX_CLASSPATH)) {
                    String resFilter = filter.substring(PREFIX_CLASSPATH.length());
                    int startOffset = dotOffset + PREFIX_CLASSPATH.length();
                    String filePart = resFilter;
                    FileObject foBase = resourcesFolder;
                    if (resFilter.contains("/")) {
                        final int slashIdx = resFilter.lastIndexOf('/');
                        final String basePart = resFilter.substring(0, slashIdx);
                        filePart = resFilter.substring(slashIdx + 1);
                        startOffset += slashIdx + 1;
                        foBase = resourcesFolder.getFileObject(basePart);
                    }
                    for (FileObject fObj : foBase.getChildren()) {
                        String fname = fObj.getNameExt();
                        if (fname.startsWith(filePart)) {
                            completionResultSet.addItem(new FileObjectCompletionItem(fObj, startOffset, caretOffset));
                        }
                    }
                } else if (filter.startsWith(PREFIX_FILE)) {
                    String fileFilter = filter.substring(PREFIX_FILE.length());
                    int startOffset = dotOffset + PREFIX_FILE.length();
                    if (fileFilter.isEmpty()) {
                        Iterable<Path> rootDirs = FileSystems.getDefault().getRootDirectories();
                        for (Path rootDir : rootDirs) {
                            FileObject foRoot = FileUtil.toFileObject(rootDir.toFile());
                            // filter out CD/DVD drives letter on Windows
                            if (foRoot != null) {
                                completionResultSet.addItem(new FileObjectCompletionItem(foRoot, startOffset, caretOffset));
                            }
                        }
                    } else {
                        Path pTest = Paths.get(fileFilter);
                        startOffset += fileFilter.length();
                        String filePart = "";
                        if (!Files.exists(pTest)) {
                            filePart = pTest.getFileName().toString();
                            pTest = pTest.getParent();
                            startOffset -= filePart.length();
                        }
                        FileObject foBase = FileUtil.toFileObject(pTest.toFile());
                        for (FileObject fObj : foBase.getChildren()) {
                            String fname = fObj.getNameExt();
                            if (fname.startsWith(filePart)) {
                                completionResultSet.addItem(new FileObjectCompletionItem(fObj, startOffset, caretOffset));
                            }
                        }
                    }
                } else {
                    for (String rp : resourcePrefixes) {
                        if (rp.startsWith(filter)) {
                            completionResultSet.addItem(new ValueCompletionItem(Utils.createHint(rp), dotOffset, caretOffset));
                        }
                    }
                }
                break;
            case "java.nio.charset.Charset":
                Utils.completeCharset(filter, hint -> {
                    completionResultSet.addItem(new ValueCompletionItem(hint, dotOffset, caretOffset));
                });
                break;
            default:
                // try to interpret the targetType as an enum
                Utils.completeEnum(cpExec, targetType, filter, hint -> {
                    completionResultSet.addItem(new ValueCompletionItem(hint, dotOffset, caretOffset));
                });
        }
        // TODO try to support one of the following
        /*
        Any java.lang.Enum: Lists the possible values for the property. (We recommend defining the property with the Enum type, as no further hint should be required for the IDE to auto-complete the values)
        java.nio.charset.Charset: Supports auto-completion of charset/encoding values (such as UTF-8)
        java.util.Locale: auto-completion of locales (such as en_US)
        org.springframework.util.MimeType: Supports auto-completion of content type values (such as text/plain)
        org.springframework.core.io.Resource: Supports auto-completion of Spring’s Resource abstraction to refer to a file on the filesystem or on the classpath (such as classpath:/sample.properties)
        [Tip] If multiple values can be provided, use a Collection or Array type to teach the IDE about it.
         */
    }

}
