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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.openide.util.NbBundle;

/**
 * A super simple table model for the navigator UI.
 *
 * @author Michael J. Simons, 2016-09-16
 */
@NbBundle.Messages({
    "resourceUrl=URL",
    "requestMethod=Method",
    "handlerMethod=Handler"
})
public class MappedElementsModel extends AbstractTableModel {

    private static final long serialVersionUID = 5870247061989235811L;

    private final List<MappedElement> data = Collections.synchronizedList(new ArrayList<MappedElement>());

    @Override
    public String getColumnName(int column) {
        String rv;
        switch (column) {
            case 0:
                rv = Bundle.resourceUrl();
                break;
            case 1:
                rv = Bundle.requestMethod();
                break;
            case 2:
                rv = Bundle.handlerMethod();
                break;
            default:
                rv = null;
        }
        return rv;
    }

    @Override
    public int getRowCount() {
        return this.data.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final MappedElement mappedElement = this.data.get(rowIndex);
        Object rv;
        switch (columnIndex) {
            case 0:
                rv = mappedElement.getResourceUrl();
                break;
            case 1:
                rv = mappedElement.getRequestMethod();
                break;
            case 2:
                rv = mappedElement.getHandlerMethod();
                break;
            default:
                rv = null;
        }
        return rv;
    }

    public MappedElement getElementAt(final int rowIndex) {
        return this.data.get(rowIndex);
    }

    void refresh(final List<MappedElement> newData) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                data.clear();
                data.addAll(newData);
                Collections.sort(data, new Comparator<MappedElement>() {
                    @Override
                    public int compare(MappedElement o1, MappedElement o2) {
                        int rv = o1.getResourceUrl().compareTo(o2.getResourceUrl());
                        if (rv == 0) {
                            if (o1.getRequestMethod() == null) {
                                rv = -1;
                            } else if (o2.getRequestMethod() == null) {
                                rv = 1;
                            } else {
                                rv = o1.getRequestMethod().compareTo(o2.getRequestMethod());
                            }
                        }
                        return rv;
                    }
                });
                fireTableDataChanged();
            }
        };
        SwingUtilities.invokeLater(r);
    }
}
