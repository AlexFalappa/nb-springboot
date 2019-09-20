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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation;

import com.github.alexfalappa.nbspringboot.projects.customizer.BootPanel;

import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_VM_OPTS;
import static com.github.alexfalappa.nbspringboot.PrefConstants.PREF_VM_OPTS_LAUNCH;
import static java.util.regex.Pattern.compile;

/**
 * Utility methods used in the plugin.
 *
 * @author Alessandro Falappa
 */
public final class Utils {

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
        final Logger logger = Logger.getLogger(Utils.class.getName());
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
}
