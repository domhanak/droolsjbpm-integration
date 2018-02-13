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
import org.drools.core.base.CalendarsImpl;
import org.drools.core.base.MapGlobalResolver;
import org.drools.core.marshalling.impl.IdentityPlaceholderResolverStrategy;
import org.drools.core.marshalling.impl.SerializablePlaceholderResolverStrategy;
import org.drools.persistence.jpa.marshaller.JPAPlaceholderResolverStrategy;
import org.jbpm.marshalling.impl.ProcessInstanceResolverStrategy;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.aries.blueprint.KieBlueprintContainer;
import org.kie.aries.blueprint.mocks.MockEntityManager;
import org.kie.aries.blueprint.mocks.MockJpaTransactionManager;
import org.kie.aries.blueprint.mocks.MockObjectMarshallingStrategy;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class KieBlueprintEnvironmentTest {

    static BlueprintContainerImpl container = null;
    
    @BeforeClass
    public static void runBeforeClass() throws Exception {
        List<URL> urls = new ArrayList<URL>();
        urls.add(KieBlueprintListenerTest.class.getResource("/org/kie/aries/blueprint/environment.xml"));
        container = new KieBlueprintContainer(ClassLoader.getSystemClassLoader(), urls);
    }

    @Test
    public void testCtxNotNull() throws Exception {
        assertThat(container).isNotNull();
    }

    @Test
    public void testEnvRef() throws Exception {
        Environment environment = (Environment) container.getComponentInstance("drools-env");
        assertThat(environment).isNotNull();
    }

    @Test
    public void testEnvRefTransManager() throws Exception {
        Environment environment = (Environment) container.getComponentInstance("drools-env");
        assertThat(environment).isNotNull();

        assertThat(environment.get(EnvironmentName.TRANSACTION_MANAGER)).isNotNull();
        assertThat(environment.get(EnvironmentName.TRANSACTION_MANAGER) instanceof MockJpaTransactionManager).isTrue();
    }

    @Test
    public void testEnvRefEMF() throws Exception {
        Environment environment = (Environment) container.getComponentInstance("drools-env");
        assertThat(environment).isNotNull();

        assertThat(environment.get(EnvironmentName.ENTITY_MANAGER_FACTORY)).isNotNull();
        assertThat(environment.get(EnvironmentName.ENTITY_MANAGER_FACTORY) instanceof MockEntityManager).isTrue();
    }

    @Test
    public void testEnvRefGlobals() throws Exception {
        Environment environment = (Environment) container.getComponentInstance("drools-env");
        assertThat(environment).isNotNull();

        assertThat(environment.get(EnvironmentName.GLOBALS)).isNotNull();
        assertThat(environment.get(EnvironmentName.GLOBALS) instanceof MapGlobalResolver).isTrue();
    }

    @Test
    public void testEnvRefCalendars() throws Exception {
        Environment environment = (Environment) container.getComponentInstance("drools-env");
        assertThat(environment).isNotNull();

        assertThat(environment.get(EnvironmentName.CALENDARS)).isNotNull();
        assertThat(environment.get(EnvironmentName.CALENDARS) instanceof CalendarsImpl).isTrue();
    }

    @Test
    public void testEnvRefUserTransaction() throws Exception {
        Environment environment = (Environment) container.getComponentInstance("drools-env");
        assertThat(environment).isNotNull();

        assertThat(environment.get(EnvironmentName.TRANSACTION)).isNotNull();
        assertThat(environment.get(EnvironmentName.TRANSACTION) instanceof MockJpaTransactionManager).isTrue();
    }

    @Test
    public void testEnvRefTransactionSyncRegistry() throws Exception {
        Environment environment = (Environment) container.getComponentInstance("drools-env");
        assertThat(environment).isNotNull();

        assertThat(environment.get(EnvironmentName.TRANSACTION_SYNCHRONIZATION_REGISTRY)).isNotNull();
        assertThat(environment.get(EnvironmentName.TRANSACTION) instanceof MockJpaTransactionManager).isTrue();
    }

    @Test
    public void testEmptyEnvRef() throws Exception {
        Environment environment = (Environment) container.getComponentInstance("drools-empty-env");
        assertThat(environment).isNotNull();
    }

    @Test
    public void testEnvCustomMarshallerRef() throws Exception {
        Environment environment = (Environment) container.getComponentInstance("drools-env-custom-marshaller-ref");
        assertThat(environment).isNotNull();

        ObjectMarshallingStrategy[] objectMarshallingStrategies = (ObjectMarshallingStrategy[]) environment.get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES);
        assertThat(objectMarshallingStrategies.length).isEqualTo(1);
        assertThat("org.kie.aries.blueprint.mocks.MockObjectMarshallingStrategy").isEqualTo(objectMarshallingStrategies[0].getClass().getName());
    }

    @Test
    public void testEnvMarshallerOrder() throws Exception {
        Environment environment = (Environment) container.getComponentInstance("drools-env");
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
        Environment environment = (Environment) container.getComponentInstance("drools-env-custom-marshaller-mixed");
        assertThat(environment).isNotNull();

        ObjectMarshallingStrategy[] objectMarshallingStrategies = (ObjectMarshallingStrategy[]) environment.get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES);
        assertThat(objectMarshallingStrategies.length).isEqualTo(5);
        assertThat(objectMarshallingStrategies[0] instanceof SerializablePlaceholderResolverStrategy).isTrue();
        assertThat(objectMarshallingStrategies[1] instanceof IdentityPlaceholderResolverStrategy).isTrue();
        assertThat(objectMarshallingStrategies[2] instanceof JPAPlaceholderResolverStrategy).isTrue();
        assertThat(objectMarshallingStrategies[3] instanceof MockObjectMarshallingStrategy).isTrue();
        assertThat(objectMarshallingStrategies[4] instanceof ProcessInstanceResolverStrategy).isTrue();
    }

    @AfterClass
    public static void tearDown(){
        container.destroy();
    }
}
