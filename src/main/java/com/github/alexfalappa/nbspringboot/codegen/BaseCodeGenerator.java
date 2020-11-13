/*
 * Copyright 2020 the original author or authors.
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
package com.github.alexfalappa.nbspringboot.codegen;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.JTextComponent;

import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.xml.xam.Model;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;

/**
 * Base class for code generators that modify a POM file.
 *
 * @author Alessandro Falappa
 */
public abstract class BaseCodeGenerator implements CodeGenerator {

    protected Logger logger = Logger.getLogger(getClass().getName());
    private final JTextComponent component;
    private final POMModel model;

    public BaseCodeGenerator(POMModel model, JTextComponent component) {
        this.model = model;
        this.component = component;
    }

    @Override
    public void invoke() {
        try {
            model.sync();
        } catch (IOException ex) {
            logger.log(Level.INFO, "Error while syncing the editor document with model for pom.xml file", ex); //NOI18N
        }
        if (!model.getState().equals(Model.State.VALID)) {
            StatusDisplayer.getDefault().setStatusText("Cannot parse document. Unable to generate content.");
            return;
        }
        int newCaretPos = -1;
        try {
            if (model.startTransaction()) {
                newCaretPos = pomInvoke(model, component.getCaretPosition());
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            try {
                model.endTransaction();
            } catch (IllegalStateException ex) {
                StatusDisplayer.getDefault()
                        .setStatusText("Cannot write to the model: " + ex.getMessage(), StatusDisplayer.IMPORTANCE_ERROR_HIGHLIGHT);
            }
        }
        try {
            component.setCaretPosition(newCaretPos);
        } catch (IllegalArgumentException ex) {
            // ignored, will not set new caretPosition
        }
    }

    /**
     * Make code generation into the POM, via its model.
     * <p>
     * Use {@code StatusDisplayer} to show notifications on status bar.
     *
     * @param model the POM model
     * @param caretPosition the caret position where code generation was invoked
     * @return new caret position or -1 if not important
     * @throws Exception in case of problems, will be reported in dialog
     */
    protected abstract int pomInvoke(POMModel model, int caretPosition) throws Exception;

}
