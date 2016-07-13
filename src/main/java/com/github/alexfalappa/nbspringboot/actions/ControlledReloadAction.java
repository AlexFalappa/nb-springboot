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

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Build",
        id = "com.github.alexfalappa.nbspringboot.actions.ControlledReloadAction"
)
@ActionRegistration(
        iconBase = "com/github/alexfalappa/nbspringboot/actions/springboot-logo.png",
        displayName = "#CTL_ControlledReloadAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/BuildProject", position = 57),
    @ActionReference(path = "Toolbars/Build", position = 500),
    @ActionReference(path = "Shortcuts", name = "DS-L")
})
@Messages("CTL_ControlledReloadAction=Enable S&pring Boot Trigger")
public final class ControlledReloadAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Controlled Reload!!!");
    }
}
