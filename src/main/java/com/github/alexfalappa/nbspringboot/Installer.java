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
package com.github.alexfalappa.nbspringboot;

import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.netbeans.api.debugger.ActionsManagerAdapter;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.DebuggerManagerAdapter;
import org.netbeans.api.debugger.Session;
import org.netbeans.contrib.yenta.Yenta;

/**
 * Avoid implementation dependencies by making the plugin friend of non-api modules.
 * <p>
 * Uses https://bitbucket.org/jglick/yenta
 *
 * @author Alessandro Falappa
 */
public class Installer extends Yenta {

    @Override
    public void restored() {
        super.restored();
        DebuggerManager dm = DebuggerManager.getDebuggerManager();
        dm.addDebuggerListener(new DebuggerManagerAdapter() {
            @Override
            public void engineAdded(DebuggerEngine engine) {
                System.out.println("DebuggerListener.engineAdded()");
            }

            @Override
            public void sessionAdded(Session session) {
                System.out.println("DebuggerListener.sessionAdded()");
            }

            @Override
            public void sessionRemoved(Session session) {
                System.out.println("DebuggerListener.sessionRemoved()");
            }

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                System.out.format("DebuggerListener.propertyChange() - %s%n", evt.getPropertyName().toUpperCase());
            }

        });
        dm.getActionsManager().addActionsManagerListener(new ActionsManagerAdapter() {
            @Override
            public void actionPerformed(Object action) {
                System.out.println("ActionsManagerListener.actionPerformed()");
            }

            @Override
            public void actionStateChanged(Object action, boolean enabled) {
                System.out.println("ActionsManagerListener.actionStateChanged()");
            }

        });
    }

    @Override
    protected Set<String> friends() {
        return new HashSet<>(Arrays.asList(
                "org.netbeans.modules.maven.embedder",
                "org.netbeans.modules.maven.model"
        ));
    }

    @Override
    protected Set<String> siblings() {
        return new HashSet<>(Arrays.asList(
                "org.netbeans.modules.maven",
                "org.openide.windows"
        ));
    }

}
