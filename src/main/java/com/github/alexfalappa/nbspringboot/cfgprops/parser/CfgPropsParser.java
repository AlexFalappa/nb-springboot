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
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;

import javax.swing.event.ChangeListener;

import org.netbeans.modules.csl.api.Error;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.support.ParsingResult;

/**
 * NetBeans Parsing & Lexing API parser for integrating the Parboiled parser.
 *
 * @author Alessandro Falappa
 */
public class CfgPropsParser extends Parser {

    private final CfgPropsParboiled parboiled;
    private Snapshot snapshot;
    private ParsingResult parbResult;

    public CfgPropsParser() {
        parboiled = Parboiled.createParser(CfgPropsParboiled.class);
    }

    @Override
    public void parse(Snapshot snapshot, Task task, SourceModificationEvent sme) throws ParseException {
        this.snapshot = snapshot;
        System.out.println("\n\nParsing...");
        RecoveringParseRunner runner = new RecoveringParseRunner(parboiled.cfgProps());
        parboiled.reset();
        parbResult = runner.run(snapshot.getText().toString());
        parboiled.getParsedProps().list(System.out);
    }

    @Override
    public Result getResult(Task task) throws ParseException {
        return new CfgPropsParserResult(snapshot, parbResult, parboiled.getParsedProps(), parboiled.getPropLines());
    }

    @Override
    public void addChangeListener(ChangeListener cl) {
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
    }

    public static class CfgPropsParserResult extends ParserResult {

        private final ParsingResult parbResult;
        private final Properties parsedProps;
        private boolean valid = true;
        private final Map<String, SortedSet<Integer>> propLines;

        CfgPropsParserResult(Snapshot snapshot, ParsingResult parbResult, Properties parsedProps, Map<String, SortedSet<Integer>> propLines) {
            super(snapshot);
            this.parbResult = parbResult;
            this.parsedProps = parsedProps;
            this.propLines = propLines;
        }

        @Override
        protected void invalidate() {
            valid = false;
        }

        public ParsingResult getParbResult() throws org.netbeans.modules.parsing.spi.ParseException {
            if (!valid) {
                throw new org.netbeans.modules.parsing.spi.ParseException();
            }
            return parbResult;
        }

        public Properties getParsedProps() {
            return parsedProps;
        }

        public Map<String, SortedSet<Integer>> getPropLines() {
            return propLines;
        }

        @Override
        public List<? extends Error> getDiagnostics() {
            return Collections.EMPTY_LIST;
        }

    }
}
