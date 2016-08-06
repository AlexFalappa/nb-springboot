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
package com.github.alexfalappa.nbspringboot.projects.customizer;

import java.util.Objects;
import java.util.logging.Logger;

import org.netbeans.modules.maven.api.customizer.ModelHandle2;
import org.netbeans.modules.maven.execute.model.ActionToGoalMapping;
import org.netbeans.modules.maven.execute.model.NetbeansActionMapping;
import org.netbeans.spi.project.ActionProvider;

import com.github.alexfalappa.nbspringboot.actions.ReloadAction;

import static com.github.alexfalappa.nbspringboot.actions.ReloadAction.PROP_RUN_ARGS;
import static com.github.alexfalappa.nbspringboot.actions.ReloadAction.TRIGGER_FILE;

/**
 * Customizer panel for maven projects with spring boot devtools dependency.
 *
 * @author Alessandro Falappa
 */
public class BootPanel extends javax.swing.JPanel {

    private static final Logger logger = Logger.getLogger(BootPanel.class.getName());
    private ModelHandle2 mh2;
    private NetbeansActionMapping namRun;
    private NetbeansActionMapping namDebug;

    /** Creates new form BootPanel */
    public BootPanel() {
        initComponents();
    }

    void setModelHandle(ModelHandle2 mh2) {
        Objects.requireNonNull(mh2);
        this.mh2 = mh2;
        ActionToGoalMapping mapps = mh2.getActionMappings();
        for (NetbeansActionMapping map : mapps.getActions()) {
            if (map.getActionName().equals(ActionProvider.COMMAND_RUN)) {
                this.namRun = map;
                chDevtools.setSelected(namRun.getProperties().containsKey(PROP_RUN_ARGS)
                        && namRun.getProperties().get(PROP_RUN_ARGS).contains(TRIGGER_FILE));
            } else if (map.getActionName().equals(ActionProvider.COMMAND_DEBUG)) {
                this.namDebug = map;
            }
        }
        chDevtools.setEnabled(true);
    }

    /** This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lDevtools = new javax.swing.JLabel();
        chDevtools = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(lDevtools, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.lDevtools.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(chDevtools, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.chDevtools.text")); // NOI18N
        chDevtools.setEnabled(false);
        chDevtools.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chDevtoolsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lDevtools)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(chDevtools)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lDevtools)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chDevtools)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void chDevtoolsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chDevtoolsActionPerformed
        final boolean flag = chDevtools.isSelected();
        if (flag) {
            // add command line option to maven run action
            // TODO the run.arguments property may already exist, manage addition/removal of the single argument
            namRun.addProperty("run.arguments", "--spring.devtools.restart.trigger-file=" + ReloadAction.TRIGGER_FILE);
            namDebug.addProperty("run.arguments", "--spring.devtools.restart.trigger-file=" + ReloadAction.TRIGGER_FILE);
            mh2.markAsModified(mh2.getActionMappings());
        } else {
            // remove command line option form maven actions
            // TODO the run.arguments property may already exist, manage addition/removal of the single argument
            namRun.getProperties().remove("run.arguments");
            namDebug.getProperties().remove("run.arguments");
            mh2.markAsModified(mh2.getActionMappings());
        }
    }//GEN-LAST:event_chDevtoolsActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox chDevtools;
    private javax.swing.JLabel lDevtools;
    // End of variables declaration//GEN-END:variables

}
