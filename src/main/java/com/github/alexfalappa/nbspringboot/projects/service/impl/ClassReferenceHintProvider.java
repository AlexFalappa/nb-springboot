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

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

import com.github.alexfalappa.nbspringboot.cfgprops.completion.items.JavaTypeCompletionItem;
import com.github.alexfalappa.nbspringboot.projects.service.api.HintProvider;

/**
 * Implementation of {@link HintProvider} for class references.
 *
 * @author Alessandro Falappa
 */
public class ClassReferenceHintProvider implements HintProvider {

    private static final EnumSet<ClassIndex.SearchScope> SEARCH_SCOPE = EnumSet.allOf(ClassIndex.SearchScope.class);
    private final Set<ClassIndex.SearchKind> searchKind = Collections.singleton(ClassIndex.SearchKind.IMPLEMENTORS);
    private final ClassIndex classIndex;
    private final ClassPath cpExec;

    public ClassReferenceHintProvider(ClassIndex classIndex, ClassPath cpExec) {
        this.classIndex = classIndex;
        this.cpExec = cpExec;
    }

    @Override
    public void provide(Map<String, Object> params, ConfigurationMetadataProperty propMetadata, String filter,
            CompletionResultSet completionResultSet, int dotOffset, int caretOffset) {
        if (filter == null) {
            filter = "";
        }
        String baseType = "java.lang.Object";
        if (params.containsKey("target")) {
            baseType = params.get("target").toString();
        }
        boolean concrete = true;
        if (params.containsKey("concrete")) {
            concrete = Boolean.valueOf(params.get("concrete").toString());
        }
        // search of classes extending class baseType
        ElementHandle<TypeElement> element = ElementHandle.createTypeElementHandle(ElementKind.CLASS, baseType);
        Set<ElementHandle<TypeElement>> elements = classIndex.getElements(element, searchKind, SEARCH_SCOPE);
        populate(elements, filter, concrete, completionResultSet, dotOffset, caretOffset);
        // search of classes implementing interface baseType
        element = ElementHandle.createTypeElementHandle(ElementKind.INTERFACE, baseType);
        elements = classIndex.getElements(element, searchKind, SEARCH_SCOPE);
        populate(elements, filter, concrete, completionResultSet, dotOffset, caretOffset);
    }

    private void populate(Set<ElementHandle<TypeElement>> elements, String filter, boolean concrete,
            CompletionResultSet completionResultSet, int dotOffset, int caretOffset) throws IllegalStateException {
        final ClassLoader classLoader = cpExec.getClassLoader(true);
        String filterLowcase = filter.toLowerCase();
        elements.forEach(handle -> {
            final String binaryName = handle.getBinaryName();
            if (binaryName.toLowerCase().contains(filterLowcase)) {
                try {
                    Class<?> loadedClass = classLoader.loadClass(binaryName);
                    boolean isAbstract = Modifier.isAbstract(loadedClass.getModifiers());
                    if (concrete ^ isAbstract) {
                        completionResultSet.addItem(new JavaTypeCompletionItem(binaryName, handle.getKind(), dotOffset,
                                caretOffset));
                    }
                } catch (ClassNotFoundException ex) {
                    // ignore unloadable classes
                }
            }
        });
    }

}
