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
package com.github.alexfalappa.nbspringboot.navigator;

import java.awt.Color;
import java.awt.Component;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Table cell renderer for {@code RequestMethods} constants.
 *
 * @author Alessandro Falappa
 */
public class RequestMethodCellRenderer extends DefaultTableCellRenderer {

    // foreground colors
    private static final Color FG_DELETE = new Color(162, 113, 88);
    private static final Color FG_GET = new Color(91, 154, 107);
    private static final Color FG_POST = new Color(114, 129, 162);
    private static final Color FG_PUT = new Color(124, 139, 172);
    private static final Color FG_SECONDARY = new Color(134, 134, 134);
    // background colors
    private static final Color BG_DELETE = new Color(252, 203, 178);
    private static final Color BG_GET = new Color(181, 244, 197);
    private static final Color BG_POST = new Color(204, 219, 252);
    private static final Color BG_PUT = new Color(214, 229, 252);
    private static final Color BG_SECONDARY = new Color(224, 224, 224);
    // color maps
    private static final Map<RequestMethod, Color> fgColors = new EnumMap<>(RequestMethod.class);
    private static final Map<RequestMethod, Color> bgColors = new EnumMap<>(RequestMethod.class);

    static {
        // setup foreground color map
        fgColors.put(RequestMethod.DELETE, FG_DELETE);
        fgColors.put(RequestMethod.GET, FG_GET);
        fgColors.put(RequestMethod.HEAD, FG_SECONDARY);
        fgColors.put(RequestMethod.OPTIONS, FG_SECONDARY);
        fgColors.put(RequestMethod.PATCH, FG_SECONDARY);
        fgColors.put(RequestMethod.POST, FG_POST);
        fgColors.put(RequestMethod.PUT, FG_PUT);
        fgColors.put(RequestMethod.TRACE, FG_SECONDARY);
        // setup background color map
        bgColors.put(RequestMethod.DELETE, BG_DELETE);
        bgColors.put(RequestMethod.GET, BG_GET);
        bgColors.put(RequestMethod.HEAD, BG_SECONDARY);
        bgColors.put(RequestMethod.OPTIONS, BG_SECONDARY);
        bgColors.put(RequestMethod.PATCH, BG_SECONDARY);
        bgColors.put(RequestMethod.POST, BG_POST);
        bgColors.put(RequestMethod.PUT, BG_PUT);
        bgColors.put(RequestMethod.TRACE, BG_SECONDARY);
    }

    /**
     * Customize foreground colors.
     *
     * @param newFgColors map of colors indexed by request method
     */
    public static void setFgColors(EnumMap<RequestMethod, Color> newFgColors) {
        fgColors.clear();
        fgColors.putAll(fgColors);
    }

    /**
     * Customize background colors.
     *
     * @param newBgColors map of colors indexed by request method
     */
    public static void setBgColors(EnumMap<RequestMethod, Color> newBgColors) {
        fgColors.clear();
        fgColors.putAll(fgColors);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (!isSelected) {
            if (value == null) {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            } else if (value instanceof RequestMethod) {
                final RequestMethod reqMethod = (RequestMethod) value;
                setForeground(fgColors.get(reqMethod));
                setBackground(bgColors.get(reqMethod));
            }
        }
        return this;
    }

}
