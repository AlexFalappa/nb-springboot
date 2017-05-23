package com.github.alexfalappa.nbspringboot.cfgprops.parser;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

/**
 * Syntax test suite for BootCfgParser: intermediate scenario.
 *
 * @author Alessandro Falappa
 */
//@Ignore
public class IntermediateTest extends TestBase {

    @Test
    public void testEmptyLines() throws URISyntaxException, IOException {
        System.out.println("\n-- empty lines");
        parseMatch("\n"
                + " \n"
                + "\t\n"
                + "\f");
    }

    @Test
    public void testCommentLines() throws URISyntaxException, IOException {
        System.out.println("\n-- comment lines");
        parseMatch("# pound sign comment\n"
                + "! exclamation mark comment\n"
                + " \t # pound sign with initial whitespace\n"
                + "  \t! exclamation mark with initial whitespace");
    }

    @Test
    public void testCommentAndEmptyLines() throws URISyntaxException, IOException {
        System.out.println("\n-- comment and emptyLines");
        parseMatch("# pound sign comment\n"
                + "\n"
                + "! exclamation mark comment\n");
    }

    @Test
    public void testKeysOnly() throws URISyntaxException, IOException {
        System.out.println("\n-- keys only");
        parseMatch("key1\n"
                + "key2");
    }

    @Test
    public void testSpaceValue() throws URISyntaxException, IOException {
        System.out.println("\n-- space in value");
        parseMatch("key=val with spaces");
    }

    @Test
    public void testTabKey() throws URISyntaxException, IOException {
        System.out.println("\n-- tab in key");
        parseMatch("key\\twith\\ttabs=val");
    }

    @Test
    public void testTabValue() throws URISyntaxException, IOException {
        System.out.println("\n-- tab in value");
        parseMatch("key=val\\twith\\ttabs");
    }

    @Test
    public void testLinefeedKey() throws URISyntaxException, IOException {
        System.out.println("\n-- linefeed in key");
        parseMatch("key\\nwith\\nlinefeeds=val");
    }

    @Test
    public void testLinefeedValue() throws URISyntaxException, IOException {
        System.out.println("\n-- linefeed in value");
        parseMatch("key=val\\nwith\\nlinefeeds");
    }

    @Test
    public void testEscapedSpaceKey() throws URISyntaxException, IOException {
        System.out.println("\n-- escaped space in key");
        parseMatch("key\\ with\\ spaces=val");
    }

    @Test
    public void testEscapedSpaceValue() throws URISyntaxException, IOException {
        System.out.println("\n-- escaped space in value");
        parseMatch("key=val\\ with\\ spaces");
    }

    @Test
    public void testEscapedBackslashKey() throws URISyntaxException, IOException {
        System.out.println("\n-- escaped backslash in key");
        parseMatch("key\\\\with\\\\slashes=val");
    }

    @Test
    public void testEscapedBackslashValue() throws URISyntaxException, IOException {
        System.out.println("\n-- escaped backslash in value");
        parseMatch("key=val\\\\with\\\\slashes");
    }

    @Test
    public void testEscapedCommentKey() throws URISyntaxException, IOException {
        System.out.println("\n-- escaped comment signs in key");
        parseMatch("key\\#with\\!comment=value");
    }

    @Test
    public void testEscapedCommentValue() throws URISyntaxException, IOException {
        System.out.println("\n-- escaped comment signs in value");
        parseMatch("key=value\\#with\\!escaped comment signs");
    }

    @Test
    public void testEscapedEqualKey() throws URISyntaxException, IOException {
        System.out.println("\n-- escaped equal in key");
        parseMatch("a\\=key=val");
    }

    @Test
    public void testEscapedEqualValue() throws URISyntaxException, IOException {
        System.out.println("\n-- escaped equal in value");
        parseMatch("key=a\\=val");
    }

    @Test
    public void testEscapedColonKey() throws URISyntaxException, IOException {
        System.out.println("\n-- escaped colon in key");
        parseMatch("a\\:key:val");
    }

    @Test
    public void testEscapedColonValue() throws URISyntaxException, IOException {
        System.out.println("\n-- escaped colon in value");
        parseMatch("key:a\\:val");
    }

    @Test
    public void testUnicodeKey() throws URISyntaxException, IOException {
        System.out.println("\n-- unicode in key");
        parseMatch("key\\u00a9=value");
    }

    @Test
    public void testUnicodeValue() throws URISyntaxException, IOException {
        System.out.println("\n-- unicode in value");
        parseMatch("key=value\\u00A9");
    }

    @Test
    public void testMultipleMixedSep() throws URISyntaxException, IOException {
        System.out.println("\n-- multiple mixed separators");
        parseMatch("key1:val1\n"
                + "key2=val2");
    }

    @Test
    public void testArrayNotation() throws URISyntaxException, IOException {
        System.out.println("\n-- array notation");
        parseMatch(" \t array[12] =\tval1");
    }

    @Test
    public void testMultipleArrayNotation() throws URISyntaxException, IOException {
        System.out.println("\n-- multiple array notation");
        parseMatch(" \t array[0] =\tval1\n"
                + " prefix.array[1]=val2");
    }

}
