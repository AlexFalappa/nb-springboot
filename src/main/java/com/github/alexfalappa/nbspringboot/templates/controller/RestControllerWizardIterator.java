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
package com.github.alexfalappa.nbspringboot.templates.controller;

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

import static com.github.alexfalappa.nbspringboot.templates.FileTemplates.CATEGORY_SPRING_MVC;
import static com.github.alexfalappa.nbspringboot.templates.FileTemplates.FOLDER_SPRING_FRAMEWORK;
import static com.github.alexfalappa.nbspringboot.templates.FileTemplates.ICON_SPRING_CLASS;

@TemplateRegistration(
        folder = FOLDER_SPRING_FRAMEWORK,
        iconBase = ICON_SPRING_CLASS,
        displayName = "#rest_displayName",
        content = "RestController.java.template",
        description = "RestController.html",
        scriptEngine = "freemarker",
        category = {CATEGORY_SPRING_MVC},
        position = 800)
@NbBundle.Messages(value = "rest_displayName=REST Controller Class")
public final class RestControllerWizardIterator implements WizardDescriptor.InstantiatingIterator<WizardDescriptor> {

    public static final String WIZ_ERROR_HANDLING = "errorHandling";
    public static final String WIZ_CRUD_METHODS = "crudMethods";
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
        props.put(WIZ_CRUD_METHODS, wizard.getProperty(WIZ_CRUD_METHODS));
        props.put(WIZ_ERROR_HANDLING, wizard.getProperty(WIZ_ERROR_HANDLING));
        DataObject doCreated = doTemplate.createFromTemplate(df, targetName, props);
        FileObject foCreated = doCreated.getPrimaryFile();
        return Collections.singleton(foCreated);
    }

    @Override
    public void initialize(WizardDescriptor wizard) {
        this.wizard = wizard;
        wizard.putProperty(WIZ_CRUD_METHODS, false);
        wizard.putProperty(WIZ_ERROR_HANDLING, 0);
        Project project = Templates.getProject(wizard);
        Sources src = ProjectUtils.getSources(project);
        SourceGroup[] groups = src.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        panel = JavaTemplates.createPackageChooser(project, groups, new RestControllerWizardPanel1(), true);
        // force creation of visual part
        JComponent cmp = (JComponent) panel.getComponent();
        cmp.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, 0);
        cmp.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, TemplateUtils.createSteps(wizard, new String[]{cmp.getName()}));
    }

    @Override
    public void uninitialize(WizardDescriptor wizard) {
        wizard.putProperty(WIZ_CRUD_METHODS, null);
        wizard.putProperty(WIZ_ERROR_HANDLING, null);
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
