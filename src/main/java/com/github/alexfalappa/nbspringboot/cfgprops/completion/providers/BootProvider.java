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
package com.github.alexfalappa.nbspringboot.cfgprops.completion.providers;

import java.util.function.Consumer;

import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ValueHint;

/**
 * Spring Boot key/value provider interface.
 *
 * @author Alessandro Falappa
 */
public interface BootProvider {

    void provide(ConfigurationMetadataProperty propMetadata, String filter, CompletionResultSet completionResultSet, int dotOffset, int caretOffset);

    void provideKeys(ConfigurationMetadataProperty propMetadata, String filter, Consumer<ValueHint> consumer);

    void provideValues(ConfigurationMetadataProperty propMetadata, String filter, Consumer<ValueHint> consumer);

}