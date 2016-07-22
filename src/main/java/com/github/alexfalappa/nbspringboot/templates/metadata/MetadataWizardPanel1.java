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
import java.net.URI;
import java.util.logging.Level;

import javax.swing.event.ChangeListener;

import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

public class MetadataWizardPanel1 implements WizardDescriptor.Panel<WizardDescriptor>, WizardDescriptor.FinishablePanel<WizardDescriptor> {

    private MetadataVisualPanel1 component;

    // Get the visual component for the panel. In this template, the component
    // is kept separate. This can be more efficient: if the wizard is created
    // but never displayed, or not all panels are displayed, it is better to
    // create only those which really need to be visible.
    @Override
    public MetadataVisualPanel1 getComponent() {
        if (component == null) {
            component = new MetadataVisualPanel1();
            component.setName(NbBundle.getMessage(MetadataWizardPanel1.class, "LBL_SectionStep"));
        }
        return component;
    }

    @Override
    public HelpCtx getHelp() {
        // Show no Help button for this panel:
        return HelpCtx.DEFAULT_HELP;
        // If you have context help:
        // return new HelpCtx("help.key.here");
    }

    @Override
    public boolean isValid() {
        return true;
    }

    private final ChangeSupport chgSupport = new ChangeSupport(this);

    @Override
    public final void addChangeListener(ChangeListener l) {
        synchronized (chgSupport) {
            chgSupport.addChangeListener(l);
        }
    }

    @Override
    public final void removeChangeListener(ChangeListener l) {
        synchronized (chgSupport) {
            chgSupport.removeChangeListener(l);
        }
    }

    protected final void fireChangeEvent() {
        chgSupport.fireChange();
    }

    @Override
    public void readSettings(WizardDescriptor wiz) {
        try {
            final Project project = Templates.getProject(wiz);
            NbMavenProject nbProj = project.getLookup().lookup(NbMavenProject.class);
            final URI[] resources = nbProj.getResources(false);
            File resourceFolder = FileUtil.normalizeFile(FileUtil.archiveOrDirForURL(resources[0].toURL()));
            File addMeta = new File(resourceFolder, "META-INF/additional-spring-configuration-metadata.json");
            if (addMeta.exists()) {
                wiz.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE, "Existing additional metadata file will be overwritten!");
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(Exceptions.attachSeverity(ex, Level.WARNING));
        }
        component.read(wiz);
    }

    @Override
    public void storeSettings(WizardDescriptor wiz) {
        component.store(wiz);
    }

    @Override
    public boolean isFinishPanel() {
        return true;
    }

}
