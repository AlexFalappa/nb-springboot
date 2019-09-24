/*
 * Copyright 2019 Alessandro Falappa.
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
package com.github.alexfalappa.nbspringboot.cfgprops.completion;

import java.util.HashMap;
import java.util.Map;

import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.project.Project;

import com.github.alexfalappa.nbspringboot.cfgprops.completion.providers.BootProvider;
import com.github.alexfalappa.nbspringboot.cfgprops.completion.providers.LoggerNameBootProvider;
import com.github.alexfalappa.nbspringboot.cfgprops.completion.providers.NoopBootProvider;

/**
 * SpringBoot key/value providers.
 *
 * @author Alessandro Falappa
 */
public class BootProviders {

    private final static NoopBootProvider NOOP_PROVIDER = new NoopBootProvider();
    private final Map<String, BootProvider> providerMap = new HashMap<>();

    public BootProviders(Project proj) {
        ClasspathInfo cpInfo = ClasspathInfo.create(proj.getProjectDirectory());
        providerMap.put("logger-name", new LoggerNameBootProvider(cpInfo.getClassIndex()));
    }

    public BootProvider getProvider(String name) {
        return providerMap.getOrDefault(name, NOOP_PROVIDER);
    }
}
