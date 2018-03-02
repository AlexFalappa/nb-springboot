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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.Scrollable;

import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;

import com.fasterxml.jackson.databind.JsonNode;

import static javax.swing.SwingConstants.HORIZONTAL;

/**
 * Specialized scrollable panel to manage a list of checkboxes groups each containing two columns of checkboxes.
 * <p>
 * The panel is dynamically filled processing a JSON tree received from the Spring Initializr REST service.
 *
 * @author Alessandro Falappa
 */
public class BootDependenciesPanel extends javax.swing.JPanel implements Scrollable {

    private static final Logger logger = Logger.getLogger(BootDependenciesPanel.class.getName());
    private static final int OUTER_GAP = 4;
    private static final int INNER_GAP = 2;
    private static final int INDENT = 10;
    private static final int GROUP_SPACE = 16;
    private static final String LABEL_FREQUENTLY_USED = "Frequently Used";
    private boolean initialized = false;
    private final Map<String, List<DependencyToggleBox>> toggleBoxesMap = new HashMap<>();
    private final List<JLabel> grpLabels = new ArrayList<>();
    private Integer unitIncrement = null;
    private Integer blockIncrement = null;
    private int freqUsedDepsNumToShow = 6;
    private final Comparator<Map.Entry<String, Integer>> depsCountComparator = new Comparator<Map.Entry<String, Integer>>() {
        @Override
        public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
            return o2.getValue().compareTo(o1.getValue());
        }
    };

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
        // read used deps counts from prefs
        Preferences prefs = depsCountPrefNode();
        Map<String, Integer> depsMap = new HashMap<>();
        try {
            logger.finer("Previously used dependencies:");
            for (String key : prefs.keys()) {
                logger.finer(String.format("%s (%d times)", key, prefs.getInt(key, -1)));
                depsMap.put(key, prefs.getInt(key, -1));
            }
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
        // use a priority queue to rank dependencies 
        PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>(freqUsedDepsNumToShow, depsCountComparator);
        for (Map.Entry<String, Integer> entry : depsMap.entrySet()) {
            pq.add(entry);
        }
        // put top N deps in a set for quick reference below
        logger.log(Level.FINER, "Top {0} dependencies:", freqUsedDepsNumToShow);
        Set<String> freqDepsSet = new HashSet<>();
        for (int i = 0; i < freqUsedDepsNumToShow && !pq.isEmpty(); i++) {
            final String depName = pq.poll().getKey();
            logger.finer(depName);
            freqDepsSet.add(depName);
        }
        // create a group of checkboxes for frequently used deps
        if (!freqDepsSet.isEmpty()) {
            // group label
            JLabel lGroup = new JLabel(LABEL_FREQUENTLY_USED);
            lGroup.setFont(lGroup.getFont().deriveFont(Font.BOLD, lGroup.getFont().getSize() + 2));
            grpLabels.add(lGroup);
            this.add(lGroup, constraintsForGroupLabel(true));
            // prepare frequently used dependencies checkboxes
            int columnCounter = 0;
            for (int i = 0; i < nodeNum; i++) {
                JsonNode gn = depArray.get(i);
                // starter checkboxes in two columns
                final JsonNode valArray = gn.path("values");
                for (int j = 0; j < valArray.size(); j++) {
                    JsonNode dn = valArray.get(j);
                    final String id = dn.path("id").asText();
                    if (freqDepsSet.contains(id)) {
                        // distribute on 2 columns
                        this.add(toggleBoxForNode(LABEL_FREQUENTLY_USED, dn),
                                (columnCounter % 2 == 0) ? constraintsForFirstColumn() : constraintsForSecondColumn());
                        columnCounter++;
                    }
                }
            }
        } else {
            logger.finer("There are no previously used dependencies available");
        }
        // prepare other dependencies checkboxes
        for (int i = 0; i < nodeNum; i++) {
            JsonNode gn = depArray.get(i);
            final String groupName = gn.path("name").asText();
            // group label
            JLabel lGroup = new JLabel(groupName);
            lGroup.setFont(lGroup.getFont().deriveFont(Font.BOLD, lGroup.getFont().getSize() + 2));
            grpLabels.add(lGroup);
            this.add(lGroup, constraintsForGroupLabel(i == 0 && freqDepsSet.isEmpty()));
            // starter checkboxes in two columns
            final JsonNode valArray = gn.path("values");
            int columnCounter = 0;
            for (int j = 0; j < valArray.size(); j++) {
                // first column
                JsonNode dn = valArray.get(j);
                final String id = dn.path("id").asText();
                if (!freqDepsSet.contains(id)) {
                    // depending on column
                    this.add(toggleBoxForNode(groupName, dn),
                            (columnCounter % 2 == 0) ? constraintsForFirstColumn() : constraintsForSecondColumn());
                    columnCounter++;
                }
            }
        }
        initialized = true;
        // force recompute of increments
        unitIncrement = null;
        blockIncrement = null;
    }

    public static Preferences depsCountPrefNode() {
        return NbPreferences.forModule(BootDependenciesPanel.class).node("depsUseCount");
    }

    public String getSelectedDependenciesString() {
        StringBuilder sb = new StringBuilder();
        for (List<DependencyToggleBox> chList : toggleBoxesMap.values()) {
            for (DependencyToggleBox dtb : chList) {
                if (dtb.isEnabled() && dtb.isSelected()) {
                    sb.append(dtb.getName()).append(',');
                }
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
        for (List<DependencyToggleBox> chList : toggleBoxesMap.values()) {
            for (DependencyToggleBox dtb : chList) {
                dtb.setSelected(hs.contains(dtb.getName()));
            }
        }
    }

    public List<String> getSelectedDependencies() {
        List<String> ret = new ArrayList<>();
        for (List<DependencyToggleBox> chList : toggleBoxesMap.values()) {
            for (DependencyToggleBox dtb : chList) {
                if (dtb.isEnabled() && dtb.isSelected()) {
                    ret.add(dtb.getName());
                }
            }
        }
        return ret;
    }

    void setSelectedDependencies(List<String> deps) {
        HashSet<String> hs = new HashSet<>(deps);
        for (List<DependencyToggleBox> chList : toggleBoxesMap.values()) {
            for (DependencyToggleBox dtb : chList) {
                dtb.setSelected(hs.contains(dtb.getName()));
            }
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
            return getPreferredSize().width / 10;
        } else {
            if (unitIncrement == null) {
                unitIncrement = computeUnitIncrement();
            }
            return unitIncrement;
        }
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == HORIZONTAL) {
            return getPreferredSize().width / 5;
        } else {
            if (blockIncrement == null) {
                blockIncrement = computeBlockIncrement();
            }
            return blockIncrement;
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

    private DependencyToggleBox toggleBoxForNode(String group, JsonNode dn) {
        DependencyToggleBox dtb = new DependencyToggleBox();
        dtb.initFromMetadata(dn);
        if (!toggleBoxesMap.containsKey(group)) {
            toggleBoxesMap.put(group, new ArrayList<DependencyToggleBox>());
        }
        toggleBoxesMap.get(group).add(dtb);
        return dtb;
    }

    private GridBagConstraints constraintsForFirstColumn() {
        GridBagConstraints gbc;
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(INNER_GAP, INDENT, 0, 0);
        gbc.anchor = GridBagConstraints.LINE_START;
        return gbc;
    }

    private GridBagConstraints constraintsForSecondColumn() {
        GridBagConstraints gbc;
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(INNER_GAP, INDENT, 0, 0);
        gbc.anchor = GridBagConstraints.LINE_START;
        return gbc;
    }

    private GridBagConstraints constraintsForGroupLabel(boolean first) {
        GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = (first) ? new Insets(OUTER_GAP, OUTER_GAP, 0, OUTER_GAP) : new Insets(GROUP_SPACE, OUTER_GAP, 0, OUTER_GAP);
        return gbc;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
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
        for (List<DependencyToggleBox> dtbList : toggleBoxesMap.values()) {
            for (DependencyToggleBox dtb : dtbList) {
                dtb.adaptToBootVersion(bootVersion);
            }
        }
    }

    void clearFilter() {
        filter(null);
    }

    void filter(String text) {
        this.removeAll();
        int cg = 0;
        for (JLabel lGroup : grpLabels) {
            List<DependencyToggleBox> dtbList = cbFilter(lGroup.getText(), text);
            if (!dtbList.isEmpty()) {
                this.add(lGroup, constraintsForGroupLabel(cg++ == 0));
                int cd = 1;
                for (DependencyToggleBox dtb : dtbList) {
                    if (cd++ % 2 == 0) {
                        this.add(dtb, constraintsForSecondColumn());
                    } else {
                        this.add(dtb, constraintsForFirstColumn());
                    }
                }
            }
        }
        this.revalidate();
        this.repaint();
    }

    private List<DependencyToggleBox> cbFilter(String group, String text) {
        ArrayList<DependencyToggleBox> ret = new ArrayList<>();
        for (DependencyToggleBox dtb : toggleBoxesMap.get(group)) {
            if (text == null || dtb.getText().toLowerCase().contains(text)) {
                ret.add(dtb);
            }
        }
        return ret;
    }

    private int computeUnitIncrement() {
        final Iterator<List<DependencyToggleBox>> it = toggleBoxesMap.values().iterator();
        if (it.hasNext()) {
            List<DependencyToggleBox> list = it.next();
            if (!list.isEmpty()) {
                return list.get(0).getPreferredSize().height;
            }
        }
        return getPreferredSize().height / 24;
    }

    private Integer computeBlockIncrement() {
        final Iterator<List<DependencyToggleBox>> it = toggleBoxesMap.values().iterator();
        if (it.hasNext()) {
            List<DependencyToggleBox> list = it.next();
            if (!list.isEmpty()) {
                return list.get(0).getPreferredSize().height * 5;
            }
        }
        return getPreferredSize().height / 8;
    }
}
