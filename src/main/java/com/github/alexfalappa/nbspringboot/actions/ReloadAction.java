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
package com.github.alexfalappa.nbspringboot.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

import static org.netbeans.api.project.Sources.TYPE_GENERIC;

@ActionID(
        category = "Build",
        id = "com.github.alexfalappa.nbspringboot.actions.ReloadAction"
)
@ActionRegistration(
        iconBase = "com/github/alexfalappa/nbspringboot/actions/springboot-logo.png",
        displayName = "#CTL_ReloadAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/BuildProject", position = 57),
    @ActionReference(path = "Toolbars/Build", position = 500),
    @ActionReference(path = "Shortcuts", name = "DS-L")
})
@Messages("CTL_ReloadAction=S&pring Boot Reload")
public final class ReloadAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO implementare ContextGlobalProvider per tracciare l'ultimo Project selezionato
        // altrimenti l'azione non funziona sempre (stesso pattern di abilitazione di quando
        // ha un Project come parametro del costruttore)
        // vedi https://blogs.oracle.com/geertjan/entry/org_openide_util_contextglobalprovider
        System.out.println("\n\nReloadAction!!!");
        Lookup lkp = org.openide.util.Utilities.actionsGlobalContext();
        Project proj = lkp.lookup(Project.class);
        if (proj != null) {
            System.out.print("Project dir: ");
            System.out.println(proj.getProjectDirectory().getName());
            Sources src = ProjectUtils.getSources(proj);
            if (src != null) {
                SourceGroup[] gr = src.getSourceGroups(TYPE_GENERIC);
                System.out.println("Source groups");
                for (SourceGroup g : gr) {
                    System.out.print("  Group ");
                    System.out.print(g.getName());
                    System.out.print("  folder ");
                    System.out.println(FileUtil.getFileDisplayName(g.getRootFolder()));
                }

            }
            NbMavenProject mvnProj = proj.getLookup().lookup(NbMavenProject.class);
            if (mvnProj != null) {
                System.out.println("Maven project");
                System.out.print("Packaging ");
                System.out.println(mvnProj.getPackagingType());
                System.out.print("Output dir ");
                System.out.println(mvnProj.getOutputDirectory(false).getAbsolutePath());
            }
        }
    }
}
