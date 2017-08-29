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
package com.github.alexfalappa.nbspringboot.cfgprops.highlighting;

import java.util.Arrays;
import java.util.Collection;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;

import com.github.alexfalappa.nbspringboot.cfgprops.lexer.CfgPropsLanguage;

/**
 * Factory for NetBeans API highlighting tasks.
 *
 * @author Alessandro Falappa
 */
@MimeRegistration(mimeType = CfgPropsLanguage.MIME_TYPE, service = CfgPropsHighlightingTaskFactory.class)
public class CfgPropsHighlightingTaskFactory extends TaskFactory {

    @Override
    public Collection<? extends SchedulerTask> create(Snapshot snpsht) {
        return Arrays.asList(
                new SyntaxErrorHighlightingTask(),
                new DuplicatesHighlightingTask(),
                new DataTypeMismatchHighlightingTask(),
                new DeprecatedPropsHighlightingTask(),
                new UnknownPropsHighlightingTask()
        );
    }

}
