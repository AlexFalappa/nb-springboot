package com.github.alexfalappa.nbspringboot.projects.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Migrate only Spring Boot run properties, preserving unrelated actions and values. */
final class NbActionsMigration {
    private static final Set<String> PROPERTIES = Set.of("jvmArguments", "mainClass", "arguments",
            "commandlineArguments", "profiles", "workingDirectory", "addResources", "fork");

    static byte[] migrate(byte[] original, boolean boot1) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
                @Override public void error(org.xml.sax.SAXParseException ex) throws org.xml.sax.SAXException { throw ex; }
                @Override public void fatalError(org.xml.sax.SAXParseException ex) throws org.xml.sax.SAXException { throw ex; }
            });
            Document document = builder.parse(new ByteArrayInputStream(original));
            boolean changed = false;
            NodeList actions = document.getElementsByTagName("action");
            String from = boot1 ? "spring-boot.run." : "run.";
            String to = boot1 ? "run." : "spring-boot.run.";
            String oldEnv = boot1 ? "Env.SPRING_DEVTOOLS_RESTART_TRIGGERFILE" : "Env.SPRING_DEVTOOLS_RESTART_TRIGGER_FILE";
            String newEnv = boot1 ? "Env.SPRING_DEVTOOLS_RESTART_TRIGGER_FILE" : "Env.SPRING_DEVTOOLS_RESTART_TRIGGERFILE";
            for (int i = 0; i < actions.getLength(); i++) {
                Element action = (Element) actions.item(i);
                NodeList goals = action.getElementsByTagName("goal");
                boolean springRun = false;
                for (int j = 0; j < goals.getLength(); j++) {
                    String goal = goals.item(j).getTextContent().trim();
                    if (goal.contains("spring-boot") && goal.endsWith(":run")) springRun = true;
                }
                if (!springRun) continue;
                NodeList properties = action.getElementsByTagName("properties");
                for (int j = 0; j < properties.getLength(); j++) {
                    Element container = (Element) properties.item(j);
                    for (Node property = container.getFirstChild(); property != null; property = property.getNextSibling()) {
                        String name = property.getNodeName();
                        String replacement = name.equals(oldEnv) ? newEnv
                                : name.startsWith(from) && PROPERTIES.contains(name.substring(from.length()))
                                ? to + name.substring(from.length()) : name;
                        // An explicit modern value takes precedence; do not create duplicate properties.
                        if (!name.equals(replacement) && container.getElementsByTagName(replacement).getLength() == 0) {
                            document.renameNode(property, null, replacement);
                            changed = true;
                        }
                    }
                }
            }
            if (!changed) return original;
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            XMLUtil.write(document, result, "UTF-8");
            return result.toByteArray();
        } catch (Exception ex) {
            throw new IOException("Cannot migrate nbactions.xml; the original file was preserved", ex);
        }
    }

    static void update(FileObject file, boolean boot1) throws IOException {
        try (FileLock lock = file.lock()) {
            byte[] original = file.asBytes();
            byte[] updated = migrate(original, boot1);
            if (updated == original) return;
            java.io.File local = FileUtil.toFile(file);
            if (local == null) throw new IOException("Automatic action migration requires a local filesystem");
            try {
                replace(local.toPath(), updated);
            } finally {
                file.refresh();
            }
        }
    }

    static void replace(Path target, byte[] updated) throws IOException {
        Path temporary = Files.createTempFile(target.getParent(), ".nbactions-", ".tmp");
        try {
            Files.write(temporary, updated);
            // If atomic replacement is unsupported, leave the original untouched.
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
