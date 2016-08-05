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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.maven.NbMavenProjectImpl;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

import static com.github.alexfalappa.nbspringboot.projects.customizer.BootPanel.PROP_TRG_ENABLED;
import static com.github.alexfalappa.nbspringboot.projects.customizer.BootPanel.PROP_TRG_FILE;
import static java.lang.Boolean.valueOf;

@ActionID(
        category = "Build",
        id = "com.github.alexfalappa.nbspringboot.actions.ReloadAction"
)
@ActionRegistration(
        iconBase = "com/github/alexfalappa/nbspringboot/actions/springboot-logo.png",
        displayName = "#CTL_ReloadAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/BuildProject", position = 57),
    @ActionReference(path = "Toolbars/Build", position = 500),
    @ActionReference(path = "Shortcuts", name = "DS-L")
})
@Messages("CTL_ReloadAction=S&pring Boot Reload")
public final class ReloadAction implements ActionListener {

    private static final Logger logger = Logger.getLogger(ReloadAction.class.getName());
    private final NbMavenProjectImpl proj;

    public ReloadAction(NbMavenProjectImpl context) {
        this.proj = context;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Preferences prefs = ProjectUtils.getPreferences(proj, ReloadAction.class, true);
        if (prefs != null) {
            boolean enabled = valueOf(prefs.get(PROP_TRG_ENABLED, "false"));
            String strFile = prefs.get(PROP_TRG_FILE, null);
            if (enabled && strFile != null) {
                // TODO seems that the trigger file must be in <proj_dir>/target/classes for devtools to monitor
                File f = new File(strFile);
                try (PrintWriter pw = new PrintWriter(f)) {
                    pw.printf("%1$tF %1$tT", new Date());
                    pw.close();
                    logger.info(String.format("Timestamp written in %s", f.getAbsolutePath()));
                } catch (FileNotFoundException ex) {
                    Exceptions.printStackTrace(ex);
                }
            } else {
                logger.info("Reload disabled!");
            }
        } else {
            logger.warning("No reloading preferences found!");
        }
    }
}
