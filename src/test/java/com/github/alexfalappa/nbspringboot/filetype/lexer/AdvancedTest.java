package com.github.alexfalappa.nbspringboot.filetype.lexer;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

/**
 *
 * @author Alessandro Falappa
 */
//@Ignore
public class AdvancedTest extends TestBase {

    @Test
    public void testSingleEqualContinuation() throws URISyntaxException, IOException {
        System.out.println("\n-- single equal continuation");
        parseMatch("key=val\\\non next line");
    }

    @Test
    public void testSingleEqualContinuationSlash() throws URISyntaxException, IOException {
        System.out.println("\n-- single equal continuation slash");
        parseMatch(" anotherkey = slash before\\\\\\\n"
                + "continuation");
    }

    @Test
    public void testSingleEqualContinuationSpace() throws URISyntaxException, IOException {
        System.out.println("\n-- single equal continuation space");
        parseMatch("key2=value\\\n"
                + "    with space at start of continuation");
    }

    @Test
    public void testMultipleEqualContinuation() throws URISyntaxException, IOException {
        System.out.println("\n-- multiple equal continuation");
        parseMatch("key1=first\\\n"
                + "value\n"
                + "key2=second\\\n"
                + "value");
    }

}
