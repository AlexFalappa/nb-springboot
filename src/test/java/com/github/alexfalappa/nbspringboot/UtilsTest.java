/*
 * Copyright 2018 Alessandro Falappa.
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

import org.junit.Test;
import static org.junit.Assert.*;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation;

/**
 *
 * @author Hector Espert
 */
public class UtilsTest {
    
    @Test
    public void testSimpleHtmlEscape() {
        String result = Utils.simpleHtmlEscape("<p>STRING</p>");
        String expected = "&lt;p&gt;STRING&lt;/p&gt;";
        assertEquals(expected, result);
    }

    /**
     * Test of vmOptsFromPrefs method, of class Utils.
     */
    @Test
    public void testVmOptsFromPrefs() {
        String expResult = "-noverify -XX:TieredStopAtLevel=1 ";
        String result = Utils.vmOptsFromPrefs();
        assertEquals(expResult, result);
    }

    /**
     * Test of isErrorDeprecated method, of class Utils.
     */
    @Test
    public void testIsErrorDeprecated() {
        ConfigurationMetadataProperty meta = new ConfigurationMetadataProperty();
        assertFalse(Utils.isErrorDeprecated(meta));
        Deprecation deprecation = new Deprecation();
        deprecation.setLevel(Deprecation.Level.WARNING);
        meta.setDeprecation(deprecation);
        assertFalse(Utils.isErrorDeprecated(meta));
        deprecation.setLevel(Deprecation.Level.ERROR);
        meta.setDeprecation(deprecation);
        assertTrue(Utils.isErrorDeprecated(meta));
    }

}
