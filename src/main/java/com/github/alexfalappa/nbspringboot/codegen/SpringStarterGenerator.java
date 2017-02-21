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
package com.github.alexfalappa.nbspringboot.codegen;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.JTextComponent;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.maven.api.Constants;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;

import com.github.alexfalappa.nbspringboot.projects.initializr.InitializrService;

public class SpringStarterGenerator extends AbstractGenerator {

    public SpringStarterGenerator(POMModel model, JTextComponent component) {
        super(model, component);
    }

    @MimeRegistration(mimeType = Constants.POM_MIME_TYPE, service = CodeGenerator.Factory.class, position = 1025)
    public static class Factory implements CodeGenerator.Factory {

        @Override
        public List<? extends CodeGenerator> create(Lookup context) {
            ArrayList<CodeGenerator> toRet = new ArrayList<>();
            POMModel model = context.lookup(POMModel.class);
            if (model != null) {
                JTextComponent component = context.lookup(JTextComponent.class);
                toRet.add(new SpringStarterGenerator(model, component));
            }
            return toRet;
        }
    }

    @Override
    protected void doInvoke() {
        // retrieve boot version from parent declaration
        try {
            String bootVersion = model.getProject().getPomParent().getVersion();
            final Frame mainWindow = WindowManager.getDefault().getMainWindow();
            SpringDependencyDialog sdd = new SpringDependencyDialog(mainWindow);
            sdd.init(InitializrService.getInstance().getMetadata(), bootVersion);
            sdd.setLocationRelativeTo(mainWindow);
            sdd.setVisible(true);
            if (sdd.getReturnStatus() == SpringDependencyDialog.RET_OK) {
                System.out.println("Selected dependencies:");
                System.out.println(sdd.getSelectedDeps());
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
//        writeModel(new DependencyModelWriter(component, model, "af.falappa.test", val));
    }

    @Override
    public String getDisplayName() {
        return "Spring Dependencies...";
    }

}
