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
package com.github.alexfalappa.nbspringboot.projects;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.netbeans.spi.project.ui.RecommendedTemplates;

import static com.github.alexfalappa.nbspringboot.templates.FileTemplates.CATEGORY_SPRING_BOOT;
import static com.github.alexfalappa.nbspringboot.templates.FileTemplates.CATEGORY_SPRING_DATA;
import static com.github.alexfalappa.nbspringboot.templates.FileTemplates.CATEGORY_SPRING_MVC;

/**
 * Provides recommended template types for Spring Boot to maven projects.
 * <p>
 * Analyzing the pom dependencies the correct categories are returned so that file templates appear in the New File... dialog only if
 * applicable.
 *
 * @author Alessandro Falappa
 */
@ProjectServiceProvider(service = RecommendedTemplates.class, projectType = {"org-netbeans-modules-maven"})
public class BootRecommendedTemplates implements RecommendedTemplates {

    private Project prj;

    public BootRecommendedTemplates(Project prj) {
        this.prj = prj;
    }

    @Override
    public String[] getRecommendedTypes() {
        NbMavenProject project = prj.getLookup().lookup(NbMavenProject.class);
        Set<String> recomTypes = new HashSet<>();
        for (Object obj : project.getMavenProject().getDependencies()) {
            if (obj instanceof Dependency) {
                Dependency dependency = (Dependency) obj;
                // check if the maven project contains spring boot dependencies
                if (dependency.getArtifactId().contains("spring-boot")) {
                    if (dependency.getArtifactId().contains("spring-boot-starter-web")) {
                        // spring boot web dependency
                        recomTypes.add(CATEGORY_SPRING_MVC);
                    }
                    if (dependency.getArtifactId().contains("spring-boot-starter-data")) {
                        // spring boot data dependency
                        recomTypes.add(CATEGORY_SPRING_DATA);
                    } else {
                        // other spring boot dependencies
                        recomTypes.add(CATEGORY_SPRING_BOOT);
                    }
                }
            }
        }
        return recomTypes.toArray(new String[0]);
    }

}
