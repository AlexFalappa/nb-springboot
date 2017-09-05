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
package com.github.alexfalappa.nbspringboot.cfgprops.ast;

/**
 * A {@link CfgElement} for a configuration property key.
 * <p>
 * Has also a {@code prefix} property (currently unused).
 *
 * @author Alessandro Falappa
 */
public class KeyElement extends CfgElement {

    String prefix;

    public KeyElement(int idxStart, int idxEnd, String text) {
        super(idxStart, idxEnd, text);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
