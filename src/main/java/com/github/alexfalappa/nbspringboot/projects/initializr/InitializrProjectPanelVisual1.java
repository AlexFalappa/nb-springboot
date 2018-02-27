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

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Objects;
import java.util.logging.Level;

import javax.lang.model.SourceVersion;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.AsyncGUIJob;
import org.openide.util.Exceptions;

import com.fasterxml.jackson.databind.JsonNode;

import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_ARTIFACT;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_DESCRIPTION;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_GROUP;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_JAVA_VERSION;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_LANGUAGE;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_NAME;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_PACKAGE;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_PACKAGING;
import static com.github.alexfalappa.nbspringboot.projects.initializr.InitializrProjectProps.WIZ_VERSION;

public class InitializrProjectPanelVisual1 extends JPanel implements DocumentListener, AsyncGUIJob {

    public static final String PROP_PROJECT_NAME = "projectName";
    private final DefaultComboBoxModel<NamedItem> dcbmLanguage = new DefaultComboBoxModel<>();
    private final DefaultComboBoxModel<NamedItem> dcbmJavaVersion = new DefaultComboBoxModel<>();
    private final DefaultComboBoxModel<NamedItem> dcbmPackaging = new DefaultComboBoxModel<>();
    private final InitializrProjectWizardPanel1 panel;
    private JsonNode meta;
    private boolean initialized = false;
    private boolean failed = false;
    private boolean linkedArtifactName = true;
    private boolean linkedGroupArtifactPackage = true;
    private FocusListener selectAllFocusListener = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            if (e.getSource() instanceof JTextField) {
                JTextField tf = (JTextField) e.getSource();
                tf.selectAll();
            }
        }
    };

    public InitializrProjectPanelVisual1(InitializrProjectWizardPanel1 panel) {
        initComponents();
        this.panel = panel;
        txGroup.addFocusListener(selectAllFocusListener);
        txArtifact.addFocusListener(selectAllFocusListener);
        txVersion.addFocusListener(selectAllFocusListener);
        txName.addFocusListener(selectAllFocusListener);
        txDesc.addFocusListener(selectAllFocusListener);
        txPackage.addFocusListener(selectAllFocusListener);
        // unidirectionally link edits of group textfield with package textfield
        addChangeListener(txGroup, new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (linkedGroupArtifactPackage) {
                    String grp = txGroup.getText();
                    String artf = txArtifact.getText();
                    String pkg = String.format("%s.%s", grp, artf);
                    txPackage.setText(pkg);
                }
            }
        });
        // unidirectionally link edits of artifact textfield with name and package textfields
        addChangeListener(txArtifact, new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                String grp = txGroup.getText();
                String artf = txArtifact.getText();
                if (linkedArtifactName) {
                    txName.setText(artf);
                }
                if (linkedGroupArtifactPackage) {
                    String pkg = String.format("%s.%s", grp, artf);
                    txPackage.setText(pkg);
                }
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lGroup = new javax.swing.JLabel();
        lArtifact = new javax.swing.JLabel();
        lName = new javax.swing.JLabel();
        lDesc = new javax.swing.JLabel();
        lPackage = new javax.swing.JLabel();
        lPackaging = new javax.swing.JLabel();
        lJavaVersion = new javax.swing.JLabel();
        lLanguage = new javax.swing.JLabel();
        txGroup = new javax.swing.JTextField();
        txArtifact = new javax.swing.JTextField();
        txName = new javax.swing.JTextField();
        txDesc = new javax.swing.JTextField();
        txPackage = new javax.swing.JTextField();
        cbPackaging = new javax.swing.JComboBox<>();
        cbJavaVersion = new javax.swing.JComboBox<>();
        cbLanguage = new javax.swing.JComboBox<>();
        lVersion = new javax.swing.JLabel();
        txVersion = new javax.swing.JTextField();

        lGroup.setLabelFor(txGroup);
        org.openide.awt.Mnemonics.setLocalizedText(lGroup, org.openide.util.NbBundle.getMessage(InitializrProjectPanelVisual1.class, "InitializrProjectPanelVisual1.lGroup.text")); // NOI18N

        lArtifact.setLabelFor(txArtifact);
        org.openide.awt.Mnemonics.setLocalizedText(lArtifact, org.openide.util.NbBundle.getMessage(InitializrProjectPanelVisual1.class, "InitializrProjectPanelVisual1.lArtifact.text")); // NOI18N

        lName.setLabelFor(txName);
        org.openide.awt.Mnemonics.setLocalizedText(lName, org.openide.util.NbBundle.getMessage(InitializrProjectPanelVisual1.class, "InitializrProjectPanelVisual1.lName.text")); // NOI18N

        lDesc.setLabelFor(txName);
        org.openide.awt.Mnemonics.setLocalizedText(lDesc, org.openide.util.NbBundle.getMessage(InitializrProjectPanelVisual1.class, "InitializrProjectPanelVisual1.lDesc.text")); // NOI18N

        lPackage.setLabelFor(txName);
        org.openide.awt.Mnemonics.setLocalizedText(lPackage, org.openide.util.NbBundle.getMessage(InitializrProjectPanelVisual1.class, "InitializrProjectPanelVisual1.lPackage.text")); // NOI18N

        lPackaging.setLabelFor(txName);
        org.openide.awt.Mnemonics.setLocalizedText(lPackaging, org.openide.util.NbBundle.getMessage(InitializrProjectPanelVisual1.class, "InitializrProjectPanelVisual1.lPackaging.text")); // NOI18N

        lJavaVersion.setLabelFor(txName);
        org.openide.awt.Mnemonics.setLocalizedText(lJavaVersion, org.openide.util.NbBundle.getMessage(InitializrProjectPanelVisual1.class, "InitializrProjectPanelVisual1.lJavaVersion.text")); // NOI18N

        lLanguage.setLabelFor(txName);
        org.openide.awt.Mnemonics.setLocalizedText(lLanguage, org.openide.util.NbBundle.getMessage(InitializrProjectPanelVisual1.class, "InitializrProjectPanelVisual1.lLanguage.text")); // NOI18N

        txGroup.setColumns(20);
        txGroup.setEnabled(false);

        txArtifact.setColumns(20);
        txArtifact.setEnabled(false);

        txName.setColumns(20);
        txName.setEnabled(false);
        txName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txNameKeyTyped(evt);
            }
        });

        txDesc.setColumns(20);
        txDesc.setEnabled(false);

        txPackage.setColumns(20);
        txPackage.setEnabled(false);
        txPackage.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txPackageKeyTyped(evt);
            }
        });

        cbPackaging.setEnabled(false);

        cbJavaVersion.setEnabled(false);

        cbLanguage.setEnabled(false);

        lVersion.setLabelFor(txName);
        org.openide.awt.Mnemonics.setLocalizedText(lVersion, org.openide.util.NbBundle.getMessage(InitializrProjectPanelVisual1.class, "InitializrProjectPanelVisual1.lVersion.text")); // NOI18N

        txVersion.setColumns(20);
        txVersion.setEnabled(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lLanguage)
                    .addComponent(lGroup)
                    .addComponent(lArtifact)
                    .addComponent(lName)
                    .addComponent(lDesc)
                    .addComponent(lPackage)
                    .addComponent(lPackaging)
                    .addComponent(lVersion)
                    .addComponent(lJavaVersion))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txGroup)
                    .addComponent(txArtifact)
                    .addComponent(txName)
                    .addComponent(txDesc)
                    .addComponent(txPackage)
                    .addComponent(cbPackaging, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbJavaVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbLanguage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txVersion))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lGroup)
                    .addComponent(txGroup, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lArtifact)
                    .addComponent(txArtifact, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lVersion)
                    .addComponent(txVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lPackaging)
                    .addComponent(cbPackaging, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lName)
                    .addComponent(txName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lDesc)
                    .addComponent(txDesc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lPackage)
                    .addComponent(txPackage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lLanguage)
                    .addComponent(cbLanguage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lJavaVersion)
                    .addComponent(cbJavaVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txNameKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txNameKeyTyped
        linkedArtifactName = false;
    }//GEN-LAST:event_txNameKeyTyped

    private void txPackageKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txPackageKeyTyped
        linkedGroupArtifactPackage = false;
    }//GEN-LAST:event_txPackageKeyTyped

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> cbJavaVersion;
    private javax.swing.JComboBox<String> cbLanguage;
    private javax.swing.JComboBox<String> cbPackaging;
    private javax.swing.JLabel lArtifact;
    private javax.swing.JLabel lDesc;
    private javax.swing.JLabel lGroup;
    private javax.swing.JLabel lJavaVersion;
    private javax.swing.JLabel lLanguage;
    private javax.swing.JLabel lName;
    private javax.swing.JLabel lPackage;
    private javax.swing.JLabel lPackaging;
    private javax.swing.JLabel lVersion;
    private javax.swing.JTextField txArtifact;
    private javax.swing.JTextField txDesc;
    private javax.swing.JTextField txGroup;
    private javax.swing.JTextField txName;
    private javax.swing.JTextField txPackage;
    private javax.swing.JTextField txVersion;
    // End of variables declaration//GEN-END:variables

    boolean valid(WizardDescriptor wizardDescriptor) {
        if (!initialized) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_INFO_MESSAGE, "Contacting service...");
            return false;
        }
        if (failed) {
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, "Problems in contacting service!");
            return false;
        }
        if (txGroup.getText().isEmpty()) {
            //Empty group
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, "Group can't be empty.");
            return false;
        }
        if (txArtifact.getText().isEmpty()) {
            //Empty artifact
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, "Artifact can't be empty.");
            return false;
        }
        if (txVersion.getText().isEmpty()) {
            //Empty version
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, "Version can't be empty.");
            return false;
        }
        if (txName.getText().isEmpty()) {
            //Empty name
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, "Name can't be empty.");
            return false;
        }
        if (!SourceVersion.isName(txPackage.getText())) {
            //Invalid package name
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, "Package Name is not a valid Java package name.");
            return false;
        }
        wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, "");
        return true;
    }

    void store(WizardDescriptor wd) {
        wd.putProperty(WIZ_GROUP, txGroup.getText().trim());
        wd.putProperty(WIZ_ARTIFACT, txArtifact.getText().trim());
        wd.putProperty(WIZ_VERSION, txVersion.getText().trim());
        wd.putProperty(WIZ_NAME, txName.getText().trim());
        wd.putProperty(WIZ_DESCRIPTION, txDesc.getText().trim());
        wd.putProperty(WIZ_PACKAGE, txPackage.getText().trim());
        wd.putProperty(WIZ_JAVA_VERSION, cbJavaVersion.getSelectedItem());
        wd.putProperty(WIZ_LANGUAGE, cbLanguage.getSelectedItem());
        wd.putProperty(WIZ_PACKAGING, cbPackaging.getSelectedItem());
    }

    void read(WizardDescriptor wd) {
        if (initialized) {
            this.txGroup.setText((String) wd.getProperty(WIZ_GROUP));
            this.txArtifact.setText((String) wd.getProperty(WIZ_ARTIFACT));
            this.txVersion.setText((String) wd.getProperty(WIZ_VERSION));
            this.txName.setText((String) wd.getProperty(WIZ_NAME));
            this.txDesc.setText((String) wd.getProperty(WIZ_DESCRIPTION));
            this.txPackage.setText((String) wd.getProperty(WIZ_PACKAGE));
            cbJavaVersion.setSelectedItem(wd.getProperty(WIZ_JAVA_VERSION));
            cbLanguage.setSelectedItem(wd.getProperty(WIZ_LANGUAGE));
            cbPackaging.setSelectedItem(wd.getProperty(WIZ_PACKAGING));
        }
    }

    private void fillCombo(JsonNode attrNode, DefaultComboBoxModel<NamedItem> comboModel, JComboBox combo) {
        JsonNode valArray = attrNode.path("values");
        comboModel.removeAllElements();
        for (JsonNode val : valArray) {
            comboModel.addElement(new NamedItem(val.get("id").asText(), val.get("name").asText()));
        }
        combo.setModel(comboModel);
        combo.setSelectedItem(new NamedItem(attrNode.path("default").asText(), ""));
    }

    void validate(WizardDescriptor d) throws WizardValidationException {
        // nothing to validate
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        panel.fireChangeEvent();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        panel.fireChangeEvent();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        panel.fireChangeEvent();
    }

    @Override
    public void construct() {
        try {
            meta = panel.getInitializrMetadata();
            initialized = true;
        } catch (Exception ex) {
            panel.wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, "Could not query Initializr service");
            Exceptions.printStackTrace(Exceptions.attachSeverity(ex, Level.WARNING));
            failed = true;
            panel.fireChangeEvent();
        }
    }

    @Override
    public void finished() {
        if (initialized) {
            // fill fields
            txGroup.setText(meta.path("groupId").path("default").asText());
            txArtifact.setText(meta.path("artifactId").path("default").asText());
            txVersion.setText(meta.path("version").path("default").asText());
            txName.setText(meta.path("name").path("default").asText());
            txDesc.setText(meta.path("description").path("default").asText());
            txPackage.setText(meta.path("packageName").path("default").asText());
            fillCombo(meta.path("javaVersion"), dcbmJavaVersion, cbJavaVersion);
            fillCombo(meta.path("language"), dcbmLanguage, cbLanguage);
            fillCombo(meta.path("packaging"), dcbmPackaging, cbPackaging);
            // add listeners for validation
            txGroup.getDocument().addDocumentListener(this);
            txArtifact.getDocument().addDocumentListener(this);
            txVersion.getDocument().addDocumentListener(this);
            txName.getDocument().addDocumentListener(this);
            txPackage.getDocument().addDocumentListener(this);
            // enable fields
            txGroup.setEnabled(true);
            txArtifact.setEnabled(true);
            txName.setEnabled(true);
            txDesc.setEnabled(true);
            txPackage.setEnabled(true);
            cbPackaging.setEnabled(true);
            cbJavaVersion.setEnabled(true);
            cbLanguage.setEnabled(true);
            txVersion.setEnabled(true);
            panel.fireChangeEvent();
            // focus on group textfield
            txGroup.requestFocus();
        }
    }

    private static void addChangeListener(final JTextComponent text, final ChangeListener changeListener) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(changeListener);
        // TODO add event coalescing below as editing a selected textfield causes a remove all followed by insert
        DocumentListener dl = new DocumentListener() {
            private int lastChange = 0, lastNotifiedChange = 0;

            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                lastChange++;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (lastNotifiedChange != lastChange) {
                            lastNotifiedChange = lastChange;
                            changeListener.stateChanged(new ChangeEvent(text));
                        }
                    }
                });
            }
        };
        Document d = text.getDocument();
        if (d != null) {
            d.addDocumentListener(dl);
        }
    }
}
