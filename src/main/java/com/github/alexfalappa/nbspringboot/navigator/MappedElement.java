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
import org.netbeans.api.java.source.ElementHandle;
import org.openide.filesystems.FileObject;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * This is a source code element of kind METHOD which is mapped by
 * {@code @RequestMapping} or derivations thereof.
 *
 * @author Michael J. Simons, 2016-09-16
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
        this.handlerMethod = element.toString();        
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
}
