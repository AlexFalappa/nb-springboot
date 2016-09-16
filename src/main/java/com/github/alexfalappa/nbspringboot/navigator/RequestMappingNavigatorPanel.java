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
package com.github.alexfalappa.nbspringboot.navigator;

import java.awt.BorderLayout;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.netbeans.api.java.source.ui.ElementOpen;
import org.netbeans.spi.navigator.NavigatorPanel;
import org.netbeans.spi.navigator.NavigatorPanel.Registration;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

/**
 * The actual navigator UI.
 *
 * @author Michael J. Simons, 2016-09-14
 */
@Messages({
    "displayName=Request Mappings",
    "displayHint=Displays all @RequestMappings of the current *Controller",
    "resourceUrl=Resource URL",
    "requestMethod=Request Method",
    "handlerMethod=Handler Method"
})
@Registration(mimeType = "text/x-java", displayName = "#displayName")
public class RequestMappingNavigatorPanel implements NavigatorPanel {

    /**
     * holds UI of this panel.
     */
    private final JComponent component;

    private final MappedElementsModel mappedElementsModel;

    private final ElementScanningTaskFactory mappedElementGatheringTaskFactory;

    /**
     * public no arg constructor needed for system to instantiate provider well
     */
    public RequestMappingNavigatorPanel() {
        this.mappedElementsModel = new MappedElementsModel();
        this.mappedElementGatheringTaskFactory = new ElementScanningTaskFactory(mappedElementsModel);
        final JTable table = new JTable(mappedElementsModel);
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        this.component = panel;

        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                final int selectedRow = ((ListSelectionModel) event.getSource()).getMinSelectionIndex();
                if (event.getValueIsAdjusting() || selectedRow < 0) {
                    return;
                }
                final MappedElement mappedElement = mappedElementsModel.getElementAt(selectedRow);
                ElementOpen.open(mappedElement.getFileObject(), mappedElement.getHandle());
                try {
                    final DataObject dataObject = DataObject.find(mappedElement.getFileObject());
                    final EditorCookie editorCookie = dataObject.getLookup().lookup(EditorCookie.class);
                    if (editorCookie != null) {
                        editorCookie.openDocument();
                        JEditorPane[] p = editorCookie.getOpenedPanes();
                        if (p.length > 0) {
                            p[0].requestFocus();
                        }
                    }
                } catch (IOException e) {
                }
            }
        });
    }

    @Override
    public String getDisplayHint() {
        return Bundle.displayHint();
    }

    @Override
    public String getDisplayName() {
        return Bundle.displayName();
    }

    @Override
    public JComponent getComponent() {
        return this.component;
    }

    @Override
    public void panelActivated(Lookup context) {
        this.mappedElementGatheringTaskFactory.activate();
    }

    @Override
    public void panelDeactivated() {
        this.mappedElementGatheringTaskFactory.deactivate();
    }

    @Override
    public Lookup getLookup() {
        return null;
    }
}
