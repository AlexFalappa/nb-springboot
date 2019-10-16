/*
 * Copyright 2015 Keevosh ULP.
 * Modifications copyright 2016 Alessandro Falappa.
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
import org.openide.util.NbPreferences;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

import com.github.alexfalappa.nbspringboot.PrefConstants;
import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.cfgprops.completion.doc.CfgPropCompletionDocumentation;

import static com.github.alexfalappa.nbspringboot.Utils.shortenJavaType;
import static com.github.alexfalappa.nbspringboot.Utils.simpleHtmlEscape;

/**
 * The implementation of {@code CompletionItem} for Spring Boot configuration property names.
 * <p>
 * Uses a {@code ConfigurationMetadataProperty} to render the completion item and spawn the documentation display.
 *
 * @author Aggelos Karalias
 * @author Alessandro Falappa
 */
public class CfgPropCompletionItem implements CompletionItem {

    private static final ImageIcon fieldIcon = new ImageIcon(ImageUtilities.loadImage(
            "com/github/alexfalappa/nbspringboot/cfgprops/completion/springboot-property.png"));
    private final ConfigurationMetadataProperty configurationMeta;
    private final int caretOffset;
    private final int propStartOffset;
    private boolean overwrite;
    private final String type;
    private final boolean sortDeprLast;

    public CfgPropCompletionItem(ConfigurationMetadataProperty configurationMeta, int propStartOffset, int caretOffset,
            boolean sortDeprLast) {
        this.overwrite = false;
        this.configurationMeta = configurationMeta;
        if (configurationMeta.getType() != null) {
            type = simpleHtmlEscape(shortenJavaType(configurationMeta.getType()));
        } else {
            type = null;
        }
        this.propStartOffset = propStartOffset;
        this.caretOffset = caretOffset;
        this.sortDeprLast = sortDeprLast;
    }

    public ConfigurationMetadataProperty getConfigurationMetadata() {
        return configurationMeta;
    }

    public String getText() {
        return configurationMeta.getId();
    }

    public String getTextRight() {
        return type;
    }

    @Override
    public void defaultAction(JTextComponent jtc) {
        try {
            StyledDocument doc = (StyledDocument) jtc.getDocument();
            // calculate the amount of chars to remove (by default from property start up to caret position)
            int lenToRemove = caretOffset - propStartOffset;
            int equalSignIndex = -1;
            if (overwrite) {
                // NOTE: the editor removes by itself the word at caret when ctrl + enter is pressed
                // the document state here is different from when the completion was invoked thus we have to
                // find again the offset of the equal sign in the line
                Element lineElement = doc.getParagraphElement(caretOffset);
                String line = doc.getText(lineElement.getStartOffset(), lineElement.getEndOffset() - lineElement.getStartOffset());
                equalSignIndex = line.indexOf('=');
                int colonIndex = line.indexOf(':');
                if (equalSignIndex >= 0) {
                    // from property start to equal sign
                    lenToRemove = lineElement.getStartOffset() + equalSignIndex - propStartOffset;
                } else if (colonIndex >= 0) {
                    // from property start to colon
                    lenToRemove = lineElement.getStartOffset() + colonIndex - propStartOffset;
                } else {
                    // from property start to end of line (except line terminator)
                    lenToRemove = lineElement.getEndOffset() - 1 - propStartOffset;
                }
            }
            // remove characters from the property name start offset
            doc.remove(propStartOffset, lenToRemove);
            // add a useful char depending on data type
            final String dataType = configurationMeta.getType();
            final boolean isSequence = dataType.contains("List") || dataType.contains("Set") || dataType.contains("[]");
            final boolean preferArray = NbPreferences.forModule(PrefConstants.class)
                    .getBoolean(PrefConstants.PREF_ARRAY_NOTATION, false);
            final boolean needEqualSign = !(overwrite && equalSignIndex >= 0);
            StringBuilder sb = new StringBuilder(getText());
            int goBack = 0;
            if (dataType.contains("Map")) {
                sb.append(".");
            } else if (isSequence && preferArray) {
                sb.append("[]");
                goBack = 1;
                if (needEqualSign) {
                    sb.append("=");
                    goBack++;
                }
            } else if (needEqualSign) {
                sb.append("=");
            }
            doc.insertString(propStartOffset, sb.toString(), null);
            if (goBack != 0) {
                jtc.setCaretPosition(jtc.getCaretPosition() - goBack);
            }
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
        String leftHtmlText = getText();
        if (configurationMeta.isDeprecated()) {
            leftHtmlText = "<s>" + leftHtmlText + "</s>";
        }
        final Color color = Utils.isErrorDeprecated(configurationMeta)
                ? UIManager.getColor("nb.errorForeground")
                : UIManager.getColor("List.foreground");
        CompletionUtilities.renderHtml(fieldIcon, leftHtmlText, getTextRight(), g, defaultFont, (selected ? UIManager.getColor(
                "List.selectionForeground") : color), width, height, selected);
    }

    @Override
    public CompletionTask createDocumentationTask() {
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int i) {
                completionResultSet.setDocumentation(new CfgPropCompletionDocumentation(configurationMeta));
                completionResultSet.finish();
            }
        });
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
        return (configurationMeta.isDeprecated() && sortDeprLast) ? 1 : 0;
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
