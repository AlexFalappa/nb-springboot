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
package com.github.alexfalappa.nbspringboot.projects.basic;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JComponent;
import javax.swing.event.ChangeListener;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.templates.FileBuilder;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;

import com.github.alexfalappa.nbspringboot.PrefConstants;
import com.github.alexfalappa.nbspringboot.Utils;

import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_FORCE_COLOR_OUTPUT;
import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_MANUAL_RESTART;

@TemplateRegistration(
        folder = "Project/Maven2",
        displayName = "#BasicSpringbootProject_displayName",
        description = "BasicSpringbootProjectDescription.html",
        iconBase = "com/github/alexfalappa/nbspringboot/projects/basic/BasicSpringbootProject.png",
        content = "basic-nbactions.xml.template",
        scriptEngine = "freemarker",
        position = 255
)
@Messages("BasicSpringbootProject_displayName=Spring Boot basic project")
public class BasicProjectWizardIterator implements WizardDescriptor.InstantiatingIterator {

    private int index;
    private WizardDescriptor.Panel[] panels;
    private WizardDescriptor wiz;

    public BasicProjectWizardIterator() {
    }

    public static BasicProjectWizardIterator createIterator() {
        return new BasicProjectWizardIterator();
    }

    private WizardDescriptor.Panel[] createPanels() {
        return new WizardDescriptor.Panel[]{
            new BasicProjectWizardPanel()
        };
    }

    private String[] createSteps() {
        return new String[]{
            NbBundle.getMessage(BasicProjectWizardIterator.class, "LBL_CreateProjectStep")
        };
    }

    @Override
    public Set<FileObject> instantiate() throws IOException {
        Set<FileObject> resultSet = new LinkedHashSet<>();
        File dirF = FileUtil.normalizeFile((File) wiz.getProperty("projdir"));
        dirF.mkdirs();
        FileObject dir = FileUtil.toFileObject(dirF);
        FileObject template = URLMapper.findFileObject(getClass().getResource("BasicSpringbootProject.zip"));
        unZipFile(template.getInputStream(), dir);
        // create nbactions.xml file
        createNbActions(dir);
        // clear non project cache
        ProjectManager.getDefault().clearNonProjectCache();
        // Always open top dir as a project:
        resultSet.add(dir);
        // trigger download of dependencies
        Project prj = ProjectManager.getDefault().findProject(dir);
        if (prj != null) {
            final NbMavenProject mvn = prj.getLookup().lookup(NbMavenProject.class);
            if (mvn != null) {
                mvn.downloadDependencyAndJavadocSource(false);
            }
        }
        // remember folder for creation of new projects
        File parent = dirF.getParentFile();
        if (parent != null && parent.exists()) {
            ProjectChooser.setProjectsFolder(parent);
        }
        return resultSet;
    }

    @Override
    public void initialize(WizardDescriptor wiz) {
        this.wiz = wiz;
        wiz.putProperty("NewProjectWizard_Title", NbBundle.getMessage(BasicProjectWizardIterator.class, "LBL_WizardTitle")); //NOI18N
        index = 0;
        panels = createPanels();
        // Make sure list of steps is accurate.
        String[] steps = createSteps();
        for (int i = 0; i < panels.length; i++) {
            Component c = panels[i].getComponent();
            if (steps[i] == null) {
                // Default step name to component name of panel.
                // Mainly useful for getting the name of the target
                // chooser to appear in the list of steps.
                steps[i] = c.getName();
            }
            if (c instanceof JComponent) { // assume Swing components
                JComponent jc = (JComponent) c;
                // Step #.
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
                // Step name (actually the whole list for reference).
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
            }
        }
    }

    @Override
    public void uninitialize(WizardDescriptor wiz) {
        this.wiz.putProperty("projdir", null);
        this.wiz.putProperty("name", null);
        this.wiz = null;
        panels = null;
    }

    @Override
    public String name() {
        return MessageFormat.format("{0} of {1}", new Object[]{index + 1, panels.length});
    }

    @Override
    public boolean hasNext() {
        return index < panels.length - 1;
    }

    @Override
    public boolean hasPrevious() {
        return index > 0;
    }

    @Override
    public void nextPanel() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        index++;
    }

    @Override
    public void previousPanel() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        index--;
    }

    @Override
    public WizardDescriptor.Panel current() {
        return panels[index];
    }

    // If nothing unusual changes in the middle of the wizard, simply:
    @Override
    public final void addChangeListener(ChangeListener l) {
    }

    @Override
    public final void removeChangeListener(ChangeListener l) {
    }

    private static void unZipFile(InputStream source, FileObject projectRoot) throws IOException {
        try {
            ZipInputStream str = new ZipInputStream(source);
            ZipEntry entry;
            while ((entry = str.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    FileUtil.createFolder(projectRoot, entry.getName());
                } else {
                    FileObject fo = FileUtil.createData(projectRoot, entry.getName());
                    writeFile(str, fo);
                }
            }
        } finally {
            source.close();
        }
    }

    private static void writeFile(ZipInputStream str, FileObject fo) throws IOException {
        try (OutputStream out = fo.getOutputStream()) {
            FileUtil.copy(str, out);
        }
    }

    private void createNbActions(FileObject dir) throws IOException {
        // retrieve default options from prefs
        final Preferences prefs = NbPreferences.forModule(PrefConstants.class);
        final boolean bForceColor = prefs.getBoolean(PREF_FORCE_COLOR_OUTPUT, true);
        final boolean bManualRestart = prefs.getBoolean(PREF_MANUAL_RESTART, false);
        final String strVmOpts = Utils.vmOptsFromPrefs();
        // create nbactions.xml from template
        FileObject foTmpl = Templates.getTemplate(wiz);
        new FileBuilder(foTmpl, dir)
                .name("nbactions")
                .param("forceColor", bForceColor)
                .param("manualRestart", bManualRestart)
                .param("vmOpts", strVmOpts)
                .build();
    }

}
