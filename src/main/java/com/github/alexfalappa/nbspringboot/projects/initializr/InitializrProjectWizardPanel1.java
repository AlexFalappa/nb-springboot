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
package com.github.alexfalappa.nbspringboot.projects.initializr;

import java.awt.Component;
import java.util.HashSet;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

import com.fasterxml.jackson.databind.JsonNode;

import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_METADATA;

/**
 * Panel just asking for basic info.
 */
public class InitializrProjectWizardPanel1 implements WizardDescriptor.Panel, WizardDescriptor.ValidatingPanel, WizardDescriptor.FinishablePanel {

    WizardDescriptor wizardDescriptor;
    private InitializrProjectPanelVisual1 component;
    private final InitializrService initializrService;

    public InitializrProjectWizardPanel1(InitializrService initializrService) {
        this.initializrService = initializrService;
    }

    @Override
    public Component getComponent() {
        if (component == null) {
            component = new InitializrProjectPanelVisual1(this);
            component.setName(NbBundle.getMessage(InitializrProjectWizardPanel1.class, "LBL_BasePropsStep"));
        }
        return component;
    }

    @Override
    public HelpCtx getHelp() {
        return new HelpCtx(InitializrProjectWizardPanel1.class);
    }

    @Override
    public boolean isValid() {
        getComponent();
        return component.valid(wizardDescriptor);
    }

    private final Set<ChangeListener> listeners = new HashSet<>(1); // or can use ChangeSupport in NB 6.0

    @Override
    public final void addChangeListener(ChangeListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    @Override
    public final void removeChangeListener(ChangeListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    protected final void fireChangeEvent() {
        Set<ChangeListener> ls;
        synchronized (listeners) {
            ls = new HashSet<>(listeners);
        }
        ChangeEvent ev = new ChangeEvent(this);
        for (ChangeListener l : ls) {
            l.stateChanged(ev);
        }
    }

    @Override
    public void readSettings(Object settings) {
        wizardDescriptor = (WizardDescriptor) settings;
        component.read(wizardDescriptor);
    }

    @Override
    public void storeSettings(Object settings) {
        WizardDescriptor d = (WizardDescriptor) settings;
        component.store(d);
    }

    @Override
    public boolean isFinishPanel() {
        return false;
    }

    @Override
    public void validate() throws WizardValidationException {
        getComponent();
        component.validate(wizardDescriptor);
    }

    public JsonNode getInitializrMetadata() throws Exception {
        // invoke initializr service to get metadata
        JsonNode metadata = initializrService.getMetadata();
        // store metadata as wizard descriptor property
        this.wizardDescriptor.putProperty(WIZ_METADATA, metadata);
        return metadata;
    }

}
