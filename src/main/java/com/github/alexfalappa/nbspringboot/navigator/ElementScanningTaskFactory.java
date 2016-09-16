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

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.support.LookupBasedJavaSourceTaskFactory;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

/**
 * This factory triggers the scanning of compilation units. If it is active, it
 * uses the {@link ElementScanningTask}, otherwise an empty task.
 *
 * @author Michael J. Simons, 2016-09-16
 */
public class ElementScanningTaskFactory extends LookupBasedJavaSourceTaskFactory {

    /**
     * This task uses the {@link MappedElementExtractor} to scan the current
     * compilation unit for mapped elements.
     *
     * @author Michael J. Simons, 2016-09-16
     */
    static class ElementScanningTask implements CancellableTask<CompilationInfo> {

        private final MappedElementsModel targetModel;
        private MappedElementExtractor mappedElementExtractor;
        private volatile boolean canceled;

        public ElementScanningTask(MappedElementsModel targetModel) {
            this.targetModel = targetModel;
        }

        @Override
        public void cancel() {
            this.canceled = true;
            if (this.mappedElementExtractor != null) {
                this.mappedElementExtractor.cancel();
                this.mappedElementExtractor = null;
            }
        }

        @Override
        public void run(CompilationInfo p) throws Exception {
            this.canceled = false;

            final CompilationUnitTree compilationUnitTree = p.getCompilationUnit();
            final TreePath rootPath = new TreePath(compilationUnitTree);

            this.mappedElementExtractor = new MappedElementExtractor(compilationUnitTree, p.getTrees(), rootPath);
            final List<MappedElement> mappedElements = compilationUnitTree.accept(this.mappedElementExtractor, null);
            Collections.sort(mappedElements, new Comparator<MappedElement>() {
                @Override
                public int compare(MappedElement o1, MappedElement o2) {
                    int rv = o1.getUrl().compareTo(o2.getUrl());
                    if (rv == 0) {
                        if (o1.getMethod() == null) {
                            rv = -1;
                        } else if (o2.getMethod() == null) {
                            rv = 1;
                        } else {
                            rv = o1.getMethod().compareTo(o2.getMethod());
                        }

                    }
                    return rv;
                }
            });
            this.targetModel.refresh(mappedElements);
        }
    }

    private final CancellableTask<CompilationInfo> EMPTY_TASK = new CancellableTask<CompilationInfo>() {
        @Override
        public void cancel() {
        }

        @Override
        public void run(CompilationInfo parameter) throws Exception {
        }
    };

    private final MappedElementsModel mappedElementsModel;
    private volatile boolean active = false;

    public ElementScanningTaskFactory(final MappedElementsModel mappedElementsModel) {
        super(JavaSource.Phase.PARSED, JavaSource.Priority.NORMAL);
        this.mappedElementsModel = mappedElementsModel;
    }

    public void activate() {
        this.active = true;
        this.setLookup(Utilities.actionsGlobalContext());
    }

    public void deactivate() {
        this.active = false;
        this.setLookup(Lookup.EMPTY);
    }

    @Override
    protected CancellableTask<CompilationInfo> createTask(final FileObject fo) {
        return this.active ? new ElementScanningTask(this.mappedElementsModel) : EMPTY_TASK;
    }
}
