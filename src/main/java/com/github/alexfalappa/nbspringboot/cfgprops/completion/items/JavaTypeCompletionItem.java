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

import javax.lang.model.element.ElementKind;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;

import org.netbeans.api.editor.completion.Completion;
import org.netbeans.api.java.source.ui.ElementIcons;
import org.netbeans.editor.BaseDocument;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.util.Exceptions;

/**
 * The implementation of {@code CompletionItem} representing Java packages or types.
 * <p>
 * Used in logger name class reference and handle-as provider results. The icon is based on the provided {@code ElementKind}.
 *
 * @author Alessandro Falappa
 */
public class JavaTypeCompletionItem implements CompletionItem {

    private static final Logger logger = Logger.getLogger(JavaTypeCompletionItem.class.getName());
    private final String name;
    private final ElementKind elementKind;
    private final int dotOffset;
    private final int caretOffset;
    private boolean overwrite;
    private boolean isKeyCompletion;

    public JavaTypeCompletionItem(String name, ElementKind elementKind, int dotOffset, int caretOffset) {
        this(name, elementKind, dotOffset, caretOffset, false);
    }

    public JavaTypeCompletionItem(String name, ElementKind elementKind, int dotOffset, int caretOffset, boolean isKeyCompletion) {
        this.name = name;
        this.dotOffset = dotOffset;
        this.caretOffset = caretOffset;
        this.elementKind = elementKind;
        this.isKeyCompletion = isKeyCompletion;
    }

    public String getText() {
        return name;
    }

    public String getTextRight() {
        return null;
    }

    @Override
    public void defaultAction(JTextComponent jtc) {
        logger.log(Level.FINER, "Accepted java type completion: {0} {1}", new Object[]{elementKind.toString(), name});
        try {
            StyledDocument doc = (StyledDocument) jtc.getDocument();
            // calculate the amount of chars to remove (by default from dot up to caret position)
            int lenToRemove = caretOffset - dotOffset;
            if (overwrite) {
                // NOTE: the editor removes by itself the word at caret when ctrl + enter is pressed
                // the document state here is different from when the completion was invoked thus we have to
                // find again the offset of the equal sign in the line
                Element lineElement = doc.getParagraphElement(caretOffset);
                String line = doc.getText(lineElement.getStartOffset(), lineElement.getEndOffset() - lineElement.getStartOffset());
                int equalSignIndex = line.indexOf('=');
                int colonIndex = line.indexOf(':');
                int commaIndex = line.indexOf(',', dotOffset - lineElement.getStartOffset());
                if (equalSignIndex >= 0 && dotOffset < equalSignIndex) {
                    // from dot to equal sign
                    lenToRemove = lineElement.getStartOffset() + equalSignIndex - dotOffset;
                } else if (colonIndex >= 0 && dotOffset < colonIndex) {
                    // from dot to colon
                    lenToRemove = lineElement.getStartOffset() + colonIndex - dotOffset;
                } else if (commaIndex >= 0) {
                    // from dot to comma
                    lenToRemove = lineElement.getStartOffset() + commaIndex - dotOffset;
                } else {
                    // from dot to end of line (except line terminator)
                    lenToRemove = lineElement.getEndOffset() - 1 - dotOffset;
                }
            }
            // remove characters from dot then insert new text
            doc.remove(dotOffset, lenToRemove);
            if (isKeyCompletion) {
                // insert and continue completion
                doc.insertString(dotOffset, name.concat("="), null);
            } else {
                // insert and close the code completion box
                doc.insertString(dotOffset, name, null);
                Completion.get().hideAll();
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void processKeyEvent(KeyEvent evt) {
        if (evt.getID() == KeyEvent.KEY_TYPED && evt.getKeyChar() == '.' && elementKind == ElementKind.PACKAGE) {
            logger.log(Level.FINER, "Accepting package ''{0}'' and continuing completion", name);
            Completion.get().hideDocumentation();
            Completion.get().hideCompletion();
            JTextComponent tc = (JTextComponent) evt.getSource();
            BaseDocument doc = (BaseDocument) tc.getDocument();
            doc.runAtomic(new Runnable() {
                @Override
                public void run() {
                    try {
                        doc.remove(dotOffset, caretOffset - dotOffset);
                        doc.insertString(dotOffset, name.concat("."), null);
                    } catch (BadLocationException ble) {
                        //ignore
                    }
                }
            });
            Completion.get().showCompletion();
            evt.consume();
        }
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
        final Color color = selected ? UIManager.getColor("List.selectionForeground") : UIManager.getColor("List.foreground");
        CompletionUtilities.renderHtml((ImageIcon) ElementIcons.getElementIcon(elementKind, null), getText(), getTextRight(),
                g, defaultFont, color, width, height, selected);
    }

    @Override
    public CompletionTask createDocumentationTask() {
        return null;
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
        return elementKind == ElementKind.PACKAGE ? 1 : 2;
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
