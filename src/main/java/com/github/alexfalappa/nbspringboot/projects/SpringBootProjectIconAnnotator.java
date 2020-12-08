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

import com.github.alexfalappa.nbspringboot.Utils;
import java.awt.EventQueue;
import java.awt.Image;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectIconAnnotator;
import org.openide.util.ChangeSupport;
import org.openide.util.ImageUtilities;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;

/**
 * Add badge to project icon of Spring Boot projects.
 * <p>
 * Based on project icon annotator for JavaFX projects in NetBeans:
 * javafx/javafx2.project/src/org/netbeans/modules/javafx2/project/JFXProjectIconAnnotator.java
 *
 * @author Alessandro Falappa
 * @author Hector Espert
 * @author Diego Díez Ricondo
 */
@ServiceProvider(service = ProjectIconAnnotator.class, position = 10)
public class SpringBootProjectIconAnnotator implements ProjectIconAnnotator {

    @StaticResource
    private static final String BOOT_PROJECT_BADGE_PATH = "com/github/alexfalappa/nbspringboot/projects/springboot-badge.png";    //NOI18N
    private static final URL BOOT_PROJECT_BADGE_URL = SpringBootProjectIconAnnotator.class.getClassLoader().getResource(BOOT_PROJECT_BADGE_PATH);
    private static final Image BOOT_PROJECT_BADGE_IMG = ImageUtilities.loadImage(BOOT_PROJECT_BADGE_PATH);
    private static final String BOOT_PROJECT_TOOLTIP_TEXT = "Spring Boot application";
    private static final Logger logger = Logger.getLogger(SpringBootProjectIconAnnotator.class.getName());
    private final ChangeSupport cs = new ChangeSupport(this);
    private final Map<Project, Boolean> projectsMap = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public Image annotateIcon(final Project p, Image original, final boolean openedNode) {
        Image annotated = original;
        Boolean isBootPrj = projectsMap.get(p);
        if (isBootPrj != null) {
            // TODO: once the project is detected as spring-boot is not evaluated anymore until netbeans restart
            if (isBootPrj) {
                logger.log(Level.FINE, "Annotating icon for {0}", p.toString());
                String tooltip = ImageUtilities.getImageToolTip(original);
                if (!tooltip.contains(BOOT_PROJECT_TOOLTIP_TEXT)) {
                    final String htmlMessage = "<img src=\"" //NOI18N
                            + BOOT_PROJECT_BADGE_URL.toExternalForm()
                            + "\">&nbsp;" //NOI18N
                            + BOOT_PROJECT_TOOLTIP_TEXT;
                    annotated = ImageUtilities.mergeImages(
                            ImageUtilities.addToolTipToImage(original, htmlMessage),
                            BOOT_PROJECT_BADGE_IMG,
                            7, 7);
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

    private void evaluateProject(final Project prj) {
        final Runnable runEvaluateProject = () -> {
            logger.log(Level.FINE, "Evaluating icon badge for {0}", prj.toString());
            boolean flag = Utils.isSpringBootProject(prj);
            projectsMap.put(prj, flag);
            if (flag == true) {
                cs.fireChange();
            }
        };
        if (!EventQueue.isDispatchThread()) {
            runEvaluateProject.run();
        } else {
            final RequestProcessor rp = new RequestProcessor(SpringBootProjectIconAnnotator.class.getName());
            rp.post(runEvaluateProject);
        }
    }

}
