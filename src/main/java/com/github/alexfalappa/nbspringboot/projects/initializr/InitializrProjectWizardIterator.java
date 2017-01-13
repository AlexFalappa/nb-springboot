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
package com.github.alexfalappa.nbspringboot.projects.initializr;

import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JComponent;
import javax.swing.event.ChangeListener;

import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.templates.FileBuilder;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.AsyncGUIJob;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;
import org.openide.xml.XMLUtil;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_FORCE_COLOR_OUTPUT;
import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_MANUAL_RESTART;
import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_VM_OPTS;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_ARTIFACT;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_BOOT_VERSION;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_DEPENDENCIES;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_DESCRIPTION;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_GROUP;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_JAVA_VERSION;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_LANGUAGE;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_NAME;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_PACKAGE;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_PACKAGING;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_PROJ_LOCATION;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_REMOVE_MVN_WRAPPER;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_USE_SB_MVN_PLUGIN;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_VERSION;

@TemplateRegistration(
        folder = "Project/Maven2",
        displayName = "#InitializrSpringbootProject_displayName",
        description = "InitializrSpringbootProjectDescription.html",
        iconBase = "com/github/alexfalappa/nbspringboot/projects/initializr/InitializrSpringbootProject.png",
        content = "nbactions.xml.template",
        scriptEngine = "freemarker",
        position = 256
)
@Messages("InitializrSpringbootProject_displayName=Spring Boot Initializr project")
public class InitializrProjectWizardIterator implements WizardDescriptor.InstantiatingIterator {

    private int index;
    private WizardDescriptor.Panel[] panels;
    private WizardDescriptor wiz;
    private final InitializrService initializrService = new InitializrService();

    public InitializrProjectWizardIterator() {
    }

    public static InitializrProjectWizardIterator createIterator() {
        return new InitializrProjectWizardIterator();
    }

    @Override
    public Set<FileObject> instantiate(/*ProgressHandle handle*/) throws IOException {
        Set<FileObject> resultSet = new LinkedHashSet<>();
        File dirF = FileUtil.normalizeFile((File) wiz.getProperty(WIZ_PROJ_LOCATION));
        dirF.mkdirs();
        FileObject dir = FileUtil.toFileObject(dirF);
        // prepare service invocation params
        String bootVersion = ((NamedItem) wiz.getProperty(WIZ_BOOT_VERSION)).getId();
        String mvnGroup = (String) wiz.getProperty(WIZ_GROUP);
        String mvnArtifact = (String) wiz.getProperty(WIZ_ARTIFACT);
        String mvnVersion = (String) wiz.getProperty(WIZ_VERSION);
        String mvnName = (String) wiz.getProperty(WIZ_NAME);
        String mvnDesc = (String) wiz.getProperty(WIZ_DESCRIPTION);
        String packaging = ((NamedItem) wiz.getProperty(WIZ_PACKAGING)).getId();
        String pkg = (String) wiz.getProperty(WIZ_PACKAGE);
        String lang = ((NamedItem) wiz.getProperty(WIZ_LANGUAGE)).getId();
        String javaVersion = ((NamedItem) wiz.getProperty(WIZ_JAVA_VERSION)).getId();
        String deps = (String) wiz.getProperty(WIZ_DEPENDENCIES);
        try {
            // invoke initializr webservice
            InputStream stream = initializrService
                    .getProject(bootVersion, mvnGroup, mvnArtifact, mvnVersion, mvnName, mvnDesc, packaging, pkg, lang, javaVersion, deps);
            // unzip response
            unZipFile(stream, dir, (boolean) wiz.getProperty(WIZ_REMOVE_MVN_WRAPPER));
            // parse pom.xml
            final FileObject foPom = dir.getFileObject("pom.xml");
            Document pomDoc = XMLUtil.parse(new InputSource(foPom.getInputStream()), false, false, null, null);
            boolean pomModified = false;
            // manage run/debug trough maven plugin
            if ((boolean) wiz.getProperty(WIZ_USE_SB_MVN_PLUGIN)) {
                // create nbactions.xml file with custom maven actions configuration
                createNbActions(pkg, mvnName, dir);
                // modify pom.xml content and add forking flag to plugin configuration
                pomConfigMvnPlugin(pomDoc);
                pomModified = true;
            }
            // save pom document if modified
            if (pomModified) {
                try (OutputStream out = foPom.getOutputStream()) {
                    XMLUtil.write(pomDoc, out, "UTF-8");
                }
            }
            // Always open top dir as a project:
            resultSet.add(dir);
            // Look for nested projects to open as well:
            Enumeration<? extends FileObject> e = dir.getFolders(true);
            while (e.hasMoreElements()) {
                FileObject subfolder = e.nextElement();
                if (ProjectManager.getDefault().isProject(subfolder)) {
                    resultSet.add(subfolder);
                }
            }
            File parent = dirF.getParentFile();
            if (parent != null && parent.exists()) {
                ProjectChooser.setProjectsFolder(parent);
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return resultSet;
    }

    @Override
    public void initialize(WizardDescriptor wiz) {
        this.wiz = wiz;
        index = 0;
        // set other defaults
        this.wiz.putProperty(WIZ_USE_SB_MVN_PLUGIN, true);
        this.wiz.putProperty(WIZ_REMOVE_MVN_WRAPPER, true);
        // create the wizard panels
        panels = new WizardDescriptor.Panel[]{
            new InitializrProjectWizardPanel1(initializrService),
            new InitializrProjectWizardPanel2(),
            new InitializrProjectWizardPanel3()
        };
        // Make sure list of steps is accurate.
        String[] steps = new String[]{
            NbBundle.getMessage(InitializrProjectWizardIterator.class, "LBL_BasePropsStep"),
            NbBundle.getMessage(InitializrProjectWizardIterator.class, "LBL_DependenciesStep"),
            NbBundle.getMessage(InitializrProjectWizardIterator.class, "LBL_CreateProjectStep")
        };
        // create wizard steps gui components
        for (int i = 0; i < panels.length; i++) {
            Component c = panels[i].getComponent();
            if (steps[i] == null) {
                // Default step name to component name of panel.
                // Mainly useful for getting the name of the target
                // chooser to appear in the list of steps.
                steps[i] = c.getName();
            }
            if (c instanceof JComponent) {
                // assume Swing components
                JComponent jc = (JComponent) c;
                // Step #.
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
                // Step name (actually the whole list for reference).
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
            }
        }
        // schedule async retrieval of initializr metadata in panel visual 1
        Utilities.attachInitJob(panels[0].getComponent(), (AsyncGUIJob) panels[0].getComponent());
    }

    @Override
    public void uninitialize(WizardDescriptor wiz) {
        this.wiz.putProperty(WIZ_NAME, null);
        this.wiz.putProperty(WIZ_GROUP, null);
        this.wiz.putProperty(WIZ_ARTIFACT, null);
        this.wiz.putProperty(WIZ_DESCRIPTION, null);
        this.wiz.putProperty(WIZ_PACKAGING, null);
        this.wiz.putProperty(WIZ_PACKAGE, null);
        this.wiz.putProperty(WIZ_JAVA_VERSION, null);
        this.wiz.putProperty(WIZ_LANGUAGE, null);
        this.wiz.putProperty(WIZ_DEPENDENCIES, null);
        this.wiz.putProperty(WIZ_PROJ_LOCATION, null);
        this.wiz.putProperty(WIZ_USE_SB_MVN_PLUGIN, null);
        this.wiz.putProperty(WIZ_REMOVE_MVN_WRAPPER, null);
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

    private static void unZipFile(InputStream source, FileObject projectRoot, boolean removeMvnWrapper) throws IOException {
        try {
            ZipInputStream str = new ZipInputStream(source);
            ZipEntry entry;
            while ((entry = str.getNextEntry()) != null) {
                final String entryName = entry.getName();
                // optionally skip entries related to maven wrapper
                if (removeMvnWrapper && (entryName.contains(".mvn") || entryName.contains("mvnw"))) {
                    continue;
                }
                if (entry.isDirectory()) {
                    FileUtil.createFolder(projectRoot, entryName);
                } else {
                    FileObject fo = FileUtil.createData(projectRoot, entryName);
                    if ("nbproject/project.xml".equals(entryName)) {
                        // Special handling for setting name of Ant-based projects; customize as needed:
                        filterProjectXML(fo, str, projectRoot.getName());
                    } else {
                        writeFile(str, fo);
                    }
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

    private static void filterProjectXML(FileObject fo, ZipInputStream str, String name) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileUtil.copy(str, baos);
            Document doc = XMLUtil.parse(new InputSource(new ByteArrayInputStream(baos.toByteArray())), false, false, null, null);
            NodeList nl = doc.getDocumentElement().getElementsByTagName("name");
            if (nl != null) {
                for (int i = 0; i < nl.getLength(); i++) {
                    Element el = (Element) nl.item(i);
                    if (el.getParentNode() != null && "data".equals(el.getParentNode().getNodeName())) {
                        NodeList nl2 = el.getChildNodes();
                        if (nl2.getLength() > 0) {
                            nl2.item(0).setNodeValue(name);
                        }
                        break;
                    }
                }
            }
            try (OutputStream out = fo.getOutputStream()) {
                XMLUtil.write(doc, out, "UTF-8");
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            writeFile(str, fo);
        }

    }

    private void createNbActions(String pkg, String mvnName, FileObject dir) throws IOException {
        // build main class string
        StringBuilder mainClass = new StringBuilder(pkg).append('.');
        mainClass.append(mvnName.substring(0, 1).toUpperCase()).append(mvnName.substring(1));
        mainClass.append("Application");
        // retrieve default options from prefs
        final boolean bForceColor = NbPreferences.forModule(InitializrProjectWizardIterator.class).getBoolean(PREF_FORCE_COLOR_OUTPUT, true);
        final boolean bManualRestart = NbPreferences.forModule(InitializrProjectWizardIterator.class).getBoolean(PREF_MANUAL_RESTART, false);
        final String strVmOpts = NbPreferences.forModule(InitializrProjectWizardIterator.class).get(PREF_VM_OPTS, "");
        // create nbactions.xml from template
        FileObject foTmpl = Templates.getTemplate(wiz);
        new FileBuilder(foTmpl, dir)
                .name("nbactions")
                .param("mainClass", mainClass.toString())
                .param("forceColor", bForceColor)
                .param("manualRestart", bManualRestart)
                .param("vmOpts", strVmOpts)
                .build();
    }

    private void pomConfigMvnPlugin(Document doc) throws DOMException, SAXException, IOException {
        // modify pom.xml content and add cfg to spring maven plugin
        NodeList nl = doc.getElementsByTagName("plugin");
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Element el = (Element) nl.item(i);
                NodeList grpId = el.getElementsByTagName("groupId");
                NodeList artId = el.getElementsByTagName("artifactId");
                if (grpId.getLength() > 0 && artId.getLength() > 0
                        && "org.springframework.boot".equals(grpId.item(0).getTextContent())
                        && "spring-boot-maven-plugin".equals(artId.item(0).getTextContent())) {
                    Node cfg = doc.createElement("configuration");
                    Node frk = doc.createElement("fork");
                    frk.setTextContent("true");
                    cfg.appendChild(frk);
                    el.appendChild(cfg);
                }
            }
        }
    }

}
