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
/*
 * Based on Java Properties lexer found on https://upsource.jetbrains.com/idea-ce/file/idea-ce-083f663c71f761cb0cb398a2d5ae4a42163507d1/plugins/properties/src/com/intellij/lang/properties/parsing/Properties.flex
 */
package com.github.alexfalappa.nbspringboot.filetype.lexer;

import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;
import com.github.alexfalappa.nbspringboot.filetype.lexer.CfgPropsTokenId;

%%

%class CfgPropsScanner
%type CfgPropsTokenId
%function nextTokenId
%unicode
%char

%eofval{
        if(input.readLength() > 0) {
            // backup eof
            input.backup(1);
            //and return the text as error token
            return CfgPropsTokenId.ERROR;
        } else {
            return null;
        }
%eofval}

%{
    private StateStack stack = new StateStack();

    private LexerInput input;

    public CfgPropsScanner(LexerRestartInfo info) {
        this.input = info.input();
        if(info.state() != null) {
            //reset state
            setState((LexerState) info.state());
        } else {
            zzState = zzLexicalState = YYINITIAL;
            stack.clear();
        }

    }

    public static final class LexerState  {
        final StateStack stack;
        /** the current state of the DFA */
        final int zzState;
        /** the current lexical state */
        final int zzLexicalState;

        LexerState(StateStack stack, int zzState, int zzLexicalState) {
            this.stack = stack;
            this.zzState = zzState;
            this.zzLexicalState = zzLexicalState;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            LexerState state = (LexerState) obj;
            return (this.stack.equals(state.stack)
                && (this.zzState == state.zzState)
                && (this.zzLexicalState == state.zzLexicalState));
        }

        @Override
        public int hashCode() {
            int hash = 11;
            hash = 31 * hash + this.zzState;
            hash = 31 * hash + this.zzLexicalState;
            if (stack != null) {
                hash = 31 * hash + this.stack.hashCode();
            }
            return hash;
        }
    }

    public LexerState getState() {
        return new LexerState(stack.createClone(), zzState, zzLexicalState);
    }

    public void setState(LexerState state) {
        this.stack.copyFrom(state.stack);
        this.zzState = state.zzState;
        this.zzLexicalState = state.zzLexicalState;
    }

    protected int getZZLexicalState() {
        return zzLexicalState;
    }

    protected void popState() {
        yybegin(stack.popStack());
    }

    protected void pushState(final int state) {
        stack.pushStack(getZZLexicalState());
        yybegin(state);
    }

   /* end user code */
%}

CRLF=\R
WHITE_SPACE_CHAR=[\ \n\r\t\f]
VALUE_CHARACTER=[^\n\r\f\\] | "\\"{CRLF} | "\\".
END_OF_LINE_COMMENT=("#"|"!")[^\r\n]*
KEY_SEPARATOR=[:=]
KEY_SEPARATOR_SPACE=\ \t
KEY_DOT="."
KEY_OBRACKET="["
KEY_CBRACKET="]"
KEY_ARR_IDX=0|[1-9][0-9]*
KEY_CHARACTER=[^:\[\]=\ \n\r\t\f\\.] | "\\"{CRLF} | "\\".
FIRST_VALUE_CHARACTER_BEFORE_SEP={VALUE_CHARACTER}
VALUE_CHARACTERS_BEFORE_SEP=([^:=\ \t\n\r\f\\] | "\\"{CRLF} | "\\".)({VALUE_CHARACTER}*)
VALUE_CHARACTERS_AFTER_SEP=([^\ \t\n\r\f\\] | "\\"{CRLF} | "\\".)({VALUE_CHARACTER}*)

%state IN_VALUE
%state IN_KEY
%state IN_KEY_VALUE_SEPARATOR_HEAD
%state IN_KEY_VALUE_SEPARATOR_TAIL

%%

<YYINITIAL> {END_OF_LINE_COMMENT}        { yybegin(YYINITIAL); return CfgPropsTokenId.COMMENT; }

<YYINITIAL> {KEY_CHARACTER}+             { yybegin(IN_KEY); return CfgPropsTokenId.KEY; }

<IN_KEY> {
    {KEY_DOT}                            { yybegin(IN_KEY); return CfgPropsTokenId.DOT; }
    {KEY_OBRACKET}                       { yybegin(IN_KEY); return CfgPropsTokenId.BRACKET; }
    {KEY_ARR_IDX}                        { yybegin(IN_KEY); return CfgPropsTokenId.ARRAY_IDX; }
    {KEY_CBRACKET}                       { yybegin(IN_KEY); return CfgPropsTokenId.BRACKET; }
    {KEY_CHARACTER}+                     { yybegin(IN_KEY); return CfgPropsTokenId.KEY; }
    {KEY_SEPARATOR_SPACE}+               { yybegin(IN_KEY_VALUE_SEPARATOR_HEAD); return CfgPropsTokenId.WHITESPACE; }
    {KEY_SEPARATOR}                      { yybegin(IN_KEY_VALUE_SEPARATOR_TAIL); return CfgPropsTokenId.SEPARATOR; }
}

<IN_KEY_VALUE_SEPARATOR_HEAD> {
    {KEY_SEPARATOR_SPACE}+               { yybegin(IN_KEY_VALUE_SEPARATOR_HEAD); return CfgPropsTokenId.WHITESPACE; }
    {KEY_SEPARATOR}                      { yybegin(IN_KEY_VALUE_SEPARATOR_TAIL); return CfgPropsTokenId.SEPARATOR; }
    {VALUE_CHARACTERS_BEFORE_SEP}        { yybegin(YYINITIAL); return CfgPropsTokenId.VALUE; }
    {CRLF}{WHITE_SPACE_CHAR}*            { yybegin(YYINITIAL); return CfgPropsTokenId.WHITESPACE; }
}

<IN_KEY_VALUE_SEPARATOR_TAIL> {
    {KEY_SEPARATOR_SPACE}+               { yybegin(IN_KEY_VALUE_SEPARATOR_TAIL); return CfgPropsTokenId.WHITESPACE; }
    {VALUE_CHARACTERS_AFTER_SEP}         { yybegin(YYINITIAL); return CfgPropsTokenId.VALUE; }
    {CRLF}{WHITE_SPACE_CHAR}*            { yybegin(YYINITIAL); return CfgPropsTokenId.WHITESPACE; }
}

<IN_VALUE> {VALUE_CHARACTER}+            { yybegin(YYINITIAL); return CfgPropsTokenId.VALUE; }

<IN_VALUE> {CRLF}{WHITE_SPACE_CHAR}*     { yybegin(YYINITIAL); return CfgPropsTokenId.WHITESPACE; }

{WHITE_SPACE_CHAR}+                      { return CfgPropsTokenId.WHITESPACE; }

[^]                                      { return CfgPropsTokenId.ERROR; }