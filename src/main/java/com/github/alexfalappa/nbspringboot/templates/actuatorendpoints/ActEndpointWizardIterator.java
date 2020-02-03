/*
 * Copyright 2016 the original author or authors.
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
package com.github.alexfalappa.nbspringboot.templates.actuatorendpoints;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.event.ChangeListener;

import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.spi.java.project.support.ui.templates.JavaTemplates;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle;

import com.github.alexfalappa.nbspringboot.templates.TemplateUtils;

import static com.github.alexfalappa.nbspringboot.templates.FileTemplates.CATEGORY_SPRING_BOOT_ACTUATOR;
import static com.github.alexfalappa.nbspringboot.templates.FileTemplates.FOLDER_SPRING_BOOT;
import static com.github.alexfalappa.nbspringboot.templates.FileTemplates.ICON_BOOT_CLASS;

/**
 * Wizard iterator for new Actuator endpoint file wizard.
 *
 * @author Alessandro Falappa
 */
@TemplateRegistration(
        folder = FOLDER_SPRING_BOOT,
        iconBase = ICON_BOOT_CLASS,
        displayName = "#actend_displayName",
        content = "ActuatorEndpoint.java.template",
        description = "ActuatorEndpoint.html",
        scriptEngine = "freemarker",
        category = {CATEGORY_SPRING_BOOT_ACTUATOR},
        position = 600)
@NbBundle.Messages(value = "actend_displayName=Actuator Endpoint")
public final class ActEndpointWizardIterator implements WizardDescriptor.InstantiatingIterator<WizardDescriptor> {

    public static final String WIZ_TECHNOLOGY = "technology";
    public static final String WIZ_SAMPLE_OPS = "sampleOperations";
    private WizardDescriptor wizard;
    private WizardDescriptor.Panel<WizardDescriptor> panel;

    @Override
    public Set<?> instantiate() throws IOException {
        // Create file from template
        String targetName = Templates.getTargetName(wizard);
        FileObject foDir = Templates.getTargetFolder(wizard);
        FileObject foTemplate = Templates.getTemplate(wizard);
        DataObject doTemplate = DataObject.find(foTemplate);
        DataFolder df = DataFolder.findFolder(foDir);
        Map<String, Object> props = new HashMap<>();
        props.put(WIZ_TECHNOLOGY, wizard.getProperty(WIZ_TECHNOLOGY));
        props.put(WIZ_SAMPLE_OPS, wizard.getProperty(WIZ_SAMPLE_OPS));
        DataObject doCreated = doTemplate.createFromTemplate(df, targetName, props);
        FileObject foCreated = doCreated.getPrimaryFile();
        return Collections.singleton(foCreated);
    }

    @Override
    public void initialize(WizardDescriptor wizard) {
        this.wizard = wizard;
        wizard.putProperty(WIZ_TECHNOLOGY, 0);
        wizard.putProperty(WIZ_SAMPLE_OPS, true);
        Project project = Templates.getProject(wizard);
        Sources src = ProjectUtils.getSources(project);
        SourceGroup[] groups = src.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        panel = JavaTemplates.createPackageChooser(project, groups, new ActEndpointWizardPanel1(), true);
        // force creation of visual part
        JComponent cmp = (JComponent) panel.getComponent();
        cmp.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, 0);
        cmp.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, TemplateUtils.createSteps(wizard, new String[]{cmp.getName()}));
    }

    @Override
    public void uninitialize(WizardDescriptor wizard) {
        wizard.putProperty(WIZ_TECHNOLOGY, null);
        wizard.putProperty(WIZ_SAMPLE_OPS, null);
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
