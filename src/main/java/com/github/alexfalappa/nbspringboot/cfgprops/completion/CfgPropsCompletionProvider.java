/*
 * Copyright 2016 the original author or authors.
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
package com.github.alexfalappa.nbspringboot.cfgprops.completion;

import java.util.logging.Logger;

import javax.swing.text.JTextComponent;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.project.Project;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.filesystems.FileUtil;

import com.github.alexfalappa.nbspringboot.Utils;
import com.github.alexfalappa.nbspringboot.cfgprops.lexer.CfgPropsLanguage;
import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;

import static java.util.logging.Level.FINE;

/**
 * The Spring Boot Configuration implementation of {@code CompletionProvider}.
 * <p>
 * The entry point of completion support. This provider is registered for text/application+properties files.
 *
 * @author Aggelos Karalias
 * @author Alessandro Falappa
 */
@MimeRegistration(mimeType = CfgPropsLanguage.MIME_TYPE, service = CompletionProvider.class)
public class CfgPropsCompletionProvider implements CompletionProvider {

    private static final Logger logger = Logger.getLogger(CfgPropsCompletionProvider.class.getName());

    @Override
    public CompletionTask createTask(int queryType, JTextComponent jtc) {
        if (queryType == CompletionProvider.COMPLETION_ALL_QUERY_TYPE) {
            return null;
        }
        Project prj = Utils.getActiveProject();
        if (prj == null) {
            return null;
        }
        logger.log(FINE, "Completing within context of prj {0}", FileUtil.getFileDisplayName(prj.getProjectDirectory()));
        final SpringBootService sbs = prj.getLookup().lookup(SpringBootService.class);
        if (sbs == null) {
            return null;
        }
        switch (queryType) {
            case CompletionProvider.COMPLETION_QUERY_TYPE:
                return new AsyncCompletionTask(new CfgPropsCompletionQuery(sbs, prj), jtc);
            case CompletionProvider.DOCUMENTATION_QUERY_TYPE:
                return new AsyncCompletionTask(new CfgPropsDocAndTooltipQuery(sbs, false), jtc);
            case CompletionProvider.TOOLTIP_QUERY_TYPE:
                return new AsyncCompletionTask(new CfgPropsDocAndTooltipQuery(sbs, true), jtc);
        }
        return null;
    }

    @Override
    public int getAutoQueryTypes(JTextComponent jtc, String string) {
        return 0;
    }

}
