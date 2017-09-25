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
package com.github.alexfalappa.nbspringboot.hints;

import javax.swing.text.Position;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.modules.maven.model.Utilities;
import org.netbeans.modules.maven.model.pom.Dependency;
import org.netbeans.modules.maven.model.pom.DependencyContainer;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.POMModelFactory;
import org.netbeans.modules.xml.xam.ModelSource;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.TriggerPattern;
import org.netbeans.spi.java.hints.TriggerPatterns;
import org.openide.awt.StatusDisplayer;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;
import com.sun.source.util.TreePath;

import static com.sun.source.tree.Tree.Kind.ANNOTATION;

public class MissingPomDependencies {

    @Hint(displayName = "#DN_CfgProcMissingHint", description = "#DESC_CfgProcMissingHint", category = "Spring Boot")
    @TriggerPatterns({
        @TriggerPattern("org.springframework.boot.context.properties.ConfigurationProperties")
        ,@TriggerPattern("org.springframework.boot.context.properties.EnableConfigurationProperties")
    })
    @Messages({
        "DN_CfgProcMissingHint=Missing Spring Boot configuration processor",
        "DESC_CfgProcMissingHint=Warns if @ConfigurationProperties and @EnableConfigurationProperties annotations are used without the <i>spring-boot-configuration-processor</i> dependency in the project pom",
        "ERR_CfgProcMissingHint=Missing Spring Boot configuration processor in project pom"
    })
    public static ErrorDescription cfgProperties(HintContext ctx) {
        return warningFor(ctx, "spring-boot-configuration-processor", Bundle.ERR_CfgProcMissingHint(), true);
    }

    @Hint(displayName = "#DN_MvcMissingHint", description = "#DESC_MvcMissingHint", category = "Spring Boot")
    @TriggerPattern("org.springframework.stereotype.Controller")
    @Messages({
        "DN_MvcMissingHint=Missing Spring Boot MVC starter",
        "DESC_MvcMissingHint=Warns if an @Controller annotation is used without the <i>spring-boot-starter-web</i> dependency in the project pom",
        "ERR_MvcMissingHint=Missing Spring Boot MVC starter in project pom"
    })
    public static ErrorDescription controller(HintContext ctx) {
        return warningFor(ctx, "spring-boot-starter-web", Bundle.ERR_MvcMissingHint(), false);
    }

    private static ErrorDescription warningFor(HintContext ctx, String artifactId, String hintMex, boolean optional) {
        final TreePath tp = ctx.getPath();
        final TreePath tpParent = tp.getParentPath();
        if (tpParent != null) {
            if (tpParent.getLeaf().getKind() == ANNOTATION) {
                Project prj = FileOwnerQuery.getOwner(ctx.getInfo().getFileObject());

                if (prj != null) {
                    SpringBootService sbs = prj.getLookup().lookup(SpringBootService.class
                    );

                    if (sbs != null) {
                        if (!sbs.hasPomDependency(artifactId)) {
                            NbMavenProject mPrj = prj.getLookup().lookup(NbMavenProject.class
                            );
                            if (mPrj != null) {
                                return ErrorDescriptionFactory.forName(ctx, tp, hintMex, new AddDepFix(mPrj, artifactId, optional));
                            } else {
                                return ErrorDescriptionFactory.forName(ctx, tp, hintMex);
                            }
                        }
                    }
                }
            }
        }
        return null;

    }

    private static final class AddDepFix implements Fix {

        private final NbMavenProject mvnPrj;
        private final String artifactId;
        private final boolean optional;

        public AddDepFix(NbMavenProject mvnPrj, String artifactId, boolean optional) {
            this.mvnPrj = mvnPrj;
            this.artifactId = artifactId;
            this.optional = optional;
        }

        @Override
        public String getText() {
            return String.format("Add '%s' dependency to pom", artifactId);
        }

        @Override
        public ChangeInfo implement() throws Exception {
            FileObject foPom = FileUtil.toFileObject(mvnPrj.getMavenProject().getFile());
            ModelSource mdlSrc = Utilities.createModelSource(foPom);
            POMModel model = POMModelFactory.getDefault().getModel(mdlSrc);
            int caretPos = 0;
            try {
                if (model.startTransaction()) {
                    DependencyContainer container = model.getProject();
                    Dependency dep = model.getFactory().createDependency();
                    dep.setGroupId("org.springframework.boot");
                    dep.setArtifactId(artifactId);
                    if (optional) {
                        dep.setOptional(Boolean.TRUE);
                    }
                    container.addDependency(dep);
                    caretPos = model.getAccess().findPosition(dep.getPeer());
                }
            } finally {
                try {
                    model.endTransaction();
                } catch (IllegalStateException ex) {
                    StatusDisplayer.getDefault().setStatusText("Cannot write to the model: " + ex.getMessage(),
                            StatusDisplayer.IMPORTANCE_ERROR_HIGHLIGHT);
                }
            }
            try {
                DataObject dobj = DataObject.find(foPom);
                EditorCookie ed = dobj.getLookup().lookup(EditorCookie.class);
                SaveCookie sv = dobj.getLookup().lookup(SaveCookie.class);
                if (sv != null) {
                    sv.save();
                }
                if (ed != null) {
                    Position pos = ed.getDocument().createPosition(caretPos);
                    return new ChangeInfo(foPom, pos, pos);
                }
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
            return new ChangeInfo();
        }

    }

}
