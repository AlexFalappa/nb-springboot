/*
 * Copyright 2020 the original author or authors.
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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.TreeSet;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.netbeans.api.project.Project;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.WindowManager;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Hints;
import org.springframework.boot.configurationmetadata.ValueProvider;

import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;

/**
 * Debug action to dump Spring Boot configuration properties metadata to a CSV file.
 *
 * @author Alessandro Falappa
 */
@ActionID(
        category = "Tools",
        id = "com.github.alexfalappa.nbspringboot.projects.initializr.DumpInitializrMetadataAction"
)
@ActionRegistration(
        displayName = "#CTL_DumpInitializrMetadataAction"
)
@ActionReference(path = "Menu/Tools", position = 1800, separatorBefore = 1750)
@Messages("CTL_DumpInitializrMetadataAction=Dump Initializr Metadata")
public final class DumpInitializrMetadataAction implements ActionListener {

    public DumpInitializrMetadataAction(DataObject context) {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final StatusDisplayer stDisp = StatusDisplayer.getDefault();
        Project prj = Utils.getActiveProject();
        SpringBootService sbs = prj.getLookup().lookup(SpringBootService.class);
        if (sbs != null) {
            try {
                // prepare file chooser
                JFileChooser jfc = new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                jfc.setAcceptAllFileFilterUsed(false);
                jfc.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
                // as for file to save
                if (JFileChooser.APPROVE_OPTION == jfc.showSaveDialog(WindowManager.getDefault().getMainWindow())) {
                    Path path = jfc.getSelectedFile().toPath();
                    if (path != null) {
                        // check filename ends with ".csv"
                        final String fileName = path.getFileName().toString();
                        if (!fileName.endsWith(".csv")) {
                            path = path.getParent().resolve(fileName.concat(".csv"));
                        }
                        // check/ask to overwrite existing file
                        if (Files.exists(path)) {
                            Object ret = DialogDisplayer.getDefault()
                                    .notify(new NotifyDescriptor.Confirmation("OK to overwrite?"));
                            if (!NotifyDescriptor.OK_OPTION.equals(ret)) {
                                return;
                            }
                        }
                        // do actual dump
                        dumpToCsv(sbs, path);
                        stDisp.setStatusText("Metadata dumped");
                    }
                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        } else {
            stDisp.setStatusText("Selected project is not a Spring Boot project");
        }
    }

    private void dumpToCsv(SpringBootService sbs, Path path) {
        try (BufferedWriter bw = Files.newBufferedWriter(path);
                PrintWriter pw = new PrintWriter(bw)) {
            pw.println("Name,DataType,"
                    + "HasKeyHints,KeyProvider,KeyProviderParams,"
                    + "HasValueHints,ValueProvider,ValueProviderParams,"
                    + "Default");
            for (String name : new TreeSet<>(sbs.getPropertyNames())) {
                ConfigurationMetadataProperty cfg = sbs.getPropertyMetadata(name);
                // skip deprecated properties
                if (cfg.isDeprecated()) {
                    continue;
                }
                String dataType = cfg.getType();
                String hasKeyHints = "";
                String hasValueHints = "";
                String keyProvider = "";
                String keyProviderParams = "";
                String valueProvider = "";
                String valueProviderParams = "";
                Hints hints = cfg.getHints();
                if (hints != null) {
                    hasKeyHints = !hints.getKeyHints().isEmpty() ? "T" : "F";
                    if (!hints.getKeyProviders().isEmpty()) {
                        ValueProvider kp = hints.getKeyProviders().get(0);
                        keyProvider = kp.getName();
                        keyProviderParams = kp.getParameters().toString();
                    }
                    hasValueHints = !hints.getValueHints().isEmpty() ? "T" : "F";
                    if (!hints.getValueProviders().isEmpty()) {
                        ValueProvider vp = hints.getValueProviders().get(0);
                        valueProvider = vp.getName();
                        valueProviderParams = vp.getParameters().toString();
                    }
                } else {
                    System.out.format("%s has null hints object%n", name);
                }
                String defaultValue = "";
                final Object def = cfg.getDefaultValue();
                if (def != null) {
                    if (def.getClass().isArray()) {
                        defaultValue = Arrays.toString((Object[]) def);
                    } else {
                        defaultValue = String.valueOf(def);
                    }
                }
                String row = String.format("%s,\"%s\",%s,%s,%s,%s,%s,%s,\"%s\"", name, dataType,
                        hasKeyHints, keyProvider, keyProviderParams,
                        hasValueHints, valueProvider, valueProviderParams,
                        defaultValue);
                pw.println(row);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
