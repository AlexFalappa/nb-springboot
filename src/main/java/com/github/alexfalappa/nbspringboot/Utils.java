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
package com.github.alexfalappa.nbspringboot;

import java.util.regex.Pattern;

import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

import static java.util.regex.Pattern.compile;

/**
 * Utility methods used in the plugin.
 *
 * @author Alessandro Falappa
 */
public final class Utils {

    private static final Pattern p = compile("(\\w+\\.)+(\\w+)");

    // prevent instantiation
    private Utils() {
    }

    /**
     * Simplistic escape of angled brackets in the given string.
     *
     * @param text the string to escape
     * @return escaped string
     */
    public static String simpleHtmlEscape(String text) {
        return text.replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Shortens a string representing a fully qualified Java type.
     * <p>
     * Strips all package names from the type. Also acts on generic parameters.
     * <p>
     * For example {@code java.util.List<java.lang.String>} gets shortened to {@code List<String>}.
     *
     * @param type a Java type string
     * @return the shortened type
     */
    public static String shortenJavaType(String type) {
        return p.matcher(type).replaceAll("$2");
    }

    /**
     * Builds an HTML formatted string with details on a Spring Boot configuration property extracted from its {@code ItemMetadata}.
     *
     * @param cfgMeta the configuration property metadata object
     * @return the HTML formatted configuration property details
     */
    public static String cfgPropDetailsHtml(ItemMetadata cfgMeta) {
        StringBuilder sb = new StringBuilder();
        // deprecation (optional)
        ItemDeprecation deprecation = cfgMeta.getDeprecation();
        if (deprecation != null) {
            sb.append("<b>Deprecated</b>");
            // deprecation reason if present
            String reason = deprecation.getReason();
            if (reason != null) {
                sb.append(": ").append(simpleHtmlEscape(reason));
            }
            sb.append("<br/>");
            String replacement = deprecation.getReplacement();
            if (replacement != null) {
                sb.append("<i>Replaced by:</i> <tt>").append(replacement).append("</tt><br/>");
            }
        }
        // description (optional)
        final String description = cfgMeta.getDescription();
        if (description != null) {
            sb.append(description).append("<br/>");
        }
        // type
        sb.append("<tt>").append(simpleHtmlEscape(shortenJavaType(cfgMeta.getType()))).append("</tt>");
        return sb.toString();
    }

}
