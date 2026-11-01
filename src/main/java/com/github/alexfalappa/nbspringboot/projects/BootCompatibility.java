package com.github.alexfalappa.nbspringboot.projects;

/** Conventions which depend on the application version, not the IDE's JDK. */
public final class BootCompatibility {
    private BootCompatibility() {}

    public static int major(String version) {
        if (version == null || !version.matches("\\d+\\..*")) return 0;
        return Integer.parseInt(version.substring(0, version.indexOf('.')));
    }

    public static boolean usesModernMavenProperties(String version) {
        return major(version) >= 2;
    }

    public static boolean usesJakarta(String version) {
        return major(version) >= 3;
    }

    public static int minimumJava(String version) {
        return usesJakarta(version) ? 17 : 8;
    }

    public static void validateJava(String bootVersion, String javaVersion) {
        String normalized = javaVersion.startsWith("1.") ? javaVersion.substring(2) : javaVersion;
        int java = Integer.parseInt(normalized);
        if (java < minimumJava(bootVersion)) {
            throw new IllegalArgumentException("Spring Boot " + bootVersion + " requires Java " + minimumJava(bootVersion) + " or later");
        }
    }
}
