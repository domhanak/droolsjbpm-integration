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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.builder.ReleaseId;
import org.kie.api.persistence.jpa.KieStoreServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.aries.blueprint.KieBlueprintContainer;
import org.kie.aries.blueprint.beans.Person;
import org.osgi.service.blueprint.container.NoSuchComponentException;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class KieBlueprintKModuleBasicTest {

    static BlueprintContainerImpl container = null;

    @BeforeClass
    public static void setup() throws Exception {
        List<URL> urls = new ArrayList<URL>();
        urls.add(KieBlueprintKModuleBasicTest.class.getResource("/org/kie/aries/blueprint/kmodule-basics.xml"));
        container = new KieBlueprintContainer(ClassLoader.getSystemClassLoader(), urls);
    }

    @Test
    public void testReleaseId() throws Exception {
        Object obj = container.getComponentInstance("dummyReleaseId");
        assertThat(obj).isNotNull();
        assertThat(obj instanceof ReleaseId).isTrue();
    }

    @Test
    public void testKieBase() throws Exception {
        KieBase kbase = (KieBase) container.getComponentInstance("drl_kiesample2");
        assertThat(kbase).isNotNull();
    }

    @Test
    public void testKieContainer() throws Exception {
        KieContainer kieContainer = (KieContainer) container.getComponentInstance("defaultContainer");
        assertThat(kieContainer).isNotNull();
    }

    @Test
    public void testKieStore() throws Exception {
        KieStoreServices sampleKstore = (KieStoreServices) container.getComponentInstance("sampleKstore");
        assertThat(sampleKstore).isNotNull();
    }

    @Test
    public void testKieSession() throws Exception {
        KieSession ksession = (KieSession) container.getComponentInstance("ksession9");
        assertThat(ksession).isNotNull();
    }

    @Test
    public void testKieSessionRef() throws Exception {
        StatelessKieSession ksession = (StatelessKieSession) container.getComponentInstance("ksession99");
        assertThat(ksession).isNotNull();
//
//        KieObjectsResolver kieObjectsResolver = new KieObjectsResolver();
//        Object obj = kieObjectsResolver.resolveKSession("ksession99", null);
//        assertThat(obj).isSameAs(ksession);
    }

    @Test
    public void testInvalidKieSessionRef() throws Exception {
        try {
            StatelessKieSession ksession = (StatelessKieSession) container.getComponentInstance("should-fail-ksession1");
            assertThat(ksession).isNull();
        } catch(Exception e){
            assertThat(e instanceof NoSuchComponentException).isTrue();
            return;
        }
        fail();
    }

    @Test
    public void testKSessionExecution() throws Exception {
        StatelessKieSession ksession = (StatelessKieSession) container.getComponentInstance("ksession99");
        assertThat(ksession).isNotNull();

        Person person = (Person) container.getComponentInstance("person1");
        assertThat(person).isNotNull();
        assertThat(person.isHappy()).isFalse();

        ksession.execute(person);
        assertThat(person.isHappy()).isTrue();
    }

    @AfterClass
    public static void tearDown(){
        container.destroy();
    }
}
