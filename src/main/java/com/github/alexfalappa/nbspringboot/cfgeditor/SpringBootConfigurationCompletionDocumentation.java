/*
 * Copyright 2015 Keevosh ULP.
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

import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.netbeans.spi.editor.completion.CompletionDocumentation;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

import static com.github.alexfalappa.nbspringboot.cfgeditor.Utils.simpleHtmlEscape;

/**
 * The Spring Boot Configuration implementation of CompletionDocumentation.
 *
 * It utilizes a {@link SpringBootConfigurationCompletionItem} to display the documentation for that item and actions like opening the
 * source type of a property in editor and navigate to a general spring boot configuration documentation page.
 *
 * @author Aggelos Karalias
 * @author Alessandro Falappa
 */
public class SpringBootConfigurationCompletionDocumentation implements CompletionDocumentation {

    private final SpringBootConfigurationCompletionItem item;

    public SpringBootConfigurationCompletionDocumentation(SpringBootConfigurationCompletionItem item) {
        this.item = item;
    }

    @Override
    public String getText() {
        ItemMetadata configurationItem = item.getConfigurationItem();
        StringBuilder sb = new StringBuilder();
        // name and type
        sb.append("<b>").append(configurationItem.getName()).append("</b>");
        sb.append("<br/><a>").append(simpleHtmlEscape(configurationItem.getType())).append("</a>");
        // deprecation (optional)
        ItemDeprecation deprecation = configurationItem.getDeprecation();
        if (deprecation != null) {
            sb.append("<br/><br/><b><i>Deprecated");
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
        if (null != configurationItem.getDefaultValue()) {
            sb.append("<br/><br/><i>Default:</i> ").append(String.valueOf(configurationItem.getDefaultValue()));
        }
        // description (optional)
        final String description = configurationItem.getDescription();
        if (description != null) {
            sb.append("<br/><br/>").append(description);
        }
        // list of values (optional)
        ItemHint hint = item.getHint();
        if (hint != null) {
            List<ItemHint.ValueHint> values = hint.getValues();
            if (values != null && !values.isEmpty()) {
                sb.append("<table><tr><th>Value</th><th>Description</th></tr>");
                for (ItemHint.ValueHint vHint : values) {
                    sb.append("<tr><td>").append(vHint.getValue()).append("</td><td>");
                    sb.append(simpleHtmlEscape(vHint.getDescription())).append("</th></tr>");
                }
                sb.append("</table>");
            }
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
        String sourceType = item.getConfigurationItem().getSourceType();
        if (sourceType == null) {
            return null;
        }
        final FileObject fo = item.getClassPath().findResource(sourceType.replaceAll("\\.", "/").concat(".class"));
        if (fo == null) {
            return null;
        }
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    DataObject dataObject = DataObject.find(fo);
                    OpenCookie oc = dataObject.getLookup().lookup(OpenCookie.class);
                    if (oc != null) {
                        oc.open();
                    }
                } catch (DataObjectNotFoundException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        };
    }

}
