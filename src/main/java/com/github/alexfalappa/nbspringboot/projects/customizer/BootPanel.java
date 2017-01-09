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

import java.awt.Container;
import java.awt.Dialog;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

import org.netbeans.modules.maven.api.customizer.ModelHandle2;
import org.netbeans.modules.maven.execute.model.ActionToGoalMapping;
import org.netbeans.modules.maven.execute.model.NetbeansActionMapping;
import org.netbeans.spi.project.ActionProvider;
import org.openide.util.NbBundle;

import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;

import static com.github.alexfalappa.nbspringboot.actions.RestartAction.PROP_RESTART;
import static com.github.alexfalappa.nbspringboot.actions.RestartAction.TRIGGER_FILE;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.WARNING;

/**
 * Customizer panel for maven projects with spring boot dependencies.
 *
 * @author Alessandro Falappa
 */
public class BootPanel extends javax.swing.JPanel implements DocumentListener {

    public static final String PROP_RUN_ARGS = "run.arguments";
    public static final String PROP_DISABLED_OVERRIDES = "disabled.overrides";
    public static final String PROP_DEBUG_MODE = "Env.DEBUG";
    private static final Logger logger = Logger.getLogger(BootPanel.class.getName());
    private ModelHandle2 mh2;
    private Map<String, String> runProps;
    private Map<String, String> debugProps;
    private boolean active = false;
    private final CfgParamsTableModel tmOverrides = new CfgParamsTableModel();
    private SpringBootService bootService;

    /** Creates new form BootPanel */
    public BootPanel() {
        initComponents();
        // adjust and fix size of first column
        final TableColumn firstCol = tbCfgOverrides.getColumnModel().getColumn(0);
        firstCol.setMaxWidth(30);
        firstCol.setResizable(false);
        // disable column reordering
        tbCfgOverrides.getTableHeader().setReorderingAllowed(false);
    }

    public void setSpringBootService(SpringBootService sbs) {
        this.bootService = sbs;
    }

    public void setDevToolsEnabled(boolean enabled) {
        if (active) {
            lDevtools.setEnabled(enabled);
            chDevtools.setEnabled(enabled);
        }
    }

    public void setModelHandle(ModelHandle2 mh2) {
        Objects.requireNonNull(mh2);
        // store reference to project properties model and to properties of maven actions for run/debug
        this.mh2 = mh2;
        boolean sbRun = false;
        ActionToGoalMapping mapps = mh2.getActionMappings();
        for (NetbeansActionMapping map : mapps.getActions()) {
            if (map.getActionName().equals(ActionProvider.COMMAND_RUN)) {
                sbRun = map.getGoals().contains("spring-boot:run");
                this.runProps = map.getProperties();
            } else if (map.getActionName().equals(ActionProvider.COMMAND_DEBUG)) {
                this.debugProps = map.getProperties();
            }
        }
        // if run trough the maven spring boot plugin
        if (sbRun) {
            // make the widgets reflect the existing cmd line args
            parseCmdLineArgs();
            chDebugMode.setSelected(runProps.containsKey(PROP_DEBUG_MODE));
            chDevtools.setSelected(runProps.containsKey(PROP_RESTART));
            // listen to widget changes
            txArgs.getDocument().addDocumentListener(this);
            tmOverrides.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    updateCmdLineArgs();
                }
            });
            // enable widgets
            lDebugMode.setEnabled(true);
            chDebugMode.setEnabled(true);
            lDevtools.setEnabled(true);
            chDevtools.setEnabled(true);
            lArgs.setEnabled(true);
            txArgs.setEnabled(true);
            lCfgOverrides.setEnabled(true);
            bAdd.setEnabled(true);
            bDel.setEnabled(true);
            tbCfgOverrides.setEnabled(true);
            // turn on active flag
            active = true;
        } else {
            lWarning.setText(NbBundle.getMessage(BootPanel.class, "BootPanel.lWarning.panelinactive.text")); // NOI18N
        }
    }

    // implementation of DocumentListener interface
    @Override
    public void insertUpdate(DocumentEvent e) {
        updateCmdLineArgs();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        updateCmdLineArgs();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        updateCmdLineArgs();
    }

    /** This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lDevtools = new javax.swing.JLabel();
        chDevtools = new javax.swing.JCheckBox();
        lArgs = new javax.swing.JLabel();
        txArgs = new javax.swing.JTextField();
        lWarning = new javax.swing.JLabel();
        lCfgOverrides = new javax.swing.JLabel();
        scroller = new javax.swing.JScrollPane();
        tbCfgOverrides = new javax.swing.JTable();
        bDel = new javax.swing.JButton();
        bAdd = new javax.swing.JButton();
        lDebugMode = new javax.swing.JLabel();
        chDebugMode = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(lDevtools, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.lDevtools.text")); // NOI18N
        lDevtools.setEnabled(false);

        org.openide.awt.Mnemonics.setLocalizedText(chDevtools, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.chDevtools.text")); // NOI18N
        chDevtools.setEnabled(false);
        chDevtools.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chDevtoolsActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lArgs, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.lArgs.text")); // NOI18N
        lArgs.setEnabled(false);

        txArgs.setColumns(15);
        txArgs.setEnabled(false);

        lWarning.setFont(lWarning.getFont().deriveFont(lWarning.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(lWarning, org.openide.util.NbBundle.getMessage(BootPanel.class, "BootPanel.lWarning.relaunch.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lCfgOverrides, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.lCfgOverrides.text")); // NOI18N
        lCfgOverrides.setEnabled(false);

        tbCfgOverrides.setModel(tmOverrides);
        tbCfgOverrides.setEnabled(false);
        tbCfgOverrides.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        scroller.setViewportView(tbCfgOverrides);

        org.openide.awt.Mnemonics.setLocalizedText(bDel, "\u2212");
        bDel.setEnabled(false);
        bDel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bDelActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(bAdd, "\u002B");
        bAdd.setEnabled(false);
        bAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bAddActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lDebugMode, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.lDebugMode.text")); // NOI18N
        lDebugMode.setEnabled(false);

        org.openide.awt.Mnemonics.setLocalizedText(chDebugMode, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.chDebugMode.text")); // NOI18N
        chDebugMode.setEnabled(false);
        chDebugMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chDebugModeActionPerformed(evt);
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
                    .addComponent(lDebugMode)
                    .addComponent(lArgs))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txArgs)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chDevtools)
                            .addComponent(chDebugMode))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(lCfgOverrides)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(bAdd)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bDel)
                .addGap(6, 6, 6))
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(lWarning, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(6, 6, 6))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scroller)
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {bAdd, bDel});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lDebugMode)
                    .addComponent(chDebugMode))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lDevtools)
                    .addComponent(chDevtools))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lArgs)
                    .addComponent(txArgs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lCfgOverrides)
                    .addComponent(bDel)
                    .addComponent(bAdd))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroller, javax.swing.GroupLayout.DEFAULT_SIZE, 161, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(lWarning)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void chDevtoolsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chDevtoolsActionPerformed
        if (chDevtools.isSelected()) {
            runProps.put(PROP_RESTART, TRIGGER_FILE);
            debugProps.put(PROP_RESTART, TRIGGER_FILE);
        } else {
            runProps.remove(PROP_RESTART);
            debugProps.remove(PROP_RESTART);
        }
        mh2.markAsModified(mh2.getActionMappings());
    }//GEN-LAST:event_chDevtoolsActionPerformed

    private void bAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bAddActionPerformed
        Container parentDialog = SwingUtilities.getAncestorOfClass(Dialog.class, this);
        CfgPropsDialog dialog = new CfgPropsDialog((Dialog) parentDialog);
        dialog.loadCfgProps(bootService);
        dialog.setLocationRelativeTo(scroller);
        dialog.setVisible(true);
        if (dialog.okPressed()) {
            CfgOverride override = new CfgOverride();
            override.enabled = true;
            override.name = dialog.getSelectedPropName();
            tmOverrides.addOverride(override);
            final int addedIndex = tbCfgOverrides.getRowCount() - 1;
            tbCfgOverrides.changeSelection(addedIndex, 2, false, false);
            tbCfgOverrides.requestFocus();
        }
    }//GEN-LAST:event_bAddActionPerformed

    private void bDelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bDelActionPerformed
        final int selRow = tbCfgOverrides.getSelectedRow();
        if (selRow >= 0) {
            tmOverrides.removeOverride(selRow);
            if (selRow < tmOverrides.getRowCount()) {
                tbCfgOverrides.setRowSelectionInterval(selRow, selRow);
            }
        }
    }//GEN-LAST:event_bDelActionPerformed

    private void chDebugModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chDebugModeActionPerformed
        if (chDebugMode.isSelected()) {
            runProps.put(PROP_DEBUG_MODE, "true");
            debugProps.put(PROP_RESTART, "true");
        } else {
            runProps.remove(PROP_RESTART);
            debugProps.remove(PROP_RESTART);
        }
        mh2.markAsModified(mh2.getActionMappings());
    }//GEN-LAST:event_chDebugModeActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bAdd;
    private javax.swing.JButton bDel;
    private javax.swing.JCheckBox chDebugMode;
    private javax.swing.JCheckBox chDevtools;
    private javax.swing.JLabel lArgs;
    private javax.swing.JLabel lCfgOverrides;
    private javax.swing.JLabel lDebugMode;
    private javax.swing.JLabel lDevtools;
    private javax.swing.JLabel lWarning;
    private javax.swing.JScrollPane scroller;
    private javax.swing.JTable tbCfgOverrides;
    private javax.swing.JTextField txArgs;
    // End of variables declaration//GEN-END:variables

    private void updateCmdLineArgs() {
        final StringBuilder sbEnabled = new StringBuilder(txArgs.getText());
        final StringBuilder sbDisabled = new StringBuilder();
        for (CfgOverride ovr : tmOverrides.getOverrides()) {
            if (ovr.enabled) {
                sbEnabled.append(" --").append(ovr.name);
                if (!ovr.value.isEmpty()) {
                    sbEnabled.append('=').append(ovr.value);
                }
            } else {
                sbDisabled.append(" --").append(ovr.name);
                if (!ovr.value.isEmpty()) {
                    sbDisabled.append('=').append(ovr.value);
                }
            }
        }
        if (sbEnabled.length() > 0) {
            final String csv = sbEnabled.toString().trim().replaceAll("\\s+", ",");
            runProps.put(PROP_RUN_ARGS, csv);
            debugProps.put(PROP_RUN_ARGS, csv);
        } else {
            runProps.remove(PROP_RUN_ARGS);
            debugProps.remove(PROP_RUN_ARGS);
        }
        if (sbDisabled.length() > 0) {
            final String csv = sbDisabled.toString().trim().replaceAll("\\s+", ",");
            runProps.put(PROP_DISABLED_OVERRIDES, csv);
            debugProps.put(PROP_DISABLED_OVERRIDES, csv);
        } else {
            runProps.remove(PROP_DISABLED_OVERRIDES);
            debugProps.remove(PROP_DISABLED_OVERRIDES);
        }
        mh2.markAsModified(mh2.getActionMappings());
        logger.log(FINER, "Command line args: {0}", runProps.get(PROP_RUN_ARGS));
    }

    private void parseCmdLineArgs() {
        if (runProps.containsKey(PROP_RUN_ARGS) && runProps.get(PROP_RUN_ARGS) != null) {
            StringBuilder sb = new StringBuilder();
            parseProperty(sb, runProps.get(PROP_RUN_ARGS), true);
            txArgs.setText(sb.toString());
        }
        if (runProps.containsKey(PROP_DISABLED_OVERRIDES) && runProps.get(PROP_DISABLED_OVERRIDES) != null) {
            parseProperty(new StringBuilder(), runProps.get(PROP_DISABLED_OVERRIDES), false);
        }
    }

    private void parseProperty(StringBuilder sb, String prop, boolean enabled) {
        logger.log(FINE, "Parsing project property: {0}", prop);
        for (String arg : prop.split(",")) {
            if (arg.startsWith("--")) {
                // configuration properties override
                String[] parts = arg.substring(2).split("=");
                CfgOverride ovr = new CfgOverride();
                ovr.enabled = enabled;
                switch (parts.length) {
                    case 2:
                        ovr.value = parts[1];
                    case 1:
                        ovr.name = parts[0];
                        tmOverrides.addOverride(ovr);
                        logger.log(FINER, "Overridden cfg property: {0}", ovr);
                        break;
                    default:
                        logger.log(WARNING, "Couldn't reparse command line argument: {0}", arg);
                }
            } else {
                // other command line arg
                sb.append(arg).append(' ');
                logger.log(FINE, "Command line arg: {0}", arg);
            }
        }
    }

}
