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
package com.github.alexfalappa.nbspringboot.navigator;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.netbeans.api.java.source.ElementHandle;
import org.openide.filesystems.FileObject;
import org.springframework.web.bind.annotation.RequestMethod;

import com.github.alexfalappa.nbspringboot.Utils;

/**
 * This is a source code element of kind METHOD which is mapped by {@code @RequestMapping} or derivations thereof.
 *
 * @author Michael J. Simons, 2016-09-16
 * @author Alessandro Falappa
 */
public final class MappedElement {

    private final FileObject fileObject;
    private final ElementHandle<Element> handle;
    private final String handlerMethod;
    private final String resourceUrl;
    private final RequestMethod requestMethod;

    public MappedElement(final FileObject fileObject, final Element element, final String url, final RequestMethod method) {
        this.fileObject = fileObject;
        this.handle = ElementHandle.create(element);
        this.handlerMethod = computeHandlerSignature(element);
        this.resourceUrl = url;
        this.requestMethod = method;
    }

    public FileObject getFileObject() {
        return fileObject;
    }

    public ElementHandle<Element> getHandle() {
        return handle;
    }

    public String getHandlerMethod() {
        return handlerMethod;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    private final String computeHandlerSignature(Element element) {
        StringBuilder sb = new StringBuilder(element.getSimpleName());
        if (element instanceof ExecutableElement) {
            // store arguments with same unqualified type name
            ExecutableElement eel = (ExecutableElement) element;
            SetValuedMap<String, String> mm = new HashSetValuedHashMap<>();
            for (VariableElement var : eel.getParameters()) {
                String fullType = var.asType().toString();
                mm.put(Utils.shortenJavaType(fullType), fullType);
            }
            // build up argument list
            sb.append('(');
            for (int i = 0; i < eel.getParameters().size(); i++) {
                VariableElement var = eel.getParameters().get(i);
                String fullType = var.asType().toString();
                final String shortType = Utils.shortenJavaType(fullType);
                if (mm.get(shortType).size() > 1) {
                    sb.append(fullType);
                } else {
                    sb.append(shortType);
                }
                if (i < eel.getParameters().size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(") : ");
            sb.append(Utils.shortenJavaType(eel.getReturnType().toString()));
        }
        return sb.toString();
    }
}
