/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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
package org.kie.aries.blueprint.tests;

import org.apache.aries.blueprint.container.BlueprintContainerImpl;
import org.drools.core.audit.ThreadedWorkingMemoryFileLogger;
import org.drools.core.audit.WorkingMemoryConsoleLogger;
import org.drools.core.audit.WorkingMemoryFileLogger;
import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.drools.core.impl.StatelessKnowledgeSessionImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.aries.blueprint.KieBlueprintContainer;
import org.kie.aries.blueprint.beans.Person;
import org.kie.aries.blueprint.factorybeans.KieLoggerAdaptor;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class KieBlueprintLoggerTest {

    static BlueprintContainerImpl container = null;

    @BeforeClass
    public static void runBeforeClass() throws Exception {
        List<URL> urls = new ArrayList<URL>();
        urls.add(KieBlueprintListenerTest.class.getResource("/org/kie/aries/blueprint/loggers.xml"));
        container = new KieBlueprintContainer(ClassLoader.getSystemClassLoader(), urls);
    }

    @AfterClass
    public static void runAfterClass() {
        container.destroy();
    }

    @Test
    public void testStatelessSessionRefConsoleLogger() throws Exception {
        StatelessKieSession session = (StatelessKieSession) container.getComponentInstance("loggerSession");
        StatelessKnowledgeSessionImpl impl = (StatelessKnowledgeSessionImpl) session;
        KieLoggerAdaptor kieLoggerAdaptor = (KieLoggerAdaptor) container.getComponentInstance("ConsoleSessionLogger");
        assertThat(kieLoggerAdaptor).isNotNull();
        assertThat(kieLoggerAdaptor.getRuntimeLogger()).isNotNull();
        for (Object listener : impl.getRuleRuntimeEventListeners()) {
            assertThat(listener instanceof WorkingMemoryConsoleLogger).isTrue();
        }
    }

    @Test
    public void testStatefulKnowledgeConsoleLogger() throws Exception {
        KieSession statefulSession = (KieSession) container.getComponentInstance("ConsoleLogger-statefulSession");
        StatefulKnowledgeSessionImpl impl = (StatefulKnowledgeSessionImpl) statefulSession;
        for (Object listener : impl.getRuleRuntimeEventListeners()) {
            assertThat(listener instanceof WorkingMemoryConsoleLogger).isTrue();
        }
    }

    @Test
    public void testStatefulKnowledgeFileLogger() throws Exception {
        KieSession statefulSession = (KieSession) container.getComponentInstance("FileLogger-statefulSession");
       // assertThat(statefulSession.getGlobals().get("persons")).isNotNull();
        StatefulKnowledgeSessionImpl impl = (StatefulKnowledgeSessionImpl) statefulSession;
        for (Object listener : impl.getRuleRuntimeEventListeners()) {
            assertThat(listener instanceof WorkingMemoryFileLogger).isTrue();
        }
        KieLoggerAdaptor adaptor = (KieLoggerAdaptor) container.getComponentInstance("sf_fl_logger");
        assertThat(adaptor).isNotNull();
        assertThat(adaptor.getRuntimeLogger()).isNotNull();

    }

    @Test
    public void testStatefulKnowledgeThreadedFileLogger() throws Exception {
        KieSession statefulSession = (KieSession) container.getComponentInstance("ThreadedFileLogger-statefulSession");
//        assertThat(statefulSession.getGlobals().get("persons")).isNotNull();
        StatefulKnowledgeSessionImpl impl = (StatefulKnowledgeSessionImpl) statefulSession;
        for (Object listener : impl.getRuleRuntimeEventListeners()) {
            assertThat(listener instanceof ThreadedWorkingMemoryFileLogger).isTrue();
        }
        KieLoggerAdaptor adaptor = (KieLoggerAdaptor) container.getComponentInstance("sf_tfl_logger");
        assertThat(adaptor).isNotNull();
        assertThat(adaptor.getRuntimeLogger()).isNotNull();
    }

    @Test
    public void testStatelessKnowledgeConsoleLogger() throws Exception {
        StatelessKieSession statelessKnowledgeSession = (StatelessKieSession) container.getComponentInstance("ConsoleLogger-statelessSession");
        StatelessKnowledgeSessionImpl impl = (StatelessKnowledgeSessionImpl) statelessKnowledgeSession;
        for (Object listener : impl.getRuleRuntimeEventListeners()) {
            assertThat(listener instanceof WorkingMemoryConsoleLogger).isTrue();
        }
    }

    @Test
    public void testStatelessKnowledgeFileLogger() throws Exception {
        StatelessKieSession statelessKnowledgeSession = (StatelessKieSession) container.getComponentInstance("FileLogger-statelessSession");
        StatelessKnowledgeSessionImpl impl = (StatelessKnowledgeSessionImpl) statelessKnowledgeSession;
        for (Object listener : impl.getRuleRuntimeEventListeners()) {
            assertThat(listener instanceof WorkingMemoryFileLogger).isTrue();
        }
        KieLoggerAdaptor adaptor = (KieLoggerAdaptor) container.getComponentInstance("ss_fl_logger");
        assertThat(adaptor).isNotNull();
        assertThat(adaptor.getRuntimeLogger()).isNotNull();
    }

    @Test
    public void testStatelessKnowledgeThreadedFileLogger() throws Exception {
        StatelessKieSession statelessKnowledgeSession = (StatelessKieSession) container.getComponentInstance("ThreadedFileLogger-statelessSession");
        StatelessKnowledgeSessionImpl impl = (StatelessKnowledgeSessionImpl) statelessKnowledgeSession;
        for (Object listener : impl.getRuleRuntimeEventListeners()) {
            assertThat(listener instanceof ThreadedWorkingMemoryFileLogger).isTrue();
        }
        KieLoggerAdaptor loggerAdaptor = (KieLoggerAdaptor) container.getComponentInstance("ss_tfl_logger");
        assertThat(loggerAdaptor).isNotNull();
        assertThat(loggerAdaptor.getRuntimeLogger()).isNotNull();
        loggerAdaptor.close();
    }

    @Test
    public void testSessionLoggersFromGroupAndNested() throws Exception {
        StatelessKieSession statelessKnowledgeSession = (StatelessKieSession) container.getComponentInstance("k1");
        StatelessKnowledgeSessionImpl impl = (StatelessKnowledgeSessionImpl) statelessKnowledgeSession;
        assertThat(impl.getRuleRuntimeEventListeners()).hasSize(2);

        List list = new ArrayList();
        statelessKnowledgeSession.setGlobal("persons", list);
        assertThat(statelessKnowledgeSession.getGlobals().get("persons")).isNotNull();
        statelessKnowledgeSession.execute(new Person("Darth", "Cheddar", 50));

        KieLoggerAdaptor adaptor = (KieLoggerAdaptor) container.getComponentInstance("k1_logger");
        assertThat(adaptor).isNotNull();
        assertThat(adaptor.getRuntimeLogger()).isNotNull();
        adaptor.close();

        adaptor = (KieLoggerAdaptor) container.getComponentInstance("k1_console_logger");
        assertThat(adaptor).isNotNull();
        assertThat(adaptor.getRuntimeLogger()).isNotNull();
    }

    @Test
    public void testStatelessNoNameFileLogger() throws Exception {
        StatelessKieSession statelessKnowledgeSession = (StatelessKieSession) container.getComponentInstance("FileLogger-statelessSession-noNameLogger");
        StatelessKnowledgeSessionImpl impl = (StatelessKnowledgeSessionImpl) statelessKnowledgeSession;
        for (Object listener : impl.getRuleRuntimeEventListeners()) {
            assertThat(listener instanceof WorkingMemoryFileLogger).isTrue();
        }
    }
}
