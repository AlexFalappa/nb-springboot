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

import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

import com.github.alexfalappa.nbspringboot.projects.service.api.HintProvider;

/**
 * Implementation of {@link HintProvider} for 'handle as' clauses.
 *
 * @author Alessandro Falappa
 */
public class HandleAsHintProvider implements HintProvider {

    private final ClassIndex classIndex;

    public HandleAsHintProvider(ClassIndex classIndex) {
        this.classIndex = classIndex;
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
