/*
 * Copyright 2019 the original author or authors.
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
package com.github.alexfalappa.nbspringboot.projects.service.api;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.filechooser.FileSystemView;

/**
 * Support data for configuration property values completion.
 *
 * @author Alessandro Falappa
 */
public final class HintSupport {

    private static Set<String> cachedCharsets = null;
    private static Set<String> cachedLocales = null;
    private final static FileSystemView fsView = FileSystemView.getFileSystemView();
    private final static Map<String, ImageIcon> iconCache = new HashMap<>();
    public final static Set<String> MIMETYPES = new HashSet<>();

    // prevent instantiation
    private HintSupport() {
    }

    static {
        MIMETYPES.add("*/*");
        MIMETYPES.add("application/json");
        MIMETYPES.add("application/octet-stream");
        MIMETYPES.add("application/xml");
        MIMETYPES.add("image/gif");
        MIMETYPES.add("image/jpeg");
        MIMETYPES.add("image/png");
        MIMETYPES.add("text/html");
        MIMETYPES.add("text/plain");
        MIMETYPES.add("text/xml");
    }

    /**
     * Returns the set of all available {@link Charset} ids caching them.
     * <p>
     * Further calls get the cached set.
     *
     * @return the Set of Charset ids
     */
    public static synchronized Set<String> getAllCharsets() {
        if (cachedCharsets == null) {
            cachedCharsets = Charset.availableCharsets().keySet();
        }
        return cachedCharsets;
    }

    /**
     * Returns the set of all available {@link Locale} ids caching them.
     * <p>
     * Further calls get the cached set.
     *
     * @return the Set of Charset ids
     */
    public static synchronized Set<String> getAllLocales() {
        if (cachedLocales == null) {
            final Locale[] availableLocales = Locale.getAvailableLocales();
            cachedLocales = new HashSet<>(availableLocales.length);
            for (Locale loc : availableLocales) {
                final String locName = loc.toString();
                if (!locName.isEmpty()) {
                    cachedLocales.add(locName);
                }
            }
        }
        return cachedLocales;
    }

    /**
     * Returns the system icon for a given file, caching it and converting it to {@link ImageIcon} if necessary.
     * <p>
     * Depending on the platform file type specific icons might be returned.
     *
     * @param file a {@link File} object
     * @return the possibly cached {@link ImageIcon} for the given file
     */
    public static synchronized ImageIcon getIconFor(File file) {
        Icon ico = fsView.getSystemIcon(file);
        final String key = ico.toString();
        if (iconCache.containsKey(key)) {
            return iconCache.get(key);
        } else {
            ImageIcon imgIco;
            if (ico instanceof ImageIcon) {
                imgIco = (ImageIcon) ico;
            } else {
                BufferedImage image = new BufferedImage(ico.getIconWidth(), ico.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = image.createGraphics();
                ico.paintIcon(new JPanel(), g2, 0, 0);
                g2.dispose();
                imgIco = new ImageIcon(image);
            }
            iconCache.put(key, imgIco);
            return imgIco;
        }
    }
}
