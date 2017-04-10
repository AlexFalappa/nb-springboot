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
package com.github.alexfalappa.nbspringboot.cfgprops.completion;

import java.net.URL;

import javax.swing.Action;

import org.netbeans.spi.editor.completion.CompletionDocumentation;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;

import static com.github.alexfalappa.nbspringboot.Utils.simpleHtmlEscape;

/**
 * @author Alessandro Falappa
 */
public class ConfigValueCompletionDocumentation implements CompletionDocumentation {

    private final ConfigValueCompletionItem item;

    public ConfigValueCompletionDocumentation(ConfigValueCompletionItem item) {
        this.item = item;
    }

    @Override
    public String getText() {
        ItemHint.ValueHint valueHint = item.getHint();
        StringBuilder sb = new StringBuilder();
        // name and type
        sb.append("<b>").append(valueHint.getValue()).append("</b>");
        sb.append("<br/>").append(simpleHtmlEscape(valueHint.getDescription()));
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
