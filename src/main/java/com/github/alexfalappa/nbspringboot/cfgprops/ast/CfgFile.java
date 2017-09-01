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

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Alessandro Falappa
 */
public class CfgFile {

    private List<PairElement> elements = new LinkedList<>();

    public List<PairElement> getElements() {
        return elements;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (PairElement p : elements) {
            CfgElement e = p.getKey();
            sb.append(String.format("[%3d;%3d]", e.getIdxStart(), e.getIdxEnd())).append(" k: ").append(e.getText());
            e = p.getValue();
            if (e != null) {
                sb.append(String.format(" - [%3d;%3d]", e.getIdxStart(), e.getIdxEnd())).append(" v: ").append(e.getText());
            }
            sb.append('\n');
        }
        return sb.toString();
    }

}
