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

import javax.swing.SwingUtilities;

import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.support.LookupBasedJavaSourceTaskFactory;
import org.netbeans.swing.etable.ETable;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;

/**
 * This factory triggers the scanning of compilation units. If it is active, it uses the {@link ElementScanningTask}, otherwise an empty
 * task.
 *
 * @author Michael J. Simons, 2016-09-16
 * @author Alessandro Falappa
 */
public class ElementScanningTaskFactory extends LookupBasedJavaSourceTaskFactory {

    /**
     * This task uses the {@link MappedElementExtractor} to scan the current compilation unit for mapped elements.
     *
     * @author Michael J. Simons, 2016-09-16
     */
    static class ElementScanningTask implements CancellableTask<CompilationInfo> {

        private final ETable table;
        private final MappedElementsModel targetModel;
        private MappedElementExtractor mappedElementExtractor;

        public ElementScanningTask(ETable table, MappedElementsModel targetModel) {
            this.table = table;
            this.targetModel = targetModel;
        }

        @Override
        public void cancel() {
            if (mappedElementExtractor != null) {
                mappedElementExtractor.cancel();
                mappedElementExtractor = null;
            }
        }

        @Override
        public void run(CompilationInfo p) throws Exception {
            final CompilationUnitTree compilationUnitTree = p.getCompilationUnit();
            final TreePath rootPath = new TreePath(compilationUnitTree);
            mappedElementExtractor = new MappedElementExtractor(p.getFileObject(), compilationUnitTree, p.getTrees(), rootPath);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    targetModel.refresh(compilationUnitTree.accept(mappedElementExtractor, null));
                    table.setModel(targetModel);
                }
            });
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
    private final ETable table;
    private final MappedElementsModel mappedElementsModel;
    private volatile boolean active = false;

    public ElementScanningTaskFactory(final ETable table, final MappedElementsModel mappedElementsModel) {
        super(JavaSource.Phase.PARSED, JavaSource.Priority.NORMAL);
        this.table = table;
        this.mappedElementsModel = mappedElementsModel;
    }

    public void activate() {
        active = true;
        setLookup(Utilities.actionsGlobalContext());
    }

    public void deactivate() {
        active = false;
        setLookup(Lookup.EMPTY);
    }

    @Override
    protected CancellableTask<CompilationInfo> createTask(final FileObject fo) {
        return active ? new ElementScanningTask(table, mappedElementsModel) : EMPTY_TASK;
    }
}
