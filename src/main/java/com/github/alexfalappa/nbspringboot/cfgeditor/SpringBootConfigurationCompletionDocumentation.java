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
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.spi.editor.completion.CompletionDocumentation;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

/**
 * The Spring Boot Configuration implementation of CompletionDocumentation.
 *
 * It utilizes a {@link SpringBootConfigurationCompletionItem} to display
 * the documentation for that item and actions like opening the source type of
 * a property in editor and navigate to a general spring boot configuration
 * documentation page.
 *
 * @author Aggelos Karalias &lt;aggelos.karalias at gmail.com&gt;
 */
public class SpringBootConfigurationCompletionDocumentation implements CompletionDocumentation {

    private final SpringBootConfigurationCompletionItem item;

    public SpringBootConfigurationCompletionDocumentation(SpringBootConfigurationCompletionItem item) {
        this.item = item;
    }

    @Override
    public String getText() {
        ItemMetadata configurationItem = item.getConfigurationItem();

        String deprecatedText = configurationItem.isDeprecated() ? ("<br/><br/><b>Deprecated</b>") : "";
        String defaultValueText = (null != configurationItem.getDefaultValue()) ? ("<br/><i>Default Value:</i> " + String.valueOf(configurationItem.getDefaultValue())) : "";
        String descriptionText = (null != configurationItem.getDescription()) ? ("<br/><br/>" + configurationItem.getDescription()) : "";
        String text = "<b>" + configurationItem.getName() + "</b>"
                + "<br/><a>" + configurationItem.getType() + "</a>"
                + defaultValueText
                + deprecatedText
                + descriptionText;

        return text;
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
        if(null == sourceType) {
            return null;
        }

        final FileObject fo = item.getClassPath().findResource(sourceType.replaceAll("\\.", "/").concat(".class"));
        if(null == fo) {
            return null;
        }

        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    DataObject dataObject;
                    dataObject = DataObject.find(fo);
                    OpenCookie oc = dataObject.getLookup().lookup(org.openide.cookies.OpenCookie.class);
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
