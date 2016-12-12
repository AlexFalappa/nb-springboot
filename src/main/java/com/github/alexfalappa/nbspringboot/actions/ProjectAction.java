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

import org.netbeans.api.project.Project;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

import com.github.alexfalappa.nbspringboot.api.TestService;

@ActionID(
        category = "Build",
        id = "com.github.alexfalappa.nbspringboot.actions.ProjectAction"
)
@ActionRegistration(
        iconBase = "com/github/alexfalappa/nbspringboot/actions/push.png",
        displayName = "#CTL_ProjectAction"
)
@ActionReference(path = "Toolbars/Build", position = 601)
@Messages("CTL_ProjectAction=Project Action")
public final class ProjectAction implements ActionListener {

    private final Project prj;

    public ProjectAction(Project project) {
        this.prj = project;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        TestService ts = prj.getLookup().lookup(TestService.class);
        NotifyDescriptor.Message message;
        if (ts != null) {
            message = new NotifyDescriptor.Message(String.format("TestService containing '%s'", ts.something()));
            message.setMessageType(NotifyDescriptor.INFORMATION_MESSAGE);
        } else {
            message = new NotifyDescriptor.Message("No TestService in project lookup");
            message.setMessageType(NotifyDescriptor.WARNING_MESSAGE);
        }
        DialogDisplayer.getDefault().notify(message);
    }
}
