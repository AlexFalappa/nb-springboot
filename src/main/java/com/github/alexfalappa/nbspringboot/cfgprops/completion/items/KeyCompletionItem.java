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
package com.github.alexfalappa.nbspringboot.cfgprops.completion.items;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
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

import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.cfgprops.completion.doc.CfgPropValueCompletionDocumentation;

/**
 * The implementation of {@code CompletionItem} for Spring Boot key hints.
 * <p>
 * Uses an {@code ValueHint} to render the completion item and spawn the documentation display.
 *
 * @author Alessandro Falappa
 */
public class KeyCompletionItem implements CompletionItem {

    private static final Logger logger = Logger.getLogger(KeyCompletionItem.class.getName());
    private final ValueHint hint;
    private static final ImageIcon fieldIcon = new ImageIcon(ImageUtilities.loadImage(
            "com/github/alexfalappa/nbspringboot/cfgprops/completion/springboot-key.png"));
    private final int caretOffset;
    private final int dotOffset;
    private boolean overwrite;

    public KeyCompletionItem(ValueHint hint, int dotOffset, int caretOffset) {
        this.hint = hint;
        this.dotOffset = dotOffset;
        this.caretOffset = caretOffset;
    }

    public ValueHint getHint() {
        return hint;
    }

    public String getText() {
        return Utils.simpleHtmlEscape(hint.getValue().toString());
    }

    public String getTextRight() {
        return null;
    }

    @Override
    public void defaultAction(JTextComponent jtc) {
        logger.log(Level.FINER, "Accepted key value hint: {0}", hint.toString());
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
            // remove characters from dot then insert new text
            doc.remove(dotOffset, lenToRemove);
            if (equalSignIndex < 0 && colonIndex < 0) {
                logger.log(Level.FINER, "Adding equal sign and continuing completion");
                doc.insertString(dotOffset, hint.getValue().toString().concat("="), null);
            } else {
                logger.log(Level.FINER, "Finish completion with no added chars");
                doc.insertString(dotOffset, hint.getValue().toString(), null);
                Completion.get().hideAll();
            }
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
        return null;
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
        return hint.getValue().toString();
    }

    @Override
    public CharSequence getInsertPrefix() {
        return hint.getValue().toString();
    }

}
