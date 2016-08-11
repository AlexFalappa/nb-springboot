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
package com.github.alexfalappa.nbspringboot.projects.initializr;

import org.openide.util.NbBundle;

/**
 * Global constants
 *
 * @author Alessandro Falappa
 */
public class InitializrProjectProps {

    public static final String WIZ_METADATA = "service.metadata";
    public static final String WIZ_BOOT_VERSION = "boot.version";
    public static final String WIZ_NAME = "maven.name";
    public static final String WIZ_GROUP = "maven.group";
    public static final String WIZ_ARTIFACT = "maven.artifact";
    public static final String WIZ_VERSION = "maven.version";
    public static final String WIZ_DESCRIPTION = "maven.description";
    public static final String WIZ_PACKAGING = "maven.packaging";
    public static final String WIZ_PACKAGE = "package";
    public static final String WIZ_JAVA_VERSION = "java.version";
    public static final String WIZ_LANGUAGE = "language";
    public static final String WIZ_DEPENDENCIES = "dependencies";
    public static final String WIZ_PROJ_NAME = "project.name";
    public static final String WIZ_PROJ_LOCATION = "project.location";
    public static final String WIZ_USE_SB_MVN_PLUGIN = "use.sbmavenplugin";
    public static final String WIZ_REMOVE_MVN_WRAPPER = "remove.mavenwrapper";
    public static final String WIZ_ADD_SB_CFGPROCESSOR = "add.config.processor";

    public static final String PREF_INITIALIZR_URL = "nbspringboot.initializr.url";
    public static final String PREF_INITIALIZR_TIMEOUT = "nbspringboot.initializr.timeout";

    public static final String REST_USER_AGENT = NbBundle.getMessage(InitializrProjectProps.class, "InitializrProjectProps.REST_USER_AGENT");
}
