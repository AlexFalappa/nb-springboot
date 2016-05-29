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
        iconBase = "com/github/alexfalappa/nbspringboot/templates/boot-class.png",
        displayName = "#cmdlinerunner_displayName",
        content = "CmdLineRunner.java.template",
        description = "description.html",
        scriptEngine = "freemarker",
        position = 400)
@Messages(value = "cmdlinerunner_displayName=CommandLineRunner class")
package com.github.alexfalappa.nbspringboot.templates.cmdlinerunner;

import org.netbeans.api.templates.TemplateRegistration;
import org.openide.util.NbBundle.Messages;
