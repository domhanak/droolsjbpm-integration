/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.spring.tests.annotations;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.spring.beans.annotations.NamedKieBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NamedInjectionTest {

    static ApplicationContext context = null;

    @BeforeClass
    public static void setup() {
        context = new ClassPathXmlApplicationContext("org/kie/spring/annotations/kie-spring-annotations.xml");
    }

    @Test
    public void testContext() throws Exception {
        assertThat(context).isNotNull();
    }

    @Test
    public void testKieBase() throws Exception {
        KieBase kbase = (KieBase) context.getBean("drl_kiesample3");
        assertThat(kbase).isNotNull();
        NamedKieBean sampleBean = (NamedKieBean) context.getBean("sampleBean");
        assertThat(sampleBean).isNotNull();
        assertThat(sampleBean.getKieBase() ).isNotNull();
        assertThat(sampleBean.getKieBase() instanceof KieBase ).isTrue();
    }

    @Test
    public void testSetterKieBase() throws Exception {
        NamedKieBean sampleBean = (NamedKieBean) context.getBean("sampleBean");
        assertThat(sampleBean).isNotNull();
        assertThat(sampleBean.getKieBase2() ).isNotNull();
        assertThat(sampleBean.getKieBase2() instanceof KieBase ).isTrue();
    }

    @Test
    public void testStatelessKSessionInjection() throws Exception {
        NamedKieBean sampleBean = (NamedKieBean) context.getBean("sampleBean");
        assertThat(sampleBean).isNotNull();
        assertThat(sampleBean.getKieSession() ).isNotNull();
        assertThat(sampleBean.getKieSession() instanceof StatelessKieSession).isTrue();
    }

    @Test
    public void testStatefulKSessionInjection() throws Exception {
        NamedKieBean sampleBean = (NamedKieBean) context.getBean("sampleBean");
        assertThat(sampleBean).isNotNull();
        assertThat(sampleBean.getStatefulSession() ).isNotNull();
        assertThat(sampleBean.getStatefulSession() instanceof KieSession).isTrue();
    }

    @AfterClass
    public static void tearDown() {

    }

}
