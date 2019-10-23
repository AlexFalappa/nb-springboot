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
package com.github.alexfalappa.nbspringboot.projects.service.impl;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

import com.github.alexfalappa.nbspringboot.cfgprops.completion.items.JavaTypeCompletionItem;
import com.github.alexfalappa.nbspringboot.projects.service.api.HintProvider;

/**
 * Implementation of {@link HintProvider} for logger names.
 *
 * @author Alessandro Falappa
 */
public class LoggerNameHintProvider implements HintProvider {

    private final static Pattern PATTERN_ANONCLASSES = Pattern.compile(".*\\$\\d+");
    private final ClassIndex classIndex;

    public LoggerNameHintProvider(ClassIndex classIndex) {
        this.classIndex = classIndex;
    }

    @Override
    public void provide(Map<String, Object> params, ConfigurationMetadataProperty propMetadata, String filter,
            CompletionResultSet completionResultSet, int dotOffset, int caretOffset) {
        if (filter == null) {
            filter = "";
        }
        // fill in packages
        Set<String> packageNames = classIndex.getPackageNames(filter, true, EnumSet.allOf(ClassIndex.SearchScope.class));
        packageNames.stream()
                .filter(name -> !name.isEmpty())
                .forEach(name -> {
                    completionResultSet.addItem(new JavaTypeCompletionItem(name, ElementKind.PACKAGE, dotOffset, caretOffset));
                });
        // fill in types
        if (filter.contains(".")) {
            final int lastDotIdx = filter.lastIndexOf('.');
            final String packageFilter = filter.substring(0, lastDotIdx);
            final String typeFilter = filter.substring(lastDotIdx + 1);
            Set<ElementHandle<TypeElement>> types = classIndex.getDeclaredTypes(typeFilter,
                    ClassIndex.NameKind.CASE_INSENSITIVE_PREFIX, Collections.singleton(new SinglePackageScope(packageFilter)));
            types.forEach(type -> {
                final String binaryName = type.getBinaryName();
                Matcher matcher = PATTERN_ANONCLASSES.matcher(binaryName);
                if (!matcher.matches()) {
                    final String name = binaryName.substring(binaryName.lastIndexOf('.') + 1);
                    completionResultSet.addItem(new JavaTypeCompletionItem(name, type.getKind(),
                            dotOffset + packageFilter.length() + 1, caretOffset));
                }
            });
        }
    }

    // a scope for searching declared types in a specified package
    private class SinglePackageScope implements ClassIndex.SearchScopeType {

        private final Set<String> theSet;

        public SinglePackageScope(String packagename) {
            theSet = Collections.singleton(packagename);
        }

        @Override
        public Set<? extends String> getPackages() {
            return theSet;
        }

        @Override
        public boolean isSources() {
            return true;
        }

        @Override
        public boolean isDependencies() {
            return true;
        }

    }
}
