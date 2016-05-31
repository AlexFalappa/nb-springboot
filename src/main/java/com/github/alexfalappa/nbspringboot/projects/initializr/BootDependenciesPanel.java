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
package com.github.alexfalappa.nbspringboot.projects.initializr;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.Scrollable;

import com.fasterxml.jackson.databind.JsonNode;

import static javax.swing.SwingConstants.HORIZONTAL;

/**
 * Specialized scrollable panel to manage a list of checkboxes groups each containing two columns of checkboxes.
 * <p>
 * The panel is dynamically filled processing a JSON tree received from the Spring Initializr rest service.
 *
 * @author Alessandro Falappa
 */
public class BootDependenciesPanel extends javax.swing.JPanel implements Scrollable {

    private static final String PROP_VERSION_RANGE = "versionRange";
    private static final int OUTER_GAP = 4;
    private static final int INNER_GAP = 2;
    private static final int INDENT = 10;
    private static final int GROUP_SPACE = 16;
    private static final int TOOLTIP_WIDTH = 40;
    private boolean initialized = false;
    private final List<JCheckBox> chkBoxes = new ArrayList<>();

    public BootDependenciesPanel() {
        initComponents();
    }

    public void init(JsonNode metaData) {
        JsonNode depArray = metaData.path("dependencies").path("values");
        final int nodeNum = depArray.size();
        // remove informative label
        if (nodeNum > 0) {
            this.remove(lNotInitialized);
        }
        // prepare dependencies checkboxes
        int row = 0;
        for (int i = 0; i < nodeNum; i++) {
            JsonNode gn = depArray.get(i);
            // group label
            JLabel lGroup = new JLabel(gn.path("name").asText());
            lGroup.setFont(lGroup.getFont().deriveFont(Font.BOLD, lGroup.getFont().getSize() + 2));
            this.add(lGroup, constraintsForGroupLabel(row));
            row++;
            // starter checkboxes in two columns
            final JsonNode valArray = gn.path("values");
            for (int j = 0; j < valArray.size(); j++) {
                // first column
                JsonNode dn = valArray.get(j);
                String name = dn.path("name").asText();
                String id = dn.path("id").asText();
                String description = dn.path("description").asText();
                JCheckBox ch1 = new JCheckBox(name);
                ch1.setName(id);
                ch1.putClientProperty(PROP_VERSION_RANGE, dn.path("versionRange").asText());
                ch1.setToolTipText(wrap(description));
                chkBoxes.add(ch1);
                this.add(ch1, constraintsForFirstColumnCheckbox(row));
                // second column (optional)
                if (++j < valArray.size()) {
                    dn = valArray.get(j);
                    name = dn.path("name").asText();
                    id = dn.path("id").asText();
                    description = dn.path("description").asText();
                    JCheckBox ch2 = new JCheckBox(name);
                    ch2.setName(id);
                    ch2.putClientProperty(PROP_VERSION_RANGE, dn.path("versionRange").asText());
                    ch2.setToolTipText(wrap(description));
                    chkBoxes.add(ch2);
                    this.add(ch2, constraintsForSecondColumnCheckbox(row));
                }
                row++;
            }
        }
        initialized = true;
    }

    public String getSelectedDependenciesString() {
        StringBuilder sb = new StringBuilder();
        for (JCheckBox ch : chkBoxes) {
            if (ch.isEnabled() && ch.isSelected()) {
                sb.append(ch.getName()).append(',');
            }
        }
        // remove last comma (if present)
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    void setSelectedDependenciesString(String deps) {
        HashSet<String> hs = new HashSet<>(Arrays.asList(deps.split(",")));
        for (JCheckBox cb : chkBoxes) {
            cb.setSelected(hs.contains(cb.getName()));
        }
    }

    public List<String> getSelectedDependencies() {
        List<String> ret = new ArrayList<>();
        for (JCheckBox ch : chkBoxes) {
            if (ch.isEnabled() && ch.isSelected()) {
                ret.add(ch.getName());
            }
        }
        return ret;
    }

    void setSelectedDependencies(List<String> deps) {
        HashSet<String> hs = new HashSet<>(deps);
        for (JCheckBox cb : chkBoxes) {
            cb.setSelected(hs.contains(cb.getName()));
        }
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        Dimension size = getPreferredSize();
        if (initialized) {
            size = new Dimension(size.width, size.height / 8);
        }
        return size;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == HORIZONTAL) {
            return getPreferredSize().width / 2;
        } else {
            return getPreferredSize().height / 24;
        }
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == HORIZONTAL) {
            return getPreferredSize().width / 2;
        } else {
            return getPreferredSize().height / 8;
        }
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    private GridBagConstraints constraintsForSecondColumnCheckbox(int row) {
        GridBagConstraints gbc;
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.insets = new Insets(INNER_GAP, INNER_GAP, 0, 0);
        gbc.anchor = GridBagConstraints.LINE_START;
        return gbc;
    }

    private GridBagConstraints constraintsForFirstColumnCheckbox(int row) {
        GridBagConstraints gbc;
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.insets = new Insets(INNER_GAP, INDENT, 0, 0);
        gbc.anchor = GridBagConstraints.LINE_START;
        return gbc;
    }

    private GridBagConstraints constraintsForGroupLabel(int row) {
        GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = (row == 0) ? new Insets(OUTER_GAP, OUTER_GAP, 0, OUTER_GAP) : new Insets(GROUP_SPACE, OUTER_GAP, 0, OUTER_GAP);
        return gbc;
    }

    /** This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lNotInitialized = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        lNotInitialized.setText("Not initialized");
        lNotInitialized.setEnabled(false);
        add(lNotInitialized, new java.awt.GridBagConstraints());
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lNotInitialized;
    // End of variables declaration//GEN-END:variables

    void adaptToBootVersion(String bootVersion) {
        for (JCheckBox cb : chkBoxes) {
            String verRange = (String) cb.getClientProperty(PROP_VERSION_RANGE);
            final boolean allowable = allowable(verRange, bootVersion);
            cb.setEnabled(allowable);
        }
    }

    private static boolean allowable(String verRange, String bootVersion) {
        boolean ret = true;
        if (verRange != null && !verRange.isEmpty()) {
            if (verRange.indexOf('[') >= 0 || verRange.indexOf('(') >= 0
                    || verRange.indexOf(']') >= 0 || verRange.indexOf(')') >= 0) {
                // bounded range
                String[] bounds = verRange.substring(1, verRange.length() - 1).split(",");
                // check there are two bounds
                if (bounds.length != 2) {
                    return false;
                }
                // test various cases
                if (bootVersion.compareTo(bounds[0]) > 0 && bootVersion.compareTo(bounds[1]) < 0) {
                    return true;
                } else if (bootVersion.compareTo(bounds[0]) == 0 && verRange.startsWith("[")) {
                    return true;
                } else if (bootVersion.compareTo(bounds[0]) == 0 && verRange.startsWith("(")) {
                    return false;
                } else if (bootVersion.compareTo(bounds[1]) == 0 && verRange.endsWith("]")) {
                    return true;
                } else if (bootVersion.compareTo(bounds[1]) == 0 && verRange.endsWith(")")) {
                    return false;
                } else {
                    return false;
                }
            } else {
                // unbounded range
                return bootVersion.compareTo(verRange) >= 0;
            }
        }
        return ret;
    }

    private String wrap(String description) {
        StringBuilder sb = new StringBuilder("<html>");
        String[] words = description.split(" ");
        String w = words[0];
        sb.append(w);
        int len = w.length();
        for (int i = 1; i < words.length; i++) {
            w = words[i];
            if (len + w.length() + 1 > TOOLTIP_WIDTH) {
                sb.append("<br/>").append(w);
                len = w.length();
            } else {
                sb.append(" ").append(w);
                len += w.length() + 1;
            }
        }
        return sb.toString();
    }

}
