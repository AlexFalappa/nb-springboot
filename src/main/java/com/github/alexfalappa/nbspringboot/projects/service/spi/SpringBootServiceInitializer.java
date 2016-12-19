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
package com.github.alexfalappa.nbspringboot.projects.service.spi;

import java.util.logging.Logger;

import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.netbeans.spi.project.ui.ProjectOpenedHook;

import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;

/**
 * {@code ProjectOpenedHook} to initialize a {@link SpringBootService} implementation on opening of maven projects.
 * <p>
 * It is registered as a project service.
 *
 * @author Alessandro Falappa
 */
@ProjectServiceProvider(
        service = ProjectOpenedHook.class,
        projectType = {
            "org-netbeans-modules-maven/" + NbMavenProject.TYPE_JAR,
            "org-netbeans-modules-maven/" + NbMavenProject.TYPE_WAR
        }
)
public class SpringBootServiceInitializer extends ProjectOpenedHook {

    private static final Logger logger = Logger.getLogger(SpringBootServiceInitializer.class.getName());
    private final Project prj;

    public SpringBootServiceInitializer(Project p) {
        this.prj = p;
    }

    @Override
    protected void projectOpened() {
        final SpringBootService bootService = prj.getLookup().lookup(SpringBootService.class);
        if (bootService == null) {
            logger.info("No Spring Boot service implementations to initialize");
        } else {
            bootService.init();
        }
    }

    @Override
    protected void projectClosed() {
    }

}
