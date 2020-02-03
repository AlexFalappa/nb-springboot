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
package com.github.alexfalappa.nbspringboot.templates;

import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.spi.java.project.support.ui.templates.JavaTemplates;
import org.openide.WizardDescriptor;
import org.openide.util.NbBundle;

import com.github.alexfalappa.nbspringboot.projects.BootRecommendedTemplates;

/**
 * Spring and Spring Boot related file templates.
 * <p>
 * Templates are categorized and grouped in folder for display.
 *
 * @see BootRecommendedTemplates
 * @author Alessandro Falappa
 */
public class FileTemplates {

    public static final String CATEGORY_SPRING_FRAMEWORK = "spring-framework-types";
    public static final String CATEGORY_SPRING_BOOT = "spring-boot-types";
    public static final String CATEGORY_SPRING_BOOT_ACTUATOR = "spring-boot-actuator-types";
    public static final String CATEGORY_SPRING_MVC = "spring-mvc-types";
    public static final String CATEGORY_SPRING_DATA = "spring-data-types";
    public static final String CATEGORY_SPRING_REACT = "spring-reactive-types";
    public static final String FOLDER_SPRING_FRAMEWORK = "springframework";
    public static final String FOLDER_SPRING_BOOT = "springboot";
    public static final String ICON_BOOT_CLASS = "com/github/alexfalappa/nbspringboot/templates/boot-class.png";
    public static final String ICON_SPRING_CLASS = "com/github/alexfalappa/nbspringboot/templates/spring-class.png";

    @TemplateRegistration(
            folder = FOLDER_SPRING_BOOT,
            iconBase = ICON_BOOT_CLASS,
            displayName = "#cfgprops_displayName",
            content = "CfgProperties.java.template",
            description = "CfgProperties.html",
            scriptEngine = "freemarker",
            category = {CATEGORY_SPRING_BOOT},
            position = 100)
    @NbBundle.Messages(value = "cfgprops_displayName=Configuration Properties Class")
    public static WizardDescriptor.InstantiatingIterator<WizardDescriptor> cfgProperties() {
        return JavaTemplates.createJavaTemplateIterator();
    }

    @TemplateRegistration(
            folder = FOLDER_SPRING_BOOT,
            iconBase = ICON_BOOT_CLASS,
            displayName = "#cmdlinerunner_displayName",
            content = "CmdLineRunner.java.template",
            description = "CmdLineRunner.html",
            scriptEngine = "freemarker",
            category = {CATEGORY_SPRING_BOOT},
            position = 200)
    @NbBundle.Messages(value = "cmdlinerunner_displayName=CommandLineRunner Class")
    public static WizardDescriptor.InstantiatingIterator<WizardDescriptor> cmdLineRunner() {
        return JavaTemplates.createJavaTemplateIterator();
    }

    @TemplateRegistration(
            folder = FOLDER_SPRING_BOOT,
            iconBase = ICON_BOOT_CLASS,
            displayName = "#applrunner_displayName",
            content = "ApplRunner.java.template",
            description = "ApplRunner.html",
            scriptEngine = "freemarker",
            category = {CATEGORY_SPRING_BOOT},
            position = 300)
    @NbBundle.Messages(value = "applrunner_displayName=ApplicationRunner Class")
    public static WizardDescriptor.InstantiatingIterator<WizardDescriptor> applicationRunner() {
        return JavaTemplates.createJavaTemplateIterator();
    }

    @TemplateRegistration(
            folder = FOLDER_SPRING_BOOT,
            iconBase = ICON_BOOT_CLASS,
            displayName = "#infocontributor_displayName",
            content = "InfoContributor.java.template",
            description = "InfoContributor.html",
            scriptEngine = "freemarker",
            category = {CATEGORY_SPRING_BOOT_ACTUATOR},
            position = 400)
    @NbBundle.Messages(value = "infocontributor_displayName=InfoContributor Class")
    public static WizardDescriptor.InstantiatingIterator<WizardDescriptor> infoContributor() {
        return JavaTemplates.createJavaTemplateIterator();
    }

    @TemplateRegistration(
            folder = FOLDER_SPRING_BOOT,
            iconBase = ICON_BOOT_CLASS,
            displayName = "#healthindicator_displayName",
            content = "HealthIndicator.java.template",
            description = "HealthIndicator.html",
            scriptEngine = "freemarker",
            category = {CATEGORY_SPRING_BOOT_ACTUATOR},
            position = 500)
    @NbBundle.Messages(value = "healthindicator_displayName=HealthIndicator Class")
    public static WizardDescriptor.InstantiatingIterator<WizardDescriptor> healthIndicator() {
        return JavaTemplates.createJavaTemplateIterator();
    }

    @TemplateRegistration(
            folder = FOLDER_SPRING_FRAMEWORK,
            iconBase = ICON_SPRING_CLASS,
            displayName = "#config_displayName",
            content = "Configuration.java.template",
            description = "Configuration.html",
            scriptEngine = "freemarker",
            category = {CATEGORY_SPRING_FRAMEWORK},
            position = 400)
    @NbBundle.Messages(value = "config_displayName=Configuration Class")
    public static WizardDescriptor.InstantiatingIterator<WizardDescriptor> configuration() {
        return JavaTemplates.createJavaTemplateIterator();
    }

    @TemplateRegistration(
            folder = FOLDER_SPRING_FRAMEWORK,
            iconBase = ICON_SPRING_CLASS,
            displayName = "#component_displayName",
            content = "Component.java.template",
            description = "Component.html",
            scriptEngine = "freemarker",
            category = {CATEGORY_SPRING_FRAMEWORK},
            position = 500)
    @NbBundle.Messages(value = "component_displayName=Component Class")
    public static WizardDescriptor.InstantiatingIterator<WizardDescriptor> component() {
        return JavaTemplates.createJavaTemplateIterator();
    }

    @TemplateRegistration(
            folder = FOLDER_SPRING_FRAMEWORK,
            iconBase = ICON_SPRING_CLASS,
            displayName = "#service_displayName",
            content = "Service.java.template",
            description = "Service.html",
            scriptEngine = "freemarker",
            category = {CATEGORY_SPRING_FRAMEWORK},
            position = 600)
    @NbBundle.Messages(value = "service_displayName=Service Class")
    public static WizardDescriptor.InstantiatingIterator<WizardDescriptor> service() {
        return JavaTemplates.createJavaTemplateIterator();
    }

    @TemplateRegistration(
            folder = FOLDER_SPRING_FRAMEWORK,
            iconBase = ICON_SPRING_CLASS,
            displayName = "#funchandler_displayName",
            content = "ReactHandler.java.template",
            description = "ReactHandler.html",
            scriptEngine = "freemarker",
            category = {CATEGORY_SPRING_REACT},
            position = 1100)
    @NbBundle.Messages(value = "funchandler_displayName=Reactive Handler Class")
    public static WizardDescriptor.InstantiatingIterator<WizardDescriptor> reactHandler() {
        return JavaTemplates.createJavaTemplateIterator();
    }

}
