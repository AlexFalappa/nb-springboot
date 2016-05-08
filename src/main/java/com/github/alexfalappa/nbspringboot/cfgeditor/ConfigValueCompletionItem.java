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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JToolTip;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;

import org.netbeans.api.editor.completion.Completion;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;

/**
 * The Spring Boot Configuration implementation of CompletionItem.
 *
 * It utilizes an ItemMetadata and the project classpath to render the completion item and support the documentation.
 *
 * @author Aggelos Karalias
 * @author Alessandro Falappa
 */
public class ConfigValueCompletionItem implements CompletionItem {

    private final ItemHint.ValueHint hint;
    private final ClassPath classPath;
    private static final ImageIcon fieldIcon = new ImageIcon(ImageUtilities.loadImage(
            "com/github/alexfalappa/nbspringboot/cfgeditor/springboot-logo.png"));
    private final int caretOffset;
    private final int dotOffset;

    public ConfigValueCompletionItem(ItemHint.ValueHint hint, ClassPath classPath, int dotOffset, int caretOffset) {
        this.hint = hint;
        this.classPath = classPath;
        this.dotOffset = dotOffset;
        this.caretOffset = caretOffset;
    }

    public ItemHint.ValueHint getHint() {
        return hint;
    }

    public ClassPath getClassPath() {
        return classPath;
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
            //Here we remove the characters starting at the start offset
            //and ending at the point where the caret is currently found:
            doc.remove(dotOffset, caretOffset - dotOffset);
            doc.insertString(dotOffset, getText(), null);
            //This statement will close the code completion box:
            Completion.get().hideAll();
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void processKeyEvent(KeyEvent evt) {
    }

    @Override
    public int getPreferredWidth(Graphics graphics, Font font) {
        return CompletionUtilities.getPreferredWidth(getText(), getTextRight(), graphics, font);
    }

    @Override
    public void render(Graphics g, Font defaultFont, Color defaultColor, Color backgroundColor, int width, int height, boolean selected) {
        CompletionUtilities.renderHtml(fieldIcon, getText(), getTextRight(), g, defaultFont, (selected ? UIManager.getColor(
                "List.selectionForeground") : UIManager.getColor("List.foreground")), width, height, selected);
    }

    @Override
    public CompletionTask createDocumentationTask() {
        return null;
        //        return new AsyncCompletionTask(new AsyncCompletionQuery() {
        //            @Override
        //            protected void query(CompletionResultSet completionResultSet, Document document, int i) {
        //                completionResultSet
        //                        .setDocumentation(new BootConfigurationCompletionDocumentation(ConfigValueCompletionItem.this));
        //                completionResultSet.finish();
        //            }
        //        });
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
