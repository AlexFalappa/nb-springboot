/*
 * Copyright 2016 Alessandro Falappa.
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
package com.github.alexfalappa.nbspringboot.projects.customizer;

import java.util.LinkedList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * An editable {@link JTable} model holding {@link CfgOverride} objects.
 * <p>
 * Backed by a {@link LinkedList}.
 *
 * @author Alessandro Falappa
 */
public class CfgParamsTableModel extends AbstractTableModel {

    List<CfgOverride> overrides = new LinkedList<>();

    @Override
    public int getRowCount() {
        return overrides.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        CfgOverride co = overrides.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return co.enabled;
            case 1:
                return co.name;
            case 2:
                return co.value;
            default:
                return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "En.";
            case 1:
                return "Name";
            case 2:
                return "Value";
            default:
                return super.getColumnName(columnIndex);
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return Boolean.class;
            case 1:
            case 2:
                return String.class;
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (rowIndex >= 0 && rowIndex < overrides.size()) {
            CfgOverride co = overrides.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    if (aValue instanceof Boolean) {
                        co.enabled = (Boolean) aValue;
                    }
                    break;
                case 1:
                    if (aValue instanceof String) {
                        co.name = (String) aValue;
                    }
                    break;
                case 2:
                    if (aValue instanceof String) {
                        co.value = (String) aValue;
                    }
                    break;
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    public List<CfgOverride> getOverrides() {
        return overrides;
    }

    public List<CfgOverride> getEnabledOverrides() {
        List<CfgOverride> ret = new LinkedList<>();
        for (CfgOverride ov : overrides) {
            if (ov.enabled) {
                ret.add(ov);
            }
        }
        return ret;
    }

    public CfgOverride getOverrideAt(int idx) {
        return overrides.get(idx);
    }

    public void addOverride(CfgOverride override) {
        overrides.add(override);
        final int numOverrides = overrides.size();
        fireTableRowsInserted(numOverrides - 1, numOverrides - 1);
    }

    public CfgOverride removeOverride(int selRow) {
        final CfgOverride removed = overrides.remove(selRow);
        fireTableRowsDeleted(selRow, selRow);
        return removed;
    }

    public void removeAllOverrides() {
        int oldSize = overrides.size();
        overrides.clear();
        fireTableRowsDeleted(0, oldSize - 1);
    }

}
