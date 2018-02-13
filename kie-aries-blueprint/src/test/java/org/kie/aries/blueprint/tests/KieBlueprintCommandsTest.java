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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.aries.blueprint.container.BlueprintContainerImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.kie.aries.blueprint.KieBlueprintContainer;
import org.kie.aries.blueprint.beans.Person;

import static org.assertj.core.api.Assertions.*;

public class KieBlueprintCommandsTest {

    static BlueprintContainerImpl container = null;

    @BeforeClass
    public static void runBeforeClass() throws Exception {
        List<URL> urls = new ArrayList<URL>();
        urls.add(KieBlueprintCommandsTest.class.getResource("/org/kie/aries/blueprint/beans-commands.xml"));
        container = new KieBlueprintContainer(ClassLoader.getSystemClassLoader(), urls);
    }


    @Test
    public void testKieBase() throws Exception {
        KieBase kbase = (KieBase) container.getComponentInstance("drl_kiesample");
        assertThat(kbase).isNotNull();
    }

    @Test
    public void testInsertObject() throws Exception {
        KieSession ksession = (KieSession) container.getComponentInstance("ksession2");
        assertThat(ksession).isNotNull();

        assertThat(ksession.getObjects()).hasSize(1);
        assertThat(ksession.getObjects().toArray()[0] instanceof Person).isTrue();

        for (Object object : ksession.getObjects()) {
            if (object instanceof Person) {
                assertThat(((Person) object).isHappy()).isFalse();
            }
        }

        ksession.fireAllRules();

        //if the rules have fired, then the setHappy(true) should have been called
        for (Object object : ksession.getObjects()) {
            if (object instanceof Person) {
                assertThat(((Person) object).isHappy()).isTrue();
            }
        }
    }

    @Test
    public void testInsertObjectAndFireAll() throws Exception {
        KieSession ksession = (KieSession) container.getComponentInstance("ksessionForCommands");
        assertThat(ksession).isNotNull();

        assertThat(ksession.getObjects()).hasSize(1);
        assertThat(ksession.getObjects().toArray()[0] instanceof Person).isTrue();

        //if the rules should have fired without any invoke of fireAllRules, then the setHappy(true) should have been called
        for (Object object : ksession.getObjects()) {
            if (object instanceof Person) {
                assertThat(((Person) object).isHappy()).isTrue();
            }
        }
    }

    @Test
    public void testSetGlobals() throws Exception {
        KieSession ksession = (KieSession) container.getComponentInstance("ksessionForCommands");
        assertThat(ksession).isNotNull();

        assertThat(ksession.getObjects()).hasSize(1);
        assertThat(ksession.getObjects().toArray()[0] instanceof Person).isTrue();
        Person p1 = (Person) ksession.getObjects().toArray()[0];
        assertThat(p1).isNotNull();
        //if the rules should have fired without any invoke of fireAllRules, then the setHappy(true) should have been called
        for (Object object : ksession.getObjects()) {
            if (object instanceof Person) {
                assertThat(((Person) object).isHappy()).isTrue();
            }
        }

        Object list = ksession.getGlobal("persons");
        assertThat(list).isNotNull();
        assertThat(list instanceof ArrayList).isTrue();
        assertThat(((ArrayList) list)).hasSize(1);
        Person p = (Person) ((ArrayList) list).get(0);
        assertThat(p).isNotNull();
        assertThat(p1).isEqualTo(p);
    }

    @AfterClass
    public static void tearDown() {
        container.destroy();
    }

}
