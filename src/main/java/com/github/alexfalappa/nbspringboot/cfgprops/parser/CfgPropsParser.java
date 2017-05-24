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
package com.github.alexfalappa.nbspringboot.cfgprops.parser;

import java.util.Collections;
import java.util.List;

import javax.swing.event.ChangeListener;

import org.netbeans.modules.csl.api.Error;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

/**
 *
 * @author Alessandro Falappa
 */
public class CfgPropsParser extends Parser {

    private final CfgPropsParboiled parboiled;
    private ReportingParseRunner runner;
    private Snapshot snapshot;
    private ParsingResult result;

    public CfgPropsParser() {
        parboiled = Parboiled.createParser(CfgPropsParboiled.class);
    }

    @Override
    public void parse(Snapshot snapshot, Task task, SourceModificationEvent sme) throws ParseException {
        this.snapshot = snapshot;
        System.out.println("Parsing...");
        runner = new ReportingParseRunner(parboiled.cfgProps());
        result = runner.run(snapshot.getText().toString());
//        if (!result.matched) {
//            throw new ParseException("errore di parsing");
//        }
    }

    @Override
    public Result getResult(Task task) throws ParseException {
        return new CfgPropsParserResult(snapshot, result);
    }

    @Override
    public void addChangeListener(ChangeListener cl) {
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
    }

    public static class CfgPropsParserResult extends ParserResult {

        private final ParsingResult result;
        private boolean valid = true;

        CfgPropsParserResult(Snapshot snapshot, ParsingResult result) {
            super(snapshot);
            this.result = result;
        }

        public ParsingResult getResult() throws org.netbeans.modules.parsing.spi.ParseException {
            if (!valid) {
                throw new org.netbeans.modules.parsing.spi.ParseException();
            }
            return result;
        }

        @Override
        protected void invalidate() {
            valid = false;
        }

        @Override
        public List<? extends Error> getDiagnostics() {
            return Collections.EMPTY_LIST;
        }

    }
}
