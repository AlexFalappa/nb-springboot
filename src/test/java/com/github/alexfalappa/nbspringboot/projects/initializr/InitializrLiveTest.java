package com.github.alexfalappa.nbspringboot.projects.initializr;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.*;

/** Opt-in smoke check against the real service; regular builds remain deterministic. */
public class InitializrLiveTest {
    @Test public void serviceDefaultsGenerateAMavenProject() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("initializr.live"));
        InitializrService service = new InitializrService("https://start.spring.io");
        JsonNode metadata = service.getMetadata();
        String boot = metadata.path("bootVersion").path("default").asText();
        String java = metadata.path("javaVersion").path("default").asText();
        assertFalse(boot.isEmpty());
        assertFalse(java.isEmpty());
        assertNotNull(service.getDependencies(boot).get("dependencies"));
        boolean pom = false;
        try (InputStream response = service.getProject(boot, "com.example", "smoke", "0.0.1-SNAPSHOT",
                "smoke", "Initializr integration check", "jar", "com.example.smoke", "java", java, "");
                ZipInputStream zip = new ZipInputStream(response)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals("pom.xml")) pom = true;
            }
        }
        assertTrue("Service response must contain pom.xml", pom);
    }
}
