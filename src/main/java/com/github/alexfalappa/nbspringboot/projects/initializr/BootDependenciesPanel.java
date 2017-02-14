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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.Scrollable;

import org.apache.commons.lang.WordUtils;
import org.openide.awt.HtmlBrowser.URLDisplayer;
import org.openide.util.Exceptions;
import org.springframework.web.util.UriTemplate;

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

    private static final String PROP_VERSION_RANGE = "versionRange";
    private static final String PROP_DESCRIPTION = "boot.description";
    private static final String PROP_REFERENCE_TEMPLATE_URL = "boot.reference.template";
    private static final int OUTER_GAP = 4;
    private static final int INNER_GAP = 2;
    private static final int INDENT = 10;
    private static final int GROUP_SPACE = 16;
    private static final int TOOLTIP_WIDTH = 40;
    private static final ImageIcon ICO_LGHT = new ImageIcon(BootDependenciesPanel.class.getResource("question_light.png"));
    private static final ImageIcon ICO_MDM = new ImageIcon(BootDependenciesPanel.class.getResource("question_medium.png"));
    private static final ImageIcon ICO_DRK = new ImageIcon(BootDependenciesPanel.class.getResource("question_dark.png"));
    private boolean initialized = false;
    private final Map<String, List<JCheckBox>> chkBoxesMap = new HashMap<>();
    private final List<JLabel> grpLabels = new ArrayList<>();
    private Integer unitIncrement = null;
    private Integer blockIncrement = null;
    private String currentBootVersion = null;
    private ActionListener linkActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JComponent c = (JComponent) e.getSource();
            final Object urlTemplate = c.getClientProperty(PROP_REFERENCE_TEMPLATE_URL);
            if (urlTemplate != null && currentBootVersion != null) {
                try {
                    UriTemplate template = new UriTemplate(urlTemplate.toString());
                    final URI uri = template.expand(currentBootVersion);
                    URLDisplayer.getDefault().showURLExternal(uri.toURL());
                } catch (MalformedURLException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
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
        // prepare dependencies checkboxes
        for (int i = 0; i < nodeNum; i++) {
            JsonNode gn = depArray.get(i);
            final String groupName = gn.path("name").asText();
            // group label
            JLabel lGroup = new JLabel(groupName);
            lGroup.setFont(lGroup.getFont().deriveFont(Font.BOLD, lGroup.getFont().getSize() + 2));
            grpLabels.add(lGroup);
            this.add(lGroup, constraintsForGroupLabel(i == 0));
            // starter checkboxes in two columns
            final JsonNode valArray = gn.path("values");
            for (int j = 0; j < valArray.size(); j++) {
                // first column
                JsonNode dn = valArray.get(j);
                this.add(checkBoxForNode(groupName, dn), constraintsForFirstColumnCheckbox());
                if (dn.has("_links") && dn.path("_links").has("reference")) {
                    this.add(linkForNode(dn), constraintsForFirstColumnLink());
                }
                // second column (optional)
                if (++j < valArray.size()) {
                    dn = valArray.get(j);
                    this.add(checkBoxForNode(groupName, dn), constraintsForSecondColumnCheckbox());
                    if (dn.has("_links") && dn.path("_links").has("reference")) {
                        this.add(linkForNode(dn), constraintsForSecondColumnLink());
                    }
                }
            }
        }
        initialized = true;
        // force recompute of increments
        unitIncrement = null;
        blockIncrement = null;
    }

    public String getSelectedDependenciesString() {
        StringBuilder sb = new StringBuilder();
        for (List<JCheckBox> chList : chkBoxesMap.values()) {
            for (JCheckBox cb : chList) {
                if (cb.isEnabled() && cb.isSelected()) {
                    sb.append(cb.getName()).append(',');
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
        for (List<JCheckBox> chList : chkBoxesMap.values()) {
            for (JCheckBox cb : chList) {
                cb.setSelected(hs.contains(cb.getName()));
            }
        }
    }

    public List<String> getSelectedDependencies() {
        List<String> ret = new ArrayList<>();
        for (List<JCheckBox> chList : chkBoxesMap.values()) {
            for (JCheckBox cb : chList) {
                if (cb.isEnabled() && cb.isSelected()) {
                    ret.add(cb.getName());
                }
            }
        }
        return ret;
    }

    void setSelectedDependencies(List<String> deps) {
        HashSet<String> hs = new HashSet<>(deps);
        for (List<JCheckBox> chList : chkBoxesMap.values()) {
            for (JCheckBox cb : chList) {
                cb.setSelected(hs.contains(cb.getName()));
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

    private JCheckBox checkBoxForNode(String group, JsonNode dn) {
        final String name = dn.path("name").asText();
        final String id = dn.path("id").asText();
        final String description = dn.path("description").asText();
        final String versRange = dn.path("versionRange").asText();
        JCheckBox ch = new JCheckBox(name);
        ch.setName(id);
        ch.putClientProperty(PROP_VERSION_RANGE, versRange);
        ch.putClientProperty(PROP_DESCRIPTION, description);
        if (!chkBoxesMap.containsKey(group)) {
            chkBoxesMap.put(group, new ArrayList<JCheckBox>());
        }
        chkBoxesMap.get(group).add(ch);
        return ch;
    }

    private GridBagConstraints constraintsForFirstColumnCheckbox() {
        GridBagConstraints gbc;
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(INNER_GAP, INDENT, 0, 0);
        gbc.anchor = GridBagConstraints.LINE_START;
        return gbc;
    }

    private GridBagConstraints constraintsForFirstColumnLink() {
        GridBagConstraints gbc;
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 1;
        gbc.insets = new Insets(INNER_GAP, 0, 0, 0);
        return gbc;
    }

    private GridBagConstraints constraintsForSecondColumnCheckbox() {
        GridBagConstraints gbc;
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 2;
        gbc.insets = new Insets(INNER_GAP, INDENT, 0, 0);
        gbc.anchor = GridBagConstraints.LINE_START;
        return gbc;
    }

    private GridBagConstraints constraintsForSecondColumnLink() {
        GridBagConstraints gbc;
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(INNER_GAP, 0, 0, 0);
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
        currentBootVersion = bootVersion;
        for (List<JCheckBox> chList : chkBoxesMap.values()) {
            for (JCheckBox cb : chList) {
                String verRange = (String) cb.getClientProperty(PROP_VERSION_RANGE);
                String description = (String) cb.getClientProperty(PROP_DESCRIPTION);
                final boolean allowable = allowable(verRange, bootVersion);
                cb.setEnabled(allowable);
                cb.setToolTipText(prepTooltip(description, allowable, verRange));
            }
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

    private String prepTooltip(String description, boolean allowable, String versRange) {
        StringBuilder sb = new StringBuilder("<html>");
        sb.append(WordUtils.wrap(description, TOOLTIP_WIDTH, "<br/>", false));
        if (!allowable) {
            sb.append("<br/><i>").append(decodeVersRange(versRange)).append("</i>");
        }
        return sb.toString();
    }

    private String decodeVersRange(String verRange) {
        StringBuilder sb = new StringBuilder();
        if (verRange != null && !verRange.isEmpty()) {
            if (verRange.indexOf('[') >= 0 || verRange.indexOf('(') >= 0
                    || verRange.indexOf(']') >= 0 || verRange.indexOf(')') >= 0) {
                // bounded range
                String[] bounds = verRange.substring(1, verRange.length() - 1).split(",");
                // check there are two bounds
                if (bounds.length == 2) {
                    sb.append(bounds[0]);
                    if (verRange.startsWith("[")) {
                        sb.append(" &lt;= ");
                    } else if (verRange.startsWith("(")) {
                        sb.append(" &lt; ");
                    }
                    sb.append("Boot version");
                    if (verRange.endsWith("]")) {
                        sb.append(" &gt;= ");
                    } else if (verRange.endsWith(")")) {
                        sb.append(" &gt; ");
                    }
                    sb.append(bounds[1]);
                }
            } else {
                // unbounded range
                sb.append("Boot version &gt;= ").append(verRange);
            }
        }
        return sb.toString();
    }

    void clearFilter() {
        filter(null);
    }

    void filter(String text) {
        this.removeAll();
        int cg = 1;
        for (JLabel lGroup : grpLabels) {
            List<JCheckBox> cbList = cbFilter(lGroup.getText(), text);
            if (!cbList.isEmpty()) {
                this.add(lGroup, constraintsForGroupLabel(cg++ == 0));
                int cd = 1;
                for (JCheckBox cb : cbList) {
                    if (cd++ % 2 == 0) {
                        this.add(cb, constraintsForSecondColumnCheckbox());
                    } else {
                        this.add(cb, constraintsForFirstColumnCheckbox());
                    }
                }
            }
        }
        this.revalidate();
        this.repaint();
    }

    private List<JCheckBox> cbFilter(String group, String text) {
        ArrayList<JCheckBox> ret = new ArrayList<>();
        for (JCheckBox cb : chkBoxesMap.get(group)) {
            if (text == null || cb.getText().toLowerCase().contains(text)) {
                ret.add(cb);
            }
        }
        return ret;
    }

    private int computeUnitIncrement() {
        final Iterator<List<JCheckBox>> it = chkBoxesMap.values().iterator();
        if (it.hasNext()) {
            List<JCheckBox> list = it.next();
            if (!list.isEmpty()) {
                return list.get(0).getPreferredSize().height;
            }
        }
        return getPreferredSize().height / 24;
    }

    private Integer computeBlockIncrement() {
        final Iterator<List<JCheckBox>> it = chkBoxesMap.values().iterator();
        if (it.hasNext()) {
            List<JCheckBox> list = it.next();
            if (!list.isEmpty()) {
                return list.get(0).getPreferredSize().height * 5;
            }
        }
        return getPreferredSize().height / 8;
    }

    private JButton linkForNode(JsonNode dn) {
        final JButton b = new JButton();
        b.setIcon(ICO_LGHT);
        b.setRolloverIcon(ICO_MDM);
        b.setPressedIcon(ICO_DRK);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.putClientProperty(PROP_REFERENCE_TEMPLATE_URL, dn.path("_links").path("reference").path("href").asText());
        b.addActionListener(linkActionListener);
        return b;
    }
}
