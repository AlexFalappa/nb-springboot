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
package com.github.alexfalappa.nbspringboot.actions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.logging.Logger;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.openide.windows.GlobalActionContextImpl;
import org.openide.explorer.ExplorerManager;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.ContextGlobalProvider;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Result;
import org.openide.util.Lookup.Template;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.SEVERE;

/**
 * This class proxies the original ContextGlobalProvider and ensures the current project remains in the GlobalContext regardless of the
 * TopComponent selection. The class also ensures that when a child node is selected within the in Projects tab, the parent Project will be
 * in the lookup.
 * <p>
 * To use this class you must have an implementation dependency on org.openide.windows module.
 * <p>
 * Taken from http://wiki.netbeans.org/DevFaqAddGlobalContext
 *
 * @see ContextGlobalProvider
 * @see GlobalActionContextImpl
 * @author Bruce Schubert
 */
//@ServiceProvider(service = ContextGlobalProvider.class, supersedes = "org.netbeans.modules.openide.windows.GlobalActionContextImpl")
public class GlobalActionContextProxy implements ContextGlobalProvider {

    /** The native NetBeans global context Lookup provider */
    private GlobalActionContextImpl globalContextProvider = null;
    /** Additional content for our proxy lookup */
    private InstanceContent content;
    /** The primary lookup managed by the platform */
    private Lookup globalContextLookup;
    /** The project lookup managed by resultChanged */
    private Lookup projectLookup;
    /** The actual proxyLookup returned by this class */
    private Lookup proxyLookup;
    /** A lookup result that we listen to for Projects */
    private Result<Project> resultProjects;
    /** Listener for changes resultProjects */
    private final LookupListener resultListener = new LookupListenerImpl();
    /** Listener for changes on the TopComponent registry */
    private final PropertyChangeListener registryListener = new RegistryPropertyChangeListener();
    /** The last project selected */
    private Project lastProject;
    /** Critical section lock */
    private final Object lock = new Object();
    private static final Logger logger = Logger.getLogger(GlobalActionContextProxy.class.getName());
    public static final String PROJECT_LOGICAL_TAB_ID = "projectTabLogical_tc";
    public static final String PROJECT_FILE_TAB_ID = "projectTab_tc";

    public GlobalActionContextProxy() {
        try {
            this.content = new InstanceContent();
            // The default GlobalContextProvider
            this.globalContextProvider = new GlobalActionContextImpl();
            this.globalContextLookup = this.globalContextProvider.createGlobalContext();
            // Monitor the activation of the Projects Tab TopComponent
            TopComponent.getRegistry().addPropertyChangeListener(this.registryListener);
            // Monitor the existance of a Project in the principle lookup
            this.resultProjects = globalContextLookup.lookupResult(Project.class);
            this.resultProjects.addLookupListener(this.resultListener);
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            @Override
            public void run() {
                // Hack to force the current Project selection when the application starts up
                TopComponent tc = WindowManager.getDefault().findTopComponent(PROJECT_LOGICAL_TAB_ID);
                if (tc != null) {
                    tc.requestActive();
                }
            }
        });
    }

    /**
     * Returns a ProxyLookup that adds the current Project instance to the global selection returned by Utilities.actionsGlobalContext().
     *
     * @return a ProxyLookup that includes the original global context lookup.
     */
    @Override
    public Lookup createGlobalContext() {
        if (proxyLookup == null) {
            logger.config("Creating a proxy for Utilities.actionsGlobalContext()");
            // Create the two lookups that will make up the proxy
            projectLookup = new AbstractLookup(content);
            proxyLookup = new ProxyLookup(globalContextLookup, projectLookup);
        }
        return proxyLookup;
    }

    /**
     * This class populates the proxy lookup with the currently selected project found in the Projects tab.
     */
    private class RegistryPropertyChangeListener implements PropertyChangeListener {

        private TopComponent projectsTab = null;

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (event.getPropertyName().equals(TopComponent.Registry.PROP_ACTIVATED_NODES)
                    || event.getPropertyName().equals(TopComponent.Registry.PROP_ACTIVATED)) {
                // Get a reference to the Projects window
                if (projectsTab == null) {
                    projectsTab = WindowManager.getDefault().findTopComponent(PROJECT_LOGICAL_TAB_ID);
                    if (projectsTab == null) {
                        logger.log(SEVERE, "propertyChange: cannot find the Projects logical window ({0})", PROJECT_LOGICAL_TAB_ID);
                        return;
                    }
                }
                // Look for the current project in the Projects window when activated and handle
                // special case at startup when lastProject hasn't been initialized.
                Node[] nodes = null;
                TopComponent activated = TopComponent.getRegistry().getActivated();
                if (activated != null && activated.equals(projectsTab)) {
                    logger.finer("propertyChange: processing activated nodes");
                    nodes = projectsTab.getActivatedNodes();
                } else if (lastProject == null) {
                    logger.finer("propertyChange: processing selected nodes");
                    ExplorerManager em = ((ExplorerManager.Provider) projectsTab).getExplorerManager();
                    nodes = em.getSelectedNodes();
                }
                // Find and use the first project that owns a node
                if (nodes != null) {
                    for (Node node : nodes) {
                        Project project = findProjectThatOwnsNode(node);
                        if (project != null) {
                            synchronized (lock) {
                                // Remember this project for when the Project Tab goes out of focus
                                lastProject = project;
                                // Add this project to the proxy if it's not in the global lookup
                                if (!resultProjects.allInstances().contains(lastProject)) {
                                    logger.log(FINER, "propertyChange: Found project [{0}] that owns current node.",
                                            ProjectUtils.getInformation(lastProject).getDisplayName());
                                    updateProjectLookup(lastProject);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * This class listens for changes in the Project results, and ensures a Project remains in the Utilities.actionsGlobalContext() if a
     * project is open.
     */
    private class LookupListenerImpl implements LookupListener {

        @Override
        public void resultChanged(LookupEvent event) {
            logger.finer("resultChanged: Entered...");
            synchronized (lock) {
                // First, handle projects in the principle lookup
                if (resultProjects.allInstances().size() > 0) {
                    // Clear the proxy, and remember this project.
                    // Note: not handling multiple selection of projects.
                    clearProjectLookup();
                    lastProject = resultProjects.allInstances().iterator().next();
                    logger.log(FINER, "resultChanged: Found project [{0}] in the normal lookup.",
                            ProjectUtils.getInformation(lastProject).getDisplayName());
                } else if (OpenProjects.getDefault().getOpenProjects().length == 0) {
                    clearProjectLookup();
                    lastProject = null;
                } else {
                    if (lastProject == null) {
                        // Find the project that owns the current Node
                        Node currrentNode = globalContextLookup.lookup(Node.class);
                        Project project = findProjectThatOwnsNode(currrentNode);
                        if (project != null) {
                            lastProject = project;
                            logger.log(FINER, "resultChanged: Found project [{0}] that owns current node.",
                                    ProjectUtils.getInformation(lastProject).getDisplayName());
                        }
                    }
                    // Add the last used project to our internal lookup
                    if (lastProject != null) {
                        updateProjectLookup(lastProject);
                    }
                }
            }
        }
    }

    /**
     * Unconditionally clears the project lookup.
     */
    private void clearProjectLookup() {
        if (projectLookup != null) {
            Collection<? extends Project> projects = projectLookup.lookupAll(Project.class);
            for (Project project : projects) {
                content.remove(project);
            }
        }
    }

    /**
     * Replaces the project lookup content.
     *
     * @param project to place in the project lookup.
     */
    private void updateProjectLookup(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("project cannot be null.");
        }
        // Add the project if an instance of it is not already in the lookup
        Template<Project> template = new Template<>(Project.class, null, project);
        if (projectLookup != null && projectLookup.lookupItem(template) == null) {
            clearProjectLookup();
            content.add(project);
            logger.log(FINE, "updateProjectLookup: added [{0}] to the proxy lookup.",
                    ProjectUtils.getInformation(lastProject).getDisplayName());
        }
    }

    /**
     * Recursively searches the node hierarchy for the project that owns a node.
     *
     * @param node a node to test for a Project in its or its ancestor's lookup.
     * @return the Project that owns the node, or null if not found
     */
    private static Project findProjectThatOwnsNode(Node node) {
        if (node != null) {
            Project project = node.getLookup().lookup(Project.class);
            if (project == null) {
                DataObject dataObject = node.getLookup().lookup(DataObject.class);
                if (dataObject != null) {
                    project = FileOwnerQuery.getOwner(dataObject.getPrimaryFile());
                }
            }
            return (project == null) ? findProjectThatOwnsNode(node.getParentNode()) : project;
        } else {
            return null;
        }
    }
}
