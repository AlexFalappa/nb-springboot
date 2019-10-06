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
package com.github.alexfalappa.nbspringboot;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.boot.configurationmetadata.ValueHint;

import com.github.alexfalappa.nbspringboot.projects.customizer.BootPanel;

import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_VM_OPTS;
import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_VM_OPTS_LAUNCH;
import static java.util.logging.Level.WARNING;
import static java.util.regex.Pattern.compile;

/**
 * Utility methods used in the plugin.
 *
 * @author Alessandro Falappa
 */
public final class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class.getName());
    private static final Pattern p = compile("(\\w+\\.)+(\\w+)");

    // prevent instantiation
    private Utils() {
    }

    /**
     * Simplistic escape of angled brackets in the given string.
     *
     * @param text the string to escape
     * @return escaped string
     */
    public static String simpleHtmlEscape(String text) {
        return text.replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Shortens a string representing a fully qualified Java type.
     * <p>
     * Strips all package names from the type. Also acts on generic parameters.
     * <p>
     * For example {@code java.util.List<java.lang.String>} gets shortened to {@code List<String>}.
     *
     * @param type a Java type string
     * @return the shortened type
     */
    public static String shortenJavaType(String type) {
        return p.matcher(type).replaceAll("$2");
    }

    /**
     * Builds an HTML formatted string with details on a Spring Boot configuration property extracted from its
     * {@code ItemMetadata}.
     *
     * @param cfgMeta the configuration property metadata object
     * @return the HTML formatted configuration property details
     */
    public static String cfgPropDetailsHtml(ConfigurationMetadataProperty cfgMeta) {
        StringBuilder sb = new StringBuilder();
        // deprecation (optional)
        Deprecation deprecation = cfgMeta.getDeprecation();
        if (deprecation != null) {
            sb.append("<b>");
            if (isErrorDeprecated(cfgMeta)) {
                sb.append("REMOVED");
            } else {
                sb.append("Deprecated");
            }
            sb.append("</b>");
            // deprecation reason if present
            String reason = deprecation.getReason();
            if (reason != null) {
                sb.append(": ").append(simpleHtmlEscape(reason));
            }
            sb.append("<br/>");
            String replacement = deprecation.getReplacement();
            if (replacement != null) {
                sb.append("<i>Replaced by:</i> <tt>").append(replacement).append("</tt><br/>");
            }
        }
        // description (optional)
        final String description = cfgMeta.getDescription();
        if (description != null) {
            sb.append(description).append("<br/>");
        }
        // type
        sb.append("<tt>").append(simpleHtmlEscape(shortenJavaType(cfgMeta.getType()))).append("</tt>");
        return sb.toString();
    }

    public static String vmOptsFromPrefs() {
        StringBuilder sb = new StringBuilder();
        if (NbPreferences.forModule(PrefConstants.class).getBoolean(PREF_VM_OPTS_LAUNCH, true)) {
            sb.append(BootPanel.VMOPTS_OPTIMIZE);
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(NbPreferences.forModule(PrefConstants.class).get(PREF_VM_OPTS, ""));
        return sb.toString();
    }

    public static boolean isErrorDeprecated(ConfigurationMetadataProperty meta) {
        Deprecation depr = meta.getDeprecation();
        return depr != null && depr.getLevel() != null && depr.getLevel().equals(Deprecation.Level.ERROR);
    }

    /**
     * Tries to retrieve the most appropriate {@link Project}.
     * <p>
     * Looks first in the global action context, then in the active {@link TopComponent} context. In each case tries first a
     * direct reference then via the owner of a file object and lastly via a data object.
     *
     * @return the active project or null if no active project found
     */
    public static Project getActiveProject() {
        // lookup in global context
        Project prj = Utilities.actionsGlobalContext().lookup(Project.class);
        if (prj != null) {
            logger.log(Level.FINE, "Found project reference in actions global context");
            return prj;
        }
        FileObject foobj = Utilities.actionsGlobalContext().lookup(FileObject.class);
        if (foobj != null) {
            prj = FileOwnerQuery.getOwner(foobj);
            if (prj != null) {
                logger.log(Level.FINE, "Found project reference via file object in actions global context");
                return prj;
            }
        }
        DataObject dobj = Utilities.actionsGlobalContext().lookup(DataObject.class);
        if (dobj != null) {
            FileObject fo = dobj.getPrimaryFile();
            prj = FileOwnerQuery.getOwner(fo);
            if (prj != null) {
                logger.log(Level.FINE, "Found project reference via data object in actions global context");
                return prj;
            }
        }
        // lookup in active editor
        final TopComponent activeEditor = TopComponent.getRegistry().getActivated();
        if (activeEditor != null) {
            final Lookup tcLookup = activeEditor.getLookup();
            prj = tcLookup.lookup(Project.class);
            if (prj != null) {
                logger.log(Level.FINE, "Found project reference in lookup of active editor");
                return prj;
            }
            foobj = tcLookup.lookup(FileObject.class);
            if (foobj != null) {
                prj = FileOwnerQuery.getOwner(foobj);
                if (prj != null) {
                    logger.log(Level.FINE, "Found project reference in lookup of active editor via file object");
                    return prj;
                }
            }
            dobj = tcLookup.lookup(DataObject.class);
            if (dobj != null) {
                FileObject fo = dobj.getPrimaryFile();
                prj = FileOwnerQuery.getOwner(fo);
                if (prj != null) {
                    logger.log(Level.FINE, "Found project reference in lookup of active editor via data object");
                    return prj;
                }
            }
        }
        logger.log(Level.FINE, "Couldn't find active project reference");
        return null;
    }

    /**
     * Retrieves the execute {@code ClassPath} object for the given project.
     *
     * @param proj the project
     * @return found ClassPath object or null
     */
    public static ClassPath execClasspathForProj(Project proj) {
        Sources srcs = ProjectUtils.getSources(proj);
        SourceGroup[] srcGroups = srcs.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        if (srcGroups.length > 0) {
            return ClassPath.getClassPath(srcGroups[0].getRootFolder(), ClassPath.EXECUTE);
        } else {
            logger.log(WARNING, "No sources found for project: {0}", new Object[]{proj.toString()});
        }
        return null;
    }

    public static void completeEnum(ClassPath cp, String dataType, String filter, Consumer<ValueHint> consumer) {
        try {
            Object[] enumvals = cp.getClassLoader(true).loadClass(dataType).getEnumConstants();
            if (enumvals != null) {
                for (Object val : enumvals) {
                    final String valName = val.toString().toLowerCase();
                    if (filter == null || valName.contains(filter)) {
                        consumer.accept(createHint(valName));
                    }
                }
            }
        } catch (ClassNotFoundException ex) {
            // enum not available in project classpath, no completion possible
        }
    }

    /**
     * Create a {@code ValueHint} object from the given value.
     * <p>
     * Created hint has no description.
     *
     * @param value the value to use
     * @return a ValueHint object
     */
    public static ValueHint createHint(String value) {
        ValueHint vh = new ValueHint();
        vh.setValue(value);
        return vh;
    }

    /**
     * Converts an icon from the current L&F defaults into an ImageIcon by painting it.
     * <p>
     * Some ui-icons misbehave in that they unconditionally class-cast to the component type they are mostly painted on.
     * Consequently they blow up if we are trying to paint them anywhere else (f.i. in a renderer). This method tries to
     * instantiate a component of the type expected by the icon.
     * <p>
     * This method is an adaption of a cool trick by Darryl Burke/Rob Camick found at
     * http://tips4java.wordpress.com/2008/12/18/icon-table-cell-renderer/#comment-120
     *
     * @param iconName the name of the icon in UIManager
     * @return an ImageIcon with the Icon image
     */
    public static ImageIcon lafDefaultIcon(String iconName) {
        Icon ico = UIManager.getIcon(iconName);
        BufferedImage image = new BufferedImage(ico.getIconWidth(), ico.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            // paint with a generic java.awt.Component
            ico.paintIcon(new JPanel(), g2, 0, 0);
        } catch (ClassCastException e) {
            try {
                // try to instantiate the needed java.awt.Component
                String className = e.getMessage();
                className = className.substring(className.lastIndexOf(" ") + 1);
                Class<?> clazz = Class.forName(className);
                JComponent standInComponent = getSubstitute(clazz);
                ico.paintIcon(standInComponent, g2, 0, 0);
            } catch (ClassNotFoundException | IllegalAccessException ex) {
                // fallback
                g2.drawRect(0, 0, 16, 16);
                g2.drawLine(0, 0, 16, 16);
                g2.drawLine(16, 0, 0, 16);
            }
        }
        g2.dispose();
        return new ImageIcon(image);
    }

    private static JComponent getSubstitute(Class<?> clazz) throws IllegalAccessException {
        JComponent standInComponent;
        try {
            standInComponent = (JComponent) clazz.newInstance();
        } catch (InstantiationException e) {
            standInComponent = new AbstractButton() {
            };
            ((AbstractButton) standInComponent).setModel(new DefaultButtonModel());
        }
        return standInComponent;
    }
}
