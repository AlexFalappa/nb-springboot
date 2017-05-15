package com.github.alexfalappa.nbspringboot.cfgprops.parser;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Ignore;
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
    public void testInvalidArrayNotation() throws URISyntaxException, IOException {
        System.out.println("\n-- invalid array notation");
        parseNoMatch(" \t array[00] =\tval1\n"
                + " prefix.array[01]= val2\n"
                + " prefix.arr[a]:v3");
    }

    @Test
    @Ignore
    public void testSpaceInKey() throws URISyntaxException, IOException {
        System.out.println("\n-- space in key");
        parseNoMatch(" \t space key :\tval1");
    }
}
