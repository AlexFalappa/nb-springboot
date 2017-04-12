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
package com.github.alexfalappa.nbspringboot.cfgprops.lexer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.spi.lexer.TokenFactory;

/**
 * NB Lexer for Spring Boot configuration properties.
 * <p>
 * Bridges the JFlex generated scanner with NB lexing API.
 *
 * @author Alessandro Falappa
 */
public class CfgPropsLexer implements Lexer<CfgPropsTokenId> {

    private final CfgPropsScanner scanner;
    private final TokenFactory<CfgPropsTokenId> tokenFactory;

    CfgPropsLexer(LexerRestartInfo<CfgPropsTokenId> info) {
        scanner = new CfgPropsScanner(info);
        tokenFactory = info.tokenFactory();
    }

    @Override
    public Token<CfgPropsTokenId> nextToken() {
        try {
            CfgPropsTokenId tokenId = scanner.nextTokenId();
            Token<CfgPropsTokenId> token = null;
            if (tokenId != null) {
                token = tokenFactory.createToken(tokenId);
            }
            return token;
        } catch (IOException ex) {
            Logger.getLogger(CfgPropsLexer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public Object state() {
        return scanner.getState();
    }

    @Override
    public void release() {
    }

}
