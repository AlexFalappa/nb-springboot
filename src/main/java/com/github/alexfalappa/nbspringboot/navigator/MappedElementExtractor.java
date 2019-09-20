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

import java.lang.annotation.IncompleteAnnotationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

import org.openide.filesystems.FileObject;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

/**
 * This scanner does the heavy lifting of extracting methods that have been mapped to URLs.
 * <p>
 * I reassambled the Spring way as much at is possible without firing up a context: {@link RequestMapping} at type level does - if
 * present - restrict all other mappings.
 *
 * @author Michael J. Simons, 2016-09-16
 * @author Alessandro Falappa
 */
public final class MappedElementExtractor extends TreeScanner<List<MappedElement>, Void> {

    /**
     * Needed for combining paths... I'd rather resort to the Spring facilities then do this my own.
     */
    private final PathMatcher pathMatcher = new AntPathMatcher();
    private final FileObject fileObject;
    private final CompilationUnitTree compilationUnitTree;
    private final Trees trees;
    private final TreePath rootPath;
    private volatile boolean canceled = false;

    public MappedElementExtractor(final FileObject fileObject, final CompilationUnitTree compilationUnitTree, final Trees trees,
            final TreePath rootPath) {
        this.fileObject = fileObject;
        this.compilationUnitTree = compilationUnitTree;
        this.trees = trees;
        this.rootPath = rootPath;
    }

    @Override
    public List<MappedElement> reduce(final List<MappedElement> r1, final List<MappedElement> r2) {
        final List<MappedElement> rv = new ArrayList<>();
        if (r1 != null) {
            rv.addAll(r1);
        }
        if (r2 != null) {
            rv.addAll(r2);
        }
        return rv;
    }

    @Override
    public List visitClass(final ClassTree node, final Void p) {
        final List<MappedElement> mappedElements = new ArrayList<>();
        if (canceled || node == null) {
            return mappedElements;
        }
        final Element clazz = trees.getElement(new TreePath(rootPath, node));
        if (clazz == null || (clazz.getAnnotation(Controller.class) == null && clazz.getAnnotation(RestController.class) == null)) {
            return mappedElements;
        }
        final RequestMapping parentRequestMapping = clazz.getAnnotation(RequestMapping.class);
        final Map<String, List<RequestMethod>> parentUrls = extractTypeLevelMappings(parentRequestMapping);
        for (Element enclosedElement : clazz.getEnclosedElements()) {
            if (enclosedElement.getKind() != ElementKind.METHOD) {
                continue;
            }
            final Map<String, List<RequestMethod>> elementUrls = new TreeMap<>();
            final RequestMapping requestMapping = enclosedElement.getAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                extractMethodLevelMappings(elementUrls, concatValues(requestMapping.value(), requestMapping.path()),
                        requestMapping.method());
            }
            final DeleteMapping deleteMapping = enclosedElement.getAnnotation(DeleteMapping.class);
            if (deleteMapping != null) {
                extractMethodLevelMappings(elementUrls, concatValues(deleteMapping.value(), deleteMapping.path()),
                        new RequestMethod[]{RequestMethod.DELETE});
            }
            final GetMapping getMapping = enclosedElement.getAnnotation(GetMapping.class);
            if (getMapping != null) {
                extractMethodLevelMappings(elementUrls, concatValues(getMapping.value(), getMapping.path()),
                        new RequestMethod[]{RequestMethod.GET});
            }
            final PatchMapping patchMapping = enclosedElement.getAnnotation(PatchMapping.class);
            if (patchMapping != null) {
                extractMethodLevelMappings(elementUrls, concatValues(patchMapping.value(), patchMapping.path()),
                        new RequestMethod[]{RequestMethod.PATCH});
            }
            final PostMapping postMapping = enclosedElement.getAnnotation(PostMapping.class);
            if (postMapping != null) {
                extractMethodLevelMappings(elementUrls, concatValues(postMapping.value(), postMapping.path()),
                        new RequestMethod[]{RequestMethod.POST});
            }
            final PutMapping putMapping = enclosedElement.getAnnotation(PutMapping.class);
            if (putMapping != null) {
                extractMethodLevelMappings(elementUrls, concatValues(putMapping.value(), putMapping.path()),
                        new RequestMethod[]{RequestMethod.PUT});
            }

            for (Map.Entry<String, List<RequestMethod>> methodLevelMapping : elementUrls.entrySet()) {
                for (Map.Entry<String, List<RequestMethod>> typeLevelMapping : parentUrls.entrySet()) {
                    final String url = pathMatcher.combine(typeLevelMapping.getKey(), methodLevelMapping.getKey());

                    final List<RequestMethod> effectiveMethods = new ArrayList<>();
                    if (methodLevelMapping.getValue().isEmpty()) {
                        effectiveMethods.add(null);
                    }
                    effectiveMethods.addAll(methodLevelMapping.getValue());
                    if (!typeLevelMapping.getValue().isEmpty()) {
                        effectiveMethods.retainAll(typeLevelMapping.getValue());
                    }

                    for (RequestMethod effectiveMethod : effectiveMethods) {
                        mappedElements.add(new MappedElement(this.fileObject, enclosedElement, url, effectiveMethod));
                    }
                }
            }
        }
        return mappedElements;
    }

    void cancel() {
        this.canceled = true;
    }

    /**
     * Helper for concatenating several arrays.
     *
     * @param <T>
     * @param data
     * @return A list with the elements of the arrays in <code>data</code> concatenated
     */
    <T> List<T> concatValues(final T[]... data) {
        final List<T> rv = new ArrayList<>();
        for (T[] values : data) {
            rv.addAll(Arrays.asList(values));
        }
        return rv;
    }

    /**
     * Extracts the type level mapping if any. Makes sure that at least "/" is mapped and restricted to the given methods, if
     * there any.
     *
     * @param parentRequestMapping
     * @return
     */
    Map<String, List<RequestMethod>> extractTypeLevelMappings(final RequestMapping parentRequestMapping) {
        final Map<String, List<RequestMethod>> parentUrls = new TreeMap<>();
        List<String> urls = new ArrayList<>();
        List<RequestMethod> methods = new ArrayList<>();
        if (parentRequestMapping != null) {
            try {
                urls = concatValues(parentRequestMapping.value(), parentRequestMapping.path());
                methods = Arrays.asList(parentRequestMapping.method());
            } catch (IncompleteAnnotationException ex) {
                // ignore as may be thrown while typing annotations
            }
        }
        final List<String> usedUrls = urls.isEmpty() ? Arrays.asList("/") : urls;
        for (final String url : usedUrls) {
            final String usedUrl = url.startsWith("/") ? url : "/" + url;
            parentUrls.put(usedUrl, methods);
        }
        return parentUrls;
    }

    /**
     * Extracts the method level mapping.
     *
     * @param target
     * @param urls
     * @param methods
     */
    void extractMethodLevelMappings(final Map<String, List<RequestMethod>> target, final List<String> urls,
            final RequestMethod[] methods) {
        final List<String> usedUrls = urls.isEmpty() ? Arrays.asList("/") : urls;
        for (String url : usedUrls) {
            final String usedUrl = url.startsWith("/") ? url : "/" + url;
            final List<RequestMethod> mappedMethods;
            if (target.containsKey(usedUrl)) {
                mappedMethods = target.get(usedUrl);
            } else {
                mappedMethods = new ArrayList<>();
                target.put(usedUrl, mappedMethods);
            }
            mappedMethods.addAll(Arrays.asList(methods));
        }
    }
}
