package com.github.alexfalappa.nbspringboot.cfgprops.lexer;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

/**
 * Syntax test suite for BootCfgParser.
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
    public void testInvalidArrayNotation1() throws URISyntaxException, IOException {
        System.out.println("\n-- invalid array notation 1");
        parseNoMatch(" \t array[00] =\tval1\n");
    }

    @Test
    public void testInvalidArrayNotation2() throws URISyntaxException, IOException {
        System.out.println("\n-- invalid array notation 2");
        parseNoMatch(" prefix.array[01]= val2\n");
    }

    @Test
    public void testInvalidArrayNotation3() throws URISyntaxException, IOException {
        System.out.println("\n-- invalid array notation 3");
        parseNoMatch(" prefix.arr[a]:v3");
    }

    @Test
    public void testSpaceInKey() throws URISyntaxException, IOException {
        System.out.println("\n-- space in key");
        parseNoMatch(" \t space key :\tval1");
    }
}
