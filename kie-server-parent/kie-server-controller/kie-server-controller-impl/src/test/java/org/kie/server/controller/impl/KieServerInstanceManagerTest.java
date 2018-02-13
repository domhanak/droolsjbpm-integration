/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.controller.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.KieScannerResource;
import org.kie.server.api.model.KieScannerStatus;
import org.kie.server.api.model.KieServerConfigItem;
import org.kie.server.api.model.Message;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.controller.api.model.runtime.Container;
import org.kie.server.controller.api.model.spec.Capability;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.controller.api.model.spec.ProcessConfig;
import org.kie.server.controller.api.model.spec.RuleConfig;
import org.kie.server.controller.api.model.spec.ServerTemplate;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class KieServerInstanceManagerTest {

    @Mock
    private ServerTemplate serverTemplate;

    @Mock
    private ContainerSpec containerSpec;

    @Mock
    private KieServicesClient client;

    @Mock
    private Container container;

    @Mock
    private ServiceResponse<?> response;

    @Mock
    private KieServerInstanceManager.RemoteKieServerOperation operation;

    @Mock
    private KieContainerResource containerResource;

    private KieServerInstanceManager instanceManager;

    @Before
    public void setUp() {
        instanceManager = spy(new KieServerInstanceManager());
    }

    @Test
    public void testGetContainers() {

        doReturn(operation).when(instanceManager).getContainersRemoteOperation(serverTemplate,
                                                                               containerSpec);

        instanceManager.getContainers(serverTemplate,
                                      containerSpec);

        verify(instanceManager).callRemoteKieServerOperation(serverTemplate,
                                                             containerSpec,
                                                             operation);
    }

    @Test
    public void testGetContainersRemoteOperationWhenResponseTypeIsSUCCESS() {

        doReturn(containerResource).when(response).getResult();
        doReturn(response).when(client).getContainerInfo(any());
        doReturn(ServiceResponse.ResponseType.SUCCESS).when(response).getType();

        final KieServerInstanceManager.RemoteKieServerOperation<Void> operation = instanceManager.getContainersRemoteOperation(serverTemplate,
                                                                                                                               containerSpec);

        operation.doOperation(client,
                              container);

        verify(container).setContainerSpecId(containerResource.getContainerId());
        verify(container).setContainerName(containerResource.getContainerId());
        verify(container).setResolvedReleasedId(containerResource.getReleaseId());
        verify(container).setServerTemplateId(serverTemplate.getId());
        verify(container).setStatus(containerResource.getStatus());
        verify(container).setMessages(containerResource.getMessages());
    }

    @Test
    public void testGetContainersRemoteOperationWhenResponseTypeIsNotSUCCESS() {

        doReturn(containerResource).when(response).getResult();
        doReturn(response).when(client).getContainerInfo(any());
        doReturn(ServiceResponse.ResponseType.FAILURE).when(response).getType();

        final KieServerInstanceManager.RemoteKieServerOperation<Void> operation = instanceManager.getContainersRemoteOperation(serverTemplate,
                                                                                                                               containerSpec);

        operation.doOperation(client,
                              container);

        verify(container,
               never()).setContainerSpecId(any());
        verify(container,
               never()).setContainerName(any());
        verify(container,
               never()).setResolvedReleasedId(any());
        verify(container,
               never()).setServerTemplateId(any());
        verify(container,
               never()).setStatus(any());
        verify(container,
               never()).setMessages(any());
    }

    @Test
    public void testMakeContainerResourceWhenConfigsIsNull() {

        final String id = "id";
        final ReleaseId releaseId = mock(ReleaseId.class);
        final ReleaseId resolvedReleasedId = mock(ReleaseId.class);
        final KieContainerStatus status = KieContainerStatus.CREATING;
        final String containerName = "containerName";
        final Collection<Message> messages = new ArrayList<>();

        doReturn(id).when(containerSpec).getId();
        doReturn(releaseId).when(containerSpec).getReleasedId();
        doReturn(resolvedReleasedId).when(container).getResolvedReleasedId();
        doReturn(status).when(container).getStatus();
        doReturn(containerName).when(containerSpec).getContainerName();
        doReturn(messages).when(container).getMessages();
        doReturn(null).when(containerSpec).getConfigs();

        final KieContainerResource resource = instanceManager.makeContainerResource(container, containerSpec);

        assertThat(resource.getContainerId()).isEqualTo(id);
        assertThat(resource.getReleaseId()).isEqualTo(releaseId);
        assertThat(resource.getResolvedReleaseId()).isEqualTo(resolvedReleasedId);
        assertThat(resource.getStatus()).isEqualTo(status);
        assertThat(resource.getContainerAlias()).isEqualTo(containerName);
        assertThat(resource.getMessages()).isEqualTo(messages);

        verify(instanceManager, never()).setRuleConfigAttributes(any(), any());
        verify(instanceManager, never()).setProcessConfigAttributes(any(), any());
    }

    @Test
    public void testStartContainer() {

        final List<?> expectedContainers = mock(List.class);

        doReturn(operation).when(instanceManager).makeStartContainerOperation(containerSpec);
        doReturn(expectedContainers).when(instanceManager).callRemoteKieServerOperation(serverTemplate, containerSpec, operation);

        final List<Container> actualContainers = instanceManager.startContainer(serverTemplate, containerSpec);

        verify(instanceManager).callRemoteKieServerOperation(serverTemplate, containerSpec, operation);

        assertThat(actualContainers).isEqualTo(expectedContainers);
    }

    @Test
    public void testMakeStartContainerOperationWhenResponseTypeIsSuccess() {

        final ServiceResponse.ResponseType responseType = ServiceResponse.ResponseType.SUCCESS;
        final String containerId = "id";

        doReturn(containerId).when(containerSpec).getId();
        doReturn(containerResource).when(instanceManager).makeContainerResource(container, containerSpec);
        doReturn(response).when(client).createContainer(containerId, containerResource);
        doReturn(responseType).when(response).getType();
        doNothing().when(instanceManager).collectContainerInfo(containerSpec, client, container);

        instanceManager.makeStartContainerOperation(containerSpec).doOperation(client, container);

        verify(client).createContainer(containerId, containerResource);
        verify(instanceManager).collectContainerInfo(containerSpec, client, container);
        verify(instanceManager, never()).log(any(), any(), any(), any());
    }

    @Test
    public void testMakeStartContainerOperationWhenResponseTypeIsNotSuccess() {

        final ServiceResponse.ResponseType responseType = ServiceResponse.ResponseType.FAILURE;
        final String containerId = "id";

        doReturn(containerId).when(containerSpec).getId();
        doReturn(containerResource).when(instanceManager).makeContainerResource(container, containerSpec);
        doReturn(response).when(client).createContainer(containerId, containerResource);
        doReturn(responseType).when(response).getType();
        doNothing().when(instanceManager).collectContainerInfo(containerSpec, client, container);

        instanceManager.makeStartContainerOperation(containerSpec).doOperation(client, container);

        verify(client).createContainer(containerId, containerResource);
        verify(instanceManager).collectContainerInfo(containerSpec, client, container);
        verify(instanceManager).log("Container {} failed to start on server instance {} due to {}", container, response, containerSpec);
    }

    @Test
    public void testMakeUpgradeContainerOperationWhenResponseTypeIsSuccess() {

        final ServiceResponse.ResponseType responseType = ServiceResponse.ResponseType.SUCCESS;
        final String containerId = "id";
        final ReleaseId releaseId = mock(ReleaseId.class);

        doReturn(containerId).when(containerSpec).getId();
        doReturn(releaseId).when(containerSpec).getReleasedId();
        doReturn(containerResource).when(instanceManager).makeContainerResource(container, containerSpec);
        doReturn(response).when(client).updateReleaseId(containerId, releaseId);
        doReturn(responseType).when(response).getType();
        doNothing().when(instanceManager).collectContainerInfo(containerSpec, client, container);

        instanceManager.makeUpgradeContainerOperation(containerSpec).doOperation(client, container);

        verify(client, never()).createContainer(anyString(), any(KieContainerResource.class));
        verify(client).updateReleaseId(containerId, releaseId);
        verify(instanceManager).collectContainerInfo(containerSpec, client, container);
        verify(instanceManager, never()).log(any(), any(), any(), any());
    }

    @Test
    public void testMakeUpgradeContainerOperationWhenResponseTypeIsNotSuccess() {

        final ServiceResponse.ResponseType responseType = ServiceResponse.ResponseType.FAILURE;
        final String containerId = "id";
        final String url = "url";
        final String msg = "msg";
        final ReleaseId releaseId = mock(ReleaseId.class);

        doReturn(containerId).when(containerSpec).getId();
        doReturn(releaseId).when(containerSpec).getReleasedId();
        doReturn(containerResource).when(instanceManager).makeContainerResource(container, containerSpec);
        doReturn(response).when(client).updateReleaseId(containerId, releaseId);
        doReturn(responseType).when(response).getType();
        doReturn(msg).when(response).getMsg();
        doReturn(url).when(container).getUrl();
        doNothing().when(instanceManager).collectContainerInfo(containerSpec, client, container);

        instanceManager.makeUpgradeContainerOperation(containerSpec).doOperation(client, container);

        verify(client, never()).createContainer(anyString(), any(KieContainerResource.class));
        verify(client).updateReleaseId(containerId, releaseId);
        verify(instanceManager).collectContainerInfo(containerSpec, client, container);
        verify(instanceManager).log("Container {} failed to upgrade on server instance {} due to {}", containerId, url, msg);
    }

    @Test
    public void testMakeUpgradeAndStartContainerOperationWhenResponseTypeIsSuccess() {

        final ServiceResponse.ResponseType responseType = ServiceResponse.ResponseType.SUCCESS;
        final String containerId = "id";
        final ReleaseId releaseId = mock(ReleaseId.class);

        doReturn(containerId).when(containerSpec).getId();
        doReturn(releaseId).when(containerSpec).getReleasedId();
        doReturn(containerResource).when(instanceManager).makeContainerResource(container, containerSpec);
        doReturn(response).when(client).updateReleaseId(containerId, releaseId);
        doReturn(responseType).when(response).getType();
        doNothing().when(instanceManager).collectContainerInfo(containerSpec, client, container);

        instanceManager.makeUpgradeAndStartContainerOperation(containerSpec).doOperation(client, container);

        verify(client).createContainer(containerId, containerResource);
        verify(client).updateReleaseId(containerId, releaseId);
        verify(instanceManager).collectContainerInfo(containerSpec, client, container);
        verify(instanceManager, never()).log(any(), any(), any(), any());
    }

    @Test
    public void testMakeUpgradeAndStartContainerOperationWhenResponseTypeIsNotSuccess() {

        final ServiceResponse.ResponseType responseType = ServiceResponse.ResponseType.FAILURE;
        final String containerId = "id";
        final String url = "url";
        final String msg = "msg";
        final ReleaseId releaseId = mock(ReleaseId.class);

        doReturn(containerId).when(containerSpec).getId();
        doReturn(releaseId).when(containerSpec).getReleasedId();
        doReturn(containerResource).when(instanceManager).makeContainerResource(container, containerSpec);
        doReturn(response).when(client).updateReleaseId(containerId, releaseId);
        doReturn(responseType).when(response).getType();
        doReturn(msg).when(response).getMsg();
        doReturn(url).when(container).getUrl();
        doNothing().when(instanceManager).collectContainerInfo(containerSpec, client, container);

        instanceManager.makeUpgradeAndStartContainerOperation(containerSpec).doOperation(client, container);

        verify(client).createContainer(containerId, containerResource);
        verify(client).updateReleaseId(containerId, releaseId);
        verify(instanceManager).collectContainerInfo(containerSpec, client, container);
        verify(instanceManager).log("Container {} failed to upgrade on server instance {} due to {}", containerId, url, msg);
    }

    @Test
    public void testMakeContainerResourceWhenConfigsIsNotNull() {

        final String id = "id";
        final ReleaseId releaseId = mock(ReleaseId.class);
        final ReleaseId resolvedReleasedId = mock(ReleaseId.class);
        final KieContainerStatus status = KieContainerStatus.CREATING;
        final String containerName = "containerName";
        final Collection<Message> messages = new ArrayList<>();
        final Map<?, ?> configs = mock(Map.class);

        doReturn(id).when(containerSpec).getId();
        doReturn(releaseId).when(containerSpec).getReleasedId();
        doReturn(resolvedReleasedId).when(container).getResolvedReleasedId();
        doReturn(status).when(container).getStatus();
        doReturn(containerName).when(containerSpec).getContainerName();
        doReturn(messages).when(container).getMessages();
        doReturn(configs).when(containerSpec).getConfigs();

        final KieContainerResource resource = instanceManager.makeContainerResource(container, containerSpec);

        assertThat(resource.getContainerId()).isEqualTo(id);
        assertThat(resource.getReleaseId()).isEqualTo(releaseId);
        assertThat(resource.getResolvedReleaseId()).isEqualTo(resolvedReleasedId);
        assertThat(resource.getStatus()).isEqualTo(status);
        assertThat(resource.getContainerAlias()).isEqualTo(containerName);
        assertThat(resource.getMessages()).isEqualTo(messages);

        verify(instanceManager).setRuleConfigAttributes(containerSpec, resource);
        verify(instanceManager).setProcessConfigAttributes(containerSpec, resource);
    }

    @Test
    public void testMakeKieServerConfigItem() {

        final String type = "type";
        final String name = "name";
        final String value = "value";

        final KieServerConfigItem configItem = instanceManager.makeKieServerConfigItem(type, name, value);

        assertThat(configItem.getType()).isEqualTo(type);
        assertThat(configItem.getName()).isEqualTo(name);
        assertThat(configItem.getValue()).isEqualTo(value);
    }

    @Test
    public void testSetRuleConfigAttributesWhenRuleConfigIsNotNull() {

        final Map<?, ?> configs = mock(Map.class);
        final RuleConfig containerConfig = mock(RuleConfig.class);
        final ArgumentCaptor<KieScannerResource> scannerResourceCaptor = ArgumentCaptor.forClass(KieScannerResource.class);
        final Long pollInterval = 1L;
        final KieScannerStatus scannerStatus = KieScannerStatus.CREATED;

        doReturn(pollInterval).when(containerConfig).getPollInterval();
        doReturn(scannerStatus).when(containerConfig).getScannerStatus();
        doReturn(containerConfig).when(configs).get(Capability.RULE);
        doReturn(configs).when(containerSpec).getConfigs();

        instanceManager.setRuleConfigAttributes(containerSpec, containerResource);

        verify(containerResource).setScanner(scannerResourceCaptor.capture());

        final KieScannerResource scannerResource = scannerResourceCaptor.getValue();

        assertThat(scannerResource.getPollInterval()).isEqualTo(pollInterval);
        assertThat(scannerResource.getStatus()).isEqualTo(scannerStatus);
    }

    @Test
    public void testSetRuleConfigAttributesWhenRuleConfigIsNull() {

        final Map<?, ?> configs = mock(Map.class);

        doReturn(null).when(configs).get(Capability.RULE);
        doReturn(configs).when(containerSpec).getConfigs();

        instanceManager.setRuleConfigAttributes(containerSpec, containerResource);

        verify(containerResource, never()).setScanner(any());
    }

    @Test
    public void testSetProcessConfigAttributesWhenProcessConfigIsNotNull() {

        final Map<?, ?> configs = mock(Map.class);
        final ProcessConfig processConfig = new ProcessConfig("runtimeStrategy", "kBase", "kSession", "mergeMode");
        final KieContainerResource containerResource = spy(new KieContainerResource());
        final List<KieServerConfigItem> actualConfigItems = containerResource.getConfigItems();
        final KieServerConfigItem expectedConfigItem0 = configItem(KieServerConstants.CAPABILITY_BPM,
                                                                   KieServerConstants.PCFG_KIE_BASE,
                                                                   processConfig.getKBase());
        final KieServerConfigItem expectedConfigItem1 = configItem(KieServerConstants.CAPABILITY_BPM,
                                                                   KieServerConstants.PCFG_KIE_SESSION,
                                                                   processConfig.getKSession());
        final KieServerConfigItem expectedConfigItem2 = configItem(KieServerConstants.CAPABILITY_BPM,
                                                                   KieServerConstants.PCFG_MERGE_MODE,
                                                                   processConfig.getMergeMode());
        final KieServerConfigItem expectedConfigItem3 = configItem(KieServerConstants.CAPABILITY_BPM,
                                                                   KieServerConstants.PCFG_RUNTIME_STRATEGY,
                                                                   processConfig.getRuntimeStrategy());

        doReturn(processConfig).when(configs).get(Capability.PROCESS);
        doReturn(configs).when(containerSpec).getConfigs();

        instanceManager.setProcessConfigAttributes(containerSpec, containerResource);

        assertThat(actualConfigItems.get(0)).isEqualTo(expectedConfigItem0);
        assertThat(actualConfigItems.get(1)).isEqualTo(expectedConfigItem1);
        assertThat(actualConfigItems.get(2)).isEqualTo(expectedConfigItem2);
        assertThat(actualConfigItems.get(3)).isEqualTo(expectedConfigItem3);
        assertThat(actualConfigItems).hasSize(4);
    }

    @Test
    public void testSetProcessConfigAttributesWhenProcessConfigIsNull() {

        final Map<?, ?> configs = mock(Map.class);
        final KieContainerResource containerResource = spy(new KieContainerResource());
        final List<KieServerConfigItem> actualConfigItems = containerResource.getConfigItems();

        doReturn(null).when(configs).get(Capability.PROCESS);
        doReturn(configs).when(containerSpec).getConfigs();

        instanceManager.setProcessConfigAttributes(containerSpec, containerResource);

        assertThat(actualConfigItems).isEmpty();
    }

    @Test
    public void testUpgradeContainer() {

        doReturn(operation).when(instanceManager).makeUpgradeContainerOperation(containerSpec);

        instanceManager.upgradeContainer(serverTemplate, containerSpec);

        verify(instanceManager).callRemoteKieServerOperation(serverTemplate, containerSpec, operation);
    }

    @Test
    public void testUpgradeAndStartContainer() {

        doReturn(operation).when(instanceManager).makeUpgradeAndStartContainerOperation(containerSpec);

        instanceManager.upgradeAndStartContainer(serverTemplate, containerSpec);

        verify(instanceManager).callRemoteKieServerOperation(serverTemplate, containerSpec, operation);
    }

    private KieServerConfigItem configItem(final String capabilityBpm,
                                           final String pcfgKieBase,
                                           final String kBase) {

        return instanceManager.makeKieServerConfigItem(capabilityBpm,
                                                       pcfgKieBase,
                                                       kBase);
    }
}
