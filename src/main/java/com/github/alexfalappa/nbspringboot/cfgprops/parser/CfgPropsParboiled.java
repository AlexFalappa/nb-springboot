package com.github.alexfalappa.nbspringboot.cfgprops.parser;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.buffers.InputBuffer;
import org.parboiled.support.ValueStack;
import org.parboiled.support.Var;

import com.github.alexfalappa.nbspringboot.cfgprops.ast.CfgElement;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.CfgFile;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.KeyElement;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.PairElement;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.ValueElement;

import static java.util.regex.Pattern.compile;

/**
 * Spring Boot configuration properties parser based on Parboiled library.
 * <p>
 * This parser implements a grammar accepting the Java Properties format (with some minor exceptions) and adding dot separated
 * keys, array notation ({@code array[index]=value} and map notation ({@code map[key]=value}.
 * <p>
 * Differences with base Java Properties syntax:
 * <ul>
 * <li>values must be explicitly separated from keys by a <tt>=</tt> (equal sign) or <tt>:</tt> (colon), first occurring
 * whitespace as separator is not supported.
 * </ul>
 *
 * @author Alessandro Falappa
 */
public class CfgPropsParboiled extends BaseParser<CfgElement> {

    private static final Pattern PAT_UNICODES = compile("\\\\u[a-fA-F0-9]{4}");
    private static final Pattern PAT_ESCAPES = compile("\\\\.");
    private static final Pattern PAT_ESCAPED_NEWLINE = compile("\\\\(\\n|\\r|\\r\\n)\\s*");
    private Properties parsedProps = new Properties();
    private CfgFile cfgFile = new CfgFile();

    public Properties getParsedProps() {
        return parsedProps;
    }

    public CfgFile getCfgFile() {
        return cfgFile;
    }

    public void reset() {
        parsedProps.clear();
        cfgFile = new CfgFile();
    }

    Action<CfgElement> keyStoreProp = new Action<CfgElement>() {
        @Override
        public boolean run(Context<CfgElement> context) {
            if (!context.inErrorRecovery()) {
                final int start = context.getMatchStartIndex();
                final int end = context.getMatchEndIndex();
                final InputBuffer ibuf = context.getInputBuffer();
                final String key = ibuf.extract(start, end);
                KeyElement elemKey = new KeyElement(ibuf.getOriginalIndex(start), ibuf.getOriginalIndex(end), key);
                context.getValueStack().push(elemKey);
            }
            return true;
        }

        @Override
        public java.lang.String toString() {
            return "KeyStoreAction";
        }

    };

    Action<CfgElement> valueStoreProp = new Action<CfgElement>() {
        @Override
        public boolean run(Context<CfgElement> context) {
            if (!context.inErrorRecovery()) {
                final int start = context.getMatchStartIndex();
                final int end = context.getMatchEndIndex();
                final InputBuffer ibuf = context.getInputBuffer();
                final String value = ibuf.extract(start, end);
                ValueElement elemVal = new ValueElement(ibuf.getOriginalIndex(start), ibuf.getOriginalIndex(end), value);
                context.getValueStack().push(elemVal);
            }
            return true;
        }

        @Override
        public java.lang.String toString() {
            return "ValueStoreAction";
        }

    };

    Action<CfgElement> actionStoreProp = new Action<CfgElement>() {
        @Override
        public boolean run(Context<CfgElement> context) {
            final ValueStack<CfgElement> stack = context.getValueStack();
            if (!context.hasError()) {
                int size = stack.size();
                switch (size) {
                    case 1:
                        CfgElement elemKey = stack.pop();
                        parsedProps.setProperty(unescape(elemKey.getText()), "");
                        cfgFile.getElements().add(new PairElement(elemKey));
                        break;
                    case 2:
                        // NOTE: stack popping order below is important!
                        final CfgElement elemValue = stack.pop();
                        elemKey = stack.pop();
                        parsedProps.setProperty(unescape(elemKey.getText()), unescape(elemValue.getText()));
                        cfgFile.getElements().add(new PairElement(elemKey, elemValue));
                        break;
                    default:
                        throw new IllegalStateException(String.format("Cannot manage %d values on the parsing stack", size));
                }
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
        return Sequence(
                FirstOf(
                        Sequence(
                                key(),
                                keyStoreProp,
                                Optional(whitespace()),
                                separator(),
                                Optional(whitespace()),
                                Sequence(value(), valueStoreProp)
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
        return Sequence(
                literal(),
                ZeroOrMore(
                        Sequence(
                                Ch('.'),
                                literal()
                        )
                ),
                Optional(collectionIndex())
        );
    }

    Rule value() {
        return ZeroOrMore(
                FirstOf(
                        Sequence(
                                escapedEolChar(),
                                Optional(whitespace()),
                                notEolWhitespace()
                        ),
                        encodedUnicode(),
                        literalOrSpace()
                )
        );
    }

    Rule literal() {
        return OneOrMore(
                FirstOf(
                        new JavaIdPartMatcher(),
                        AnyOf("(){}-+*/^|;,`°§<>\"'%&@?"),
                        encodedSpecialChar(),
                        encodedTab(),
                        encodedLinefeed(),
                        encodedUnicode()
                )
        );
    }

    Rule literalOrSpace() {
        return OneOrMore(
                FirstOf(
                        new JavaIdPartMatcher(),
                        AnyOf(" \t\f"),
                        encodedSpecialChar(),
                        encodedTab(),
                        encodedLinefeed(),
                        encodedUnicode(),
                        malformedEscape(),
                        AnyOf("=:[].(){}-+*/^|;,`°§<>\"'%&@?")
                )
        );
    }

    Rule collectionIndex() {
        return Sequence(
                Ch('['),
                literal(),
                Ch(']')
        );
    }

    Rule whitespace() {
        return OneOrMore(AnyOf(" \t\f"));
    }

    Rule eolChar() {
        return FirstOf("\r\n", '\n', '\r');
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

    Rule encodedSpecialChar() {
        return Sequence(Ch('\\'), AnyOf(" \\=:#!"));
    }

    Rule encodedLinefeed() {
        return Sequence(Ch('\\'), Ch('n'));
    }

    Rule encodedTab() {
        return Sequence(Ch('\\'), Ch('t'));
    }

    Rule encodedUnicode() {
        return Sequence(
                Ch('\\'),
                Ch('u'),
                Sequence(hexDigit(), hexDigit(), hexDigit(), hexDigit())
        );
    }

    Rule escapedEolChar() {
        return Sequence(Ch('\\'), eolChar());
    }

    Rule malformedEscape() {
        return Sequence(Ch('\\'), NoneOf("ntu \t\f\r\n=:#!\\"));
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
        String ret = "";
        int codePoint = 0;
        try {
            codePoint = Integer.parseInt(str, 16);
            ret = new String(Character.toChars(codePoint));
        } catch (NumberFormatException ex) {
            // may happen while typing a partial edit. Ignore.
        }
        return ret;
    }

    private String unescape(String text) {
        StringBuffer sb = new StringBuffer();
        Matcher m = PAT_UNICODES.matcher(text);
        while (m.find()) {
            m.appendReplacement(sb, uniToStr(m.group().substring(2)));
        }
        m.appendTail(sb);
        m = PAT_ESCAPES.matcher(sb.toString());
        sb = new StringBuffer();
        while (m.find()) {
            switch (m.group()) {
                case "\\:":
                    m.appendReplacement(sb, ":");
                    break;
                case "\\=":
                    m.appendReplacement(sb, "=");
                    break;
                case "\\#":
                    m.appendReplacement(sb, "#");
                    break;
                case "\\!":
                    m.appendReplacement(sb, "!");
                    break;
                case "\\n":
                    m.appendReplacement(sb, "\n");
                    break;
                case "\\t":
                    m.appendReplacement(sb, "\t");
                    break;
                case "\\ ":
                    m.appendReplacement(sb, " ");
                    break;
                case "\\\\":
                    m.appendReplacement(sb, "\\\\");
                    break;
                default:
                    m.appendReplacement(sb, m.group().substring(1));
            }
        }
        m.appendTail(sb);
        m = PAT_ESCAPED_NEWLINE.matcher(sb.toString());
        return m.replaceAll("");
    }
}
