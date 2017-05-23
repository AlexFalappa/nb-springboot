package com.github.alexfalappa.nbspringboot.cfgprops.parser;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

/**
 * Syntax test suite for BootCfgParser: basic scenario.
 *
 * @author Alessandro Falappa
 */
//@Ignore
public class BasicTest extends TestBase {

    @Test
    public void testEmpty() throws URISyntaxException, IOException {
        System.out.println("\n-- empty");
        parseMatch("");
    }

    @Test
    public void testComment1() throws URISyntaxException, IOException {
        System.out.println("\n-- comment1");
        parseMatch("# pound sign comment");
    }

    @Test
    public void testComment2() throws URISyntaxException, IOException {
        System.out.println("\n-- comment2");
        parseMatch("! exclamation mark comment");
    }

    @Test
    public void testKeyOnly() throws URISyntaxException, IOException {
        System.out.println("\n-- key only");
        parseMatch("key");
    }

    @Test
    public void testSingleEqualEmpty() throws URISyntaxException, IOException {
        System.out.println("\n-- single empty equal");
        parseMatch("key=");
    }

    @Test
    public void testSingleEqualValued() throws URISyntaxException, IOException {
        System.out.println("\n-- single equal");
        parseMatch("key=val");
    }

    @Test
    public void testSingleEqualDotted() throws URISyntaxException, IOException {
        System.out.println("\n-- single dotted equal");
        parseMatch("prefix.key=val");
    }

    @Test
    public void testSingleColonEmpty() throws URISyntaxException, IOException {
        System.out.println("\n-- single empty colon");
        parseMatch("key:");
    }

    @Test
    public void testSingleColonValued() throws URISyntaxException, IOException {
        System.out.println("\n-- single colon");
        parseMatch("key:val");
    }

    @Test
    public void testSingleDottedColon() throws URISyntaxException, IOException {
        System.out.println("\n-- single dotted colon");
        parseMatch("prefix.middle.key:val");
    }

    @Test
    public void testSingleWhitespace1() throws URISyntaxException, IOException {
        System.out.println("\n-- single with withespace 1");
        parseMatch(" \t key =\tval ");
    }

    @Test
    public void testSingleWhitespace2() throws URISyntaxException, IOException {
        System.out.println("\n-- single with withespace 2");
        parseMatch(" \t key =\t val");
    }

    @Test
    public void testSingleWhitespace3() throws URISyntaxException, IOException {
        System.out.println("\n-- single with withespace 3");
        parseMatch(" \t key  =val");
    }

    @Test
    public void testSingleWhitespace4() throws URISyntaxException, IOException {
        System.out.println("\n-- single with withespace 4");
        parseMatch(" \t key=val");
    }
}
