/*
 * Copyright 2016 Alessandro Falappa <alex.falappa at gmail.com>.
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

import javax.swing.JLabel;
import javax.swing.event.ChangeListener;

import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

/**
 *
 * @author Alessandro Falappa <alex.falappa at gmail.com>
 */
class ErrorWizardPanel implements WizardDescriptor.Panel {

    private JLabel lErrMessage = new JLabel();

    public ErrorWizardPanel() {
    }

    @Override
    public Component getComponent() {
        return lErrMessage;
    }

    @Override
    public HelpCtx getHelp() {
        return new HelpCtx(ErrorWizardPanel.class);
    }

    @Override
    public void readSettings(Object settings) {
    }

    @Override
    public void storeSettings(Object settings) {
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addChangeListener(ChangeListener l) {
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
    }

    void setError(String message) {
        lErrMessage.setText(message);
    }

}
