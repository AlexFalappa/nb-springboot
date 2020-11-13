/*
 * Copyright 2017 the original author or authors.
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
package com.github.alexfalappa.nbspringboot.cfgprops.ast;

import java.util.Set;
import java.util.TreeSet;

/**
 * Content of a configuration properties file.
 * <p>
 * Basically a set of {@link PairElement} sorted by position in text document.
 *
 * @author Alessandro Falappa
 */
public class CfgFile {

    private Set<PairElement> elements = new TreeSet<>();

    public Set<PairElement> getElements() {
        return elements;
    }

}
