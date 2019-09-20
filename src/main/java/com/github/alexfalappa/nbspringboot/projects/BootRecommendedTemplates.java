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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.netbeans.spi.project.ui.RecommendedTemplates;

import static com.github.alexfalappa.nbspringboot.templates.FileTemplates.CATEGORY_SPRING_BOOT;
import static com.github.alexfalappa.nbspringboot.templates.FileTemplates.CATEGORY_SPRING_BOOT_ACTUATOR;
import static com.github.alexfalappa.nbspringboot.templates.FileTemplates.CATEGORY_SPRING_DATA;
import static com.github.alexfalappa.nbspringboot.templates.FileTemplates.CATEGORY_SPRING_FRAMEWORK;
import static com.github.alexfalappa.nbspringboot.templates.FileTemplates.CATEGORY_SPRING_MVC;
import static com.github.alexfalappa.nbspringboot.templates.FileTemplates.CATEGORY_SPRING_REACT;

/**
 * Provides recommended template types for Spring Boot to maven projects.
 * <p>
 * Analyzing the pom dependencies the correct categories are returned so that file templates appear in the New File... dialog only
 * if applicable.
 *
 * @author Alessandro Falappa
 */
@ProjectServiceProvider(service = RecommendedTemplates.class, projectType = {"org-netbeans-modules-maven"})
public class BootRecommendedTemplates implements RecommendedTemplates {

    private enum SpringDeps {
        BOOT, CONTEXT, WEB, DATA, WEBFLUX, ACTUATOR
    }

    private Project prj;

    public BootRecommendedTemplates(Project prj) {
        this.prj = prj;
    }

    @Override
    public String[] getRecommendedTypes() {
        NbMavenProject project = prj.getLookup().lookup(NbMavenProject.class);
        EnumSet<SpringDeps> deps = EnumSet.noneOf(SpringDeps.class);
        List compileArtifacts = project.getMavenProject().getCompileArtifacts();
        for (Object obj : compileArtifacts) {
            if (obj instanceof Artifact) {
                Artifact artifact = (Artifact) obj;
                if (artifact.getScope().equals(Artifact.SCOPE_COMPILE)) {
                    final String artifactId = artifact.getArtifactId();
                    if (artifactId.contains("spring-data")) {
                        deps.add(SpringDeps.DATA);
                    }
                    switch (artifactId) {
                        case "spring-context":
                            deps.add(SpringDeps.CONTEXT);
                            break;
                        case "spring-web":
                            deps.add(SpringDeps.WEB);
                            break;
                        case "spring-webflux":
                            deps.add(SpringDeps.WEBFLUX);
                            break;
                        case "spring-boot":
                            deps.add(SpringDeps.BOOT);
                            break;
                        case "spring-boot-actuator":
                            deps.add(SpringDeps.ACTUATOR);
                            break;
                    }
                }
            }
        }
        Set<String> recomTypes = new HashSet<>();
        if (deps.contains(SpringDeps.BOOT)) {
            recomTypes.add(CATEGORY_SPRING_BOOT);
        }
        if (deps.contains(SpringDeps.DATA)) {
            recomTypes.add(CATEGORY_SPRING_DATA);
        }
        if (deps.contains(SpringDeps.CONTEXT)) {
            recomTypes.add(CATEGORY_SPRING_FRAMEWORK);
        }
        if (deps.contains(SpringDeps.CONTEXT) && deps.contains(SpringDeps.WEB)) {
            recomTypes.add(CATEGORY_SPRING_MVC);
        }
        if (deps.contains(SpringDeps.CONTEXT) && deps.contains(SpringDeps.WEBFLUX)) {
            recomTypes.add(CATEGORY_SPRING_REACT);
        }
        if (deps.contains(SpringDeps.ACTUATOR)) {
            recomTypes.add(CATEGORY_SPRING_BOOT_ACTUATOR);
        }
        return recomTypes.toArray(new String[0]);
    }

}
