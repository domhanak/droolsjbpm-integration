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
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.spring.beans.annotations.KContainerBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class KContainerInjectionTest {

    static ApplicationContext context = null;

    @BeforeClass
    public static void setup() {
        context = new ClassPathXmlApplicationContext("org/kie/spring/annotations/kcontainer-tests.xml");
    }

    @Test
    public void testContext() throws Exception {
        assertThat(context).isNotNull();
    }

    @Test
    public void testKContainer() throws Exception {
        KContainerBean sampleBean = (KContainerBean) context.getBean("sampleBean");
        assertThat(sampleBean).isNotNull();
        assertThat(sampleBean.getKieContainer()).isNotNull();
        Collection<String> kieBaseNames = sampleBean.getKieContainer().getKieBaseNames();
        assertThat(kieBaseNames).as("Expecting different number of KieBases!").hasSize(2);
        assertThat(kieBaseNames.contains("drl_kiesample")).as("Expecting KieBase 'drl_kiesample'!").isTrue();
        assertThat(kieBaseNames.contains("drl_kiesample3")).as("Expecting KieBase 'drl_kiesample3'!").isTrue();
    }

    @Test
    public void testSetterKContainer() throws Exception {
        KContainerBean sampleBean = (KContainerBean) context.getBean("sampleBean");
        assertThat(sampleBean).isNotNull();
        assertThat(sampleBean.getKieContainer2()).isNotNull();
        Collection<String> kieBaseNames = sampleBean.getKieContainer2().getKieBaseNames();
        assertThat(kieBaseNames).as("Expecting different number of KieBases!").hasSize(2);
        assertThat(kieBaseNames.contains("drl_kiesample")).as("Expecting KieBase 'drl_kiesample'!").isTrue();
        assertThat(kieBaseNames.contains("drl_kiesample3")).as("Expecting KieBase 'drl_kiesample3'!").isTrue();
    }


    @AfterClass
    public static void tearDown() {

    }

}
