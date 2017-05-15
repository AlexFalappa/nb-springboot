package com.github.alexfalappa.nbspringboot.cfgprops.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.errors.ParseError;
import org.parboiled.support.ParsingResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test comparing {@code java.util.Properties} loading versus parsing.
 *
 * @author Alessandro Falappa
 */
//@Ignore
public class CfgVsPropsTest extends TestBase {

    @Test
    public void testCompareProps() throws IOException, URISyntaxException {
        System.out.println("\n--- compare props");
        try (InputStream is = getClass().getResourceAsStream("/load.properties")) {
            System.out.println("\nLOADED");
            Properties loaded = new Properties();
            loaded.load(is);
            listPropsOrdered(loaded);
            System.out.println("\nPARSED");
            final String strFile = readResource("/load.properties");
            ParsingResult pr = reportingRunner.run(strFile);
            final Properties parsed = parser.getParsedProps();
            listPropsOrdered(parsed);
            if (!pr.matched) {
//                pr = tracingRunner.run(strFile);
                System.out.println("\n\nParser did not match input:");
                for (Object err : reportingRunner.getParseErrors()) {
                    ParseError pe = (ParseError) err;
                    System.out.format("\t%s%n", ErrorUtils.printParseError(pe));
                }
            }
            assertTrue(pr.matched);
            for (Map.Entry<Object, Object> entry : loaded.entrySet()) {
                assertTrue("Missing key in parsed", parsed.containsKey(entry.getKey()));
                assertTrue("Missing value in parsed", parsed.containsValue(entry.getValue()));
                assertEquals("Different loaded-parsed value", entry.getValue(), parsed.getProperty(entry.getKey().toString()));
            }
        }
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
