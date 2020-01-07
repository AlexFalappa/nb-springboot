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
package com.github.alexfalappa.nbspringboot.templates.controller;

import javax.swing.event.ChangeListener;

import org.openide.WizardDescriptor;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;

/**
 * Wizard descriptor for new RestController class file wizard.
 *
 * @author Alessandro Falappa
 */
public class RestControllerWizardPanel1 implements WizardDescriptor.Panel<WizardDescriptor> {

    private RestControllerVisualPanel1 component;

    // Get the visual component for the panel. In this template, the component
    // is kept separate. This can be more efficient: if the wizard is created
    // but never displayed, or not all panels are displayed, it is better to
    // create only those which really need to be visible.
    @Override
    public RestControllerVisualPanel1 getComponent() {
        if (component == null) {
            component = new RestControllerVisualPanel1(this);
        }
        return component;
    }

    @Override
    public HelpCtx getHelp() {
        // Show no Help button for this panel:
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    private final ChangeSupport chgSupport = new ChangeSupport(this);

    @Override
    public final void addChangeListener(ChangeListener l) {
        synchronized (chgSupport) {
            chgSupport.addChangeListener(l);
        }
    }

    @Override
    public final void removeChangeListener(ChangeListener l) {
        synchronized (chgSupport) {
            chgSupport.removeChangeListener(l);
        }
    }

    protected final void fireChangeEvent() {
        chgSupport.fireChange();
    }

    @Override
    public void readSettings(WizardDescriptor wiz) {
        getComponent().read(wiz);
    }

    @Override
    public void storeSettings(WizardDescriptor wiz) {
        getComponent().store(wiz);
    }

}
