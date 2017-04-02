/*
 * Copyright 2017 Alessandro Falappa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.alexfalappa.nbspringboot.filetype.lexer;

import org.netbeans.api.lexer.Token;
import org.netbeans.editor.BaseTokenID;
import org.netbeans.editor.TokenID;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 *
 * @author Alessandro Falappa
 */
public class CfgPropsLexer implements Lexer<CfgPropsTokenId> {

    private final LexerRestartInfo<CfgPropsTokenId> info;
    private int state = INIT;

    CfgPropsLexer(LexerRestartInfo<CfgPropsTokenId> info) {
        this.info = info;
        if (info.state() != null) {
            state = (Integer) info.state();
        }
    }

    @Override
    public Token<CfgPropsTokenId> nextToken() {
        LexerInput input = info.input();
        int ch = input.read();
        while (ch != LexerInput.EOF) {
            switch (state) {
                case INIT:
                    switch (ch) {
                        case '\n':
                        case '\t':
                        case '\f':
                        case ' ':
                            return info.tokenFactory().createToken(CfgPropsTokenId.WHITESPACE);
                        case '#':
                        case '!':
                            state = ISI_LINE_COMMENT;
                            break;
                        case '=': // in case the key is an empty string (first non-white is '=' or ':')
                        case ':':
                            input.backup(1);
                            state = ISI_EQUAL;
                            if (input.readLength() > 0) {
                                return info.tokenFactory().createToken(CfgPropsTokenId.WHITESPACE);
                            }
                        case '\\': // when key begins with escape
                            state = ISI_KEY_A_BSLASH;
                            break;
                        default:
                            state = ISI_KEY;
                            break;
                    }
                    break;

                case ISI_LINE_COMMENT:
                    switch (ch) {
                        case '\n':
                            state = INIT;
                            return info.tokenFactory().createToken(CfgPropsTokenId.COMMENT);
                    }
                    break;

                case ISI_KEY:
                    switch (ch) {
                        case '\n':
                            state = INIT;
                            return info.tokenFactory().createToken(CfgPropsTokenId.KEY);
                        case '.':
                            input.backup(1);
                            state = ISI_DOT;
                            return info.tokenFactory().createToken(CfgPropsTokenId.KEY);
                        case '[':
                            input.backup(1);
                            state = ISI_A_BRACKET;
                            return info.tokenFactory().createToken(CfgPropsTokenId.KEY);
                        case '\\':
                            state = ISI_KEY_A_BSLASH;
                            break;
                        case '=':
                        case ':':
                            input.backup(1);
                            state = ISI_EQUAL;
                            if (input.readLength() > 0) {
                                return info.tokenFactory().createToken(CfgPropsTokenId.KEY);
                            }
                        case ' ': // the whitspaces after key
                        case '\t':
                            state = INIT;
                    }
                    break;

                case ISI_DOT:
                    switch (ch) {
                        case '.':
                            state = ISI_KEY;
                            return info.tokenFactory().createToken(CfgPropsTokenId.DOT);
                        default:
                            state = ISI_KEY;
                    }
                    break;

                case ISI_A_BRACKET:
                    switch (ch) {
                        case '[':
                            state = ISI_ARR_INDEX;
                            return info.tokenFactory().createToken(CfgPropsTokenId.BRACKET);
                        case ']':
                            state = ISI_KEY;
                            return info.tokenFactory().createToken(CfgPropsTokenId.BRACKET);
                        default:
                            state = ISI_KEY;
                    }
                    break;

                case ISI_ARR_INDEX:
                    if (ch == ']') {
                        input.backup(1);
                        state = ISI_A_BRACKET;
                        return info.tokenFactory().createToken(CfgPropsTokenId.ARRAY_IDX);
                    } else if (ch < '0' && ch > '9') {
                        input.backup(1);
                        state = INIT;
                        return info.tokenFactory().createToken(CfgPropsTokenId.ARRAY_IDX);
                    }
                    break;

                case ISI_KEY_A_BSLASH:
                    switch (ch) {
                        case '\n':
                            state = INIT;
                            return info.tokenFactory().createToken(CfgPropsTokenId.KEY);
                        default:
                            state = INIT;
                    }
                    break;

                case ISI_EQUAL:
                    switch (ch) {
                        case '=':
                        case ':':
                            state = ISI_VALUE;
                            return info.tokenFactory().createToken(CfgPropsTokenId.SEPARATOR);
                        case ' ': // whitespaces also separates key from value: note which whitespaces can do that
                        case '\t':
                            break;
                        case '\\': // in case of alone '\\' line continuation character
                            state = ISI_EQUAL2;
                            break;
                        case '\n':
                            state = INIT;
                            return info.tokenFactory().createToken(CfgPropsTokenId.SEPARATOR);
                        default:
                            state = ISI_VALUE;
                    }
                    break;

                // only for case the last "\\" continuation char is but was not startes value yet (still can appear : or = char)
                case ISI_EQUAL2:
                    switch (ch) {
                        case '\n':
                            state = ISI_EQUAL_AT_NL;
                            return info.tokenFactory().createToken(CfgPropsTokenId.SEPARATOR);
                        default:
                            state = ISI_VALUE;
                    }
                    break;

                // in case of end of line
                case ISI_EQUAL_AT_NL:
                    switch (ch) {
                        case '\n':
                            state = ISI_EQUAL;
                            return info.tokenFactory().createToken(CfgPropsTokenId.WHITESPACE);
                    }
                    break;

                case ISI_VALUE:
                    switch (ch) {
                        case '\n':
                            state = INIT;
                            return info.tokenFactory().createToken(CfgPropsTokenId.VALUE);
                        case '\\':
                            state = ISI_VALUE_A_BSLASH;
                            break;
                    }
                    break;

                case ISI_VALUE_A_BSLASH:
                    switch (ch) {
                        case '\n':
                            state = ISI_VALUE_AT_NL;
                            return info.tokenFactory().createToken(CfgPropsTokenId.VALUE);
                        default:
                            state = ISI_VALUE;
                    }
                    break;

                case ISI_VALUE_AT_NL:
                    switch (ch) {
                        case '\n':
                            state = ISI_VALUE;
                            return info.tokenFactory().createToken(CfgPropsTokenId.WHITESPACE);
                    }
                    break;
                default:
                    return info.tokenFactory().createToken(CfgPropsTokenId.ERROR);
            } // end of the outer switch statement
            ch = input.read();
        }
        if (input.readLength() > 0) {
            return info.tokenFactory().createToken(CfgPropsTokenId.ERROR);
        }
        return null;
    }

    private Token<CfgPropsTokenId> nextTokenManifest() {
        LexerInput input = info.input();
        if (state == 0) {
            int i = input.read();
            if (i == '#') {
                do {
                    i = input.read();
                } while (i != '\n'
                        && i != '\r'
                        && i != LexerInput.EOF);
                do {
                    i = input.read();
                } while (i == '\n'
                        || i == '\r');
                input.backup(1);
                state = 0;
                return info.tokenFactory().createToken(CfgPropsTokenId.COMMENT);
            }
            if (i == '=') {
                i = input.read();
            }
            while (i != '\n'
                    && i != '\r'
                    && i != '='
                    && i != LexerInput.EOF) {
                i = input.read();
            }
            if (i == '\n' || i == '\r') {
                do {
                    i = input.read();
                } while (i == '\n'
                        || i == '\r');
            }
            if (i != LexerInput.EOF) {
                input.backup(1);
            }
            state = i == '=' ? 1 : 0;
            if (input.readLength() == 0) {
                return null;
            }
            return info.tokenFactory().createToken(CfgPropsTokenId.KEY);
        }
        if (state == 1) {
            input.read();
            state = 2;
            return info.tokenFactory().createToken(CfgPropsTokenId.SEPARATOR);
        }
        int i = 0;
        do {
            i = input.read();
            while (i != '\n'
                    && i != '\r'
                    && i != LexerInput.EOF) {
                i = input.read();
            }
            do {
                i = input.read();
            } while (i == '\n'
                    || i == '\r');
        } while (i == ' ');
        if (i != LexerInput.EOF) {
            input.backup(1);
        }
        state = 0;
        if (input.readLength() == 0) {
            return null;
        }
        return info.tokenFactory().createToken(CfgPropsTokenId.VALUE);
    }

    @Override
    public Object state() {
        return state;
    }

    @Override
    public void release() {
    }
    //------------------ FROM PropertiesSyntax and Syntax -------------------------
    /** Text buffer to scan */
    protected char buffer[];
    /** Current offset in the buffer */
    protected int offset;
    /** Offset holding the begining of the current token */
    protected int tokenOffset;
    /** This variable is the length of the token that was found */
    protected int tokenLength;
    /** On which offset in the buffer scanning should stop. */
    protected int stopOffset;
    /** Initial internal state of the analyzer */
    private static final int INIT = -1;
    // Token numeric-IDs
    private static final int TEXT_ID = 1; // plain text
    private static final int LINE_COMMENT_ID = 2; // line comment
    private static final int KEY_ID = 3; // key
    private static final int EQ_ID = 4; // equal-sign
    private static final int VALUE_ID = 5; // value
    private static final int EOL_ID = 6; // EOL

    // TokenIDs
    private static final BaseTokenID TEXT = new BaseTokenID("text", TEXT_ID);
    private static final BaseTokenID LINE_COMMENT = new BaseTokenID("line-comment", LINE_COMMENT_ID);
    private static final BaseTokenID KEY = new BaseTokenID("key", KEY_ID);
    private static final BaseTokenID EQ = new BaseTokenID("equal-sign", EQ_ID);
    private static final BaseTokenID VALUE = new BaseTokenID("value", VALUE_ID);
    private static final BaseTokenID EOL = new BaseTokenID("EOL", EOL_ID);
    // Internal states
    private static final int ISI_LINE_COMMENT = 2; // inside line comment
    private static final int ISI_KEY = 3; // inside a key
    private static final int ISI_KEY_A_BSLASH = 4; // inside a key after backslash
    private static final int ISI_EQUAL = 5; // inside an equal sign
    private static final int ISI_EQUAL2 = 6; // after key but not yet value or equal Note: EQUAL2 was revised
    private static final int ISI_VALUE = 7; // inside a value
    private static final int ISI_VALUE_A_BSLASH = 8; // inside a value after backslash
    private static final int ISI_VALUE_AT_NL = 9; // inside a value at new line
    private static final int ISI_EQUAL_AT_NL = 10; // between key and not yet value at new line
    private static final int ISI_DOT = 11; // between key and before dot
    private static final int ISI_A_BRACKET = 12; // after opening bracket
    private static final int ISI_ARR_INDEX = 13; // between brackets

    protected TokenID parseToken() {
        char actChar;

        while (offset < stopOffset) {
            actChar = buffer[offset];

            switch (state) {
                case INIT:
                    switch (actChar) {
                        case '\n':
                            offset++;
                            return EOL;
                        case '\t':
                        case '\f':
                        case ' ':
                            offset++;
                            return TEXT;
                        case '#':
                        case '!':
                            state = ISI_LINE_COMMENT;
                            break;
                        case '=': // in case the key is an empty string (first non-white is '=' or ':')
                        case ':':
                            state = ISI_EQUAL;
                            return TEXT;
                        case '\\': // when key begins with escape
                            state = ISI_KEY_A_BSLASH;
                            break;
                        default:
                            state = ISI_KEY;
                            break;
                    }
                    break; // end state INIT

                case ISI_LINE_COMMENT:
                    switch (actChar) {
                        case '\n':
                            state = INIT;
                            return LINE_COMMENT;
                    }
                    break; // end state ISI_LINE_COMMENT

                case ISI_KEY:
                    switch (actChar) {
                        case '\n':
                            state = INIT;
                            return KEY;
                        case '\\':
                            state = ISI_KEY_A_BSLASH;
                            break;
                        case '=':
                        case ':':
                        case ' ': // the whitspaces after key
                        case '\t':
                            state = ISI_EQUAL;
                            return KEY;
                    }
                    break; // end state ISI_KEY

                case ISI_KEY_A_BSLASH:
                    switch (actChar) {
                        case '\n':
                            state = INIT;
                            return KEY;
                        default:
                            state = ISI_KEY;
                    }
                    break; // end state ISI_KEY_A_BSLASH

                case ISI_EQUAL:
                    switch (actChar) {
                        case '=':
                        case ':':
                            offset++;
                            state = ISI_VALUE;
                            return EQ;
                        case ' ': // whitespaces also separates key from value: note which whitespaces can do that
                        case '\t':
                            break;
                        case '\\': // in case of alone '\\' line continuation character
                            state = ISI_EQUAL2;
                            break;
                        case '\n':
                            state = INIT;
                            return EQ;
                        default:
                            state = ISI_VALUE;
                    }
                    break; // end state ISI_KEY

                // only for case the last "\\" continuation char is but was not startes value yet (still can appear : or = char)
                case ISI_EQUAL2:
                    switch (actChar) {
                        case '\n':
                            state = ISI_EQUAL_AT_NL;
                            return EQ; // PENDING
                        default:
                            state = ISI_VALUE;
                    }
                    break; // end state ISI_EQUAL_A_BSLASH

                // in case of end of line
                case ISI_EQUAL_AT_NL:
                    switch (actChar) {
                        case '\n':
                            offset++;
                            state = ISI_EQUAL;
                            return EOL;
                        default:
                            throw new Error("Something smells 4");
                    }

// this previous version of ISI_EQUAL2 is needless because ':=' is not separator the second = char belongs to the value already
//            case ISI_EQUAL2:
//                switch (actChar) {
//                case '\n':
//                    state = INIT;
//                    return EQ;
//                case '=':
//                case ':':
//                    offset++;
//                    state = ISI_VALUE;
//                    return EQ;
//                default:
//                    state = ISI_VALUE;
//                    return EQ;
//                }
                //break; // end state ISI_KEY
                case ISI_VALUE:
                    switch (actChar) {
                        case '\n':
                            state = INIT;
                            return VALUE;
                        case '\\':
                            state = ISI_VALUE_A_BSLASH;
                            break;
                    }
                    break; // end state ISI_KEY

                case ISI_VALUE_A_BSLASH:
                    switch (actChar) {
                        case '\n':
                            state = ISI_VALUE_AT_NL;
                            return VALUE;
                        default:
                            state = ISI_VALUE;
                    }
                    break; // end state ISI_KEY

                case ISI_VALUE_AT_NL:
                    switch (actChar) {
                        case '\n':
                            offset++;
                            state = ISI_VALUE;
                            return EOL;
                        default:
                            throw new Error("Something smells 2");
                    }
                //break; // end state ISI_KEY

                default:
                    throw new Error("Unhandled state " + state);

            } // end of the outer switch statement

            offset = ++offset;

        } // end of while loop

        /* At this stage there's no more text in the scanned buffer. */
        switch (state) {
            case ISI_LINE_COMMENT:
                return LINE_COMMENT;
            case ISI_KEY:
            case ISI_KEY_A_BSLASH:
                return KEY;
            case ISI_EQUAL:
            case ISI_EQUAL2:
                return EQ;
            case ISI_VALUE:
            case ISI_VALUE_A_BSLASH:
                return VALUE;
            case ISI_VALUE_AT_NL:
            case ISI_EQUAL_AT_NL: // TEMP
                throw new Error("Something smells 3");
        }

        return null;

    } // parseToken

}
