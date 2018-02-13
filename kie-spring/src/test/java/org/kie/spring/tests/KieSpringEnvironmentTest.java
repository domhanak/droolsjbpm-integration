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

import org.drools.core.marshalling.impl.IdentityPlaceholderResolverStrategy;
import org.drools.core.marshalling.impl.SerializablePlaceholderResolverStrategy;
import org.drools.persistence.jpa.marshaller.JPAPlaceholderResolverStrategy;
import org.h2.tools.DeleteDbFiles;
import org.h2.tools.Server;
import org.jbpm.marshalling.impl.ProcessInstanceResolverStrategy;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.spring.mocks.MockObjectMarshallingStrategy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.*;

public class KieSpringEnvironmentTest {

    static ApplicationContext context = null;
    private static Server h2Server;

    @BeforeClass
    public static void startH2Database() throws Exception {
        DeleteDbFiles.execute("",
                "DroolsFlow",
                true);
        h2Server = Server.createTcpServer(new String[0]);
        h2Server.start();
    }

    @AfterClass
    public static void stopH2Database() throws Exception {
        h2Server.stop();
        DeleteDbFiles.execute("",
                "DroolsFlow",
                true);
    }

    @BeforeClass
    public static void runBeforeClass() {
        context = new ClassPathXmlApplicationContext("org/kie/spring/environment.xml");
    }

    @Test
    public void testCtxNotNull() throws Exception {
        assertThat(context).isNotNull();
    }

    @Test
    public void testEnvRef() throws Exception {
        Environment environment = (Environment) context.getBean("drools-env");
        assertThat(environment).isNotNull();

        assertThat(environment.get(EnvironmentName.TRANSACTION_MANAGER)).isNotNull();
        assertThat(environment.get(EnvironmentName.ENTITY_MANAGER_FACTORY)).isNotNull();
        assertThat(environment.get(EnvironmentName.GLOBALS)).isNotNull();
        assertThat(environment.get(EnvironmentName.CALENDARS)).isNotNull();

        assertThat(environment.get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES)).isNotNull();
        assertThat(((ObjectMarshallingStrategy[]) environment.get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES)).length).isEqualTo(4);
    }

    @Test
    public void testEnvEmb() throws Exception {
        Environment environment = (Environment) context.getBean("drools-env-embedded");
        assertThat(environment).isNotNull();

        assertThat(environment.get(EnvironmentName.TRANSACTION_MANAGER)).isNotNull();
        assertThat(environment.get(EnvironmentName.ENTITY_MANAGER_FACTORY)).isNotNull();
        assertThat(environment.get(EnvironmentName.GLOBALS)).isNotNull();
        assertThat(environment.get(EnvironmentName.CALENDARS)).isNotNull();

        assertThat(environment.get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES)).isNotNull();
        assertThat(((ObjectMarshallingStrategy[]) environment.get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES)).length).isEqualTo(2);
    }

    @Test
    public void testEnvCustomMarshallerNested() throws Exception {
        Environment environment = (Environment) context.getBean("drools-env-custom-marshaller-nested");
        assertThat(environment).isNotNull();

        assertThat(environment.get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES)).isNotNull();
        ObjectMarshallingStrategy[] objectMarshallingStrategies = (ObjectMarshallingStrategy[]) environment.get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES);
        assertThat(objectMarshallingStrategies.length).isEqualTo(1);
        assertThat("org.kie.spring.mocks.MockObjectMarshallingStrategy").isEqualTo(objectMarshallingStrategies[0].getClass().getName());
    }

    @Test
    public void testEnvCustomMarshallerRef() throws Exception {
        Environment environment = (Environment) context.getBean("drools-env-custom-marshaller-ref");
        assertThat(environment).isNotNull();

        ObjectMarshallingStrategy[] objectMarshallingStrategies = (ObjectMarshallingStrategy[]) environment.get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES);
        assertThat(objectMarshallingStrategies.length).isEqualTo(1);
        assertThat("org.kie.spring.mocks.MockObjectMarshallingStrategy").isEqualTo(objectMarshallingStrategies[0].getClass().getName());
    }

    @Test
    public void testEnvMarshallerOrder() throws Exception {
        Environment environment = (Environment) context.getBean("drools-env");
        assertThat(environment).isNotNull();

        ObjectMarshallingStrategy[] objectMarshallingStrategies = (ObjectMarshallingStrategy[]) environment.get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES);
        assertThat(objectMarshallingStrategies.length).isEqualTo(4);
        assertThat(objectMarshallingStrategies[0] instanceof SerializablePlaceholderResolverStrategy).isTrue();
        assertThat(objectMarshallingStrategies[1] instanceof IdentityPlaceholderResolverStrategy).isTrue();
        assertThat(objectMarshallingStrategies[2] instanceof JPAPlaceholderResolverStrategy).isTrue();
        assertThat(objectMarshallingStrategies[3] instanceof ProcessInstanceResolverStrategy).isTrue();
    }

    @Test
    public void testEnvMarshallerOrderWithCustom() throws Exception {
        Environment environment = (Environment) context.getBean("drools-env-custom-marshaller-mixed");
        assertThat(environment).isNotNull();

        ObjectMarshallingStrategy[] objectMarshallingStrategies = (ObjectMarshallingStrategy[]) environment.get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES);
        assertThat(objectMarshallingStrategies.length).isEqualTo(5);
        assertThat(objectMarshallingStrategies[0] instanceof SerializablePlaceholderResolverStrategy).isTrue();
        assertThat(objectMarshallingStrategies[1] instanceof IdentityPlaceholderResolverStrategy).isTrue();
        assertThat(objectMarshallingStrategies[2] instanceof JPAPlaceholderResolverStrategy).isTrue();
        assertThat(objectMarshallingStrategies[3] instanceof MockObjectMarshallingStrategy).isTrue();
        assertThat(objectMarshallingStrategies[4] instanceof ProcessInstanceResolverStrategy).isTrue();
    }
}
