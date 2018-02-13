/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.server.controller.impl.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.server.controller.api.model.runtime.Container;
import org.kie.server.controller.api.model.runtime.ContainerList;
import org.kie.server.controller.api.model.runtime.ServerInstanceKey;
import org.kie.server.controller.api.model.runtime.ServerInstanceKeyList;
import org.kie.server.controller.impl.KieServerInstanceManager;
import org.kie.server.controller.impl.storage.InMemoryKieServerTemplateStorage;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class RuntimeManagementServiceImplTest extends AbstractServiceImplTest {

    @Before
    public void setup() {
        runtimeManagementService = new RuntimeManagementServiceImpl();
        specManagementService = new SpecManagementServiceImpl();
        kieServerInstanceManager = Mockito.mock(KieServerInstanceManager.class);

        ((RuntimeManagementServiceImpl)runtimeManagementService).setKieServerInstanceManager(kieServerInstanceManager);

        createServerTemplateWithContainer();
    }

    @After
    public void cleanup() {
        InMemoryKieServerTemplateStorage.getInstance().clear();
    }

    @Test
    public void testGetServerInstances() {

        ServerInstanceKeyList found = runtimeManagementService.getServerInstances(serverTemplate.getId());
        assertThat(found).isNotNull();

        assertThat(found.getServerInstanceKeys().length).isEqualTo(0);

        serverTemplate.addServerInstance(new ServerInstanceKey(serverTemplate.getId(), "test server","instanceId" , "http://fake.url.org"));
        specManagementService.saveServerTemplate(serverTemplate);

        found = runtimeManagementService.getServerInstances(serverTemplate.getId());
        assertThat(found).isNotNull();

        assertThat(found.getServerInstanceKeys().length).isEqualTo(1);

        org.kie.server.controller.api.model.runtime.ServerInstanceKey server = found.getServerInstanceKeys()[0];
        assertThat(server).isNotNull();

        assertThat(server.getServerTemplateId()).isEqualTo(serverTemplate.getId());
        assertThat(server.getServerInstanceId()).isEqualTo("instanceId");
        assertThat(server.getServerName()).isEqualTo("test server");
        assertThat(server.getUrl()).isEqualTo("http://fake.url.org");
    }

    @Test
    public void testGetContainers() {

        List<Container> fakeResult = new ArrayList<Container>();
        fakeResult.add(container);
        when(kieServerInstanceManager.getContainers(any(ServerInstanceKey.class))).thenReturn(fakeResult);

        org.kie.server.controller.api.model.runtime.ServerInstanceKey instanceKey = new ServerInstanceKey("instanceId", "test server", serverTemplate.getId(), "http://fake.url.org");
        serverTemplate.addServerInstance(instanceKey);
        specManagementService.saveServerTemplate(serverTemplate);

        ContainerList containers = runtimeManagementService.getContainers(instanceKey);
        assertThat(containers).isNotNull();

        assertThat(containers.getContainers().length).isEqualTo(1);
        verify(kieServerInstanceManager, times(1)).getContainers(any(ServerInstanceKey.class));
    }

}
