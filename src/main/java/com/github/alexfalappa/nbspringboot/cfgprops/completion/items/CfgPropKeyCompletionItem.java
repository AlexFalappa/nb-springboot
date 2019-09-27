/*
 * Copyright 2019 Alessandro Falappa.
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
package com.github.alexfalappa.nbspringboot.cfgprops.completion.items;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JToolTip;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;

import org.netbeans.api.editor.completion.Completion;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.springframework.boot.configurationmetadata.ValueHint;

import com.github.alexfalappa.nbspringboot.cfgprops.completion.doc.CfgPropValueCompletionDocumentation;

/**
 * The Spring Boot configuration property key implementation of {@code CompletionItem}.
 * <p>
 * It utilizes an {@code ValueHint} to render the completion item and spawn the documentation display.
 *
 * @author Alessandro Falappa
 */
public class CfgPropKeyCompletionItem implements CompletionItem {

    private final ValueHint hint;
    private static final ImageIcon fieldIcon = new ImageIcon(ImageUtilities.loadImage(
            "com/github/alexfalappa/nbspringboot/cfgprops/completion/springboot-key.png"));
    private final int caretOffset;
    private final int dotOffset;
    private boolean overwrite;

    public CfgPropKeyCompletionItem(ValueHint hint, int dotOffset, int caretOffset) {
        this.hint = hint;
        this.dotOffset = dotOffset;
        this.caretOffset = caretOffset;
    }

    public ValueHint getHint() {
        return hint;
    }

    public String getText() {
        return hint.getValue().toString();
    }

    public String getTextRight() {
        return null;
    }

    @Override
    public void defaultAction(JTextComponent jtc) {
        try {
            StyledDocument doc = (StyledDocument) jtc.getDocument();
            // calculate the amount of chars to remove (by default from dot up to caret position)
            int lenToRemove = caretOffset - dotOffset;
            int equalSignIndex = -1;
            int colonIndex = -1;
            if (overwrite) {
                // NOTE: the editor removes by itself the word at caret when ctrl + enter is pressed
                // the document state here is different from when the completion was invoked thus we have to
                // find again the offset of the equal sign in the line
                Element lineElement = doc.getParagraphElement(caretOffset);
                String line = doc.getText(lineElement.getStartOffset(), lineElement.getEndOffset() - lineElement.getStartOffset());
                equalSignIndex = line.indexOf('=');
                colonIndex = line.indexOf(':');
                if (equalSignIndex >= 0) {
                    // from dot to equal sign
                    lenToRemove = lineElement.getStartOffset() + equalSignIndex - dotOffset;
                } else if (colonIndex >= 0) {
                    // from dot to colon
                    lenToRemove = lineElement.getStartOffset() + colonIndex - dotOffset;
                }
            }
            // add a final equal sign if not already detected
            StringBuilder sb = new StringBuilder(getText());
            if (equalSignIndex < 0 && colonIndex < 0) {
                sb.append("=");
            }
            // remove characters from dot then insert new text
            doc.remove(dotOffset, lenToRemove);
            doc.insertString(dotOffset, sb.toString(), null);
            // close the code completion box
            Completion.get().hideAll();
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void processKeyEvent(KeyEvent evt) {
        // detect if Ctrl + Enter is pressed
        overwrite = evt.getKeyCode() == KeyEvent.VK_ENTER && (evt.getModifiers() & KeyEvent.CTRL_MASK) != 0;
    }

    @Override
    public int getPreferredWidth(Graphics graphics, Font font) {
        return CompletionUtilities.getPreferredWidth(getText(), getTextRight(), graphics, font);
    }

    @Override
    public void render(Graphics g, Font defaultFont, Color defaultColor, Color backgroundColor, int width, int height,
            boolean selected) {
        CompletionUtilities.renderHtml(fieldIcon, getText(), getTextRight(), g, defaultFont, (selected ? UIManager.getColor(
                "List.selectionForeground") : UIManager.getColor("List.foreground")), width, height, selected);
    }

    @Override
    public CompletionTask createDocumentationTask() {
        if (hint.getDescription() != null) {
            return new AsyncCompletionTask(new AsyncCompletionQuery() {
                @Override
                protected void query(CompletionResultSet completionResultSet, Document document, int i) {
                    completionResultSet.setDocumentation(new CfgPropValueCompletionDocumentation(hint));
                    completionResultSet.finish();
                }
            });
        } else {
            return null;
        }
    }

    @Override
    public CompletionTask createToolTipTask() {
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int i) {
                JToolTip toolTip = new JToolTip();
                toolTip.setTipText("Press Enter to insert \"" + getText() + "\"");
                completionResultSet.setToolTip(toolTip);
                completionResultSet.finish();
            }
        });
    }

    @Override
    public boolean instantSubstitution(JTextComponent component) {
        return false;
    }

    @Override
    public int getSortPriority() {
        return 0;
    }

    @Override
    public CharSequence getSortText() {
        return getText();
    }

    @Override
    public CharSequence getInsertPrefix() {
        return getText();
    }

}
