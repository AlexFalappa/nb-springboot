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

import static java.lang.Integer.compare;

/**
 *
 * @author Alessandro Falappa
 */
public class CfgElement implements Comparable<CfgElement> {

    private int idxStart;
    private int idxEnd;
    private String text;

    public CfgElement() {
    }

    public CfgElement(int idxStart, int idxEnd, String text) {
        this.idxStart = idxStart;
        this.idxEnd = idxEnd;
        this.text = text;
    }

    public int getIdxStart() {
        return idxStart;
    }

    public void setIdxStart(int idxStart) {
        this.idxStart = idxStart;
    }

    public int getIdxEnd() {
        return idxEnd;
    }

    public void setIdxEnd(int idxEnd) {
        this.idxEnd = idxEnd;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public int compareTo(CfgElement o) {
        int compareStarts = compare(idxStart, o.idxStart);
        return compareStarts == 0 ? compare(idxEnd, o.idxEnd) : compareStarts;
    }
}
