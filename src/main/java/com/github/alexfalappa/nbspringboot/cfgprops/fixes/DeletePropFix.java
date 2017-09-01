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
 * {@link EnhancedFix} implementation to remove a deprecated config property.
 *
 * @author Alessandro Falappa
 */
public class DeletePropFix implements EnhancedFix {

    private final StyledDocument document;
    private final int line;
    private final String bodyText;

    public DeletePropFix(StyledDocument document, int line, String bodyText) {
        this.document = document;
        this.line = line;
        this.bodyText = bodyText;
    }

    @Override
    public String getText() {
        return "Remove property";
    }

    @Override
    public CharSequence getSortText() {
        return "delete";
    }

    @Override
    public ChangeInfo implement() throws Exception {
        int start = NbDocument.findLineOffset(document, line - 1);
        document.remove(start, bodyText.length());
        StatusDisplayer.getDefault().setStatusText("Removed property: " + bodyText);
        return null;
    }

}
