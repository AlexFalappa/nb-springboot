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
package com.github.alexfalappa.nbspringboot;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.netbeans.contrib.yenta.Yenta;

/**
 * Avoid implementation dependencies by making the plugin friend of non-api modules.
 * <p>
 * Uses https://bitbucket.org/jglick/yenta
 *
 * @author Alessandro Falappa
 */
public class Installer extends Yenta {

    @Override
    protected Set<String> friends() {
        return new HashSet<>(Arrays.asList(
                "org.netbeans.modules.maven",
                "org.netbeans.modules.maven.embedder",
                "org.netbeans.modules.maven.model"//,
        //                "org.openide.windows"
        ));
    }

}
