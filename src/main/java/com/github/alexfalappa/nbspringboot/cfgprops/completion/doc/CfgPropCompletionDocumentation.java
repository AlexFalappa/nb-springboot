/*
 * Copyright 2015 Keevosh ULP.
 * Modifications copyright 2016 Alessandro Falappa.
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
package com.github.alexfalappa.nbspringboot.cfgprops.completion.doc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;

import org.netbeans.spi.editor.completion.CompletionDocumentation;
import org.openide.util.Exceptions;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.boot.configurationmetadata.Hints;
import org.springframework.boot.configurationmetadata.ValueHint;

import com.github.alexfalappa.nbspringboot.Utils;

import static com.github.alexfalappa.nbspringboot.Utils.simpleHtmlEscape;

/**
 * The Spring Boot Configuration implementation of CompletionDocumentation.
 * <p>
 * It utilizes a {@link ConfigurationMetadataProperty} to display the documentation for that item and actions like opening the
 * source type of a property in editor and navigate to a general spring boot configuration documentation page.
 *
 * @author Aggelos Karalias
 * @author Alessandro Falappa
 */
public class CfgPropCompletionDocumentation implements CompletionDocumentation {

    private final ConfigurationMetadataProperty configurationMeta;

    public CfgPropCompletionDocumentation(ConfigurationMetadataProperty configurationMeta) {
        this.configurationMeta = configurationMeta;
    }

    @Override
    public String getText() {
        StringBuilder sb = new StringBuilder();
        // name
        sb.append("<b>").append(configurationMeta.getId()).append("</b>");
        // type (may be null for deprecated props)
        final String type = configurationMeta.getType();
        if (type != null) {
            sb.append("<br/>").append(simpleHtmlEscape(type));
        }
        // deprecation (optional)
        Deprecation deprecation = configurationMeta.getDeprecation();
        if (deprecation != null) {
            sb.append("<br/><br/><b><i>");
            if (Utils.isErrorDeprecated(configurationMeta)) {
                sb.append("REMOVED");
            } else {
                sb.append("Deprecated");
            }
            // deprecation reason if present
            String reason = deprecation.getReason();
            if (reason != null) {
                sb.append(":</i></b> ").append(simpleHtmlEscape(reason));
            } else {
                sb.append("</i></b>");
            }
            String replacement = deprecation.getReplacement();
            if (replacement != null) {
                sb.append("<br/><i>Replaced by:</i> <code>").append(replacement).append("</code>");
            }
        }
        // default value (optional)
        final Object defaultValue = configurationMeta.getDefaultValue();
        if (null != defaultValue) {
            sb.append("<br/><br/><i>Default:</i> ");
            if (defaultValue instanceof Object[]) {
                sb.append(Arrays.toString((Object[]) defaultValue));
            } else {
                sb.append(String.valueOf(defaultValue));
            }
        }
        // description (optional)
        final String description = configurationMeta.getDescription();
        if (description != null) {
            sb.append("<br/><br/>").append(description);
        }
        // list of values (optional)
        Hints hints = configurationMeta.getHints();
        List<ValueHint> valueHints = hints.getValueHints();
        if (valueHints != null && !valueHints.isEmpty()) {
            sb.append("<br/><br/><table><tr><td><i>Value</i></td><td><i>Description</i></td></tr>");
            for (ValueHint vHint : valueHints) {
                sb.append("<tr><td>").append(vHint.getValue()).append("</td><td>");
                final String vDesc = vHint.getDescription();
                if (vDesc != null) {
                    sb.append(simpleHtmlEscape(vDesc)).append("</th></tr>");
                }
            }
            sb.append("</table>");
        }
        return sb.toString();
    }

    @Override
    public URL getURL() {
        try {
            return new URL("http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#common-application-properties");
        } catch (MalformedURLException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    @Override
    public CompletionDocumentation resolveLink(String string) {
        return null;
    }

    @Override
    public Action getGotoSourceAction() {
        return null;
    }

}
