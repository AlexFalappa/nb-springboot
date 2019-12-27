/*
 * Copyright 2019 Hector Espert.
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

import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;
import java.awt.Image;
import java.util.Objects;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.NbMavenProjectImpl;
import org.netbeans.spi.project.ProjectIconAnnotator;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.ServiceProvider;

/**
 * Overrides project icon for Spring Boot projects. 
 * @author Hector Espert
 */
@ServiceProvider(service = ProjectIconAnnotator.class, position = 0)
public class SpringBootProjectIconAnnotator implements ProjectIconAnnotator {

    @Override
    public Image annotateIcon(Project project, Image original, boolean openedNode) {
        if (project instanceof NbMavenProjectImpl) {
            return annotateIcon((NbMavenProjectImpl) project, original);
        }
        return original;
    }
    
    private Image annotateIcon(NbMavenProjectImpl project, Image original) {
        SpringBootService springBootService = project.getLookup().lookup(SpringBootService.class);
        if (Objects.nonNull(springBootService) && springBootService.hasPomDependency("spring-boot")) {
            return ImageUtilities.loadImage("com/github/alexfalappa/nbspringboot/springboot-logo.png");
        }
        return original;
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        
    }

    
    
}
