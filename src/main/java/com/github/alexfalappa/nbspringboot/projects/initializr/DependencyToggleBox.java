/*
 * Copyright 2017 Alessandro Falappa.
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

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Objects;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.UIDefaults;

import org.apache.commons.lang.WordUtils;
import org.openide.awt.HtmlBrowser;
import org.openide.util.Exceptions;
import org.springframework.web.util.UriTemplate;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Custom widget to display a dependency selection checkbox on the left and optionally up to two link buttons on the right.
 * <p>
 * The buttons open in an external browser the first "reference" and "guide" urls found in the initializr service metadata.
 *
 * @author Alessandro Falappa
 */
public class DependencyToggleBox extends javax.swing.JPanel {

    private static final String PROP_VERSION_RANGE = "versionRange";
    private static final String PROP_DESCRIPTION = "boot.description";
    private static final String PROP_REFERENCE_TEMPLATE_URL = "urltemplate.reference";
    private static final String PROP_GUIDE_TEMPLATE_URL = "urltemplate.guide";
    private static final int TOOLTIP_WIDTH = 40;
    private static final ImageIcon ICO_QST_LGHT = new ImageIcon(BootDependenciesPanel.class.getResource("question_light.png"));
    private static final ImageIcon ICO_QST_MDM = new ImageIcon(BootDependenciesPanel.class.getResource("question_medium.png"));
    private static final ImageIcon ICO_QST_DRK = new ImageIcon(BootDependenciesPanel.class.getResource("question_dark.png"));
    private static final ImageIcon ICO_BOK_LGHT = new ImageIcon(BootDependenciesPanel.class.getResource("book_light.png"));
    private static final ImageIcon ICO_BOK_MDM = new ImageIcon(BootDependenciesPanel.class.getResource("book_medium.png"));
    private static final ImageIcon ICO_BOK_DRK = new ImageIcon(BootDependenciesPanel.class.getResource("book_dark.png"));
    private static final Insets INSETS_SMALLBUTTON = new Insets(1, 1, 1, 1);
    private static String currentBootVersion = null;
    private static final ActionListener refActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JComponent c = (JComponent) e.getSource();
            final Object urlTemplate = c.getClientProperty(PROP_REFERENCE_TEMPLATE_URL);
            if (urlTemplate != null && currentBootVersion != null) {
                try {
                    UriTemplate template = new UriTemplate(urlTemplate.toString());
                    final URI uri = template.expand(currentBootVersion);
                    HtmlBrowser.URLDisplayer.getDefault().showURLExternal(uri.toURL());
                } catch (MalformedURLException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    };
    private static final ActionListener guideActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JComponent c = (JComponent) e.getSource();
            final Object urlTemplate = c.getClientProperty(PROP_GUIDE_TEMPLATE_URL);
            if (urlTemplate != null && currentBootVersion != null) {
                try {
                    UriTemplate template = new UriTemplate(urlTemplate.toString());
                    final URI uri = template.expand(currentBootVersion);
                    HtmlBrowser.URLDisplayer.getDefault().showURLExternal(uri.toURL());
                } catch (MalformedURLException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    };
    private JButton bReference;
    private JButton bGuide;
    private final UIDefaults uiDef = new UIDefaults();

    public DependencyToggleBox() {
        initComponents();
        uiDef.put("Button.contentMargins", INSETS_SMALLBUTTON);
    }

    public void initFromMetadata(JsonNode dn) {
        final String name = dn.path("name").asText();
        final String id = dn.path("id").asText();
        final String description = dn.path("description").asText();
        final String versRange = dn.path("versionRange").asText();
        cbDep.setText(name);
        this.setName(id);
        this.putClientProperty(PROP_VERSION_RANGE, versRange);
        this.putClientProperty(PROP_DESCRIPTION, description);
        if (dn.has("_links")) {
            final JsonNode links = dn.path("_links");
            if (links.has("reference")) {
                JsonNode ref = links.path("reference");
                if (ref.isArray()) {
                    ref = ref.get(0);
                }
                setLinkReference(ref.path("href").asText(), ref.path("title").asText());
            }
            if (links.has("guide")) {
                JsonNode ref = links.path("guide");
                if (ref.isArray()) {
                    ref = ref.get(0);
                }
                setLinkGuide(ref.path("href").asText(), ref.path("title").asText());
            }
        }
    }

    public void setLinkReference(String url, String title) {
        Objects.requireNonNull(url);
        if (bReference == null) {
            bReference = new JButton();
            bReference.setIcon(ICO_QST_LGHT);
            bReference.setRolloverIcon(ICO_QST_MDM);
            bReference.setPressedIcon(ICO_QST_DRK);
            bReference.setMargin(INSETS_SMALLBUTTON);
            bReference.setOpaque(false);
            bReference.setContentAreaFilled(false);
            bReference.setBorderPainted(false);
            bReference.putClientProperty("Nimbus.Overrides", uiDef);
            bReference.addActionListener(refActionListener);
            this.add(bReference);
        }
        bReference.setToolTipText(title != null && !title.isEmpty() ? String.format("Reference: %s", title) : "Reference");
        bReference.putClientProperty(PROP_REFERENCE_TEMPLATE_URL, url);
    }

    public void setLinkGuide(String url, String title) {
        Objects.requireNonNull(url);
        if (bGuide == null) {
            bGuide = new JButton();
            bGuide.setIcon(ICO_BOK_LGHT);
            bGuide.setRolloverIcon(ICO_BOK_MDM);
            bGuide.setPressedIcon(ICO_BOK_DRK);
            bGuide.setMargin(INSETS_SMALLBUTTON);
            bGuide.setOpaque(false);
            bGuide.setContentAreaFilled(false);
            bGuide.setBorderPainted(false);
            bGuide.putClientProperty("Nimbus.Overrides", uiDef);
            bGuide.addActionListener(guideActionListener);
            this.add(bGuide);
        }
        bGuide.setToolTipText(title != null && !title.isEmpty() ? String.format("Guide: %s", title) : "Guide");
        bGuide.putClientProperty(PROP_GUIDE_TEMPLATE_URL, url);
    }

    public void adaptToBootVersion(String bootVersion) {
        currentBootVersion = bootVersion;
        String verRange = (String) this.getClientProperty(PROP_VERSION_RANGE);
        String description = (String) this.getClientProperty(PROP_DESCRIPTION);
        final boolean allowable = allowable(verRange, bootVersion);
        cbDep.setEnabled(allowable);
        cbDep.setToolTipText(prepTooltip(description, allowable, verRange));
    }

    public boolean isSelected() {
        return cbDep.isSelected();
    }

    public void setSelected(boolean flag) {
        cbDep.setSelected(flag);
    }

    public String getText() {
        return cbDep.getText();
    }

    /** This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cbDep = new javax.swing.JCheckBox();
        filler = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));
        add(cbDep);
        add(filler);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox cbDep;
    private javax.swing.Box.Filler filler;
    // End of variables declaration//GEN-END:variables

    private boolean allowable(String verRange, String bootVersion) {
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
}
