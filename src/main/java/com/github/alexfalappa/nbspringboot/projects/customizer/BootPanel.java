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

import static com.github.alexfalappa.nbspringboot.actions.RestartAction.TRIGGER_FILE;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.WARNING;

/**
 * Customizer panel for maven projects with spring boot dependencies.
 *
 * @author Alessandro Falappa
 */
public class BootPanel extends javax.swing.JPanel {

    public static final String PROP_DEBUG_MODE = "Env.DEBUG";
    public static final String PROP_FORCE_COLOR = "Env.SPRING_OUTPUT_ANSI_ENABLED";
    public static final String PROP_JPDA = "jpda.listen";
    public static final String VMOPTS_OPTIMIZE = "-noverify -XX:TieredStopAtLevel=1";
    public static final String VMOPTS_DEBUG = "-Xdebug -Xrunjdwp:transport=dt_socket,server=n,address=${jpda.address}";
    private static final Logger logger = Logger.getLogger(BootPanel.class.getName());
    private ModelHandle2 mh2;
    private Map<String, String> runProps;
    private Map<String, String> debugProps;
    private boolean active = false;
    private final CfgParamsTableModel tmOverrides = new CfgParamsTableModel();
    private SpringBootService bootService;
    private String propRestart;
    private String propRunArgs;
    private String propRunVmOptions;
    private String propDisablebArgs;

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

    public void init(ModelHandle2 mh2, SpringBootService bootService) {
        // store reference to project properties model and to boot service
        this.mh2 = Objects.requireNonNull(mh2);
        this.bootService = Objects.requireNonNull(bootService);
        // initialize some strings that depends on boot version
        propRestart = String.format("Env.%s", bootService.getRestartEnvVarName());
        propRunVmOptions = String.format("%s.jvmArguments", bootService.getPluginPropsPrefix());
        propRunArgs = String.format("%s.arguments", bootService.getPluginPropsPrefix());
        propDisablebArgs = String.format("%s.disabledArguments", bootService.getPluginPropsPrefix());
        // store reference to properties of maven actions for run/debug
        boolean sbRun = false;
        boolean sbDebug = false;
        ActionToGoalMapping mapps = mh2.getActionMappings();
        for (NetbeansActionMapping map : mapps.getActions()) {
            if (map.getActionName().equals(ActionProvider.COMMAND_RUN)) {
                sbRun = map.getGoals().contains("spring-boot:run");
                this.runProps = map.getProperties();
            } else if (map.getActionName().equals(ActionProvider.COMMAND_DEBUG)) {
                sbDebug = map.getGoals().contains("spring-boot:run");
                this.debugProps = map.getProperties();
            }
        }
        if (runProps == null) {
            logger.warning("No runProps available");
        }
        if (debugProps == null) {
            logger.warning("No debugProps available");
        }
        // if run trough the maven spring boot plugin
        if (sbRun && sbDebug && runProps != null && debugProps != null) {
            // remove 'exec.args' and 'exec.executable' run properties
            runProps.remove("exec.args");
            runProps.remove("exec.executable");
            // ensure debug properties contain 'run.jvmArguments' for debug
            final String debugVmOpts = debugProps.get(propRunVmOptions);
            if (debugVmOpts == null) {
                debugProps.put(propRunVmOptions, VMOPTS_DEBUG);
            } else if (!debugVmOpts.startsWith(VMOPTS_DEBUG)) {
                debugProps.put(propRunVmOptions, String.format("%s %s", VMOPTS_DEBUG, debugVmOpts));
            }
            // ensure debug properties contain 'jpda.listen' prop set to true
            debugProps.put(PROP_JPDA, "true");
            // remove 'exec.args' and 'exec.executable' debug properties
            debugProps.remove("exec.args");
            debugProps.remove("exec.executable");
            // make the widgets reflect the existing cmd line args
            parseCmdLineArgs();
            parseVmOptions();
            chDebugMode.setSelected(runProps.containsKey(PROP_DEBUG_MODE));
            chForceColor.setSelected(runProps.containsKey(PROP_FORCE_COLOR));
            chDevtools.setSelected(runProps.containsKey(propRestart));
            // listen to widget changes
            txArgs.getDocument().addDocumentListener(new DocumentListener() {
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
            });
            txVmOpts.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateVmOptions();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateVmOptions();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateVmOptions();
                }
            });
            tmOverrides.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    updateCmdLineArgs();
                }
            });
            // enable widgets
            lLaunchOpts.setEnabled(true);
            chDebugMode.setEnabled(true);
            chForceColor.setEnabled(true);
            lDevtools.setEnabled(true);
            chDevtools.setEnabled(true);
            lArgs.setEnabled(true);
            txArgs.setEnabled(true);
            lVmOpts.setEnabled(true);
            txVmOpts.setEnabled(true);
            chVmOptsLaunch.setEnabled(true);
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

    public void setDevToolsEnabled(boolean enabled) {
        if (active) {
            lDevtools.setEnabled(enabled);
            chDevtools.setEnabled(enabled);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lDevtools = new javax.swing.JLabel();
        chDevtools = new javax.swing.JCheckBox();
        lLaunchOpts = new javax.swing.JLabel();
        chDebugMode = new javax.swing.JCheckBox();
        chForceColor = new javax.swing.JCheckBox();
        lArgs = new javax.swing.JLabel();
        txArgs = new javax.swing.JTextField();
        lVmOpts = new javax.swing.JLabel();
        txVmOpts = new javax.swing.JTextField();
        chVmOptsLaunch = new javax.swing.JCheckBox();
        lCfgOverrides = new javax.swing.JLabel();
        bAdd = new javax.swing.JButton();
        bDel = new javax.swing.JButton();
        scroller = new javax.swing.JScrollPane();
        tbCfgOverrides = new javax.swing.JTable();
        lWarning = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(lDevtools, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.lDevtools.text")); // NOI18N
        lDevtools.setEnabled(false);

        org.openide.awt.Mnemonics.setLocalizedText(chDevtools, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.chDevtools.text")); // NOI18N
        chDevtools.setEnabled(false);
        chDevtools.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chDevtoolsActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lLaunchOpts, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.lLaunchOpts.text")); // NOI18N
        lLaunchOpts.setEnabled(false);

        org.openide.awt.Mnemonics.setLocalizedText(chDebugMode, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.chDebugMode.text")); // NOI18N
        chDebugMode.setEnabled(false);
        chDebugMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chDebugModeActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(chForceColor, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.chForceColor.text")); // NOI18N
        chForceColor.setEnabled(false);
        chForceColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chForceColorActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lArgs, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.lArgs.text")); // NOI18N
        lArgs.setEnabled(false);

        txArgs.setColumns(15);
        txArgs.setEnabled(false);

        org.openide.awt.Mnemonics.setLocalizedText(lVmOpts, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.lVmOpts.text")); // NOI18N
        lVmOpts.setEnabled(false);

        txVmOpts.setEnabled(false);

        org.openide.awt.Mnemonics.setLocalizedText(chVmOptsLaunch, org.openide.util.NbBundle.getMessage(BootPanel.class, "BootPanel.chVmOptsLaunch.text")); // NOI18N
        chVmOptsLaunch.setEnabled(false);
        chVmOptsLaunch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chVmOptsLaunchActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(lCfgOverrides, org.openide.util.NbBundle.getBundle(BootPanel.class).getString("BootPanel.lCfgOverrides.text")); // NOI18N
        lCfgOverrides.setEnabled(false);

        org.openide.awt.Mnemonics.setLocalizedText(bAdd, "\u002B");
        bAdd.setEnabled(false);
        bAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bAddActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(bDel, "\u2212");
        bDel.setEnabled(false);
        bDel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bDelActionPerformed(evt);
            }
        });

        tbCfgOverrides.setModel(tmOverrides);
        tbCfgOverrides.setEnabled(false);
        tbCfgOverrides.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        scroller.setViewportView(tbCfgOverrides);

        lWarning.setFont(lWarning.getFont().deriveFont(lWarning.getFont().getSize()-2f));
        org.openide.awt.Mnemonics.setLocalizedText(lWarning, org.openide.util.NbBundle.getMessage(BootPanel.class, "BootPanel.lWarning.relaunch.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lDevtools)
                    .addComponent(lVmOpts)
                    .addComponent(lArgs)
                    .addComponent(lLaunchOpts))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txArgs)
                    .addComponent(txVmOpts)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chVmOptsLaunch)
                            .addComponent(chDevtools)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(chDebugMode)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(chForceColor)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
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
                    .addComponent(lDevtools)
                    .addComponent(chDevtools))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lLaunchOpts)
                    .addComponent(chDebugMode)
                    .addComponent(chForceColor))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lArgs)
                    .addComponent(txArgs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lVmOpts)
                    .addComponent(txVmOpts, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chVmOptsLaunch)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lCfgOverrides)
                    .addComponent(bDel)
                    .addComponent(bAdd))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroller, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(lWarning)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void chDevtoolsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chDevtoolsActionPerformed
        if (chDevtools.isSelected()) {
            runProps.put(propRestart, TRIGGER_FILE);
            debugProps.put(propRestart, TRIGGER_FILE);
        } else {
            runProps.remove(propRestart);
            debugProps.remove(propRestart);
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
            debugProps.put(PROP_DEBUG_MODE, "true");
        } else {
            runProps.remove(PROP_DEBUG_MODE);
            debugProps.remove(PROP_DEBUG_MODE);
        }
        mh2.markAsModified(mh2.getActionMappings());
    }//GEN-LAST:event_chDebugModeActionPerformed

    private void chForceColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chForceColorActionPerformed
        if (chForceColor.isSelected()) {
            runProps.put(PROP_FORCE_COLOR, "always");
            debugProps.put(PROP_FORCE_COLOR, "always");
        } else {
            runProps.remove(PROP_FORCE_COLOR);
            debugProps.remove(PROP_FORCE_COLOR);
        }
        mh2.markAsModified(mh2.getActionMappings());
    }//GEN-LAST:event_chForceColorActionPerformed

    private void chVmOptsLaunchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chVmOptsLaunchActionPerformed
        updateVmOptions();
    }//GEN-LAST:event_chVmOptsLaunchActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bAdd;
    private javax.swing.JButton bDel;
    private javax.swing.JCheckBox chDebugMode;
    private javax.swing.JCheckBox chDevtools;
    private javax.swing.JCheckBox chForceColor;
    private javax.swing.JCheckBox chVmOptsLaunch;
    private javax.swing.JLabel lArgs;
    private javax.swing.JLabel lCfgOverrides;
    private javax.swing.JLabel lDevtools;
    private javax.swing.JLabel lLaunchOpts;
    private javax.swing.JLabel lVmOpts;
    private javax.swing.JLabel lWarning;
    private javax.swing.JScrollPane scroller;
    private javax.swing.JTable tbCfgOverrides;
    private javax.swing.JTextField txArgs;
    private javax.swing.JTextField txVmOpts;
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
            runProps.put(propRunArgs, csv);
            debugProps.put(propRunArgs, csv);
        } else {
            runProps.remove(propRunArgs);
            debugProps.remove(propRunArgs);
        }
        if (sbDisabled.length() > 0) {
            final String csv = sbDisabled.toString().trim().replaceAll("\\s+", ",");
            runProps.put(propDisablebArgs, csv);
            debugProps.put(propDisablebArgs, csv);
        } else {
            runProps.remove(propDisablebArgs);
            debugProps.remove(propDisablebArgs);
        }
        mh2.markAsModified(mh2.getActionMappings());
        logger.log(FINER, "Command line args: {0}", runProps.get(propRunArgs));
    }

    private void parseCmdLineArgs() {
        if (runProps.containsKey(propRunArgs) && runProps.get(propRunArgs) != null) {
            StringBuilder sb = new StringBuilder();
            parseProperty(sb, runProps.get(propRunArgs), true);
            txArgs.setText(sb.toString());
        }
        if (runProps.containsKey(propDisablebArgs) && runProps.get(propDisablebArgs) != null) {
            parseProperty(new StringBuilder(), runProps.get(propDisablebArgs), false);
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

    private void updateVmOptions() {
        StringBuilder sb = new StringBuilder();
        if (chVmOptsLaunch.isSelected()) {
            sb.append(VMOPTS_OPTIMIZE).append(' ');
        }
        sb.append(txVmOpts.getText().trim());
        final String strVmOpts = sb.toString();
        if (strVmOpts == null || strVmOpts.isEmpty()) {
            runProps.remove(propRunVmOptions);
            debugProps.put(propRunVmOptions, VMOPTS_DEBUG);
        } else {
            runProps.put(propRunVmOptions, strVmOpts);
            debugProps.put(propRunVmOptions, String.format("%s %s", VMOPTS_DEBUG, strVmOpts));
        }
        mh2.markAsModified(mh2.getActionMappings());
        logger.log(FINER, "VM options: {0}", runProps.get(propRunVmOptions));
    }

    private void parseVmOptions() {
        if (runProps.containsKey(propRunVmOptions) && runProps.get(propRunVmOptions) != null) {
            boolean isVmOptsLaunch = runProps.get(propRunVmOptions).startsWith(VMOPTS_OPTIMIZE);
            chVmOptsLaunch.setSelected(isVmOptsLaunch);
            if (isVmOptsLaunch) {
                txVmOpts.setText(runProps.get(propRunVmOptions).substring(VMOPTS_OPTIMIZE.length()).trim());
            } else {
                txVmOpts.setText(runProps.get(propRunVmOptions));
            }
        }
    }

}
