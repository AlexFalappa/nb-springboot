/*
 * Copyright 2017 Alessandro Falappa.
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BDPTest {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (UnsupportedLookAndFeelException ex) {
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    JFrame frame = new JFrame("dep panel");
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    final BootDependenciesPanel bdp = new BootDependenciesPanel();
                    bdp.init(createMetaData());
                    bdp.adaptToBootVersion("2.0.0.BUILD-SNAPSHOT");
//                    bdp.filter("web");
                    frame.getContentPane().add(new JScrollPane(bdp));
                    frame.pack();
                    frame.setVisible(true);
                } catch (IOException | URISyntaxException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private static JsonNode createMetaData() throws IOException, URISyntaxException {
        ObjectMapper mapper = new ObjectMapper();
        File f = new File(BDPTest.class.getResource("metadata.json").toURI());
        return mapper.readTree(f);
    }

}
