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

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.JTextComponent;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.modules.maven.api.Constants;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

public class ActuatorGenerator extends AbstractGenerator {

    private final String GROUP_ID = "org.springframework.boot";
    private final String ARTIFACT_ID = "spring-boot-actuator";

    private ActuatorGenerator(POMModel model, JTextComponent component) {
        super(model, component);
    }

    @MimeRegistration(mimeType = Constants.POM_MIME_TYPE, service = CodeGenerator.Factory.class, position = 1000)
    public static class Factory implements CodeGenerator.Factory {

        @Override
        public List<? extends CodeGenerator> create(Lookup context) {
            ArrayList<CodeGenerator> toRet = new ArrayList<>();
            POMModel model = context.lookup(POMModel.class);
            JTextComponent component = context.lookup(JTextComponent.class);
            if (model != null) {
                toRet.add(new ActuatorGenerator(model, component));
            }
            return toRet;
        }
    }

    /**
     * The name which will be inserted inside Insert Code dialog
     */
    @Override
    public String getDisplayName() {
        return "Spring Boot actuator";
    }

    @Override
    protected void doInvoke() {
        FileObject fo = model.getModelSource().getLookup().lookup(FileObject.class);
        assert fo != null;
        org.netbeans.api.project.Project prj = FileOwnerQuery.getOwner(fo);
        assert prj != null;
        writeModel(new DependencyModelWriter(component, model, GROUP_ID, ARTIFACT_ID));
    }

}
