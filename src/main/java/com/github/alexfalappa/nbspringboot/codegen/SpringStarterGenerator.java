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
package com.github.alexfalappa.nbspringboot.codegen;

import java.awt.Frame;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.JTextComponent;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.maven.api.Constants;
import org.netbeans.modules.maven.model.pom.Dependency;
import org.netbeans.modules.maven.model.pom.DependencyContainer;
import org.netbeans.modules.maven.model.pom.DependencyManagement;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.Parent;
import org.netbeans.modules.maven.model.pom.Repository;
import org.netbeans.modules.maven.model.pom.RepositoryPolicy;
import org.netbeans.modules.xml.xam.Model;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.alexfalappa.nbspringboot.projects.initializr.InitializrService;

import static java.util.logging.Level.INFO;
import static java.util.logging.Logger.getLogger;

/**
 * Maven POM code generator to add a Spring Boot dependency.
 * <p>
 * Uses the metadata exposed by the Spring Initializr service.
 *
 * @see InitializrService
 * @author Alessandro Falappa
 */
public class SpringStarterGenerator implements CodeGenerator {

    protected final Logger logger = getLogger(getClass().getName());
    protected final JTextComponent component;
    protected final POMModel model;

    public SpringStarterGenerator(POMModel model, JTextComponent component) {
        this.model = model;
        this.component = component;
    }

    @MimeRegistration(mimeType = Constants.POM_MIME_TYPE, service = CodeGenerator.Factory.class, position = 1025)
    public static class Factory implements CodeGenerator.Factory {

        @Override
        public List<? extends CodeGenerator> create(Lookup context) {
            ArrayList<CodeGenerator> toRet = new ArrayList<>();
            POMModel model = context.lookup(POMModel.class);
            if (model != null) {
                // check if there is at least a dependency whose artifactId contains 'spring-boot-starter'
                boolean found = false;
                if (model.getProject().getDependencies() != null) {
                    for (Dependency dep : model.getProject().getDependencies()) {
                        if (dep.getArtifactId().contains("spring-boot-starter")) {
                            found = true;
                            break;
                        }
                    }
                }
                if (found) {
                    JTextComponent component = context.lookup(JTextComponent.class);
                    toRet.add(new SpringStarterGenerator(model, component));
                }
            }
            return toRet;
        }
    }

    @Override
    public String getDisplayName() {
        return "Spring Boot Dependencies...";
    }

    @Override
    public void invoke() {
        try {
            model.sync();
        } catch (IOException ex) {
            logger.log(INFO, "Error while syncing the editor document with model for pom.xml file", ex); //NOI18N
        }
        if (!model.getState().equals(Model.State.VALID)) {
            StatusDisplayer.getDefault().setStatusText("Cannot parse document. Unable to generate content.");
            return;
        }
        try {
            // retrieve boot version from parent pom declaration if present
            String bootVersion = null;
            final Parent pomParent = model.getProject().getPomParent();
            if (pomParent != null) {
                if (pomParent.getArtifactId().contains("spring-boot")) {
                    bootVersion = pomParent.getVersion();
                } else {
                    logger.fine("Parent pom is not 'spring-boot-starter-parent'. Unable to determine boot version.");
                }
            } else {
                logger.fine("No parent pom declaration found. Unable to determine boot version.");
            }
            // prepare and show dependency chooser dialog
            final Frame mainWindow = WindowManager.getDefault().getMainWindow();
            SpringDependencyDialog sdd = new SpringDependencyDialog(mainWindow);
            sdd.init(InitializrService.getInstance().getMetadata());
            if (bootVersion != null) {
                sdd.fixBootVersion(bootVersion);
            }
            sdd.setLocationRelativeTo(mainWindow);
            sdd.setVisible(true);
            // if OK pressed add selected dependencies
            if (sdd.getReturnStatus() == SpringDependencyDialog.RET_OK) {
                addDeps(bootVersion, sdd.getSelectedDeps());
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void addDeps(String bootVersion, final Set<String> selectedDeps) throws Exception {
        logger.log(Level.INFO, "Adding Spring Boot dependencies: {0}", selectedDeps.toString());
        JsonNode depsMeta = InitializrService.getInstance().getDependencies(bootVersion);
        Iterator<Map.Entry<String, JsonNode>> it = depsMeta.path("dependencies").fields();
        int newPos = component.getCaretPosition();
        try {
            if (model.startTransaction()) {
                DependencyContainer container = model.getProject();
                // iterate on "dependencies" JSON object (fields are dependency ids)
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    if (selectedDeps.contains(entry.getKey())) {
                        JsonNode depInfo = entry.getValue();
                        String groupId = depInfo.path("groupId").asText();
                        String artifactId = depInfo.path("artifactId").asText();
                        String scope = depInfo.path("scope").asText();
                        Dependency dep = container.findDependencyById(groupId, artifactId, null);
                        if (dep == null) {
                            dep = model.getFactory().createDependency();
                            dep.setGroupId(groupId);
                            dep.setArtifactId(artifactId);
                            // set scope only if not 'compile'
                            if (!scope.equals("compile")) {
                                if (scope.equals("compileOnly")) {
                                    dep.setOptional(Boolean.TRUE);
                                } else {
                                    // scope is 'runtime' or 'test'
                                    dep.setScope(scope);
                                }
                            }
                            // manage optional version
                            if (depInfo.hasNonNull("version")) {
                                dep.setVersion(depInfo.get("version").asText());
                            }
                            // manage optional need of extra repository
                            if (depInfo.hasNonNull("repository")) {
                                addRepository(depsMeta, depInfo.get("repository").asText());
                                addPluginRepository(depsMeta, depInfo.get("repository").asText());
                            }
                            // manage optional need of BOM inclusion
                            if (depInfo.hasNonNull("bom")) {
                                addBom(depsMeta, depInfo.get("bom").asText());
                            }
                            container.addDependency(dep);
                            newPos = model.getAccess().findPosition(dep.getPeer());
                        }
                    }
                }
            }
        } finally {
            try {
                model.endTransaction();
            } catch (IllegalStateException ex) {
                StatusDisplayer.getDefault().setStatusText("Cannot write to the model: " + ex.getMessage(),
                        StatusDisplayer.IMPORTANCE_ERROR_HIGHLIGHT);
            }
        }
        component.setCaretPosition(newPos);
    }

    private void addBom(JsonNode depsMeta, String bomId) {
        JsonNode bomInfo = depsMeta.path("boms").path(bomId);
        // create dependency management section in pom if missing
        DependencyManagement depMan = model.getProject().getDependencyManagement();
        if (depMan == null) {
            depMan = model.getFactory().createDependencyManagement();
            model.getProject().setDependencyManagement(depMan);
        }
        // add a dependency with type pom and scope import
        Dependency dep = model.getFactory().createDependency();
        dep.setGroupId(bomInfo.path("groupId").asText());
        dep.setArtifactId(bomInfo.path("artifactId").asText());
        dep.setVersion(bomInfo.path("version").asText());
        dep.setType("pom");
        dep.setScope("import");
        depMan.addDependency(dep);
        // bom may require extra repositories
        if (bomInfo.hasNonNull("repositories")) {
            for (JsonNode rep : bomInfo.path("repositories")) {
                addRepository(depsMeta, rep.asText());
                addPluginRepository(depsMeta, rep.asText());
            }
        }
    }

    private void addRepository(JsonNode depsMeta, String repoId) {
        Repository repository = model.getFactory().createRepository();
        fillRepository(depsMeta, repoId, repository);
        model.getProject().addRepository(repository);
    }

    private void addPluginRepository(JsonNode depsMeta, String repoId) {
        Repository repository = model.getFactory().createPluginRepository();
        fillRepository(depsMeta, repoId, repository);
        model.getProject().addPluginRepository(repository);
    }

    private void fillRepository(JsonNode depsMeta, String repoId, Repository repository) {
        JsonNode repoInfo = depsMeta.path("repositories").path(repoId);
        repository.setId(repoId);
        repository.setName(repoInfo.path("name").asText());
        repository.setUrl(repoInfo.path("url").asText());
        // set repository policies (release, snapshots)
        if (repoInfo.hasNonNull("snapshotEnabled")) {
            RepositoryPolicy snapshots = model.getFactory().createSnapshotRepositoryPolicy();
            snapshots.setEnabled(repoInfo.get("snapshotEnabled").asBoolean());
            repository.setSnapshots(snapshots);
        }
    }

}
