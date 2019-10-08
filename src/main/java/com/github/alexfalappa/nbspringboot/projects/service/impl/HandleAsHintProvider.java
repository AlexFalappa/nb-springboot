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
        this.resourcesFolder=Utils.resourcesFolderForProj(prj);
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
        String filterLowcase = filter.toLowerCase();
        switch (targetType) {
            case "org.springframework.core.io.Resource":
                Utils.completeSrpingResource(resourcesFolder, filter, completionResultSet, dotOffset, caretOffset);
                break;
            case "java.nio.charset.Charset":
                Utils.completeCharset(filterLowcase, hint -> {
                    completionResultSet.addItem(new ValueCompletionItem(hint, dotOffset, caretOffset));
                });
                break;
            default:
                // try to interpret the targetType as an enum
                Utils.completeEnum(cpExec, targetType, filterLowcase, hint -> {
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
