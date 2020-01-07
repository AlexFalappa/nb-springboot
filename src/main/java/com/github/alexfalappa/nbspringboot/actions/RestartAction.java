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
package com.github.alexfalappa.nbspringboot.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.maven.NbMavenProjectImpl;
import org.netbeans.modules.maven.configurations.M2ConfigProvider;
import org.netbeans.modules.maven.configurations.M2Configuration;
import org.netbeans.modules.maven.execute.model.NetbeansActionMapping;
import org.netbeans.spi.project.ActionProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;

import static java.util.logging.Level.FINE;

/**
 * Project action for restarting Spring Boot application when launched with Dev Tools.
 *
 * @author Alessandro Falappa
 */
@ActionID(
        category = "Build",
        id = "com.github.alexfalappa.nbspringboot.actions.RestartAction"
)
@ActionRegistration(
        iconBase = "com/github/alexfalappa/nbspringboot/actions/springboot-logo.png",
        displayName = "#CTL_RestartAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/BuildProject", position = 57),
    @ActionReference(path = "Toolbars/Build", position = 500),
    @ActionReference(path = "Shortcuts", name = "DS-A")
})
@Messages("CTL_RestartAction=S&pring Boot Restart")
public final class RestartAction implements ActionListener {
    /*
 * NOTE: an alternative way to obtain an action that operates on the currently active project is using
 * ProjectSensitiveActions.projectSensitiveAction(...) from projectuiapi module as indicated by Jan Lahoda on
 * Apache NetBeans devs mailing lists:
 * http://mail-archives.apache.org/mod_mbox/netbeans-dev/201807.mbox/%3CCALd%3D3dEa815Kik-cgZ6Cdymwnqd6FELruJoLesALxJac1SmofA%40mail.gmail.com%3E
     */

    public static final String TRIGGER_FILE = ".nbRestartTrigger";
    private static final Logger logger = Logger.getLogger(RestartAction.class.getName());
    private final DataObject unused;

    public RestartAction(DataObject context) {
        this.unused = context;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final StatusDisplayer stDisp = StatusDisplayer.getDefault();
        Project p = Utils.getActiveProject();
        if (!(p instanceof NbMavenProjectImpl)) {
            String mex = "No maven project to restart!";
            stDisp.setStatusText(mex);
            logger.info(mex);
            return;
        }
        NbMavenProjectImpl proj = (NbMavenProjectImpl) p;
        File outDir = proj.getProjectWatcher().getOutputDirectory(false);
        M2ConfigProvider cp = proj.getLookup().lookup(M2ConfigProvider.class);
        SpringBootService sbs = proj.getLookup().lookup(SpringBootService.class);
        if (cp != null && sbs != null) {
            boolean enabled = sbs.hasPomDependency("spring-boot-devtools");
            M2Configuration m2 = cp.getActiveConfiguration();
            List<NetbeansActionMapping> nams = m2.getRawMappings().getActions();
            if (enabled && !nams.isEmpty()) {
                String propRestart = String.format("Env.%s", sbs.getRestartEnvVarName());
                for (NetbeansActionMapping nam : nams) {
                    if (nam.getActionName().equals(ActionProvider.COMMAND_RUN)) {
                        enabled = nam.getProperties().containsKey(propRestart);
                        break;
                    }
                }
            }
            ProjectInformation prjInfo = ProjectUtils.getInformation(proj);
            StringBuilder sb = new StringBuilder("Project [").append(prjInfo.getDisplayName()).append("]: ");
            if (enabled) {
                File f = new File(outDir, TRIGGER_FILE);
                if (outDir.exists()) {
                    try (PrintWriter pw = new PrintWriter(f)) {
                        pw.printf("%1$tF %1$tT", new Date());
                        pw.close();
                        sb.append("Spring Boot application restart triggered");
                        stDisp.setStatusText(sb.toString());
                        logger.info(sb.toString());
                        logger.log(FINE, "Timestamp written in {0}", f.getAbsolutePath());
                    } catch (FileNotFoundException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                } else {
                    sb.append("No output directory found! Build the project.");
                    stDisp.setStatusText(sb.toString());
                    logger.warning(sb.toString());
                }
            } else {
                sb.append("Application restart disabled or not applicable!");
                stDisp.setStatusText(sb.toString());
                logger.info(sb.toString());
            }
        } else {
            logger.warning("No application restart preferences found!");
        }
    }
}
