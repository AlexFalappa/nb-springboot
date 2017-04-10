package com.github.alexfalappa.nbspringboot.filetype.lexer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test comparing {@code java.util.Properties} loading versus parsing.
 *
 * @author Alessandro Falappa
 */
@Ignore
public class CfgVsPropsTest extends TestBase {

    @Test
    public void testCompareProps() throws IOException, URISyntaxException {
        System.out.println("\n--- compare props");
        Properties loaded = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/load.properties")) {
            System.out.println("\nLOADED");
            loaded.load(is);
            listPropsOrdered(loaded);
        }
        InputStream is = getClass().getResourceAsStream("/load.properties");
//        try (InputStream is = getClass().getResourceAsStream("/load.properties")) {
        System.out.println("\nPARSED");
//            BootCfgParser cp = new BootCfgParser(is);
//            cp.disable_tracing();
//            cp.parse();
        final Properties parsed = new Properties();//cp.getParsedProps();
        listPropsOrdered(parsed);
        for (Map.Entry<Object, Object> entry : loaded.entrySet()) {
            final Object loadedKey = entry.getKey();
            assertTrue(String.format("Missing key '%s' in parsed", loadedKey.toString()), parsed.containsKey(loadedKey));
            final Object loadedVal = entry.getValue();
            final String parsedVal = parsed.getProperty(loadedKey.toString());
            assertEquals(String.format("Loaded value '%s' differs from parsed one '%s'", loadedVal, parsedVal), loadedVal, parsedVal);
        }
//        } catch (ParseException ex) {
//            fail(ex.getMessage());
//        }
    }

    public void testWriteProps() throws IOException {
        System.out.println("\n--- write props");
        Properties p = new Properties();
        p.setProperty("key", "value");
        p.setProperty("a=key", "value");
        p.setProperty("the#key", "value");
        p.setProperty("one!key", "value");
        p.setProperty("my key", "value");
        p.setProperty("anoth:key", "value");
        p.setProperty("key1", "the value");
        p.setProperty("key2", "a#value");
        p.setProperty("key3", "one!value");
        p.setProperty("key4", "my=value");
        p.setProperty("key5", "anoth:value");
        p.setProperty("spaces", "a value with spaces");
        p.setProperty("slashes", "a\\value\\with\\slashes");
        p.setProperty("linefeed", "a value\nwith line\nfeeds");
        p.setProperty("unicode", "©àèìòù");
        try (OutputStream os = Files.newOutputStream(Paths.get("write.properties"))) {
            p.store(os, "This is a comment");
        }
    }

}
