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
package com.github.alexfalappa.nbspringboot.projects.customizer;

import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;
import javax.swing.JComponent;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.modules.maven.api.customizer.ModelHandle2;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.Lookup;

/**
 * Creates the Spring Boot customizer panel when maven projects have a dependency named 'spring-boot-####'.
 *
 * @author Alessandro Falappa
 * @author Diego Díez Ricondo
 */
@ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-maven", position = 1300)
public class BootPanelProvider implements ProjectCustomizer.CompositeCategoryProvider {

    @Override
    public ProjectCustomizer.Category createCategory(Lookup context) {
        if (Utils.isSpringBootProject(context.lookup(Project.class))) {
            return ProjectCustomizer.Category.create("boot", "Spring Boot", null);
        }
        return null;
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category category, Lookup context) {
        final BootPanel bootPanel = new BootPanel();
        // NOTE order of invocations is important here!
        bootPanel.init(context.lookup(ModelHandle2.class), prjBootService(context));
        bootPanel.setDevToolsEnabled(prjHasDepContaining(context, "spring-boot-devtools"));
        return bootPanel;
    }

    private SpringBootService prjBootService(Lookup context) {
        Project prj = context.lookup(Project.class);
        if (prj != null) {
            return prj.getLookup().lookup(SpringBootService.class);
        }
        return null;
    }

    private boolean prjHasDepContaining(Lookup context, String txt) {
        Project prj = context.lookup(Project.class);
        if (prj != null) {
            NbMavenProject nbMvn = prj.getLookup().lookup(NbMavenProject.class);
            if (nbMvn != null) {
                return Utils.dependencyArtifactIdContains(nbMvn, txt);
            }
        }
        return false;
    }
}
