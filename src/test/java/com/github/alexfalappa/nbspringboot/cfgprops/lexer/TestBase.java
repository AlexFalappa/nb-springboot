package com.github.alexfalappa.nbspringboot.cfgprops.lexer;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.TreeSet;

import org.junit.experimental.categories.Category;

import com.github.alexfalappa.nbspringboot.cfgprops.LexerTests;

import static org.junit.Assert.fail;

/**
 * Base test class to factor out parsing methods.
 *
 * @author Alessandro Falappa
 */
@Category(LexerTests.class)
public class TestBase {

    protected void parseNoMatch(String input) {
        System.out.println(input);
        try {
            try (StringReader sr = new StringReader(input)) {
                CfgPropsScanner bcps = new CfgPropsScanner(sr);
                CfgPropsTokenId tok = bcps.nextTokenId();
                while (tok != null) {
                    System.out.println(tok);
                    tok = bcps.nextTokenId();
                }
                fail("Parsed while it should not");
            }
        } catch (IOException ex) {
            // should fail
            System.out.println(ex.getMessage());
            System.out.println("OK");
        }
    }

    protected void parseMatch(String input) {
        System.out.println(input);
        try {
            try (StringReader sr = new StringReader(input)) {
                CfgPropsScanner bcps = new CfgPropsScanner(sr);
                CfgPropsTokenId tok = bcps.nextTokenId();
                while (tok != null) {
                    System.out.println(tok);
                    tok = bcps.nextTokenId();
                }
            }
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    protected String readResource(String name) throws IOException, URISyntaxException {
        byte[] encoded = Files.readAllBytes(Paths.get(TestBase.class.getResource(name).toURI()));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    protected void listPropsOrdered(Properties p) {
        if (p.isEmpty()) {
            System.out.println("No properties");
        } else {
            System.out.println("Listing properties ordered by key in [key] -> [value] format:");
            TreeSet<Object> sortedKeys = new TreeSet<>(p.keySet());
            for (Object k : sortedKeys) {
                System.out.printf("[%s] -> [%s]%n", k.toString(), p.get(k).toString());
            }
        }
    }

}
