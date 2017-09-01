package com.github.alexfalappa.nbspringboot.cfgprops.parser;

import java.util.Properties;

import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.support.StringBuilderVar;
import org.parboiled.support.ValueStack;
import org.parboiled.support.Var;

import com.github.alexfalappa.nbspringboot.cfgprops.ast.CfgElement;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.CfgFile;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.KeyElement;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.PairElement;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.ValueElement;

/**
 * Spring Boot configuration properties parser based on Parboiled library.
 * <p>
 * This parser implements a grammar accepting the Java Properties format (with some minor exceptions) and adding dot separated keys, array
 * notation ({@code array[index]=value}.
 * <p>
 * Differences with base Java Properties syntax:
 * <ul>
 * <li>values must be explicitly separated from keys by a <tt>=</tt> (equal sign) or <tt>:</tt> (colon), first occurring whitespace as
 * separator is not supported.
 * </ul>
 *
 * @author Alessandro Falappa
 */
public class CfgPropsParboiled extends BaseParser<CfgElement> {

    private Properties parsedProps = new Properties();
//    private Map<Integer, Pair<String, String>> propLines = new HashMap<>();
    private CfgFile cfgFile = new CfgFile();

    public Properties getParsedProps() {
        return parsedProps;
    }

//    public Map<Integer, Pair<String, String>> getPropLines() {
//        return propLines;
//    }
    public CfgFile getCfgFile() {
        return cfgFile;
    }

    public void reset() {
        parsedProps.clear();
//        propLines.clear();
        cfgFile = new CfgFile();
    }

    Action<CfgElement> keyStoreProp = new Action<CfgElement>() {
        @Override
        public boolean run(Context<CfgElement> context) {
            if (!context.inErrorRecovery()) {
                final int start = context.getMatchStartIndex();
                final int end = context.getMatchEndIndex();
                final String key = context.getInputBuffer().extract(start, end);
                System.out.printf("[%3d;%3d] key: '%s'%n", start, end, key);
                KeyElement elemKey = new KeyElement(start, end, key);
                context.getValueStack().push(elemKey);
            }
            return true;
        }
    };

    Action<CfgElement> valueStoreProp = new Action<CfgElement>() {
        @Override
        public boolean run(Context<CfgElement> context) {
            if (!context.inErrorRecovery()) {
                final int start = context.getMatchStartIndex();
                final int end = context.getMatchEndIndex();
                final String value = context.getInputBuffer().extract(start, end);
                System.out.printf("[%3d;%3d] value: '%s'%n", start, end, value);
                ValueElement elemVal = new ValueElement(start, end, value);
                context.getValueStack().push(elemVal);
            }
            return true;
        }
    };

    Action<CfgElement> actionStoreProp = new Action<CfgElement>() {
        @Override
        public boolean run(Context<CfgElement> context) {
            final ValueStack<CfgElement> stack = context.getValueStack();
            if (!context.hasError()) {
                PairElement pair = new PairElement();
                int size = stack.size();
                switch (size) {
                    case 1:
                        CfgElement elemKey = stack.pop();
                        parsedProps.setProperty(elemKey.getText(), "");
                        pair.setKey(elemKey);
//                        propLines.put(line, Pair.of(elem, ""));
                        break;
                    case 2:
                        // NOTE: stack popping order below is important!
                        final CfgElement elemValue = stack.pop();
                        elemKey = stack.pop();
                        parsedProps.setProperty(elemKey.getText(), elemValue.getText());
                        pair.setKey(elemKey);
                        pair.setValue(elemValue);
//                        propLines.put(line, Pair.of(elemKey, elemValue));
                        break;
                    default:
                        throw new IllegalStateException(String.format("Cannot manage %d values on the parsing stack", size));
                }
                cfgFile.getElements().add(pair);
            } else {
                stack.clear();
            }
            return true;
        }

        @Override
        public String toString() {
            return "StorePropsAction";
        }

    };

    Rule cfgProps() {
        return Sequence(
                ZeroOrMore(
                        FirstOf(
                                kvPair(),
                                comment(),
                                whitespace(),
                                eolChar()
                        )
                ),
                EOI
        );
    }

    Rule kvPair() {
        StringBuilderVar sbvValue = new StringBuilderVar();
        return Sequence(
                FirstOf(
                        Sequence(
                                key(),
                                keyStoreProp,
                                Optional(whitespace()),
                                separator(),
                                Optional(whitespace()),
                                Sequence(value(sbvValue), valueStoreProp)
                        ),
                        Sequence(
                                key(),
                                keyStoreProp,
                                Optional(whitespace()),
                                FirstOf(eolChar(), EOI)
                        )
                ),
                actionStoreProp
        );
    }

    Rule comment() {
        return Sequence(commentStart(), ZeroOrMore(notEolChar()));
    }

    Rule key() {
        StringBuilderVar sbvKey = new StringBuilderVar();
        return Sequence(
                literal(sbvKey),
                ZeroOrMore(
                        Sequence(
                                Ch('.'), sbvKey.append('.'),
                                literal(sbvKey)
                        )
                ),
                Optional(arrayIndex(sbvKey))
        );
    }

    Rule value(StringBuilderVar sbv) {
        return ZeroOrMore(
                FirstOf(
                        Sequence(
                                escapedEolChar(),
                                Optional(whitespace()),
                                Sequence(notEolWhitespace(), sbv.append(matchedChar()))
                        ),
                        encodedUnicode(sbv),
                        literalOrSpace(sbv)
                )
        );
    }

    Rule literal(StringBuilderVar sbv) {
        return OneOrMore(
                FirstOf(
                        new JavaIdPartMatcher(sbv),
                        Sequence(AnyOf("(){}-+*/^|;,`°§<>\"'%&@?"), sbv.append(matchedChar())),
                        encodedSpecialChar(sbv),
                        encodedTab(sbv),
                        encodedLinefeed(sbv),
                        encodedUnicode(sbv)
                )
        );
    }

    Rule literalOrSpace(StringBuilderVar sbv) {
        return OneOrMore(
                FirstOf(
                        new JavaIdPartMatcher(sbv),
                        Sequence(AnyOf(" \t\f"), sbv.append(matchedChar())),
                        encodedSpecialChar(sbv),
                        encodedTab(sbv),
                        encodedLinefeed(sbv),
                        encodedUnicode(sbv),
                        malformedEscape(sbv),
                        Sequence(AnyOf("=:[].(){}-+*/^|;,`°§<>\"'%&@?"), sbv.append(matchedChar()))
                )
        );
    }

    Rule arrayIndex(StringBuilderVar sbv) {
        return Sequence(
                Sequence(Ch('['), sbv.append('[')),
                Sequence(integer(), sbv.append(match())),
                Sequence(Ch(']'), sbv.append(']'))
        );
    }

    Rule whitespace() {
        return OneOrMore(AnyOf(" \t\f"));
    }

    Rule eolChar() {
        return FirstOf('\n', "\r\n", '\r');
    }

    Rule notEolChar() {
        return NoneOf("\r\n");
    }

    Rule notEolWhitespace() {
        return NoneOf(" \t\f\r\n");
    }

    Rule separator() {
        return AnyOf("=:");
    }

    Rule commentStart() {
        return AnyOf("#!");
    }

    Rule encodedSpecialChar(StringBuilderVar sbv) {
        return Sequence(Ch('\\'), AnyOf(" \\=:#!"), sbv.append(matchedChar()));
    }

    Rule encodedLinefeed(StringBuilderVar sbv) {
        return Sequence(Ch('\\'), Ch('n'), sbv.append('\n'));
    }

    Rule encodedTab(StringBuilderVar sbv) {
        return Sequence(Ch('\\'), Ch('t'), sbv.append('\t'));
    }

    Rule encodedUnicode(StringBuilderVar sbv) {
        return Sequence(
                Ch('\\'),
                Ch('u'),
                Sequence(hexDigit(), hexDigit(), hexDigit(), hexDigit()), sbv.append(uniToStr(match()))
        );
    }

    Rule escapedEolChar() {
        return Sequence(Ch('\\'), eolChar());
    }

    Rule malformedEscape(StringBuilderVar sbv) {
        return Sequence(Ch('\\'), NoneOf("ntu \t\f\r\n=:#!\\"), sbv.append(matchedChar()));
    }

    Rule integer() {
        return FirstOf('0', Sequence(CharRange('1', '9'), ZeroOrMore(digit())));
    }

    Rule hexDigit() {
        return FirstOf(CharRange('a', 'f'), CharRange('A', 'F'), CharRange('0', '9'));
    }

    Rule digit() {
        return CharRange('0', '9');
    }

    boolean debug(Var v) {
        System.out.println(String.valueOf(v.get()));
        return true;
    }

    String uniToStr(String str) {
        int codePoint = Integer.parseInt(str, 16);
        return new String(Character.toChars(codePoint));
    }

}
