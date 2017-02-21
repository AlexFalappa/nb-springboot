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
import java.util.logging.Logger;

import javax.swing.text.JTextComponent;

import org.netbeans.modules.maven.model.pom.Dependency;
import org.netbeans.modules.maven.model.pom.DependencyContainer;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.xml.xam.Component;
import org.netbeans.modules.xml.xam.Model;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.openide.awt.StatusDisplayer;

import static java.util.logging.Level.INFO;
import static java.util.logging.Logger.getLogger;

/**
 * Base class for Maven POM code generators.
 *
 * @author Alessandro Falappa
 */
public abstract class AbstractGenerator implements CodeGenerator {

    protected final Logger logger = getLogger(getClass().getName());
    protected final JTextComponent component;
    protected final POMModel model;

    protected AbstractGenerator(POMModel model, JTextComponent component) {
        this.model = model;
        this.component = component;
    }

    protected abstract void doInvoke();

    @Override
    public final void invoke() {
        try {
            model.sync();
        } catch (IOException ex) {
            logger.log(INFO, "Error while syncing the editor document with model for pom.xml file", ex); //NOI18N
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

    public static class DependencyModelWriter implements ModelWriter {

        final JTextComponent component;
        final POMModel model;
        private final String groupId;
        private final String artifactId;

        public DependencyModelWriter(JTextComponent component, POMModel model, String groupId, String artifactId) {
            this.component = component;
            this.model = model;
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        @Override
        public int write() {
            int pos = component.getCaretPosition();
            DependencyContainer container = findContainer(pos, model);
            Dependency dep = container.findDependencyById(groupId, artifactId, null);
            if (dep == null) {
                dep = model.getFactory().createDependency();
                dep.setGroupId(groupId);
                dep.setArtifactId(artifactId);
                container.addDependency(dep);
            }
            return model.getAccess().findPosition(dep.getPeer());
        }

        private DependencyContainer findContainer(int pos, POMModel model) {
            Component dc = model.findComponent(pos);
            while (dc != null) {
                if (dc instanceof DependencyContainer) {
                    return (DependencyContainer) dc;
                }
                dc = dc.getParent();
            }
            return model.getProject();
        }
    }
}
