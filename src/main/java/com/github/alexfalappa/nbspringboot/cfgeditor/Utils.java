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
package com.github.alexfalappa.nbspringboot.cfgeditor;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * Utility methods used in the configuration editor.
 *
 * @author Alessandro Falappa
 */
public final class Utils {

    private static Pattern p = compile("(\\w+\\.)+(\\w+)");

    public Utils() {
    }

    public static String simpleHtmlEscape(String text) {
        return text.replace("<", "&lt;").replace(">", "&gt;");
    }

    public static String shortenJavaType(String type) {
        return p.matcher(type).replaceAll("$2");
    }

}
