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
package com.github.alexfalappa.nbspringboot;

/**
 * Plugin preference constants.
 *
 * @author Alessandro Falappa
 */
public final class PrefConstants {

    public static final String PREF_INITIALIZR_TIMEOUT = "nbspringboot.initializr.timeout";
    public static final String PREF_INITIALIZR_URL = "nbspringboot.initializr-service.url";
    public static final String PREF_FORCE_COLOR_OUTPUT = "nbspringboot.defaultopts.force-color";
    public static final String PREF_MANUAL_RESTART = "nbspringboot.defaultopts.manual-restart";
    public static final String PREF_VM_OPTS = "nbspringboot.defaultopts.vm-options";
    public static final String PREF_VM_OPTS_LAUNCH = "nbspringboot.defaultopts.launch-vm-options";
    public static final String PREF_DEPR_SORT_LAST = "nbspringboot.defaultopts.sort-deprecated-last";
    public static final String PREF_DEPR_ERROR_SHOW = "nbspringboot.defaultopts.show-error-deprecated";
    public static final String PREF_HLIGHT_LEV_SYNERR = "nbspringboot.highlight-level.syntax-errors";
    public static final String PREF_HLIGHT_LEV_DUPLICATES = "nbspringboot.highlight-level.duplicates";
    public static final String PREF_HLIGHT_LEV_DTMISMATCH = "nbspringboot.highlight-level.data-type-mismatches";
    public static final String PREF_HLIGHT_LEV_UNKNOWN = "nbspringboot.highlight-level.unknown-props";
    public static final String PREF_HLIGHT_LEV_DEPRECATED = "nbspringboot.highlight-level.deprecated-props";

    public static final String DEFAULT_INITIALIZR_URL = "https://start.spring.io";

    // prevent instantiation
    private PrefConstants() {
    }

}
