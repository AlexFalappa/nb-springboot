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
package com.github.alexfalappa.nbspringboot.cfgprops.fixes;

import javax.swing.text.StyledDocument;

import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.EnhancedFix;
import org.openide.awt.StatusDisplayer;
import org.openide.text.NbDocument;

/**
 * {@link EnhancedFix} implementation to replace a deprecated config property with its replacement.
 *
 * @author Alessandro Falappa
 */
public class ReplacePropFix implements BaseFix {

    private final StyledDocument document;
    private final int line;
    private final String bodyText;
    private final String replacement;

    public ReplacePropFix(StyledDocument document, int line, String bodyText, String replacement) {
        this.document = document;
        this.line = line;
        this.bodyText = bodyText;
        this.replacement = replacement;
    }

    @Override
    public String getText() {
        return String.format("Use replacement '%s'", replacement);
    }

    @Override
    public CharSequence getSortText() {
        return SORT_REPLACEMENT;
    }

    @Override
    public ChangeInfo implement() throws Exception {
        int start = NbDocument.findLineOffset(document, line - 1);
        document.remove(start, bodyText.length());
        document.insertString(start, replacement, null);
        StatusDisplayer.getDefault().setStatusText("Replaced property: " + bodyText);
        return null;
    }

}
