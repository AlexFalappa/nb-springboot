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
package com.github.alexfalappa.nbspringboot.cfgprops.hints;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.TriggerPattern;
import org.openide.util.NbBundle.Messages;

import com.github.alexfalappa.nbspringboot.projects.service.api.SpringBootService;
import com.sun.source.util.TreePath;

import static com.sun.source.tree.Tree.Kind.ANNOTATION;

@Hint(displayName = "#DN_CfgProcMissingHint", description = "#DESC_CfgProcMissingHint", category = "general")
@Messages({
    "DN_CfgProcMissingHint=Spring Boot Configuration Processor",
    "DESC_CfgProcMissingHint=Warns if an @ConfigurationProperties annotation is used without the spring-boot-configuration-processor dependency in the project pom"
})
public class CfgProcMissingHint {

    @TriggerPattern("org.springframework.boot.context.properties.ConfigurationProperties")
    @Messages("ERR_CfgProcMissingHint=Missing Spring Boot configuration processor in project pom")
    public static ErrorDescription computeWarning(HintContext ctx) {
        final TreePath tp = ctx.getPath();
        final TreePath tpParent = tp.getParentPath();
        if (tpParent != null) {
            if (tpParent.getLeaf().getKind() == ANNOTATION) {
                Project prj = FileOwnerQuery.getOwner(ctx.getInfo().getFileObject());
                if (prj != null) {
                    SpringBootService sbs = prj.getLookup().lookup(SpringBootService.class);
                    if (sbs != null) {
                        if (!sbs.hasPomDependency("spring-boot-configuration-processor")) {
//                            Fix fix = new FixImpl(ctx.getInfo(), tp, mPrj).toEditorFix();
                            return ErrorDescriptionFactory.forName(ctx, tp, Bundle.ERR_CfgProcMissingHint()/*, fix*/);
                        }
                    }
                }
            }
        }
        return null;
    }

//    private static final class FixImpl extends JavaFix {
//
//        private final NbMavenProject mvnPrj;
//
//        public FixImpl(CompilationInfo info, TreePath tp, NbMavenProject mvnPrj) {
//            super(info, tp);
//            this.mvnPrj = mvnPrj;
//        }
//
//        @Override
//        @Messages("FIX_CfgProcMissingHint=Add Spring Boot configuration processor dependency")
//        protected String getText() {
//            return Bundle.FIX_CfgProcMissingHint();
//        }
//
//        @Override
//        protected void performRewrite(TransformationContext ctx) {
//            System.out.println("com.github.alexfalappa.nbspringboot.cfgprops.hints.CfgProcMissingHint.FixImpl.performRewrite()");
//            Model model = mvnPrj.getMavenProject().getModel();
//            final Dependency dep = new Dependency();
//            dep.setGroupId("pippo");
//            dep.setArtifactId("pluto");
//            dep.setOptional("true");
//            model.getDependencies().add(dep);
//        }
//    }
}
