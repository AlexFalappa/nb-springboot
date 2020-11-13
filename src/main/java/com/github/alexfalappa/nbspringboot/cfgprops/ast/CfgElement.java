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

import static java.lang.Integer.compare;

/**
 * The basic configuration element.
 * <p>
 * It is an immutable object and groups a text and the char index range of its position in the document.
 *
 * @author Alessandro Falappa
 */
public class CfgElement implements Comparable<CfgElement> {

    private final int idxStart;
    private final int idxEnd;
    private final String text;

    public CfgElement(int idxStart, int idxEnd, String text) {
        this.idxStart = idxStart;
        this.idxEnd = idxEnd;
        this.text = text;
    }

    public int getIdxStart() {
        return idxStart;
    }

    public int getIdxEnd() {
        return idxEnd;
    }

    public String getText() {
        return text;
    }

    @Override
    public int compareTo(CfgElement o) {
        int compareStarts = compare(idxStart, o.idxStart);
        return compareStarts == 0 ? compare(idxEnd, o.idxEnd) : compareStarts;
    }
}
