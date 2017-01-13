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
    public static final String PREF_INITIALIZR_URL = "nbspringboot.initializr.url";
    public static final String PREF_FORCE_COLOR_OUTPUT = "nbspringboot.defaultopts.force-color";
    public static final String PREF_MANUAL_RESTART = "nbspringboot.defaultopts.manual-restart";
    public static final String PREF_VM_OPTS = "nbspringboot.defaultopts.vm-options";

    // prevent instantiation
    private PrefConstants() {
    }

}
