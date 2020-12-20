/*
 * Copyright 2017 the original author or authors.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.projects.initializr.BootDependenciesPanel;
import com.github.alexfalappa.nbspringboot.projects.initializr.InitializrService;
import java.awt.Frame;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.text.JTextComponent;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.model.pom.Dependency;
import org.netbeans.modules.maven.model.pom.DependencyContainer;
import org.netbeans.modules.maven.model.pom.DependencyManagement;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.Repository;
import org.netbeans.modules.maven.model.pom.RepositoryPolicy;
import org.openide.windows.WindowManager;

/**
 * Maven POM code generator to add a Spring Boot dependency.
 * <p>
 * Uses the metadata exposed by the Spring Initializr service.
 *
 * @see InitializrService
 * @author Alessandro Falappa
 * @author Diego Díez Ricondo
 */
public class SpringDependenciesGenerator extends BaseCodeGenerator {

    public SpringDependenciesGenerator(POMModel model, JTextComponent component) {
        super(model, component);
    }

    @Override
    public String getDisplayName() {
        return "Spring Boot Dependencies...";
    }

    @Override
    protected int pomInvoke(POMModel model, int caretPosition) throws Exception {
        final Project prj = Utils.getActiveProject();
        String bootVersion = Utils.getSpringBootVersion(prj).orElse(null);
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
            return addDeps(model, bootVersion, sdd.getSelectedDeps());
        }
        return -1;
    }

    private int addDeps(POMModel model, String bootVersion, final Set<String> selectedDeps) throws Exception {
        logger.log(Level.INFO, "Adding Spring Boot dependencies: {0}", selectedDeps.toString());
        JsonNode depsMeta = InitializrService.getInstance().getDependencies(bootVersion);
        Iterator<Map.Entry<String, JsonNode>> it = depsMeta.path("dependencies").fields();
        DependencyContainer container = model.getProject();
        // iterate on "dependencies" JSON object (fields are dependency ids)
        int newCaretPos = -1;
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
                        if (scope.equals("compileOnly") || scope.equals("annotationProcessor")) {
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
                        addRepository(model, depsMeta, depInfo.get("repository").asText());
                        addPluginRepository(model, depsMeta, depInfo.get("repository").asText());
                    }
                    // manage optional need of BOM inclusion
                    if (depInfo.hasNonNull("bom")) {
                        addBom(model, depsMeta, depInfo.get("bom").asText());
                    }
                    container.addDependency(dep);
                    logger.log(Level.FINE, "Added {0}:{1}", new Object[]{groupId, artifactId});
                    newCaretPos = model.getAccess().findPosition(dep.getPeer());
                } else {
                    // remove for later remembering
                    selectedDeps.remove(entry.getKey());
                }
            }
        }
        BootDependenciesPanel.updateRememberedDeps(selectedDeps);
        return newCaretPos;
    }

    private void addBom(POMModel model, JsonNode depsMeta, String bomId) {
        JsonNode bomInfo = depsMeta.path("boms").path(bomId);
        final String bomGroupId = bomInfo.path("groupId").asText();
        final String bomArtifactId = bomInfo.path("artifactId").asText();
        final String bomVersion = bomInfo.path("version").asText();
        DependencyManagement depMan = model.getProject().getDependencyManagement();
        if (depMan == null) {
            // create dependency management section in pom if missing
            depMan = model.getFactory().createDependencyManagement();
            model.getProject().setDependencyManagement(depMan);
        } else {
            // check bom already present
            List<Dependency> dependencies = depMan.getDependencies();
            if (dependencies != null) {
                for (Dependency dep : dependencies) {
                    if (dep.getGroupId().equals(bomGroupId)
                            && dep.getArtifactId().equals(bomArtifactId)
                            && dep.getVersion().equals(bomVersion)) {
                        return;
                    }
                }
            }
        }
        // add a dependency with type pom and scope import
        Dependency dep = model.getFactory().createDependency();
        dep.setGroupId(bomGroupId);
        dep.setArtifactId(bomArtifactId);
        dep.setVersion(bomVersion);
        dep.setType("pom");
        dep.setScope("import");
        depMan.addDependency(dep);
        // bom may require extra repositories
        if (bomInfo.hasNonNull("repositories")) {
            for (JsonNode rep : bomInfo.path("repositories")) {
                addRepository(model, depsMeta, rep.asText());
                addPluginRepository(model, depsMeta, rep.asText());
            }
        }
    }

    private void addRepository(POMModel model, JsonNode depsMeta, String repoId) {
        // check repository with given id already exists
        List<Repository> repositories = model.getProject().getRepositories();
        if (repositories != null) {
            for (Repository repo : repositories) {
                if (repo.getId().equals(repoId)) {
                    return;
                }
            }
        }
        // proceed to add new repository
        Repository repository = model.getFactory().createRepository();
        fillRepository(model, depsMeta, repoId, repository);
        model.getProject().addRepository(repository);
    }

    private void addPluginRepository(POMModel model, JsonNode depsMeta, String repoId) {
        // check plugin repository with given id already exists
        List<Repository> plugRepositories = model.getProject().getPluginRepositories();
        if (plugRepositories != null) {
            for (Repository plugRepo : plugRepositories) {
                if (plugRepo.getId().equals(repoId)) {
                    return;
                }
            }
        }
        // proceed to add new plugin repository
        Repository repository = model.getFactory().createPluginRepository();
        fillRepository(model, depsMeta, repoId, repository);
        model.getProject().addPluginRepository(repository);
    }

    private void fillRepository(POMModel model, JsonNode depsMeta, String repoId, Repository repository) {
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
