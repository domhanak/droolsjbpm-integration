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
import org.kie.api.runtime.KieSession;
import org.kie.aries.blueprint.KieBlueprintContainer;
import org.kie.aries.blueprint.factorybeans.KieListenerAdaptor;
import org.kie.aries.blueprint.mocks.MockAgendaEventListener;
import org.kie.aries.blueprint.mocks.MockProcessEventListener;
import org.kie.aries.blueprint.mocks.MockRuleRuntimeEventListener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.StatelessKieSession;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class KieBlueprintListenerTest {

    static BlueprintContainerImpl container = null;

    @BeforeClass
    public static void setup() throws Exception {
        List<URL> urls = new ArrayList<URL>();
        urls.add(KieBlueprintListenerTest.class.getResource("/org/kie/aries/blueprint/listeners.xml"));
        container = new KieBlueprintContainer(ClassLoader.getSystemClassLoader(), urls);
    }


    @Test
    public void testKieBase() throws Exception {
        KieBase kbase = (KieBase) container.getComponentInstance("drl_kiesample");
        System.out.println(kbase);
        assertThat(kbase).isNotNull();
    }

    @Test
    public void testKieSession() throws Exception {
        StatelessKieSession ksession = (StatelessKieSession) container.getComponentInstance("statelessWithGroupedListeners");
        assertThat(ksession).isNotNull();
    }

    @Test
    public void testEventListenersGroup() throws Exception {
        List<KieListenerAdaptor> listeners = (List<KieListenerAdaptor>) container.getComponentInstance("debugListeners");
        assertThat(listeners).isNotNull();
        for (KieListenerAdaptor obj : listeners){
          assertThat(obj.getObjectRef()).isNotNull();
        }
    }

    @Test
    public void testStatelessRefWMEventListener() throws Exception {
        StatelessKieSession ksession = (StatelessKieSession) container.getComponentInstance("ksession1");
        assertThat(ksession).isNotNull();

        assertThat(ksession.getRuleRuntimeEventListeners()).hasSize(1);
        boolean mockWMEventListenerFound = false;
        for (RuleRuntimeEventListener listener : ksession.getRuleRuntimeEventListeners()){
            if (listener instanceof MockRuleRuntimeEventListener){
                mockWMEventListenerFound = true;
                break;
            }
        }
        assertThat(mockWMEventListenerFound).isTrue();
    }

    @Test
    public void testStatelessRefAgendaEventListener() throws Exception {
        StatelessKieSession kSession = (StatelessKieSession) container.getComponentInstance("ksession1");
        assertThat(kSession.getAgendaEventListeners().size() > 0).isTrue();
        boolean mockAgendaEventListenerFound = false;
        for (AgendaEventListener listener : kSession.getAgendaEventListeners()){
            if (listener instanceof MockAgendaEventListener){
                mockAgendaEventListenerFound = true;
                break;
            }
        }
        assertThat(mockAgendaEventListenerFound).isTrue();
    }

    @Test
    public void testStatelessRefProcessEventListener() throws Exception {
        StatelessKieSession kSession = (StatelessKieSession) container.getComponentInstance("ksession1");
        assertThat(kSession.getProcessEventListeners().size() > 0).isTrue();
        boolean mockProcessEventListenerFound = false;
        for (ProcessEventListener listener : kSession.getProcessEventListeners()){
            if (listener instanceof MockProcessEventListener){
                mockProcessEventListenerFound = true;
                break;
            }
        }
        assertThat(mockProcessEventListenerFound).isTrue();
    }

    @Test
    public void testStatefulWMEventListener() throws Exception {
        KieSession ksession = (KieSession) container.getComponentInstance("ksession99");
        assertThat(ksession).isNotNull();

        assertThat(ksession.getRuleRuntimeEventListeners()).hasSize(1);
        boolean mockWMEventListenerFound = false;
        for (RuleRuntimeEventListener listener : ksession.getRuleRuntimeEventListeners()){
            if (listener instanceof MockRuleRuntimeEventListener){
                mockWMEventListenerFound = true;
                break;
            }
        }
        assertThat(mockWMEventListenerFound).isTrue();
    }

    @Test
    public void testStatefulAgendaEventListener() throws Exception {
        KieSession kSession = (KieSession) container.getComponentInstance("ksession99");
        assertThat(kSession.getAgendaEventListeners().size() > 0).isTrue();
        boolean mockAgendaEventListenerFound = false;
        for (AgendaEventListener listener : kSession.getAgendaEventListeners()){
            if (listener instanceof MockAgendaEventListener){
                mockAgendaEventListenerFound = true;
                break;
            }
        }
        assertThat(mockAgendaEventListenerFound).isTrue();
    }

    @Test
    public void testStatefulProcessEventListener() throws Exception {
        KieSession kSession = (KieSession) container.getComponentInstance("ksession99");
        assertThat(kSession.getProcessEventListeners().size() > 0).isTrue();
        boolean mockProcessEventListenerFound = false;
        for (ProcessEventListener listener : kSession.getProcessEventListeners()){
            if (listener instanceof MockProcessEventListener){
                mockProcessEventListenerFound = true;
                break;
            }
        }
        assertThat(mockProcessEventListenerFound).isTrue();
    }

    @Test
    public void testEventListenersStandAlone() throws Exception {
        Object obj = container.getComponentInstance("mock-wm-listener");
        assertThat(obj).isNotNull();
        assertThat(obj instanceof MockRuleRuntimeEventListener).isTrue();

        obj = container.getComponentInstance("mock-agenda-listener");
        assertThat(obj).isNotNull();
        assertThat(obj instanceof MockAgendaEventListener).isTrue();

        obj = container.getComponentInstance("mock-process-listener");
        assertThat(obj).isNotNull();
        assertThat(obj instanceof MockProcessEventListener).isTrue();
    }

    @AfterClass
    public static void tearDown(){
        container.destroy();
    }
}
