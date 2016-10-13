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
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openide.util.NbBundle;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * A super simple table model for the navigator UI.
 *
 * @author Michael J. Simons, 2016-09-16
 * @author Alessandro Falappa
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
        switch (column) {
            case 0:
                return Bundle.resourceUrl();
            case 1:
                return Bundle.requestMethod();
            case 2:
                return Bundle.handlerMethod();
            default:
                return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0:
            case 2:
                return String.class;
            case 1:
                return RequestMethod.class;
            default:
                return null;
        }
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
        switch (columnIndex) {
            case 0:
                return mappedElement.getResourceUrl();
            case 1:
                return mappedElement.getRequestMethod();
            case 2:
                return mappedElement.getHandlerMethod();
            default:
                return null;
        }
    }

    public MappedElement getElementAt(final int rowIndex) {
        return this.data.get(rowIndex);
    }

    // Note: this method must be called on the Swing Event Dispatch Thread
    void refresh(final List<MappedElement> newData) {
        data.clear();
        data.addAll(newData);
        fireTableDataChanged();
    }
}
