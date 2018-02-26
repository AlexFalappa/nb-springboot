package com.github.alexfalappa.nbspringboot.cfgprops.parser;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

/**
 * Syntax test suite for BootCfgParser: error scenario.
 *
 * @author Alessandro Falappa
 */
//@Ignore
public class ErrorTest extends TestBase {

    @Test
    public void testInvalidUnicodeKey() throws URISyntaxException, IOException {
        System.out.println("\n-- invalid unicode in key");
        parseNoMatch("key\\u00g9=value");
    }

    @Test
    public void testInvalidUnicodeValue() throws URISyntaxException, IOException {
        System.out.println("\n-- invalid unicode in value");
        parseNoMatch("key=value\\u0GA9");
    }

    @Test
    public void testSpaceInKey() throws URISyntaxException, IOException {
        System.out.println("\n-- space in key");
        parseNoMatch(" \t space key :\tval1");
    }

    @Test
    public void testOnlyOpeningBracket() {
        System.out.println("\n-- only opening bracket");
        parseNoMatch(" \t key[0 :\tval1");
    }

    @Test
    public void testOnlyClosingBracket() {
        System.out.println("\n-- only closing bracket");
        parseNoMatch(" \t key0] :\tval1");
    }
}
