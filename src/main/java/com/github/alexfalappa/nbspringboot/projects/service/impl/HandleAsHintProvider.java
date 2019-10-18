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

import java.util.Map;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.openide.filesystems.FileObject;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.cfgprops.completion.items.ValueCompletionItem;
import com.github.alexfalappa.nbspringboot.projects.service.api.HintProvider;

/**
 * Implementation of {@link HintProvider} for 'handle-as' clauses.
 *
 * @author Alessandro Falappa
 */
public class HandleAsHintProvider implements HintProvider {

    private ClassPath cpExec = null;
    private final FileObject resourcesFolder;

    public HandleAsHintProvider(Project prj) {
        Sources srcs = ProjectUtils.getSources(prj);
        SourceGroup[] srcGroups = srcs.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        if (srcGroups.length > 0) {
            // the first sourcegroup is src/main/java (the second is src/test/java)
            this.cpExec = ClassPath.getClassPath(srcGroups[0].getRootFolder(), ClassPath.EXECUTE);
        }
        this.resourcesFolder = Utils.resourcesFolderForProj(prj);
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
            case "java.util.List<org.springframework.core.io.Resource>":
            case "java.util.Set<org.springframework.core.io.Resource>":
            case "org.springframework.core.io.Resource":
                Utils.completeSpringResource(resourcesFolder, filter, completionResultSet, dotOffset, caretOffset);
                break;
            case "java.util.List<java.nio.charset.Charset>":
            case "java.util.Set<java.nio.charset.Charset>":
            case "java.nio.charset.Charset":
                Utils.completeCharset(filter, hint -> {
                    completionResultSet.addItem(new ValueCompletionItem(hint, dotOffset, caretOffset));
                });
                break;
            case "java.util.List<java.util.Locale>":
            case "java.util.Set<java.util.Locale>":
            case "java.util.Locale":
                Utils.completeLocale(filter, hint -> {
                    completionResultSet.addItem(new ValueCompletionItem(hint, dotOffset, caretOffset));
                });
                break;
            case "org.springframework.util.MimeType":
                Utils.completeMimetype(filter, hint -> {
                    completionResultSet.addItem(new ValueCompletionItem(hint, dotOffset, caretOffset));
                });
                break;
            default:
                // try to interpret the targetType as an enum
                Utils.completeEnum(cpExec, targetType, filter, hint -> {
                    completionResultSet.addItem(new ValueCompletionItem(hint, dotOffset, caretOffset));
                });
        }
    }

}
