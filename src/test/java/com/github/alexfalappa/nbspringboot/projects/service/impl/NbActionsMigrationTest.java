package com.github.alexfalappa.nbspringboot.projects.service.impl;

import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class NbActionsMigrationTest {
    @org.junit.Rule public org.junit.rules.TemporaryFolder folder = new org.junit.rules.TemporaryFolder();
    private byte[] bytes(String xml) { return xml.getBytes(StandardCharsets.UTF_8); }
    private String action(String goal, String properties) {
        return "<action><actionName>run</actionName><goals><goal>" + goal
                + "</goal></goals><properties>" + properties + "</properties></action>";
    }

    @Test public void migratesOnlySpringActionsAndKeepsValues() throws Exception {
        String spring = action("spring-boot:run", "<run.jvmArguments>-Dname=José &amp; x</run.jvmArguments>"
                + "<run.custom>keep</run.custom><Env.SPRING_DEVTOOLS_RESTART_TRIGGER_FILE>.nbRestartTrigger</Env.SPRING_DEVTOOLS_RESTART_TRIGGER_FILE>");
        String custom = action("exec:java", "<run.jvmArguments>unchanged</run.jvmArguments>");
        String result = new String(NbActionsMigration.migrate(bytes("<actions><!-- keep -->" + spring + custom + "</actions>"), false), StandardCharsets.UTF_8);
        assertTrue(result.contains("<spring-boot.run.jvmArguments>-Dname=José &amp; x</spring-boot.run.jvmArguments>"));
        assertTrue(result.contains("<run.jvmArguments>unchanged</run.jvmArguments>"));
        assertTrue(result.contains("<run.custom>keep</run.custom>"));
        assertTrue(result.contains("Env.SPRING_DEVTOOLS_RESTART_TRIGGERFILE"));
        assertTrue(result.contains("<!-- keep -->"));
    }

    @Test public void modernFilesAreNotRewritten() throws Exception {
        byte[] original = bytes("<actions>" + action("spring-boot:run", "<spring-boot.run.arguments>x</spring-boot.run.arguments>") + "</actions>");
        assertSame(original, NbActionsMigration.migrate(original, false));
    }

    @Test public void supportsLegacyProjectsWithoutDuplicateNames() throws Exception {
        String original = "<actions>" + action("spring-boot:run", "<spring-boot.run.mainClass>App</spring-boot.run.mainClass>") + "</actions>";
        String migrated = new String(NbActionsMigration.migrate(bytes(original), true), StandardCharsets.UTF_8);
        assertTrue(migrated.contains("<run.mainClass>App</run.mainClass>"));
        String conflicting = "<actions>" + action("spring-boot:run", "<run.arguments>old</run.arguments><spring-boot.run.arguments>new</spring-boot.run.arguments>") + "</actions>";
        byte[] input = bytes(conflicting);
        assertSame(input, NbActionsMigration.migrate(input, false));
    }

    @Test(expected = java.io.IOException.class) public void invalidXmlIsRejectedBeforeWriting() throws Exception {
        NbActionsMigration.migrate(bytes("<actions><broken>"), false);
    }

    @Test public void atomicReplacementDoesNotCollideWithAnExistingTemporaryFile() throws Exception {
        java.nio.file.Path actions = folder.newFile("nbactions.xml").toPath();
        java.nio.file.Path other = folder.newFile("nbactions.tmp").toPath();
        java.nio.file.Files.writeString(other, "unrelated");
        java.nio.file.Files.writeString(actions, "original");
        NbActionsMigration.replace(actions, bytes("updated"));
        assertEquals("updated", java.nio.file.Files.readString(actions));
        assertEquals("unrelated", java.nio.file.Files.readString(other));
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(folder.getRoot().toPath())) {
            assertEquals(2, files.count());
        }
    }
}
