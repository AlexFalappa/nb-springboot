/*
 * Copyright 2015 the original author or authors.
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
import org.openide.util.NbPreferences;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Hints;

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

    private static final Logger logger = Logger.getLogger(CfgPropCompletionItem.class.getName());
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
        logger.log(Level.FINER, "Accepted name completion: {0}", configurationMeta.getId());
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
            // add some useful chars depending on data type and presence of successive equal signs
            final String dataType = configurationMeta.getType();
            final boolean isSequence = dataType.contains("List") || dataType.contains("Set") || dataType.contains("[]");
            final boolean preferArray = NbPreferences.forModule(PrefConstants.class)
                    .getBoolean(PrefConstants.PREF_ARRAY_NOTATION, false);
            final boolean needEqualSign = !(overwrite && equalSignIndex >= 0);
            StringBuilder sb = new StringBuilder(getText());
            boolean continueCompletion = false;
            int goBack = 0;
            if (dataType.contains("Map")) {
                sb.append(".");
                continueCompletion = canCompleteKey();
            } else if (isSequence) {
                if (preferArray) {
                    sb.append("[]");
                    goBack = 1;
                    if (needEqualSign) {
                        sb.append("=");
                        goBack++;
                    }
                } else {
                    if (needEqualSign) {
                        sb.append("=");
                        continueCompletion = canCompleteValue();
                    }
                }
            } else if (needEqualSign) {
                sb.append("=");
                continueCompletion = canCompleteValue();
            }
            doc.insertString(propStartOffset, sb.toString(), null);
            if (goBack != 0) {
                jtc.setCaretPosition(jtc.getCaretPosition() - goBack);
            }
            // optinally close the code completion box
            if (!continueCompletion) {
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
        return null;
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

    private boolean canCompleteKey() {
        final Hints hints = configurationMeta.getHints();
        if (hints == null) {
            return false;
        }
        if (!hints.getKeyHints().isEmpty()) {
            return true;
        }
        if (!hints.getKeyProviders().isEmpty()) {
            return true;
        }
        return isCompletableType();
    }

    private boolean canCompleteValue() {
        final Hints hints = configurationMeta.getHints();
        if (hints == null) {
            return false;
        }
        if (!hints.getValueHints().isEmpty()) {
            return true;
        }
        if (!hints.getValueProviders().isEmpty()) {
            return true;
        }
        return isCompletableType();
    }

    private boolean isCompletableType() {
        final String dataType = configurationMeta.getType();
        switch (dataType) {
            case "java.lang.Boolean":
            case "java.nio.charset.Charset":
            case "java.util.Locale":
            case "org.springframework.core.io.Resource":
            case "org.springframework.util.MimeType":
            case "java.util.List<java.lang.Boolean>":
            case "java.util.Set<java.lang.Boolean>":
            case "java.util.List<org.springframework.core.io.Resource>":
            case "java.util.Set<org.springframework.core.io.Resource>":
            case "java.util.List<java.nio.charset.Charset>":
            case "java.util.Set<java.nio.charset.Charset>":
            case "java.util.List<java.util.Locale>":
            case "java.util.Set<java.util.Locale>":
                return true;
            default:
// TODO try to interpret the targetType as an enum
//                try {
//                    Object[] enumvals = cp.getClassLoader(true).loadClass(dataType).getEnumConstants();
//                    return enumvals != null;
//                } catch (ClassNotFoundException ex) {
//                    return false;
//                }
        }
        return false;
    }

}
