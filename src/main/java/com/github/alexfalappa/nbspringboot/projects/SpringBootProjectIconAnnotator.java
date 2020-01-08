/*
 * Copyright 2019 the original author or authors.
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

import java.awt.EventQueue;
import java.awt.Image;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.event.ChangeListener;

import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.NbMavenProjectImpl;
import org.netbeans.spi.project.ProjectIconAnnotator;
import org.openide.util.ChangeSupport;
import org.openide.util.ImageUtilities;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;

import com.github.alexfalappa.nbspringboot.Utils;

/**
 * Add badge to project icon for Spring Boot projects.
 * <p>
 * Based on project icon annotator for JavaFX projects in NetBeans:
 * javafx/javafx2.project/src/org/netbeans/modules/javafx2/project/JFXProjectIconAnnotator.java
 *
 * @author Hector Espert
 * @author Alessandro Falappa
 */
@ServiceProvider(service = ProjectIconAnnotator.class, position = 10)
public class SpringBootProjectIconAnnotator implements ProjectIconAnnotator {

    @StaticResource
    private static final String BADGE_PATH = "com/github/alexfalappa/nbspringboot/projects/badge.png";    //NOI18N
    private final AtomicReference<Image> badgeCache = new AtomicReference<>();
    private final ChangeSupport cs = new ChangeSupport(this);
    private final Map<Project, Boolean> projectsMap = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public Image annotateIcon(final Project p, Image original, final boolean openedNode) {
        Image annotated = original;
        Boolean type = projectsMap.get(p);
        if (type != null && type == true) {
            final Image badge = getBootBadge();
            if (badge != null) {
                String tooltip = ImageUtilities.getImageToolTip(original);
                if (!tooltip.contains("Boot")) {
                    annotated = ImageUtilities.mergeImages(
                            ImageUtilities.addToolTipToImage(original, "Spring Boot application"),
                            badge, 8, 8);
                }
            }
        } else {
            evaluateProject(p);
        }
        return annotated;
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        cs.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        cs.removeChangeListener(listener);
    }

    public void fireChange(final Project p, boolean type) {
        projectsMap.put(p, type);
        cs.fireChange();
    }

    private Image getBootBadge() {
        Image img = badgeCache.get();
        if (img == null) {
            if (!EventQueue.isDispatchThread()) {
                img = ImageUtilities.loadImage(BADGE_PATH);
                badgeCache.set(img);
            } else {
                final RequestProcessor RP = new RequestProcessor(SpringBootProjectIconAnnotator.class.getName());
                RP.post(() -> {
                    badgeCache.set(ImageUtilities.loadImage(BADGE_PATH));
                    cs.fireChange();
                });
            }
        }
        return img;
    }

    private void evaluateProject(final Project prj) {
        final Runnable runEvaluateProject = () -> {
            boolean flag = isBootProject(prj);
            projectsMap.put(prj, flag);
            if (flag == true) {
                cs.fireChange();
            }
        };
        if (!EventQueue.isDispatchThread()) {
            runEvaluateProject.run();
        } else {
            final RequestProcessor RP = new RequestProcessor(SpringBootProjectIconAnnotator.class.getName());
            RP.post(runEvaluateProject);
        }
    }

    private static boolean isBootProject(final Project prj) {
        if (prj instanceof NbMavenProjectImpl) {
            NbMavenProjectImpl mvnPrj = (NbMavenProjectImpl) prj;
            return Utils.dependencyArtifactIdContains(mvnPrj.getProjectWatcher(), "spring-boot");
        }
        return false;
    }

}
