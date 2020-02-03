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
package com.github.alexfalappa.nbspringboot.cfgprops.filetype;

import java.io.IOException;

import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

import com.github.alexfalappa.nbspringboot.cfgprops.lexer.CfgPropsLanguage;

/**
 * Data Object for Spring Boot configuration properties files.
 *
 * @author Alessandro Falappa
 */
@Messages({
    "MimeTypeDisplayName=Spring Boot Configuration Properties"
})
@MIMEResolver.Registration(
        displayName = "#MimeTypeDisplayName",
        resource = "cfgprops-resolver.xml",
        position = 1000
)
@DataObject.Registration(
        mimeType = CfgPropsLanguage.MIME_TYPE,
        iconBase = "com/github/alexfalappa/nbspringboot/springboot-logo.png",
        displayName = "#MimeTypeDisplayName",
        position = 300
)
@ActionReferences({
    @ActionReference(
            path = "Loaders/text/application+properties/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
            position = 100,
            separatorAfter = 200
    ),
    @ActionReference(
            path = "Loaders/text/application+properties/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CutAction"),
            position = 300
    ),
    @ActionReference(
            path = "Loaders/text/application+properties/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"),
            position = 400,
            separatorAfter = 500
    ),
    @ActionReference(
            path = "Loaders/text/application+properties/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
            position = 600
    ),
    @ActionReference(
            path = "Loaders/text/application+properties/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.RenameAction"),
            position = 700,
            separatorAfter = 800
    ),
    @ActionReference(
            path = "Loaders/text/application+properties/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.SaveAsTemplateAction"),
            position = 900,
            separatorAfter = 1000
    ),
    @ActionReference(
            path = "Loaders/text/application+properties/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
            position = 1100,
            separatorAfter = 1200
    ),
    @ActionReference(
            path = "Loaders/text/application+properties/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
            position = 1300
    ),
    @ActionReference(
            path = "Loaders/text/application+properties/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
            position = 1400
    )
})
public class CfgPropsDataObject extends MultiDataObject {

    public CfgPropsDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        registerEditor(CfgPropsLanguage.MIME_TYPE, true);
    }

    @Override
    protected Node createNodeDelegate() {
        DataNode node = new DataNode(this, Children.LEAF, getLookup());
        return node;
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    @Messages("Source=&Source")
    @MultiViewElement.Registration(
            displayName = "#Source",
            iconBase = "com/github/alexfalappa/nbspringboot/springboot-logo.png",
            persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
            mimeType = CfgPropsLanguage.MIME_TYPE,
            preferredID = "bootcfgprops.source",
            position = 1
    )
    public static MultiViewEditorElement createMultiViewEditorElement(Lookup context) {
        return new MultiViewEditorElement(context);
    }

}
