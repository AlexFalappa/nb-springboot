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
@TemplateRegistration(
        folder = "Spring Boot",
        iconBase = "com/github/alexfalappa/nbspringboot/templates/springboot-logo.png",
        displayName = "#applprop_displayName",
        content = "application.properties.template",
        description = "description.html",
        scriptEngine = "freemarker",
        position = 600)
@Messages(value = "applprop_displayName=Spring Boot application properties")
package com.github.alexfalappa.nbspringboot.templates.applproperties;

import org.netbeans.api.templates.TemplateRegistration;
import org.openide.util.NbBundle.Messages;
