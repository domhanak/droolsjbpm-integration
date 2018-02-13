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

import org.drools.decisiontable.Cheese;
import org.drools.decisiontable.Person;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.ExecutionResults;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class KieSpringBasicsDecisionTest {

    static ApplicationContext context = null;

    @BeforeClass
    public static void setup() {
        context = new ClassPathXmlApplicationContext("org/kie/spring/decision/decision-beans.xml");
    }

    @Test
    public void testContext() throws Exception {
        assertThat(context).isNotNull();
    }

    @Test
    public void testKieBase() throws Exception {
        KieBase kbase = (KieBase) context.getBean("decisionCSV");
        assertThat(kbase).isNotNull();
    }

    @Test
    public void testDecisionTableRules() throws Exception {
        StatelessKieSession ksession = (StatelessKieSession) context.getBean("ksession-table-1");

        assertThat(ksession).isNotNull();

        // Cheeses selection
        Cheese cheese = new Cheese();
        cheese.setPrice(250);
        cheese.setType("cheddar");

        // Young person
        Person person = new Person();
        person.setName("Young Scott");
        person.setAge(21);

        List cmds = new ArrayList();
        cmds.add( CommandFactory.newSetGlobal("list", new ArrayList(), true) );
        cmds.add( CommandFactory.newInsert(person,"yscott"));
        cmds.add( CommandFactory.newInsert(cheese,"cheddar"));
        cmds.add( CommandFactory.newFireAllRules());

        // Execute the list
        ExecutionResults results = ksession.execute(CommandFactory.newBatchExecution(cmds));
        List list = (List) results.getValue("list");
        assertThat(list).hasSize(1);
        assertThat(list.contains("Young man cheddar")).isTrue();

        // Old person
        person = new Person();
        person.setName("Old Scott");
        person.setAge(42);

        cheese = new Cheese();
        cheese.setPrice(150);
        cheese.setType("stilton");

        cmds = new ArrayList();
        cmds.add( CommandFactory.newSetGlobal("list", new ArrayList(), true) );
        cmds.add( CommandFactory.newInsert(person,"oscott"));
        cmds.add( CommandFactory.newInsert(cheese,"stilton"));
        cmds.add( CommandFactory.newFireAllRules());

        // Execute the list
        results = ksession.execute(CommandFactory.newBatchExecution(cmds));
        list = (List) results.getValue("list");
        assertThat(list).hasSize(1);
        assertThat(list.contains("Old man stilton")).isTrue();

    }

    @AfterClass
    public static void tearDown() { }

}
