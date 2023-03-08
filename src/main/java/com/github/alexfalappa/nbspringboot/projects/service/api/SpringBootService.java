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
package com.github.alexfalappa.nbspringboot.projects.service.api;

import java.util.List;
import java.util.Set;

import org.netbeans.api.java.classpath.ClassPath;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

/**
 * Service API for implementing support of Spring Boot related functionalities.
 *
 * @author Alessandro Falappa
 */
public interface SpringBootService {

    public static final String ENV_RESTART_15 = "SPRING_DEVTOOLS_RESTART_TRIGGER_FILE";
    public static final String ENV_RESTART = "SPRING_DEVTOOLS_RESTART_TRIGGERFILE";

    void refresh();

    ClassPath getManagedClassPath();

    Set<String> getPropertyNames();

    Set<String> getCollectionPropertyNames();

    Set<String> getMapPropertyNames();

    ConfigurationMetadataProperty getPropertyMetadata(String propertyName);

    List<ConfigurationMetadataProperty> queryPropertyMetadata(String filter);

    HintProvider getHintProvider(String name);

    boolean hasPomDependency(String artifactId);

    String getRestartEnvVarName();

    public String getPluginPropsPrefix();
}
