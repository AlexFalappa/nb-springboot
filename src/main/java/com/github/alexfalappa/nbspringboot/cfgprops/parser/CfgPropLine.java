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
package com.github.alexfalappa.nbspringboot.cfgprops.parser;

/**
 * Parsing element representing a key value pari of a configuration property on a line.
 *
 * @author Alessandro Falappa
 */
public class CfgPropLine {

    private final int line;
    private final String key;
    private final String value;

    public CfgPropLine(int line, String key, String value) {
        this.line = line;
        this.key = key;
        this.value = value;
    }

    public int getLine() {
        return line;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "[" + line + "] " + key + " -> " + value;
    }

    public static CfgPropLine of(int line, String key, String value) {
        return new CfgPropLine(line, key, value);
    }
}
