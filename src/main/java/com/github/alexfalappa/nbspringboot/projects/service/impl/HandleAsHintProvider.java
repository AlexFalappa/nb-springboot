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

import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.cfgprops.completion.items.ValueCompletionItem;
import java.util.Map;

import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

import com.github.alexfalappa.nbspringboot.projects.service.api.HintProvider;
import java.util.HashSet;
import java.util.Set;
import org.netbeans.api.java.classpath.ClassPath;

/**
 * Implementation of {@link HintProvider} for 'handle-as' clauses.
 *
 * @author Alessandro Falappa
 */
public class HandleAsHintProvider implements HintProvider {

    private final Set<String> resourcePrefixes = new HashSet<>();
    private final ClassIndex classIndex;
    private final ClassPath cpExec;

    public HandleAsHintProvider(ClassIndex classIndex, ClassPath cpExec) {
        this.classIndex = classIndex;
        this.cpExec = cpExec;
        resourcePrefixes.add("classpath:");
        resourcePrefixes.add("file://");
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
                for (String rp : resourcePrefixes) {
                    if (rp.startsWith(filter)) {
                        completionResultSet.addItem(new ValueCompletionItem(Utils.createHint(rp), dotOffset, caretOffset));
                    }
                }
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
