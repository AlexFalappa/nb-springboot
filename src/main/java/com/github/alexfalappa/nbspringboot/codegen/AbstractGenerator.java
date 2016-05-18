/*
 * Copyright 2016 Alessandro Falappa.
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

import org.netbeans.modules.xml.xam.Model;
import org.netbeans.modules.xml.xam.dom.AbstractDocumentModel;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.openide.awt.StatusDisplayer;

/**
 *
 * @author Alessandro Falappa
 */
public abstract class AbstractGenerator<T extends AbstractDocumentModel> implements CodeGenerator {

    protected final JTextComponent component;
    protected final T model;

    protected AbstractGenerator(T model, JTextComponent component) {
        this.model = model;
        this.component = component;
    }

    protected abstract void doInvoke();

    @Override
    public final void invoke() {
        try {
            model.sync();
        } catch (IOException ex) {
            Logger.getLogger(AbstractGenerator.class.getName()).log(Level.INFO,
                    "Error while syncing the editor document with model for pom.xml file", ex); //NOI18N
        }
        if (!model.getState().equals(Model.State.VALID)) {
            StatusDisplayer.getDefault().setStatusText("Cannot parse document. Unable to generate content.");
            return;
        }
        doInvoke();
    }

    protected final void writeModel(ModelWriter writer) {
        int newPos = -1;
        try {
            if (model.startTransaction()) {
                newPos = writer.write();
            }
        } finally {
            try {
                model.endTransaction();
            } catch (IllegalStateException ex) {
                StatusDisplayer.getDefault().setStatusText("Cannot write to the model: " + ex.getMessage(),
                        StatusDisplayer.IMPORTANCE_ERROR_HIGHLIGHT);
            }
        }
        if (newPos != -1) {
            component.setCaretPosition(newPos);
        }

    }

    public static interface ModelWriter {

        int write();
    }

}
