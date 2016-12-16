/*
 * Copyright 2016 Alessandro Falappa.
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
package com.github.alexfalappa.nbspringboot.api;

import org.netbeans.spi.editor.completion.CompletionResultSet;

/**
 * Service API for implementing support of Spring Boot related functionalities.
 * <p>
 * configuration properties metadata.
 *
 * @author Alessandro Falappa
 */
public interface SpringBootService {

    void init();

    boolean cfgPropsCompletionEnabled();

    /**
     * Create a completion result list of properties values based on a property name, filter string, classpath and document offsets.
     *
     * @param completionResultSet
     * @param propPrefix
     * @param valPrefix
     * @param i
     * @param caretOffset
     */
    void completePropValue(CompletionResultSet completionResultSet, String propPrefix, String valPrefix, int i, int caretOffset);

    /**
     * Create a completion result list of config properties based on a filter string, classpath and document offsets.
     *
     * @param completionResultSet
     * @param propPrefix
     * @param i
     * @param caretOffset
     */
    void completePropName(CompletionResultSet completionResultSet, String propPrefix, int i, int caretOffset);
}
