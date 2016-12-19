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

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;

import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;

@ActionID(
        category = "Build",
        id = "com.github.alexfalappa.nbspringboot.actions.ProjectAction"
)
@ActionRegistration(
        iconBase = "com/github/alexfalappa/nbspringboot/actions/push.png",
        displayName = "#CTL_ProjectAction"
)
@ActionReference(path = "Toolbars/Build", position = 601)
@Messages("CTL_ProjectAction=Project Action")
public final class ProjectAction implements ActionListener {

    public ProjectAction(Project project) {
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        Project prj = Utilities.actionsGlobalContext().lookup(Project.class);
        if (prj != null) {
            SpringBootService sbs = prj.getLookup().lookup(SpringBootService.class);
            if (sbs != null) {
                System.out.println("Found BootConfigurationPropertiesService in Project lookup got via actions global lookup");
            }
            SourceGroup[] groups = ProjectUtils.getSources(prj).getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
            for (SourceGroup group : groups) {
                System.out.println("---");
                System.out.printf("[%s] %s: %s%n", group.getName(), group.getDisplayName(), FileUtil.getFileDisplayName(group
                        .getRootFolder()));
                ClassPath cpSrc = ClassPath.getClassPath(group.getRootFolder(), ClassPath.SOURCE);
                if (cpSrc != null) {
                    System.out.printf("Source classpath:%n\t%s%n", cpSrc.toString().replace(":", "\n\t"));
                }
                ClassPath cpExec = ClassPath.getClassPath(group.getRootFolder(), ClassPath.EXECUTE);
                if (cpExec != null) {
                    System.out.printf("Execute classpath:%n\t%s%n", cpExec.toString().replace(":", "\n\t"));
                }
                ClassPath cpCompile = ClassPath.getClassPath(group.getRootFolder(), ClassPath.COMPILE);
                if (cpCompile != null) {
                    System.out.printf("Compile classpath:%n\t%s%n", cpCompile.toString().replace(":", "\n\t"));
                }
            }
        }
    }
}
