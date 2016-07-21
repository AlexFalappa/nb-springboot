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
package com.github.alexfalappa.nbspringboot.templates.metadata;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.swing.event.ChangeListener;

import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle.Messages;

@TemplateRegistration(
        folder = "Spring Boot",
        iconBase = "com/github/alexfalappa/nbspringboot/templates/metadata/boot-json.png",
        displayName = "#metadata_displayName",
        content = "additional-spring-configuration-metadata.json.template",
        description = "description.html",
        scriptEngine = "freemarker",
        position = 500)
@Messages(value = "metadata_displayName=Additional properties metadata")
public final class MetadataWizardIterator implements WizardDescriptor.InstantiatingIterator<WizardDescriptor> {

    private WizardDescriptor wizard;
    private MetadataWizardPanel1 panel;

    @Override
    public Set<?> instantiate() throws IOException {
        File fDir = panel.getComponent().getCreatedFile().getParentFile();
        // ensure target folder exists and set it into Templates
        fDir.mkdirs();
        FileObject foDir = FileUtil.toFileObject(fDir);
        Templates.setTargetFolder(wizard, foDir);
        // Create file from template
        FileObject foTemplate = Templates.getTemplate(wizard);
        DataObject doTemplate = DataObject.find(foTemplate);
        DataFolder df = DataFolder.findFolder(foDir);
        DataObject doCreated = doTemplate.createFromTemplate(df, Templates.getTargetName(wizard));
        FileObject foCreated = doCreated.getPrimaryFile();
        return Collections.singleton(foCreated);
    }

    @Override
    public void initialize(WizardDescriptor wizard) {
        this.wizard = wizard;
        panel = new MetadataWizardPanel1();
        panel.getComponent();
    }

    @Override
    public void uninitialize(WizardDescriptor wizard) {
        wizard.putProperty(WIZ_BASE_NAME, null);
        wizard.putProperty(WIZ_PROFILE, null);
        panel = null;
    }

    @Override
    public WizardDescriptor.Panel<WizardDescriptor> current() {
        return panel;
    }

    @Override
    public String name() {
        return "1 of 1";
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    @Override
    public void nextPanel() {
    }

    @Override
    public void previousPanel() {
    }

    @Override
    public void addChangeListener(ChangeListener l) {
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
    }

}
