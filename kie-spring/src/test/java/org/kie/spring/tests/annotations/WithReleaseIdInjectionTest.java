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
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.spring.beans.annotations.BeanWithReleaseId;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WithReleaseIdInjectionTest {

    static ApplicationContext context = null;

    @BeforeClass
    public static void setup() {
        context = new ClassPathXmlApplicationContext("org/kie/spring/annotations/annotations-releaseId.xml");
    }

    @Test
    public void testContext() throws Exception {
        assertThat(context).isNotNull();
    }

    @Test
    public void testKContainer() throws Exception {
        BeanWithReleaseId sampleBean = (BeanWithReleaseId) context.getBean("sampleBean");
        assertThat(sampleBean).isNotNull();
        assertThat(sampleBean.getKieContainer() ).isNotNull();
        assertThat(sampleBean.getKieContainer() instanceof KieContainer ).isTrue();
        ReleaseId releaseId = sampleBean.getKieContainer().getReleaseId();
        assertThat("named-kiesession".equalsIgnoreCase(releaseId.getArtifactId())).isTrue();
    }

    @Test
    public void testKieBase() throws Exception {
        BeanWithReleaseId sampleBean = (BeanWithReleaseId) context.getBean("sampleBean");
        assertThat(sampleBean).isNotNull();
        assertThat(sampleBean.getKieBase() ).isNotNull();
        assertThat(sampleBean.getKieBase() instanceof KieBase ).isTrue();
    }


    @AfterClass
    public static void tearDown() {

    }

}
