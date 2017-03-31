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

import java.util.Collection;
import java.util.EnumSet;

import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.spi.lexer.LanguageHierarchy;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

import com.github.alexfalappa.nbspringboot.filetype.CfgPropsLanguage;

/**
 *
 * @author Alessandro Falappa
 */
public enum CfgPropsTokenId implements TokenId {
    COMMENT("comment"), // NOI18N
    SECTION_DELIM("section_delim"), // NOI18N
    SECTION("section"), // NOI18N
    KEY("key"), // NOI18N
    EQUALS("equals"), // NOI18N
    VALUE("value"), // NOI18N
    WHITESPACE("whitespace"), // NOI18N
    ERROR("error"); // NOI18N

    private final String name;

    private static final Language<CfgPropsTokenId> LANGUAGE = new LanguageHierarchy<CfgPropsTokenId>() {

        @Override
        protected Collection<CfgPropsTokenId> createTokenIds() {
            return EnumSet.allOf(CfgPropsTokenId.class);
        }

        @Override
        protected Lexer<CfgPropsTokenId> createLexer(LexerRestartInfo<CfgPropsTokenId> info) {
            return new CfgPropsLexer(info);
        }

        @Override
        protected String mimeType() {
            return CfgPropsLanguage.MIME_TYPE;
        }
    }.language();

    CfgPropsTokenId(String name) {
        this.name = name;
    }

    @Override
    public String primaryCategory() {
        return name;
    }

    public static Language<CfgPropsTokenId> language() {
        return LANGUAGE;
    }
}
