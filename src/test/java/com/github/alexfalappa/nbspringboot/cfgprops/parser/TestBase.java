package com.github.alexfalappa.nbspringboot.cfgprops.parser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.TreeSet;

import org.junit.experimental.categories.Category;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.errors.ParseError;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

import com.github.alexfalappa.nbspringboot.cfgprops.ParserTests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Base test class to factor out parsing methods.
 *
 * @author Alessandro Falappa
 */
@Category(ParserTests.class)
public class TestBase {

    final protected BootCfgParser parser;
    final protected ReportingParseRunner reportingRunner;
    final protected TracingParseRunner tracingRunner;

    TestBase() {
        parser = Parboiled.createParser(BootCfgParser.class);
        tracingRunner = new TracingParseRunner(parser.cfgProps());
        reportingRunner = new ReportingParseRunner(parser.cfgProps());
    }

    protected void parseNoMatch(String input) {
        ParsingResult<?> result = reportingRunner.run(input);
        if (result.matched) {
            System.out.println("Parser erroneously matched input:");
            listPropsOrdered(parser.getParsedProps());
            result = tracingRunner.run(input);
        }
        assertFalse(result.matched);
    }

    protected void parseMatch(String input) {
        ParsingResult<?> result = reportingRunner.run(input);
        if (result.matched) {
            System.out.println(ParseTreeUtils.printNodeTree(result));
            final Properties pp = parser.getParsedProps();
            if (!pp.isEmpty()) {
                listPropsOrdered(pp);
            }
        } else {
            result = tracingRunner.run(input);
            System.out.println("\n\nParser did not match input:");
            for (ParseError pe : result.parseErrors) {
                System.out.format("\t%s%n", ErrorUtils.printParseError(pe));
            }
        }
        assertTrue(result.matched);
        assertFalse(result.hasErrors());
    }

    protected String readResource(String name) throws IOException, URISyntaxException {
        byte[] encoded = Files.readAllBytes(Paths.get(TestBase.class.getResource(name).toURI()));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    protected void listPropsOrdered(Properties p) {
        if (p.isEmpty()) {
            System.out.println("No properties");
        } else {
            TreeSet<Object> sortedKeys = new TreeSet<>(p.keySet());
            for (Object k : sortedKeys) {
                System.out.printf("[%s] -> [%s]%n", k.toString(), p.get(k).toString());
            }
        }
    }

}
