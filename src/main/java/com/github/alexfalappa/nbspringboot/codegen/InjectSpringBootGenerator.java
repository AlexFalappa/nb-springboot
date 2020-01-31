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

import java.util.Map;

import javax.swing.text.JTextComponent;

import org.netbeans.modules.maven.model.pom.Build;
import org.netbeans.modules.maven.model.pom.Dependency;
import org.netbeans.modules.maven.model.pom.Exclusion;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.Parent;
import org.netbeans.modules.maven.model.pom.Plugin;
import org.netbeans.modules.maven.model.pom.Project;
import org.netbeans.modules.maven.model.pom.Properties;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.github.alexfalappa.nbspringboot.projects.basic.BasicProjectWizardIterator;

/**
 * Maven POM code generator to convert an existing maven project into a basic Spring Boot project.
 * <p>
 * Adds a parent pom, some essential Spring Boot dependencies and the maven spring boot plugin invocation to repackage the final
 * artifact.
 *
 * @author Alessandro Falappa
 */
public class InjectSpringBootGenerator extends BaseCodeGenerator {

    private static final String PROP_JAVAVERSION = "java.version";
    private static final String PROP_COMPILER_SOURCE = "maven.compiler.source";
    private static final String PROP_COMPILER_TARGET = "maven.compiler.target";

    public InjectSpringBootGenerator(POMModel model, JTextComponent component) {
        super(model, component);
    }

    @Override
    public String getDisplayName() {
        return "Inject Spring Boot Setup";
    }

    @Override
    protected int pomInvoke(POMModel model, int caretPosition) throws Exception {
        Project prj = model.getProject();
        // add parent pom declaration
        Parent parent = model.getFactory().createParent();
        parent.setGroupId("org.springframework.boot");
        parent.setArtifactId("spring-boot-starter-parent");
        parent.setVersion(BasicProjectWizardIterator.BOOTVERSION);
        parent.setRelativePath("");
        prj.setPomParent(parent);
        // add 'java.version' property deducing it from 'maven.compiler.source' if present
        final Properties propertiesElement = prj.getProperties();
        final Map<String, String> propertiesMap = propertiesElement.getProperties();
        if (propertiesMap.containsKey(PROP_COMPILER_SOURCE)) {
            propertiesElement.setProperty(PROP_JAVAVERSION, propertiesMap.get(PROP_COMPILER_SOURCE));
        } else {
            propertiesElement.setProperty(PROP_JAVAVERSION, "1.8");
        }
        // remove maven.compiler.source and maven.compiler.target properties if present
        final Element propertiesElem = propertiesElement.getPeer();
        NodeList nodes = propertiesElem.getElementsByTagName(PROP_COMPILER_SOURCE);
        if (nodes.getLength() > 0) {
            model.removeChildComponent(propertiesElement.findChildComponent((Element) nodes.item(0)));
        }
        nodes = propertiesElem.getElementsByTagName(PROP_COMPILER_TARGET);
        if (nodes.getLength() > 0) {
            model.removeChildComponent(propertiesElement.findChildComponent((Element) nodes.item(0)));
        }
        // add 'spring-boot-starter' compile dependency
        Dependency dep = model.getFactory().createDependency();
        dep.setGroupId("org.springframework.boot");
        dep.setArtifactId("spring-boot-starter");
        prj.addDependency(dep);
        // add 'spring-boot-starter-test' test dependency with exclusion of JUnit vintage engine
        dep = model.getFactory().createDependency();
        dep.setGroupId("org.springframework.boot");
        dep.setArtifactId("spring-boot-starter-test");
        dep.setScope("test");
        Exclusion exclsn = model.getFactory().createExclusion();
        exclsn.setGroupId("org.junit.vintage");
        exclsn.setArtifactId("junit-vintage-engine");
        dep.addExclusion(exclsn);
        // add 'spring-boot-maven-plugin'
        prj.addDependency(dep);
        Build build = model.getFactory().createBuild();
        Plugin plugin = model.getFactory().createPlugin();
        plugin.setGroupId("org.springframework.boot");
        plugin.setArtifactId("spring-boot-maven-plugin");
        build.addPlugin(plugin);
        prj.setBuild(build);
        // position caret at newly added parent declaration
        return model.getAccess().findPosition(parent.getPeer());
    }

}
