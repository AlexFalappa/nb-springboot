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
package com.github.alexfalappa.nbspringboot.templates;

import org.openide.WizardDescriptor;

/**
 * Utility methods for project wizard classes.
 *
 * @author Alessandro Falappa
 */
public final class TemplateUtils {

    // prevent instantiation
    private TemplateUtils() {
    }

    /**
     * Merge the steps list to set in visual components of wizard pages.
     * <p>
     * This is usually used in initialize(WizardDescriptor wz) methods of Iterators classes when preparing the wizard pages and
     * related visual components.
     *
     * @param before previous step names
     * @param after step names of the wizard
     * @return combined step names
     */
    public static String[] createSteps(String[] before, String[] after) {
        assert after != null;
        // hack to use the steps set before this panel processed
        int diff = 0;
        if (before == null) {
            before = new String[0];
        } else if (before.length > 0) {
            diff = ("...".equals(before[before.length - 1])) ? 1 : 0; // NOI18N
        }
        String[] res = new String[(before.length - diff) + after.length];
        for (int i = 0; i < res.length; i++) {
            if (i < (before.length - diff)) {
                res[i] = before[i];
            } else {
                res[i] = after[i - before.length + diff];
            }
        }
        return res;
    }

    /**
     * Merge the steps list to set in visual components of wizard pages.
     * <p>
     * This is usually used in initialize(WizardDescriptor wz) methods of Iterators classes when preparing the wizard pages and
     * related visual components.
     *
     * @param wizard the wizard descriptor to take previous step names from
     * @param after step names of the wizard
     * @return combined step names
     */
    public static String[] createSteps(WizardDescriptor wizard, String[] after) {
        assert after != null;
        String[] before = new String[0];
        Object prop = wizard.getProperty(WizardDescriptor.PROP_CONTENT_DATA);
        if (prop != null && prop instanceof String[]) {
            before = (String[]) prop;
        }        // hack to use the steps set before this panel processed
        int diff = 0;
        if (before.length > 0) {
            diff = ("...".equals(before[before.length - 1])) ? 1 : 0; // NOI18N
        }
        String[] res = new String[(before.length - diff) + after.length];
        for (int i = 0; i < res.length; i++) {
            if (i < (before.length - diff)) {
                res[i] = before[i];
            } else {
                res[i] = after[i - before.length + diff];
            }
        }
        return res;
    }

}
