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
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import com.github.alexfalappa.nbspringboot.cfgprops.ast.CfgElement;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.CfgFile;
import com.github.alexfalappa.nbspringboot.cfgprops.ast.PairElement;

/**
 * NetBeans Parsing & Lexing API parser for integrating the Parboiled parser.
 *
 * @author Alessandro Falappa
 */
public class CfgPropsParser extends Parser {

    private static final Logger logger = Logger.getLogger(CfgPropsParser.class.getName());
    private final CfgPropsParboiled parboiled;
    private Snapshot snapshot;
    private ParsingResult parbResult;

    public CfgPropsParser() {
        parboiled = Parboiled.createParser(CfgPropsParboiled.class);
    }

    @Override
    public void parse(Snapshot snapshot, Task task, SourceModificationEvent sme) throws ParseException {
        logger.fine("Parsing...");
        this.snapshot = snapshot;
        RecoveringParseRunner runner = new RecoveringParseRunner(parboiled.cfgProps());
        parboiled.reset();
        parbResult = runner.run(snapshot.getText().toString());
        logParsingResult();
    }

    @Override
    public Result getResult(Task task) throws ParseException {
        return new CfgPropsParserResult(snapshot, parbResult, parboiled.getParsedProps(), parboiled.getCfgFile());
    }

    @Override
    public void addChangeListener(ChangeListener cl) {
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
    }

    private void logParsingResult() {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Parsed properties:");
            final Properties parsedProps = parboiled.getParsedProps();
            for (String pname : parsedProps.stringPropertyNames()) {
                logger.log(Level.FINER, "\t{0} -> {1}", new Object[]{pname, parsedProps.getProperty(pname)});
            }
            logger.finer("Parsed AST:");
            final CfgFile cfgFile = parboiled.getCfgFile();
            for (PairElement p : cfgFile.getElements()) {
                CfgElement e = p.getKey();
                logger.finer(String.format("\t(%3d;%3d) key: %s", e.getIdxStart(), e.getIdxEnd(), e.getText()));
                e = p.getValue();
                if (e != null) {
                    logger.finer(String.format("\t(%3d;%3d) val: %s", e.getIdxStart(), e.getIdxEnd(), e.getText()));
                }
            }
        }
    }

    public static class CfgPropsParserResult extends ParserResult {

        private final ParsingResult parbResult;
        private final Properties parsedProps;
        private boolean valid = true;
        private final CfgFile cfgFile;

        CfgPropsParserResult(Snapshot snapshot, ParsingResult parbResult, Properties parsedProps, CfgFile cfgFile) {
            super(snapshot);
            this.parbResult = parbResult;
            this.parsedProps = parsedProps;
            this.cfgFile = cfgFile;
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

        public CfgFile getCfgFile() {
            return cfgFile;
        }

        @Override
        public List<? extends Error> getDiagnostics() {
            return Collections.EMPTY_LIST;
        }

    }
}
