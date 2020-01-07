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
import org.netbeans.modules.maven.model.pom.Build;
import org.netbeans.modules.maven.model.pom.Configuration;
import org.netbeans.modules.maven.model.pom.Dependency;
import org.netbeans.modules.maven.model.pom.DependencyContainer;
import org.netbeans.modules.maven.model.pom.DependencyManagement;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.Parent;
import org.netbeans.modules.maven.model.pom.Plugin;
import org.netbeans.modules.maven.model.pom.Project;
import org.netbeans.modules.maven.model.pom.Repository;
import org.netbeans.modules.maven.model.pom.RepositoryPolicy;
import org.netbeans.modules.xml.xam.Model;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.alexfalappa.nbspringboot.projects.initializr.InitializrService;

import static java.util.logging.Level.INFO;
import static java.util.logging.Logger.getLogger;

/**
 * Maven POM code generator to convert an existing maven project into a basic Spring Boot project.
 * <p>
 * Adds a parent pom, some essential Spring Boot dependencies and the maven spring boot plugin invocation to repackage the final
 * artifact.
 *
 * @author Alessandro Falappa
 */
public class InjectSpringBootGenerator implements CodeGenerator {

    protected final Logger logger = getLogger(getClass().getName());
    protected final JTextComponent component;
    protected final POMModel model;

    public InjectSpringBootGenerator(POMModel model, JTextComponent component) {
        this.model = model;
        this.component = component;
    }

    @MimeRegistration(mimeType = Constants.POM_MIME_TYPE, service = CodeGenerator.Factory.class, position = 950)
    public static class Factory implements CodeGenerator.Factory {

        @Override
        public List<? extends CodeGenerator> create(Lookup context) {
            ArrayList<CodeGenerator> toRet = new ArrayList<>();
            POMModel model = context.lookup(POMModel.class);
            if (model != null) {
                // check if there is already a dependency whose artifactId contains 'spring-boot-starter'
                boolean found = false;
                if (model.getProject().getDependencies() != null) {
                    for (Dependency dep : model.getProject().getDependencies()) {
                        if (dep.getArtifactId().contains("spring-boot-starter")) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    JTextComponent component = context.lookup(JTextComponent.class);
                    toRet.add(new InjectSpringBootGenerator(model, component));
                }
            }
            return toRet;
        }
    }

    @Override
    public String getDisplayName() {
        return "Inject Spring Boot Setup";
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
        int newPos = component.getCaretPosition();
        try {
            if (model.startTransaction()) {
                JsonNode meta = InitializrService.getInstance().getMetadata();
                String bootVersion = meta.path("bootVersion").path("default").asText();
                Project prj = model.getProject();
                // add parent pom declaration
                Parent parent = model.getFactory().createParent();
                parent.setGroupId("org.springframework.boot");
                parent.setArtifactId("spring-boot-starter-parent");
                parent.setVersion(bootVersion);
                prj.setPomParent(parent);
                // add 'java.version' property deducing it from 'maven.compiler.source' if present
                final Map<String, String> propertiesMap = prj.getProperties().getProperties();
                if (propertiesMap.containsKey(PROP_COMPILER_SOURCE)) {
                    prj.getProperties().setProperty(PROP_JAVAVERSION, propertiesMap.get(PROP_COMPILER_SOURCE));
                } else {
                    prj.getProperties().setProperty(PROP_JAVAVERSION, "1.7");
                }
                // add 'spring-boot-starter' compile dependency
                Dependency dep = model.getFactory().createDependency();
                dep.setGroupId("org.springframework.boot");
                dep.setArtifactId("spring-boot-starter");
                prj.addDependency(dep);
                // add 'spring-boot-starter-test' test dependency
                dep = model.getFactory().createDependency();
                dep.setGroupId("org.springframework.boot");
                dep.setArtifactId("spring-boot-starter-test");
                dep.setScope("test");
                prj.addDependency(dep);
                // add 'spring-boot-maven-plugin'
                Build build = model.getFactory().createBuild();
                Plugin plugin = model.getFactory().createPlugin();
                plugin.setGroupId("org.springframework.boot");
                plugin.setArtifactId("spring-boot-maven-plugin");
                Configuration config = model.getFactory().createConfiguration();
                config.setSimpleParameter("fork", "true");
                plugin.setConfiguration(config);
                build.addPlugin(plugin);
                prj.setBuild(build);
                // position caret at newly added parent declaration
                newPos = model.getAccess().findPosition(parent.getPeer());
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
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
    private static final String PROP_JAVAVERSION = "java.version";
    private static final String PROP_COMPILER_SOURCE = "maven.compiler.source";

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
