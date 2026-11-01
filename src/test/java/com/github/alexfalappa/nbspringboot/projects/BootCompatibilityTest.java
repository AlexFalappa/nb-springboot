package com.github.alexfalappa.nbspringboot.projects;

import org.junit.Test;
import static org.junit.Assert.*;

public class BootCompatibilityTest {
    @Test public void mavenPropertiesRemainModernAfterBoot2() {
        assertFalse(BootCompatibility.usesModernMavenProperties("1.5.22.RELEASE"));
        for (String version : new String[]{"2.7.18", "3.5.7", "4.0.0-RC1", "10.0.0"}) {
            assertTrue(version, BootCompatibility.usesModernMavenProperties(version));
        }
    }

    @Test public void servletNamespacesFollowTheApplicationVersion() {
        assertFalse(BootCompatibility.usesJakarta("2.7.18"));
        assertTrue(BootCompatibility.usesJakarta("3.5.7"));
        assertTrue(BootCompatibility.usesJakarta("4.0.0"));
    }

    @Test public void acceptsSupportedJavaVersions() {
        BootCompatibility.validateJava("2.7.18", "1.8");
        BootCompatibility.validateJava("3.5.7", "17");
        BootCompatibility.validateJava("4.0.0", "21");
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsJava8ForBoot3() {
        BootCompatibility.validateJava("3.5.7", "1.8");
    }
}
