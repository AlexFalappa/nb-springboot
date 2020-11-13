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

/**
 * A key-value pair.
 * <p>
 * Groups two {@link CfgElement}s, one for the key and another optional one for the value.
 *
 * @author Alessandro Falappa
 */
public class PairElement implements Comparable<PairElement> {

    private final CfgElement key;
    private CfgElement value;

    public PairElement(CfgElement key) {
        this.key = key;
    }

    public PairElement(CfgElement key, CfgElement value) {
        this.key = key;
        this.value = value;
    }

    public CfgElement getKey() {
        return key;
    }

    public CfgElement getValue() {
        return value;
    }

    public void setValue(CfgElement value) {
        this.value = value;
    }

    @Override
    public int compareTo(PairElement o) {
        return key.compareTo(o.key);
    }
}
