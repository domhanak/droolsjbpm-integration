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

package org.kie.spring.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.spring.beans.Person;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class KieSpringCommandsTest {

    static ApplicationContext context = null;

    @BeforeClass
    public static void setup() {
        context = new ClassPathXmlApplicationContext("org/kie/spring/beans-commands.xml");
    }

    @Test
    public void testKieBase() throws Exception {
        KieBase kbase = (KieBase) context.getBean("drl_kiesample3");
        assertThat(kbase).isNotNull();
    }

    @Test
    public void testInsertObject() throws Exception {
        KieSession ksession = (KieSession) context.getBean("ksession2");
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
        KieSession ksession = (KieSession) context.getBean("ksessionForCommands");
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
    public void testStatelessKieSessionWithGlobal() throws Exception {
        StatelessKieSession ksession = (StatelessKieSession) context.getBean( "statlessKsessionWithGlobal" );
        assertThat(ksession).isNotNull();

        Person person = new Person("HAL2", 42);
        person.setHappy(false);
        ksession.execute(person);
        assertThat(person.isHappy()).isTrue();
    }

    @Test
    public void testStatelessKieSessionWithGlobalExecutingList() throws Exception {
        StatelessKieSession ksession = (StatelessKieSession) context.getBean( "statlessKsessionWithGlobal" );
        assertThat(ksession).isNotNull();

        List<Person> persons = new ArrayList<Person>();

        Person person1 = new Person("HAL", 42);
        person1.setHappy(false);
        persons.add(person1);

        Person person2 = new Person("HAL2", 42);
        person2.setHappy(false);
        persons.add(person2);

        ksession.execute(persons);
        assertThat(person1.isHappy()).isTrue();
        assertThat(person2.isHappy()).isTrue();
    }

    @Test
    public void testSetGlobals() throws Exception {
        KieSession ksession = (KieSession) context.getBean("ksessionForCommands");
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
    }

}
