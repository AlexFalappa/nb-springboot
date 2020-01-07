/*
 * Copyright 2019 the original author or authors.
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

import java.net.URL;

import javax.swing.Action;

import org.netbeans.spi.editor.completion.CompletionDocumentation;
import org.springframework.boot.configurationmetadata.ValueHint;

import static com.github.alexfalappa.nbspringboot.Utils.simpleHtmlEscape;

/**
 * The implementation of {@code CompletionItem} for configuration properties values documentation.
 *
 * @author Alessandro Falappa
 */
public class CfgPropValueCompletionDocumentation implements CompletionDocumentation {

    private final ValueHint valueHint;

    public CfgPropValueCompletionDocumentation(ValueHint valueHint) {
        this.valueHint = valueHint;
    }

    @Override
    public String getText() {
        StringBuilder sb = new StringBuilder();
        // name and type
        sb.append("<b>").append(valueHint.getValue()).append("</b>");
        final String description = valueHint.getDescription();
        if (description != null) {
            sb.append("<br/>").append(simpleHtmlEscape(description));
        }
        return sb.toString();
    }

    @Override
    public URL getURL() {
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
