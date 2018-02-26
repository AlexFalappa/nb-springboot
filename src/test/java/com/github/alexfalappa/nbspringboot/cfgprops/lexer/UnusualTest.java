package com.github.alexfalappa.nbspringboot.cfgprops.lexer;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

/**
 * Syntax test suite for CfgPropsScanner.
 *
 * @author Alessandro Falappa
 */
//@Ignore
public class UnusualTest extends TestBase {

    @Test
    public void testOnlyOpenBracket() throws URISyntaxException, IOException {
        System.out.println("\n-- only opening bracket");
        parseMatch("map[one=val\narr[2=val2");
    }

    @Test
    public void testOnlyCloseBracket() throws URISyntaxException, IOException {
        System.out.println("\n-- only closing bracket");
        parseMatch("keyone]=val\narr2]=val2");
    }

    @Test
    public void testOpenDanglingBracket() throws URISyntaxException, IOException {
        System.out.println("\n-- only open dangling bracket");
        parseMatch("map[\nkey=val2");
    }

}
