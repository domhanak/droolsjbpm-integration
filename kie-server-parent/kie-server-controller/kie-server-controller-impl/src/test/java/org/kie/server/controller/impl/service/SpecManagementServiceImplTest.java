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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.KieScannerStatus;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.controller.api.KieServerControllerNotFoundException;
import org.kie.server.controller.api.model.events.ServerTemplateUpdated;
import org.kie.server.controller.api.model.runtime.Container;
import org.kie.server.controller.api.model.runtime.ServerInstanceKey;
import org.kie.server.controller.api.model.spec.*;
import org.kie.server.controller.api.service.NotificationService;
import org.kie.server.controller.api.storage.KieServerTemplateStorage;
import org.kie.server.controller.impl.KieServerInstanceManager;
import org.kie.server.controller.impl.storage.InMemoryKieServerTemplateStorage;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SpecManagementServiceImplTest extends AbstractServiceImplTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private KieServerTemplateStorage templateStorage;

    @Mock
    private KieServerInstanceManager kieServerInstanceManager;

    @Mock
    private NotificationService notificationService;

    @Before
    public void setup() {
        specManagementService = new SpecManagementServiceImpl();

        final SpecManagementServiceImpl specManagementService = (SpecManagementServiceImpl) this.specManagementService;

        specManagementService.setKieServerInstanceManager(kieServerInstanceManager);
    }

    @After
    public void cleanup() {
        InMemoryKieServerTemplateStorage.getInstance().clear();
    }

    @Test
    public void testCreateServerTemplate() {

        ServerTemplate serverTemplate = new ServerTemplate();

        serverTemplate.setName("test server");
        serverTemplate.setId(UUID.randomUUID().toString());

        specManagementService.saveServerTemplate(serverTemplate);

        ServerTemplateKeyList existing = specManagementService.listServerTemplateKeys();
        assertThat(existing).isNotNull();
        assertThat(existing.getServerTemplates().length).isEqualTo(1);

        org.kie.server.controller.api.model.spec.ServerTemplateKey saved = existing.getServerTemplates()[0];

        assertThat(saved.getName()).isEqualTo(serverTemplate.getName());
        assertThat(saved.getId()).isEqualTo(serverTemplate.getId());
    }

    @Test
    public void testCreateServerTemplateAndContainer() {

        ServerTemplate serverTemplate = new ServerTemplate();

        serverTemplate.setName("test server");
        serverTemplate.setId(UUID.randomUUID().toString());

        specManagementService.saveServerTemplate(serverTemplate);

        ServerTemplateKeyList existing = specManagementService.listServerTemplateKeys();
        assertThat(existing).isNotNull();
        assertThat(existing.getServerTemplates().length).isEqualTo(1);

        Map<Capability, ContainerConfig> configs = new HashMap<Capability, ContainerConfig>();
        RuleConfig ruleConfig = new RuleConfig();
        ruleConfig.setPollInterval(1000l);
        ruleConfig.setScannerStatus(KieScannerStatus.STARTED);

        configs.put(Capability.RULE, ruleConfig);

        ProcessConfig processConfig = new ProcessConfig();
        processConfig.setKBase("defaultKieBase");
        processConfig.setKSession("defaultKieSession");
        processConfig.setMergeMode("MERGE_COLLECTION");
        processConfig.setRuntimeStrategy("PER_PROCESS_INSTANCE");

        configs.put(Capability.PROCESS, processConfig);

        ContainerSpec containerSpec = new ContainerSpec();
        containerSpec.setId("test container");
        containerSpec.setServerTemplateKey(new ServerTemplateKey(serverTemplate.getId(), serverTemplate.getName()));
        containerSpec.setReleasedId(new ReleaseId("org.kie", "kie-server-kjar", "1.0"));
        containerSpec.setStatus(KieContainerStatus.STOPPED);
        containerSpec.setConfigs(configs);

        specManagementService.saveContainerSpec(serverTemplate.getId(), containerSpec);

        org.kie.server.controller.api.model.spec.ServerTemplate createdServerTemplate = specManagementService.getServerTemplate(serverTemplate.getId());
        assertThat(createdServerTemplate).isNotNull();
        assertThat(createdServerTemplate.getContainersSpec()).isNotNull();
        assertThat(createdServerTemplate.getContainersSpec()).hasSize(1);

        org.kie.server.controller.api.model.spec.ContainerSpec container = createdServerTemplate.getContainersSpec().iterator().next();
        assertThat(container).isNotNull();

        assertThat(container.getId()).isEqualTo(containerSpec.getId());
        assertThat(container.getStatus()).isEqualTo(containerSpec.getStatus());
        assertThat(container.getServerTemplateKey()).isEqualTo(containerSpec.getServerTemplateKey());
        assertThat(container.getReleasedId()).isEqualTo(containerSpec.getReleasedId());

        assertThat(container.getConfigs()).isNotNull();
        assertThat(container.getConfigs().size()).isEqualTo(containerSpec.getConfigs().size());

        ContainerSpecList specs = specManagementService.listContainerSpec(serverTemplate.getId());
        assertThat(specs).isNotNull();
        assertThat(specs.getContainerSpecs().length).isEqualTo(1);

        container = specs.getContainerSpecs()[0];
        assertThat(container).isNotNull();

        assertThat(container.getId()).isEqualTo(containerSpec.getId());
        assertThat(container.getStatus()).isEqualTo(containerSpec.getStatus());
        assertThat(container.getServerTemplateKey()).isEqualTo(containerSpec.getServerTemplateKey());
        assertThat(container.getReleasedId()).isEqualTo(containerSpec.getReleasedId());

        assertThat(container.getConfigs()).isNotNull();
        assertThat(container.getConfigs().size()).isEqualTo(containerSpec.getConfigs().size());
    }

    @Test
    public void testListServerTemplates() {

        int limit = getRandomInt(5, 10);
        for (int x = 0; x < limit; x++) {
            ServerTemplate serverTemplate = new ServerTemplate();

            serverTemplate.setName("test server " + x);
            serverTemplate.setId(UUID.randomUUID().toString());

            specManagementService.saveServerTemplate(serverTemplate);
        }
        ServerTemplateKeyList existing = specManagementService.listServerTemplateKeys();
        assertThat(existing).isNotNull();
        assertThat(existing.getServerTemplates().length).isEqualTo(limit);

        ServerTemplateList allTemplates = specManagementService.listServerTemplates();
        assertThat(allTemplates).isNotNull();
        assertThat(allTemplates.getServerTemplates().length).isEqualTo(limit);
    }

    @Test
    public void testCreateAndDeleteServerTemplate() {

        ServerTemplate serverTemplate = new ServerTemplate();

        serverTemplate.setName("test server");
        serverTemplate.setId(UUID.randomUUID().toString());

        specManagementService.saveServerTemplate(serverTemplate);

        ServerTemplateKeyList existing = specManagementService.listServerTemplateKeys();
        assertThat(existing).isNotNull();
        assertThat(existing.getServerTemplates().length).isEqualTo(1);

        org.kie.server.controller.api.model.spec.ServerTemplateKey saved = existing.getServerTemplates()[0];

        assertThat(saved.getName()).isEqualTo(serverTemplate.getName());
        assertThat(saved.getId()).isEqualTo(serverTemplate.getId());

        specManagementService.deleteServerTemplate(serverTemplate.getId());
        existing = specManagementService.listServerTemplateKeys();
        assertThat(existing).isNotNull();
        assertThat(existing.getServerTemplates().length).isEqualTo(0);
    }

    @Test
    public void testCreateServerTemplateAndAddRemoveContainer() {

        ServerTemplate serverTemplate = new ServerTemplate();

        serverTemplate.setName("test server");
        serverTemplate.setId(UUID.randomUUID().toString());

        specManagementService.saveServerTemplate(serverTemplate);

        ServerTemplateKeyList existing = specManagementService.listServerTemplateKeys();
        assertThat(existing).isNotNull();
        assertThat(existing.getServerTemplates().length).isEqualTo(1);

        int limit = getRandomInt(3, 6);
        for (int x = 0; x < limit; x++) {

            Map<Capability, ContainerConfig> configs = new HashMap<Capability, ContainerConfig>();
            RuleConfig ruleConfig = new RuleConfig();
            ruleConfig.setPollInterval(1000l);
            ruleConfig.setScannerStatus(KieScannerStatus.STARTED);

            ProcessConfig processConfig = new ProcessConfig();
            processConfig.setKBase("defaultKieBase");
            processConfig.setKSession("defaultKieSession");
            processConfig.setMergeMode("MERGE_COLLECTION");
            processConfig.setRuntimeStrategy("PER_PROCESS_INSTANCE");

            ContainerSpec containerSpec = new ContainerSpec();
            containerSpec.setId("test container " + x);
            containerSpec.setServerTemplateKey(new ServerTemplateKey(serverTemplate.getId(), serverTemplate.getName()));
            containerSpec.setReleasedId(new ReleaseId("org.kie", "kie-server-kjar", x + ".0"));
            containerSpec.setStatus(KieContainerStatus.STOPPED);
            containerSpec.setConfigs(configs);

            specManagementService.saveContainerSpec(serverTemplate.getId(), containerSpec);
        }

        org.kie.server.controller.api.model.spec.ServerTemplate createdServerTemplate = specManagementService.getServerTemplate(serverTemplate.getId());
        assertThat(createdServerTemplate).isNotNull();
        assertThat(createdServerTemplate.getContainersSpec()).isNotNull();
        assertThat(createdServerTemplate.getContainersSpec().size()).isEqualTo(limit);

        // remove first container with suffix 0
        specManagementService.deleteContainerSpec(serverTemplate.getId(), "test container " + 0);

        createdServerTemplate = specManagementService.getServerTemplate(serverTemplate.getId());
        assertThat(createdServerTemplate).isNotNull();
        assertThat(createdServerTemplate.getContainersSpec()).isNotNull();
        assertThat(createdServerTemplate.getContainersSpec().size()).isEqualTo(limit - 1);
    }

    @Test
    public void testCreateServerTemplateAndCreateThenCopyContainer() {

        ServerTemplate serverTemplate = new ServerTemplate();

        serverTemplate.setName("test server");
        serverTemplate.setId(UUID.randomUUID().toString());

        specManagementService.saveServerTemplate(serverTemplate);

        ServerTemplateKeyList existing = specManagementService.listServerTemplateKeys();
        assertThat(existing).isNotNull();
        assertThat(existing.getServerTemplates().length).isEqualTo(1);

        Map<Capability, ContainerConfig> configs = new HashMap<Capability, ContainerConfig>();
        RuleConfig ruleConfig = new RuleConfig();
        ruleConfig.setPollInterval(1000l);
        ruleConfig.setScannerStatus(KieScannerStatus.STARTED);

        ProcessConfig processConfig = new ProcessConfig();
        processConfig.setKBase("defaultKieBase");
        processConfig.setKSession("defaultKieSession");
        processConfig.setMergeMode("MERGE_COLLECTION");
        processConfig.setRuntimeStrategy("PER_PROCESS_INSTANCE");

        ContainerSpec containerSpec = new ContainerSpec();
        containerSpec.setId("test container");
        containerSpec.setServerTemplateKey(new ServerTemplateKey(serverTemplate.getId(), serverTemplate.getName()));
        containerSpec.setReleasedId(new ReleaseId("org.kie", "kie-server-kjar", "1.0"));
        containerSpec.setStatus(KieContainerStatus.STOPPED);
        containerSpec.setConfigs(configs);

        specManagementService.saveContainerSpec(serverTemplate.getId(), containerSpec);

        org.kie.server.controller.api.model.spec.ServerTemplate createdServerTemplate = specManagementService.getServerTemplate(serverTemplate.getId());
        assertThat(createdServerTemplate).isNotNull();
        assertThat(createdServerTemplate.getContainersSpec()).isNotNull();
        assertThat(createdServerTemplate.getContainersSpec()).hasSize(1);

        org.kie.server.controller.api.model.spec.ContainerSpec container = createdServerTemplate.getContainersSpec().iterator().next();
        assertThat(container).isNotNull();

        assertThat(container.getId()).isEqualTo(containerSpec.getId());
        assertThat(container.getStatus()).isEqualTo(containerSpec.getStatus());
        assertThat(container.getServerTemplateKey()).isEqualTo(containerSpec.getServerTemplateKey());
        assertThat(container.getReleasedId()).isEqualTo(containerSpec.getReleasedId());

        assertThat(container.getConfigs()).isNotNull();
        assertThat(container.getConfigs().size()).isEqualTo(containerSpec.getConfigs().size());

        String newServerTemplateId = "Copied server id";
        String newServerTemplateName = "Copied server name";

        specManagementService.copyServerTemplate(serverTemplate.getId(), newServerTemplateId, newServerTemplateName);

        existing = specManagementService.listServerTemplateKeys();
        assertThat(existing).isNotNull();
        assertThat(existing.getServerTemplates().length).isEqualTo(2);

        createdServerTemplate = specManagementService.getServerTemplate(newServerTemplateId);
        assertThat(createdServerTemplate).isNotNull();
        assertThat(createdServerTemplate.getName()).isEqualTo(newServerTemplateName);
        assertThat(createdServerTemplate.getId()).isEqualTo(newServerTemplateId);
        assertThat(createdServerTemplate.getContainersSpec()).isNotNull();
        assertThat(createdServerTemplate.getContainersSpec()).hasSize(1);

        container = createdServerTemplate.getContainersSpec().iterator().next();
        assertThat(container).isNotNull();

        assertThat(container.getId()).isEqualTo(containerSpec.getId());
        assertThat(container.getStatus()).isEqualTo(containerSpec.getStatus());
        assertThat(container.getServerTemplateKey().getId()).isEqualTo(newServerTemplateId);
        assertThat(container.getServerTemplateKey().getName()).isEqualTo(newServerTemplateName);
        assertThat(container.getReleasedId()).isEqualTo(containerSpec.getReleasedId());

        assertThat(container.getConfigs()).isNotNull();
        assertThat(container.getConfigs().size()).isEqualTo(containerSpec.getConfigs().size());
    }

    @Test
    public void testCreateServerTemplateAndUpdateContainerConfig() {

        ServerTemplate serverTemplate = new ServerTemplate();

        serverTemplate.setName("test server");
        serverTemplate.setId(UUID.randomUUID().toString());

        specManagementService.saveServerTemplate(serverTemplate);

        ServerTemplateKeyList existing = specManagementService.listServerTemplateKeys();
        assertThat(existing).isNotNull();
        assertThat(existing.getServerTemplates().length).isEqualTo(1);

        Map<Capability, ContainerConfig> configs = new HashMap<Capability, ContainerConfig>();
        RuleConfig ruleConfig = new RuleConfig();
        ruleConfig.setPollInterval(1000l);
        ruleConfig.setScannerStatus(KieScannerStatus.STARTED);

        configs.put(Capability.RULE, ruleConfig);

        ProcessConfig processConfig = new ProcessConfig();
        processConfig.setKBase("defaultKieBase");
        processConfig.setKSession("defaultKieSession");
        processConfig.setMergeMode("MERGE_COLLECTION");
        processConfig.setRuntimeStrategy("PER_PROCESS_INSTANCE");

        configs.put(Capability.PROCESS, processConfig);

        ContainerSpec containerSpec = new ContainerSpec();
        containerSpec.setId("test container");
        containerSpec.setServerTemplateKey(new ServerTemplateKey(serverTemplate.getId(), serverTemplate.getName()));
        containerSpec.setReleasedId(new ReleaseId("org.kie", "kie-server-kjar", "1.0"));
        containerSpec.setStatus(KieContainerStatus.STOPPED);
        containerSpec.setConfigs(configs);

        specManagementService.saveContainerSpec(serverTemplate.getId(), containerSpec);

        org.kie.server.controller.api.model.spec.ServerTemplate createdServerTemplate = specManagementService.getServerTemplate(serverTemplate.getId());
        assertThat(createdServerTemplate).isNotNull();
        assertThat(createdServerTemplate.getContainersSpec()).isNotNull();
        assertThat(createdServerTemplate.getContainersSpec()).hasSize(1);

        org.kie.server.controller.api.model.spec.ContainerSpec container = createdServerTemplate.getContainersSpec().iterator().next();
        assertThat(container).isNotNull();

        assertThat(container.getId()).isEqualTo(containerSpec.getId());
        assertThat(container.getStatus()).isEqualTo(containerSpec.getStatus());
        assertThat(container.getServerTemplateKey()).isEqualTo(containerSpec.getServerTemplateKey());
        assertThat(container.getReleasedId()).isEqualTo(containerSpec.getReleasedId());

        assertThat(container.getConfigs()).isNotNull();
        assertThat(container.getConfigs().size()).isEqualTo(containerSpec.getConfigs().size());

        ContainerConfig ruleConfigCurrent = containerSpec.getConfigs().get(Capability.RULE);
        assertThat(ruleConfigCurrent).isNotNull();
        assertThat(ruleConfigCurrent instanceof org.kie.server.controller.api.model.spec.RuleConfig).isTrue();
        assertThat(((org.kie.server.controller.api.model.spec.RuleConfig) ruleConfigCurrent).getPollInterval()).isEqualTo(ruleConfig.getPollInterval());
        assertThat(((org.kie.server.controller.api.model.spec.RuleConfig) ruleConfigCurrent).getScannerStatus()).isEqualTo(ruleConfig.getScannerStatus());

        ContainerConfig containerConfig = new RuleConfig();
        ((RuleConfig) containerConfig).setScannerStatus(KieScannerStatus.SCANNING);
        ((RuleConfig) containerConfig).setPollInterval(10l);

        specManagementService.updateContainerConfig(serverTemplate.getId(), containerSpec.getId(), Capability.RULE, containerConfig);

        ContainerSpecList specs = specManagementService.listContainerSpec(serverTemplate.getId());
        assertThat(specs).isNotNull();
        assertThat(specs.getContainerSpecs().length).isEqualTo(1);

        container = specs.getContainerSpecs()[0];
        assertThat(container).isNotNull();

        assertThat(container.getId()).isEqualTo(containerSpec.getId());
        assertThat(container.getStatus()).isEqualTo(containerSpec.getStatus());
        assertThat(container.getServerTemplateKey()).isEqualTo(containerSpec.getServerTemplateKey());
        assertThat(container.getReleasedId()).isEqualTo(containerSpec.getReleasedId());

        assertThat(container.getConfigs()).isNotNull();
        assertThat(container.getConfigs().size()).isEqualTo(containerSpec.getConfigs().size());

        ContainerConfig ruleConfigCurrent2 = containerSpec.getConfigs().get(Capability.RULE);
        assertThat(ruleConfigCurrent2).isNotNull();
        assertThat(ruleConfigCurrent2 instanceof org.kie.server.controller.api.model.spec.RuleConfig).isTrue();
        assertThat(((org.kie.server.controller.api.model.spec.RuleConfig) ruleConfigCurrent2).getPollInterval()).isEqualTo(((org.kie.server.controller.api.model.spec.RuleConfig) containerConfig).getPollInterval());
        assertThat(((org.kie.server.controller.api.model.spec.RuleConfig) ruleConfigCurrent2).getScannerStatus()).isEqualTo(((org.kie.server.controller.api.model.spec.RuleConfig) containerConfig).getScannerStatus());
    }

    @Test
    public void testStartContainer() {
        createServerTemplateWithContainer();
        List<Container> fakeResult = new ArrayList<Container>();
        fakeResult.add(container);
        when(kieServerInstanceManager.startContainer(any(ServerTemplate.class), any(ContainerSpec.class))).thenReturn(fakeResult);

        specManagementService.startContainer(containerSpec);

        verify(kieServerInstanceManager, times(1)).startContainer(any(ServerTemplate.class), any(ContainerSpec.class));

        ServerTemplate updated = specManagementService.getServerTemplate(serverTemplate.getId());
        assertThat(updated).isNotNull();

        ContainerSpec updatedContainer = updated.getContainerSpec(containerSpec.getId());
        assertThat(updatedContainer).isNotNull();

        assertThat(updatedContainer.getStatus()).isEqualTo(KieContainerStatus.STARTED);
    }

    @Test
    public void testStopContainer() {
        createServerTemplateWithContainer();
        List<Container> fakeResult = new ArrayList<Container>();
        fakeResult.add(container);
        when(kieServerInstanceManager.stopContainer(any(ServerTemplate.class), any(ContainerSpec.class))).thenReturn(fakeResult);

        specManagementService.stopContainer(containerSpec);

        verify(kieServerInstanceManager, times(1)).stopContainer(any(ServerTemplate.class), any(ContainerSpec.class));

        ServerTemplate updated = specManagementService.getServerTemplate(serverTemplate.getId());
        assertThat(updated).isNotNull();

        ContainerSpec updatedContainer = updated.getContainerSpec(containerSpec.getId());
        assertThat(updatedContainer).isNotNull();

        assertThat(updatedContainer.getStatus()).isEqualTo(KieContainerStatus.STOPPED);
    }

    @Test
    public void testUpdateContainerConfigWhenContainerConfigIsARuleConfig() {

        final SpecManagementServiceImpl specManagementService = spy((SpecManagementServiceImpl) this.specManagementService);
        final Capability capability = Capability.RULE;
        final RuleConfig ruleConfig = mock(RuleConfig.class);
        final ServerTemplate serverTemplate = mock(ServerTemplate.class);
        final ContainerSpec containerSpec = mock(ContainerSpec.class);

        final List<?> expectedContainers = mock(List.class);

        doReturn(expectedContainers).when(specManagementService).updateContainerRuleConfig(ruleConfig,
                                                                                           serverTemplate,
                                                                                           containerSpec);

        final List<Container> actualContainers = specManagementService.updateContainerConfig(capability,
                                                                                             ruleConfig,
                                                                                             serverTemplate,
                                                                                             containerSpec);
        assertThat(actualContainers).isEqualTo(expectedContainers);
    }

    @Test
    public void testUpdateContainerConfigWhenContainerConfigIsAProcessConfig() {

        final SpecManagementServiceImpl specManagementService = spy((SpecManagementServiceImpl) this.specManagementService);
        final Capability capability = Capability.PROCESS;
        final ProcessConfig processConfig = mock(ProcessConfig.class);
        final ServerTemplate serverTemplate = mock(ServerTemplate.class);
        final ContainerSpec containerSpec = mock(ContainerSpec.class);

        final List<?> expectedContainers = mock(List.class);

        doReturn(expectedContainers).when(specManagementService).updateContainerProcessConfig(processConfig,
                                                                                              capability,
                                                                                              serverTemplate,
                                                                                              containerSpec);

        final List<Container> actualContainers = specManagementService.updateContainerConfig(capability,
                                                                                             processConfig,
                                                                                             serverTemplate,
                                                                                             containerSpec);
        assertThat(actualContainers).isEqualTo(expectedContainers);
    }

    @Test
    public void testUpdateContainerConfigWhenServerTemplateIsNull() {

        final SpecManagementServiceImpl specManagementService = (SpecManagementServiceImpl) this.specManagementService;
        final String serverTemplateId = "serverTemplateId";
        final String containerSpecId = "containerSpecId";
        final Capability capability = Capability.PROCESS;
        final ContainerConfig containerConfig = mock(ContainerConfig.class);

        specManagementService.setTemplateStorage(templateStorage);

        doReturn(null).when(templateStorage).load(serverTemplateId);

        expectedException.expect(KieServerControllerNotFoundException.class);
        expectedException.expectMessage("No server template found for id serverTemplateId");

        specManagementService.updateContainerConfig(serverTemplateId, containerSpecId, capability, containerConfig);
    }

    @Test
    public void testUpdateContainerConfigWhenContainerSpecIsNull() {

        final SpecManagementServiceImpl specManagementService = (SpecManagementServiceImpl) this.specManagementService;
        final String serverTemplateId = "serverTemplateId";
        final String containerSpecId = "containerSpecId";
        final Capability capability = Capability.PROCESS;
        final ContainerConfig containerConfig = mock(ContainerConfig.class);
        final ServerTemplate serverTemplate = mock(ServerTemplate.class);

        specManagementService.setTemplateStorage(templateStorage);

        doReturn(serverTemplate).when(templateStorage).load(serverTemplateId);
        doReturn(null).when(serverTemplate).getContainersSpec();

        expectedException.expect(KieServerControllerNotFoundException.class);
        expectedException.expectMessage("No container spec found for id containerSpecId within server template with id serverTemplateId");

        specManagementService.updateContainerConfig(serverTemplateId, containerSpecId, capability, containerConfig);
    }

    @Test
    public void testUpdateContainerConfigWhenAffectedContainersIsEmpty() {

        final SpecManagementServiceImpl specManagementService = spy((SpecManagementServiceImpl) this.specManagementService);
        final String serverTemplateId = "serverTemplateId";
        final String containerSpecId = "containerSpecId";
        final Capability capability = Capability.PROCESS;
        final ContainerConfig containerConfig = mock(ContainerConfig.class);
        final ServerTemplate serverTemplate = mock(ServerTemplate.class);
        final ContainerSpec containerSpec = mock(ContainerSpec.class);
        final Map<Capability, ContainerConfig> configs = spy(new HashMap<>());
        final List<?> expectedContainers = new ArrayList<>();

        specManagementService.setTemplateStorage(templateStorage);
        specManagementService.setNotificationService(notificationService);

        doReturn(serverTemplate).when(templateStorage).load(serverTemplateId);
        doReturn(containerSpec).when(serverTemplate).getContainerSpec(containerSpecId);
        doReturn(expectedContainers).when(specManagementService).updateContainerConfig(capability, containerConfig, serverTemplate, containerSpec);
        doReturn(configs).when(containerSpec).getConfigs();

        specManagementService.updateContainerConfig(serverTemplateId, containerSpecId, capability, containerConfig);

        verify(specManagementService).logInfo("Update of container configuration resulted in no changes to containers running on kie-servers");
        verify(specManagementService, never()).logDebug(any(), any());
        verify(configs).put(capability, containerConfig);
        verify(templateStorage).update(serverTemplate);
        verify(notificationService).notify(any(ServerTemplateUpdated.class));
    }

    @Test
    public void testUpdateContainerConfigWhenAffectedContainersIsNotEmpty() {

        final SpecManagementServiceImpl specManagementService = spy((SpecManagementServiceImpl) this.specManagementService);
        final String serverTemplateId = "serverTemplateId";
        final String containerSpecId = "containerSpecId";
        final Capability capability = Capability.PROCESS;
        final ContainerConfig containerConfig = mock(ContainerConfig.class);
        final ServerTemplate serverTemplate = mock(ServerTemplate.class);
        final ContainerSpec containerSpec = mock(ContainerSpec.class);
        final Map<Capability, ContainerConfig> configs = spy(new HashMap<>());
        final Container container1 = makeContainer("1");
        final Container container2 = makeContainer("2");
        final List<Container> expectedContainers = new ArrayList<Container>() {{
            add(container1);
            add(container2);
        }};

        specManagementService.setTemplateStorage(templateStorage);
        specManagementService.setNotificationService(notificationService);

        doReturn(serverTemplate).when(templateStorage).load(serverTemplateId);
        doReturn(containerSpec).when(serverTemplate).getContainerSpec(containerSpecId);
        doReturn(expectedContainers).when(specManagementService).updateContainerConfig(capability, containerConfig, serverTemplate, containerSpec);
        doReturn(configs).when(containerSpec).getConfigs();

        specManagementService.updateContainerConfig(serverTemplateId, containerSpecId, capability, containerConfig);

        verify(specManagementService).logDebug("Container {} on server {} was affected by a change in the scanner",
                                               container1.getContainerSpecId(),
                                               container1.getServerInstanceKey());
        verify(specManagementService).logDebug("Container {} on server {} was affected by a change in the scanner",
                                               container2.getContainerSpecId(),
                                               container2.getServerInstanceKey());
        verify(specManagementService, never()).logInfo(any());
        verify(configs).put(capability, containerConfig);
        verify(templateStorage).update(serverTemplate);
        verify(notificationService).notify(any(ServerTemplateUpdated.class));
    }

    private Container makeContainer(final String seed) {

        final Container container = mock(Container.class);

        doReturn(seed).when(container).getContainerSpecId();
        doReturn(mock(ServerInstanceKey.class)).when(container).getServerInstanceKey();

        return container;
    }

    @Test
    public void testUpdateContainerConfigWhenContainerConfigIsNotAProcessConfigNeitherARuleConfig() {

        final SpecManagementServiceImpl specManagementService = spy((SpecManagementServiceImpl) this.specManagementService);
        final Capability capability = Capability.PROCESS;
        final ContainerConfig containerConfig = mock(ContainerConfig.class);
        final ServerTemplate serverTemplate = mock(ServerTemplate.class);
        final ContainerSpec containerSpec = mock(ContainerSpec.class);
        final List<?> expectedContainers = new ArrayList<>();

        final List<Container> actualContainers = specManagementService.updateContainerConfig(capability,
                                                                                             containerConfig,
                                                                                             serverTemplate,
                                                                                             containerSpec);
        assertThat(actualContainers).isEqualTo(expectedContainers);
    }

    @Test
    public void testUpdateContainerProcessConfig() {

        final SpecManagementServiceImpl specManagementService = (SpecManagementServiceImpl) this.specManagementService;
        final ProcessConfig processConfig = mock(ProcessConfig.class);
        final Capability capability = Capability.PROCESS;
        final ServerTemplate serverTemplate = mock(ServerTemplate.class);
        final ContainerSpec containerSpec = mock(ContainerSpec.class);
        final Map<Capability, ProcessConfig> configs = spy(new HashMap<>());
        final List<?> expectedContainers = mock(List.class);

        doReturn(configs).when(containerSpec).getConfigs();
        doReturn(expectedContainers).when(kieServerInstanceManager).upgradeContainer(serverTemplate, containerSpec);

        final List<Container> actualContainers = specManagementService.updateContainerProcessConfig(processConfig,
                                                                                                    capability,
                                                                                                    serverTemplate,
                                                                                                    containerSpec);

        assertThat(actualContainers).isEqualTo(expectedContainers);
    }

    @Test
    public void testUpdateContainerRuleConfigWhenKieScannerStatusIsStarted() {

        final SpecManagementServiceImpl specManagementService = (SpecManagementServiceImpl) this.specManagementService;
        final RuleConfig ruleConfig = mock(RuleConfig.class);
        final ServerTemplate serverTemplate = mock(ServerTemplate.class);
        final ContainerSpec containerSpec = mock(ContainerSpec.class);
        final Long interval = 1L;
        final List<?> expectedContainers = mock(List.class);

        doReturn(interval).when(ruleConfig).getPollInterval();
        doReturn(KieScannerStatus.STARTED).when(ruleConfig).getScannerStatus();
        doReturn(expectedContainers).when(kieServerInstanceManager).startScanner(serverTemplate, containerSpec, interval);

        final List<Container> actualContainers = specManagementService.updateContainerRuleConfig(ruleConfig,
                                                                                                 serverTemplate,
                                                                                                 containerSpec);

        assertThat(actualContainers).isEqualTo(expectedContainers);
    }

    @Test
    public void testUpdateContainerRuleConfigWhenKieScannerStatusIsStopped() {

        final SpecManagementServiceImpl specManagementService = (SpecManagementServiceImpl) this.specManagementService;
        final RuleConfig ruleConfig = mock(RuleConfig.class);
        final ServerTemplate serverTemplate = mock(ServerTemplate.class);
        final ContainerSpec containerSpec = mock(ContainerSpec.class);
        final List<?> expectedContainers = mock(List.class);

        doReturn(KieScannerStatus.STOPPED).when(ruleConfig).getScannerStatus();
        doReturn(expectedContainers).when(kieServerInstanceManager).stopScanner(serverTemplate, containerSpec);

        final List<Container> actualContainers = specManagementService.updateContainerRuleConfig(ruleConfig, serverTemplate, containerSpec);

        assertThat(actualContainers).isEqualTo(expectedContainers);
    }

    @Test
    public void testUpdateContainerRuleConfigWhenKieScannerStatusIsNotStartedNeitherStopped() {

        final SpecManagementServiceImpl specManagementService = (SpecManagementServiceImpl) this.specManagementService;
        final RuleConfig ruleConfig = mock(RuleConfig.class);
        final ServerTemplate serverTemplate = mock(ServerTemplate.class);
        final ContainerSpec containerSpec = mock(ContainerSpec.class);
        final List<?> expectedContainers = new ArrayList<>();

        doReturn(KieScannerStatus.UNKNOWN).when(ruleConfig).getScannerStatus();

        final List<Container> actualContainers = specManagementService.updateContainerRuleConfig(ruleConfig, serverTemplate, containerSpec);

        assertThat(actualContainers).isEqualTo(expectedContainers);
    }

    protected int getRandomInt(int min, int max) {
        return (int) Math.floor(Math.random() * (max - min + 1)) + min;
    }

    @Test
    public void testDeleteServerInstance() {
        final ServerTemplate serverTemplate = new ServerTemplate("serverTemplateId",
                                                                 "serverTemplateName");
        final ServerInstanceKey serverInstanceKey = new ServerInstanceKey(serverTemplate.getId(),
                                                                          "serverName",
                                                                          "serverInstanceId",
                                                                          "url");

        serverTemplate.addServerInstance(serverInstanceKey);
        specManagementService.saveServerTemplate(serverTemplate);
        when(kieServerInstanceManager.isAlive(serverInstanceKey)).thenReturn(false);

        specManagementService.deleteServerInstance(serverInstanceKey);

        final ServerTemplate updatedServerTemplate = specManagementService.getServerTemplate(serverTemplate.getId());

        assertEquals(0,
                     updatedServerTemplate.getServerInstanceKeys().size());
    }

    @Test
    public void testDeleteServerInstanceAlive() {
        final ServerTemplate serverTemplate = new ServerTemplate("serverTemplateId",
                                                                 "serverTemplateName");
        final ServerInstanceKey serverInstanceKey = new ServerInstanceKey(serverTemplate.getId(),
                                                                          "serverName",
                                                                          "serverInstanceId",
                                                                          "url");

        serverTemplate.addServerInstance(serverInstanceKey);
        specManagementService.saveServerTemplate(serverTemplate);
        when(kieServerInstanceManager.isAlive(serverInstanceKey)).thenReturn(true);

        try {
            specManagementService.deleteServerInstance(serverInstanceKey);
            fail("Deleting a live server instance should fail");
        } catch (Exception ex) {
            assertEquals("Can't delete live instance.",
                         ex.getMessage());
        }

        final ServerTemplate updatedServerTemplate = specManagementService.getServerTemplate(serverTemplate.getId());

        assertEquals(1,
                     updatedServerTemplate.getServerInstanceKeys().size());
    }
}
