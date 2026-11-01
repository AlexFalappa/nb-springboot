package com.github.alexfalappa.nbspringboot.templates;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModernTemplatesTest {
    private String render(String resource, Map<String, Object> values) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_34);
        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate("license", "");
        cfg.setTemplateLoader(loader);
        try (InputStream in = getClass().getResourceAsStream("/com/github/alexfalappa/nbspringboot/" + resource)) {
            assertNotNull(resource, in);
            Template template = new Template(resource, new StringReader(new String(in.readAllBytes(), StandardCharsets.UTF_8)), cfg);
            StringWriter output = new StringWriter();
            template.process(values, output);
            return output.toString();
        }
    }

    @Test public void controllerTemplatesUseTheSelectedServletNamespace() throws Exception {
        for (String name : new String[]{"Controller", "RestController"}) {
            for (String namespace : new String[]{"javax.servlet", "jakarta.servlet"}) {
                Map<String, Object> values = new HashMap<>();
                values.put("project", Map.of("licensePath", "/license"));
                values.put("package", "com.example"); values.put("user", "test");
                values.put("name", "Example"); values.put("errorHandling", name.equals("Controller") ? 3 : 2);
                values.put("crudMethods", false); values.put("servletPackage", namespace);
                String output = render("templates/controller/" + name + ".java.template", values);
                assertTrue(name + ": " + output, output.contains("import " + namespace + ".http.HttpServletRequest;"));
            }
        }
    }

    @Test public void boot3And4RunTemplatesUseModernProperties() throws Exception {
        String output = render("projects/initializr/initializr-nbactions.xml.template", Map.of(
                "isBoot2", true, "vmOpts", "-Dvalue=a&b", "mainClass", "com.example.App", "forceColor", true, "manualRestart", true));
        assertTrue(output.contains("<spring-boot.run.jvmArguments>"));
        assertTrue(output.contains("<spring-boot.run.mainClass>com.example.App"));
        assertFalse(output.contains("<run."));
        assertTrue(output.contains("Env.SPRING_DEVTOOLS_RESTART_TRIGGERFILE"));
        assertTrue(output.contains("a&amp;b"));
        javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new java.io.ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));
    }
}
