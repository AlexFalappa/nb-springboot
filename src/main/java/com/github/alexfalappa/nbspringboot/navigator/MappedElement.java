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
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * This is a source code element of kind METHOD which is mapped by
 * {@code @RequestMapping} or derivations thereof.
 *
 * @author Michael J. Simons, 2016-09-16
 */
public final class MappedElement {

    private final Element element;

    private final String url;

    private final RequestMethod method;

    public MappedElement(Element element, String url, RequestMethod method) {
        this.element = element;
        this.url = url;
        this.method = method;
    }

    public Element getElement() {
        return element;
    }

    public String getUrl() {
        return url;
    }

    public RequestMethod getMethod() {
        return method;
    }
}
