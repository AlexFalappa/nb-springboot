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

/**
 * A super simple table model for the navigator UI.
 *
 * @author Michael J. Simons, 2016-09-16
 */
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
                rv = mappedElement.getUrl();
                break;
            case 1:
                rv = mappedElement.getMethod();
                break;
            case 2:
                rv = mappedElement.getElement().toString();
                break;
            default:
                rv = null;
        }

        return rv;
    }

    void refresh(final List<MappedElement> newData) {
        this.data.clear();
        this.data.addAll(newData);
        this.fireTableDataChanged();
    }
}
