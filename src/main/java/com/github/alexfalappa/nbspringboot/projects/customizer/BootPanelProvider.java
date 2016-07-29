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
package com.github.alexfalappa.nbspringboot.projects.customizer;

import javax.swing.JComponent;

import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.Lookup;

/**
 *
 * @author Alessandro Falappa
 */
@ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-maven", position = 1000)
public class BootPanelProvider implements ProjectCustomizer.CompositeCategoryProvider {

    @Override
    public ProjectCustomizer.Category createCategory(Lookup context) {
        return ProjectCustomizer.Category.create("boot", "Spring Boot", null);
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category arg0, Lookup arg1) {
        return new BootPanel();
    }

}
