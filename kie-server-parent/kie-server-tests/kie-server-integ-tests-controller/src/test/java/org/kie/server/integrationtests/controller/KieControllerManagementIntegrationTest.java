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

package org.kie.server.integrationtests.controller;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.model.*;
import org.kie.server.controller.api.ModelFactory;
import org.kie.server.controller.api.model.runtime.ServerInstanceKey;
import org.kie.server.controller.api.model.spec.*;
import org.kie.server.controller.impl.storage.InMemoryKieServerTemplateStorage;
import org.kie.server.controller.client.exception.KieServerControllerClientException;
import org.kie.server.integrationtests.category.Smoke;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.kie.server.integrationtests.shared.KieServerSynchronization;

import static org.assertj.core.api.Assertions.*;

public abstract class KieControllerManagementIntegrationTest<T extends KieServerControllerClientException> extends KieControllerManagementBaseTest {

    protected static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "1.0.0-SNAPSHOT");
    protected static ReleaseId releaseId101 = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "1.1.0-SNAPSHOT");

    protected static final String CONTAINER_ID = "kie-concurrent";
    protected static final String CONTAINER_NAME = "containerName";

    protected KieServerInfo kieServerInfo;

    @BeforeClass
    public static void initialize() throws Exception {
        KieServerDeployer.createAndDeployKJar(releaseId);
        KieServerDeployer.createAndDeployKJar(releaseId101);
    }

    @Before
    public void getKieServerInfo() {
        InMemoryKieServerTemplateStorage.getInstance().clear();
        // Getting info from currently started kie server.
        ServiceResponse<KieServerInfo> reply = client.getServerInfo();
        KieServerAssert.assertSuccess(reply);
        kieServerInfo = reply.getResult();
    }

    protected abstract void assertNotFoundException(T exception);

    protected abstract void assertBadRequestException(T exception);

    protected void checkServerTemplate(ServerTemplate actual) {
        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(kieServerInfo.getServerId());
        assertThat(actual.getName()).isEqualTo(kieServerInfo.getName());
    }

    protected ServerTemplate createServerTemplate() {
        return createServerTemplate(kieServerInfo.getServerId(), kieServerInfo.getName(), kieServerInfo.getLocation());
    }

    protected ServerTemplate createServerTemplate(String id, String name, String location) {
        ServerTemplate serverTemplate = new ServerTemplate();
        serverTemplate.setId( id );
        serverTemplate.setName( name );

        serverTemplate.addServerInstance(ModelFactory.newServerInstanceKey(serverTemplate.getId(), location));
        controllerClient.saveServerTemplate(serverTemplate);

        return serverTemplate;
    }

    protected void checkContainer(ContainerSpec container, KieContainerStatus status) {
        assertThat(container).isNotNull();
        assertThat(container.getId()).isEqualTo(CONTAINER_ID);
        assertThat(container.getReleasedId()).isEqualTo(releaseId);
        assertThat(container.getStatus()).isEqualTo(status);
    }

    protected void checkContainerConfig(String serverTemplateId, String containerId, ContainerConfig... configs) {
        Map<Capability, ContainerConfig> configMap = controllerClient.getContainerInfo(serverTemplateId, containerId).getConfigs();
        assertThat(configMap).isNotNull();

        for (ContainerConfig config : configs) {
            if (config instanceof ProcessConfig) {
                ProcessConfig pc = (ProcessConfig) config;
                ProcessConfig processConfig = (ProcessConfig) configMap.get(Capability.PROCESS);
                assertThat(processConfig).isNotNull();
                assertThat(processConfig.getKBase()).isEqualTo(pc.getKBase());
                assertThat(processConfig.getKSession()).isEqualTo(pc.getKSession());
                assertThat(processConfig.getMergeMode()).isEqualTo(pc.getMergeMode());
                assertThat(processConfig.getRuntimeStrategy()).isEqualTo(pc.getRuntimeStrategy());
            } else if (config instanceof RuleConfig) {
                RuleConfig rc = (RuleConfig) config;
                RuleConfig ruleConfig = (RuleConfig) configMap.get(Capability.RULE);
                assertThat(ruleConfig).isNotNull();
                assertThat(ruleConfig.getPollInterval()).isEqualTo(rc.getPollInterval());
                assertThat(ruleConfig.getScannerStatus()).isEqualTo(rc.getScannerStatus());
            }
        }
    }

    @Test
    public void testDeleteNotExistingServerTemplate() {
        try {
            // Try to delete not existing server template.
            controllerClient.deleteServerTemplate("not existing");
            fail("Should throw exception about kie server instance not existing.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }
    }

    @Test
    @Category(Smoke.class)
    public void testCreateKieServerTemplate() {
        ServerTemplate serverTemplate = createServerTemplate();

        ServerTemplate storedServerTemplate = controllerClient.getServerTemplate(serverTemplate.getId());
        checkServerTemplate(storedServerTemplate);

        ServerTemplateList serverTemplates = controllerClient.listServerTemplates();
        assertThat(serverTemplates).isNotNull();
        assertThat(serverTemplates.getServerTemplates().length).isEqualTo(1);

        storedServerTemplate = serverTemplates.getServerTemplates()[0];
        checkServerTemplate(storedServerTemplate);
    }

    @Test
    public void testCreateDuplicateServerTemplate() {
        // Create kie server template.
        ServerTemplate serverTemplate = createServerTemplate();

        // Trying to create same kie server will result in updating instead.
        controllerClient.saveServerTemplate(serverTemplate);
        ServerTemplateList serverTemplates = controllerClient.listServerTemplates();
        assertThat(serverTemplates).isNotNull();
        assertThat(serverTemplates.getServerTemplates().length).isEqualTo(1);
    }

    @Test
    public void testDeleteServerTemplate() {
        ServerTemplate serverTemplate = createServerTemplate();

        ServerTemplateList serverTemplates = controllerClient.listServerTemplates();
        assertThat(serverTemplates).isNotNull();
        assertThat(serverTemplates.getServerTemplates().length).isEqualTo(1);

        // Delete created server template.
        controllerClient.deleteServerTemplate(serverTemplate.getId());

        // There are no kie server instances in controller now.
        serverTemplates = controllerClient.listServerTemplates();
        KieServerAssert.assertNullOrEmpty("Active kie server instance found!", serverTemplates.getServerTemplates());
    }

    @Test
    @Category(Smoke.class)
    public void testGetKieServerInstance() {
        // Create kie server instance in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        ServerInstanceKey serverInstanceKey = serverTemplate.getServerInstanceKeys().iterator().next();

        // Get kie server instance.
        ServerTemplate serverInstance = controllerClient.getServerTemplate(serverTemplate.getId());

        checkServerTemplate(serverInstance);

        assertThat(serverInstance.getServerInstanceKeys()).as("Kie server instance isn't managed!").isNotNull();
        assertThat(serverInstance.getServerInstanceKeys()).hasSize(1);

        ServerInstanceKey managedInstance = serverInstance.getServerInstanceKeys().iterator().next();
        assertThat(managedInstance).isNotNull();
        assertThat(managedInstance.getUrl()).isEqualTo(kieServerInfo.getLocation());
        assertThat(managedInstance.getServerTemplateId()).isEqualTo(serverTemplate.getId());
        assertThat(managedInstance.getServerName()).isEqualTo(serverInstanceKey.getServerName());
    }

    @Test
    public void testGetNotExistingServerTemplate() {
        try {
            // Try to get not existing kie server template.
            controllerClient.getServerTemplate(kieServerInfo.getServerId());
            fail("Should throw exception about kie server instance not existing.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }
    }

    @Test
    public void testListKieServerInstances() {
        // Create kie server instance in controller.
        createServerTemplate();

        // List kie server instances.
        ServerTemplateList instanceList = controllerClient.listServerTemplates();

        assertThat(instanceList).isNotNull();
        assertThat(instanceList.getServerTemplates().length).isEqualTo(1);

        ServerTemplate serverInstance = instanceList.getServerTemplates()[0];
        checkServerTemplate(serverInstance);

        assertThat(serverInstance.getServerInstanceKeys()).as("Kie server instance isn't managed!").isNotNull();
        assertThat(serverInstance.getServerInstanceKeys()).hasSize(1);

        ServerInstanceKey managedInstance = serverInstance.getServerInstanceKeys().iterator().next();
        assertThat(managedInstance).isNotNull();
        assertThat(managedInstance.getUrl()).isEqualTo(kieServerInfo.getLocation());
    }

    @Test
    public void testEmptyListServerTemplates() throws Exception {
        ServerTemplateList instanceList = controllerClient.listServerTemplates();
        KieServerAssert.assertNullOrEmpty("Server templates found!", instanceList.getServerTemplates());
    }

    @Test
    public void testContainerHandling() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap());
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Check that container is deployed.
        ContainerSpec containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);

        // Container is in stopped state, so there are no containers deployed in kie server.
        ServiceResponse<KieContainerResourceList> containersList = client.listContainers();
        assertThat(containersList.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        KieServerAssert.assertNullOrEmpty("Active containers found!", containersList.getResult().getContainers());

        // Undeploy container for kie server instance.
        controllerClient.deleteContainerSpec(serverTemplate.getId(), CONTAINER_ID);

        // Check that container is disposed.
        try {
            controllerClient.getContainerInfo(serverTemplate.getId(), CONTAINER_ID);
            fail("Should throw exception about container info not found.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }
    }

    @Test
    public void testCreateContainerAutoStart() throws Exception {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STARTED, new HashMap());
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Check that container is deployed.
        ContainerSpec containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STARTED);

        // Container is in started state, so it should already be in kie server
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        ServiceResponse<KieContainerResourceList> containersList = client.listContainers();
        assertThat(containersList.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        assertThat(containersList.getResult().getContainers()).hasSize(1);

        ServiceResponse<KieContainerResource> containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertThat(containerInfo.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        assertThat(containerInfo.getResult().getContainerId()).isEqualTo(CONTAINER_ID);
        assertThat(containerInfo.getResult().getStatus()).isEqualTo(KieContainerStatus.STARTED);
        assertThat(containerInfo.getResult().getReleaseId()).isEqualTo(releaseId);
    }

    @Test
    public void testCreateServerTemplateWithContainersAutoStart() throws Exception {
        ServerTemplate serverTemplate = new ServerTemplate();
        serverTemplate.setId( kieServerInfo.getServerId() );
        serverTemplate.setName( kieServerInfo.getName() );

        serverTemplate.addServerInstance(ModelFactory.newServerInstanceKey(serverTemplate.getId(), kieServerInfo.getLocation()));

        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, new ServerTemplateKey(serverTemplate.getId(), serverTemplate.getName()), releaseId, KieContainerStatus.STARTED, new HashMap());
        serverTemplate.addContainerSpec(containerToDeploy);

        controllerClient.saveServerTemplate(serverTemplate);

        // Check that container is deployed.
        ContainerSpec containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STARTED);

        // Container is in started state, so it should already be in kie server
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        ServiceResponse<KieContainerResourceList> containersList = client.listContainers();
        assertThat(containersList.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        assertThat(containersList.getResult().getContainers()).hasSize(1);

        ServiceResponse<KieContainerResource> containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertThat(containerInfo.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        assertThat(containerInfo.getResult().getContainerId()).isEqualTo(CONTAINER_ID);
        assertThat(containerInfo.getResult().getStatus()).isEqualTo(KieContainerStatus.STARTED);
        assertThat(containerInfo.getResult().getReleaseId()).isEqualTo(releaseId);
    }

    @Test
    public void testGetContainer() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap());
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertThat(containerResponseEntity.getContainerName()).isEqualTo(CONTAINER_NAME);
    }

    @Test
    public void testGetAndUpdateContainer() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap());
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertThat(containerResponseEntity.getContainerName()).isEqualTo(CONTAINER_NAME);
        assertThat(containerResponseEntity.getReleasedId()).isEqualTo(releaseId);

        containerToDeploy.setReleasedId(releaseId101);
        controllerClient.updateContainerSpec(serverTemplate.getId(), containerToDeploy);

        containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        assertThat(containerResponseEntity).isNotNull();
        assertThat(containerResponseEntity.getId()).isEqualTo(CONTAINER_ID);
        assertThat(containerResponseEntity.getStatus()).isEqualTo(KieContainerStatus.STOPPED);
        assertThat(containerResponseEntity.getContainerName()).isEqualTo(CONTAINER_NAME);
        assertThat(containerResponseEntity.getReleasedId()).isEqualTo(releaseId101);
    }

    @Test
    public void testStartAndStopContainer() throws Exception {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap());
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);

        // Check that container is not deployed in kie server (as container is in STOPPED state).
        ServiceResponse<KieContainerResource> containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertThat(containerInfo.getType()).isEqualTo(ServiceResponse.ResponseType.FAILURE);
        KieServerAssert.assertResultContainsString(containerInfo.getMsg(), "Container " + CONTAINER_ID + " is not instantiated.");

        controllerClient.startContainer(containerToDeploy);

        containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STARTED);

        // Check that container is deployed in kie server.
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertThat(containerInfo.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        assertThat(containerInfo.getResult().getContainerId()).isEqualTo(CONTAINER_ID);
        assertThat(containerInfo.getResult().getStatus()).isEqualTo(KieContainerStatus.STARTED);
        assertThat(containerInfo.getResult().getReleaseId()).isEqualTo(releaseId);

        controllerClient.stopContainer(containerToDeploy);

        containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);

        // Check that container is not deployed in kie server (as container is in STOPPED state).
        KieServerSynchronization.waitForKieServerSynchronization(client, 0);
        containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertThat(containerInfo.getType()).isEqualTo(ServiceResponse.ResponseType.FAILURE);
        KieServerAssert.assertResultContainsString(containerInfo.getMsg(), "Container " + CONTAINER_ID + " is not instantiated.");
    }

    @Test
    public void testGetNotExistingContainer() {
        // Try to get not existing container using kie controller without created kie server instance.
        try {
            controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
            fail("Should throw exception about container info not found.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }

        createServerTemplate();
        // Try to get not existing container using kie controller with created kie server instance.
        try {
            controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
            fail("Should throw exception about container info not found.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }
    }

    @Test
    public void testListContainers() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap());
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        ContainerSpecList containerList = controllerClient.listContainerSpec(kieServerInfo.getServerId());

        assertThat(containerList).isNotNull();
        assertThat(containerList.getContainerSpecs().length).isEqualTo(1);

        ContainerSpec containerResponseEntity = containerList.getContainerSpecs()[0];
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertThat(containerResponseEntity.getContainerName()).isEqualTo(CONTAINER_NAME);
    }

    @Test
    public void testUpdateContainerConfig() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        Map<Capability, ContainerConfig> containerConfigMap = new HashMap();

        ProcessConfig processConfig = new ProcessConfig("PER_PROCESS_INSTANCE", "kieBase", "kieSession", "MERGE_COLLECTION");
        containerConfigMap.put(Capability.PROCESS, processConfig);

        RuleConfig ruleConfig = new RuleConfig(500l, KieScannerStatus.SCANNING);
        containerConfigMap.put(Capability.RULE, ruleConfig);

        ContainerSpec containerSpec = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, containerConfigMap);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerSpec);

        ContainerSpec containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);

        // Check process config and rule config
        checkContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, processConfig, ruleConfig);

        processConfig = new ProcessConfig("SINGLETON", "defaultKieBase", "defaultKieSession", "OVERRIDE_ALL");
        controllerClient.updateContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, Capability.PROCESS, processConfig);

        // Check process config and rule config
        checkContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, processConfig, ruleConfig);

        ruleConfig = new RuleConfig(1000l, KieScannerStatus.STOPPED);
        controllerClient.updateContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, Capability.RULE, ruleConfig);

        // Check process config and rule config
        checkContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, processConfig, ruleConfig);
    }

    @Test
    public void testCreateContainerOnNotExistingKieServerInstance() {
        // Try to create container using kie controller without created kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, new ServerTemplate(), releaseId, KieContainerStatus.STOPPED, new HashMap());
        try {

            controllerClient.saveContainerSpec(kieServerInfo.getServerId(), containerToDeploy);
            fail("Should throw exception about kie server instance not found.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }
    }

    @Test
    public void testCreateDuplicitContainer() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap());
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        try {
            // Try to create same container.
            controllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);
            fail("Should throw exception about container being created already.");
        } catch (KieServerControllerClientException e) {
            assertBadRequestException((T)e);
        }
    }

    @Test
    public void testDeleteNotExistingContainer() {
        // Try to dispose not existing container using kie controller without created kie server instance.
        try {
            controllerClient.deleteContainerSpec(kieServerInfo.getServerId(), CONTAINER_ID);
            fail("Should throw exception about kie server instance not exists.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }

        createServerTemplate();
        // Try to dispose not existing container using kie controller with created kie server instance.
        try {
            controllerClient.deleteContainerSpec(kieServerInfo.getServerId(), CONTAINER_ID);
            fail("Should throw exception about container not exists.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }
    }

    @Test
    public void testStartNotExistingContainer() throws Exception {
        ContainerSpec containerSpec = new ContainerSpec();
        containerSpec.setId(CONTAINER_ID);
        containerSpec.setServerTemplateKey(new ServerTemplateKey(kieServerInfo.getServerId(), null));
        // Try to start not existing container using kie controller without created kie server instance.
        try {
            controllerClient.startContainer(containerSpec);
            fail("Should throw exception about container not found.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }

        createServerTemplate();
        // Try to start not existing container using kie controller with created kie server instance.
        try {
            controllerClient.startContainer(containerSpec);
            fail("Should throw exception about container not found.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }
    }

    @Test
    public void testStopNotExistingContainer() throws Exception {
        ContainerSpec containerSpec = new ContainerSpec();
        containerSpec.setId(CONTAINER_ID);
        containerSpec.setServerTemplateKey(new ServerTemplateKey(kieServerInfo.getServerId(), null));
        // Try to stop not existing container using kie controller without created kie server instance.
        try {
            controllerClient.stopContainer(containerSpec);
            fail("Should throw exception about container not found.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }

        createServerTemplate();
        // Try to stop not existing container using kie controller with created kie server instance.
        try {
            controllerClient.stopContainer(containerSpec);
            fail("Should throw exception about container not found.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }
    }

    @Test
    public void testEmptyListContainers() {
        try {
            ContainerSpecList emptyList = controllerClient.listContainerSpec(kieServerInfo.getServerId());
            fail("Should throw exception about kie server instance not existing.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }

        // Create kie server instance connection in controller.
        createServerTemplate();

        ContainerSpecList emptyList = controllerClient.listContainerSpec(kieServerInfo.getServerId());
        KieServerAssert.assertNullOrEmpty("Active containers found!", emptyList.getContainerSpecs());
    }

    @Test
    public void testUpdateContainerConfigOnNotExistingContainer() {
        ProcessConfig config = new ProcessConfig("PER_PROCESS_INSTANCE", "kieBase", "kieSession", "MERGE_COLLECTION");
        try {
            controllerClient.updateContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, Capability.PROCESS, config);
            fail("Should throw exception about kie server instance not existing.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }

        // Create kie server instance connection in controller.
        createServerTemplate();

        try {
            controllerClient.updateContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, Capability.PROCESS, config);
            fail("Should throw exception about container info not found.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }
    }

    @Test
    public void testTemplateKeyChangeDuringUpdate() {
        ServerTemplate serverTemplate = createServerTemplate();

        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, Collections.EMPTY_MAP);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertThat(containerResponseEntity.getContainerName()).isEqualTo(CONTAINER_NAME);
        assertThat(containerResponseEntity.getReleasedId()).isEqualTo(releaseId);

        // setting new template to container
        ServerTemplate secondTemplate = createServerTemplate("st-id", "st-id", kieServerInfo.getLocation());
        containerToDeploy.setServerTemplateKey(secondTemplate);

        try {
            controllerClient.updateContainerSpec(serverTemplate.getId(), containerToDeploy);
            fail("Template key should not be allowed to be changed during update.");
        } catch (KieServerControllerClientException e) {
            assertBadRequestException((T)e);
            KieServerAssert.assertResultContainsString(e.getMessage(), "Cannot change container template key during update.");
        }

        assertThat(controllerClient.listServerTemplates().getServerTemplates().length).isEqualTo(2);
        // Check that on other server template is not any container.
        KieServerAssert.assertNullOrEmpty("Found container in second server template.", controllerClient.listContainerSpec(secondTemplate.getId()).getContainerSpecs());
        assertThat(controllerClient.listContainerSpec(serverTemplate.getId()).getContainerSpecs().length).isEqualTo(1);

        // Check that container is not changed
        containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertThat(containerResponseEntity.getServerTemplateKey().getId()).isEqualTo(serverTemplate.getId());
        assertThat(containerResponseEntity.getServerTemplateKey().getName()).isEqualTo(serverTemplate.getName());
        assertThat(containerResponseEntity.getContainerName()).isEqualTo(CONTAINER_NAME);
        assertThat(containerResponseEntity.getReleasedId()).isEqualTo(releaseId);
    }

    @Test
    public void testUpdateContainerWithDifferrentID() {
        ServerTemplate serverTemplate = createServerTemplate();
        final String ONE_ID = "one";
        final String TWO_ID = "two";

        // Deploy container for kie server instance.
        ContainerSpec containerOneToDeploy = new ContainerSpec(ONE_ID, ONE_ID, serverTemplate, releaseId, KieContainerStatus.STOPPED, Collections.EMPTY_MAP);
        ContainerSpec containerTwoToDeploy = new ContainerSpec(TWO_ID, TWO_ID, serverTemplate, releaseId, KieContainerStatus.STOPPED, Collections.EMPTY_MAP);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerOneToDeploy);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerTwoToDeploy);

        containerOneToDeploy.setReleasedId(releaseId101);

        try {
            controllerClient.updateContainerSpec(serverTemplate.getId(), containerTwoToDeploy.getId(), containerOneToDeploy);
            fail("Container one was updated from container two REST endpoint.");
        } catch (KieServerControllerClientException e) {
            assertBadRequestException((T)e);
            KieServerAssert.assertResultContainsString(e.getMessage(), "Cannot update container " + containerOneToDeploy.getId() + " on container " + containerTwoToDeploy.getId());
        }

        // Check container that are not changed
        ContainerSpec containerResponseEntity = controllerClient.getContainerInfo(serverTemplate.getId(), ONE_ID);
        assertThat(containerResponseEntity.getId()).isEqualTo(ONE_ID);
        assertThat(containerResponseEntity.getReleasedId()).isEqualTo(releaseId);
        assertThat(containerResponseEntity.getStatus()).isEqualTo(KieContainerStatus.STOPPED);

        containerResponseEntity = controllerClient.getContainerInfo(serverTemplate.getId(), TWO_ID);
        assertThat(containerResponseEntity.getId()).isEqualTo(TWO_ID);
        assertThat(containerResponseEntity.getReleasedId()).isEqualTo(releaseId);
        assertThat(containerResponseEntity.getStatus()).isEqualTo(KieContainerStatus.STOPPED);
    }

    @Test
    public void testCreateContainerByUpdateContainer() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, Collections.EMPTY_MAP);
        try {
            controllerClient.updateContainerSpec(serverTemplate.getId(), containerToDeploy);
            fail("Container was created by update command - REST Post method.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T)e);
        }

        KieServerAssert.assertNullOrEmpty("Found deployed container.", controllerClient.listContainerSpec(serverTemplate.getId()).getContainerSpecs());
    }

    @Test
    public void testUpdateContainerId() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, Collections.EMPTY_MAP);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertThat(containerResponseEntity.getContainerName()).isEqualTo(CONTAINER_NAME);

        containerToDeploy.setId("newID");
        try {
            controllerClient.updateContainerSpec(serverTemplate.getId(), CONTAINER_ID, containerToDeploy);
            fail("Container has updated id.");
        } catch (KieServerControllerClientException e) {
            assertBadRequestException((T)e);
            KieServerAssert.assertResultContainsString(e.getMessage(), "Cannot update container newID on container " + CONTAINER_ID);
        }
    }

    @Test
    public void testUpdateNotExistingContainerConfig() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        ContainerSpec containerSpec = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap());
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerSpec);

        ContainerSpec containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        KieServerAssert.assertNullOrEmpty("Config is not empty.", containerResponseEntity.getConfigs().values());

        // Try update not existing ProcessConfig
        ProcessConfig processConfig = new ProcessConfig("PER_PROCESS_INSTANCE", "kieBase", "kieSession", "MERGE_COLLECTION");
        controllerClient.updateContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, Capability.PROCESS, processConfig);


        // Try update not existing RuleConfig
        RuleConfig ruleConfig = new RuleConfig(500l, KieScannerStatus.SCANNING);
        controllerClient.updateContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, Capability.RULE, ruleConfig);
    }

    @Test
    public void testUpdateContainerNonValidReleaseId() throws Exception {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STARTED, Collections.EMPTY_MAP);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STARTED);
        assertThat(containerResponseEntity.getContainerName()).isEqualTo(CONTAINER_NAME);

        // Check that container is deployed in kie server.
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        ServiceResponse<KieContainerResource> containerInfoResponse = client.getContainerInfo(CONTAINER_ID);
        assertThat(containerInfoResponse.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        KieContainerResource containerResource = containerInfoResponse.getResult();
        assertThat(containerResource.getContainerId()).isEqualTo(CONTAINER_ID);
        assertThat(containerResource.getStatus()).isEqualTo(KieContainerStatus.STARTED);
        assertThat(containerResource.getReleaseId()).isEqualTo(releaseId);

        // Update container with non valid ReleaseId
        ReleaseId nonValidReleaseId = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "2.0.0-SNAPSHOT");
        containerToDeploy.setReleasedId(nonValidReleaseId);

        // We can update container to new version, but if can't be found, then deployed container is not changed.
        controllerClient.updateContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller. Container has changed ReleaseId
        containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        assertThat(containerResponseEntity.getId()).isEqualTo(CONTAINER_ID);
        assertThat(containerResponseEntity.getContainerName()).isEqualTo(CONTAINER_NAME);
        assertThat(containerResponseEntity.getStatus()).isEqualTo(KieContainerStatus.STARTED);
        assertThat(containerResponseEntity.getReleasedId()).isEqualTo(nonValidReleaseId);

        // Check deployed container
        String updateErrorMessage = "Error updating releaseId for container " + CONTAINER_ID + " to version " + nonValidReleaseId.toString();
        KieServerSynchronization.waitForKieServerMessage(client, CONTAINER_ID, updateErrorMessage);
        containerInfoResponse = client.getContainerInfo(CONTAINER_ID);
        assertThat(containerInfoResponse.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        containerResource = containerInfoResponse.getResult();

        // Check that Kie Container has message about error during updating
        // Kie Container store last message
        assertThat(containerResource.getMessages()).hasSize(1);
        Collection<String> messages = containerResource.getMessages().get(0).getMessages();
        assertThat(messages).hasSize(2);
        Assertions.assertThat(messages).contains(updateErrorMessage);

        assertThat(containerResource.getContainerId()).isEqualTo(CONTAINER_ID);
        assertThat(containerResource.getStatus()).isEqualTo(KieContainerStatus.STARTED);
        assertThat(containerResource.getReleaseId()).isEqualTo(releaseId);
    }

    @Test
    public void testStartContainerByUpdateKieContainerStatus() throws Exception {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, Collections.EMPTY_MAP);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertThat(containerResponseEntity.getContainerName()).isEqualTo(CONTAINER_NAME);

        // Check that container is not deployed in kie server (as container is in STOPPED state).
        ServiceResponse<KieContainerResource> containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertThat(containerInfo.getType()).isEqualTo(ServiceResponse.ResponseType.FAILURE);
        KieServerAssert.assertResultContainsString(containerInfo.getMsg(), "Container " + CONTAINER_ID + " is not instantiated.");

        // Update container
        containerToDeploy.setStatus(KieContainerStatus.STARTED);
        controllerClient.updateContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STARTED);

        // Check that container is deployed in kie server.
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertThat(containerInfo.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        assertThat(containerInfo.getResult().getContainerId()).isEqualTo(CONTAINER_ID);
        assertThat(containerInfo.getResult().getStatus()).isEqualTo(KieContainerStatus.STARTED);
        assertThat(containerInfo.getResult().getReleaseId()).isEqualTo(releaseId);
    }

    @Test
    public void testUpdateContainerWitoutContainerConfig() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Create container configMap
        Map<Capability, ContainerConfig> containerConfigMap = new HashMap();

        ProcessConfig processConfig = new ProcessConfig("PER_PROCESS_INSTANCE", "kieBase", "kieSession", "MERGE_COLLECTION");
        containerConfigMap.put(Capability.PROCESS, processConfig);
        RuleConfig ruleConfig = new RuleConfig(5000l, KieScannerStatus.SCANNING);
        containerConfigMap.put(Capability.RULE, ruleConfig);

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, containerConfigMap);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertThat(containerResponseEntity.getContainerName()).isEqualTo(CONTAINER_NAME);
        assertThat(containerResponseEntity.getReleasedId()).isEqualTo(releaseId);

        // Check process config and rule config.
        checkContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, processConfig, ruleConfig);

        // Update container
        containerToDeploy.setConfigs(Collections.EMPTY_MAP);
        controllerClient.updateContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        containerResponseEntity = controllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertThat(containerResponseEntity.getContainerName()).isEqualTo(CONTAINER_NAME);
        assertThat(containerResponseEntity.getReleasedId()).isEqualTo(releaseId);
        KieServerAssert.assertNullOrEmpty("Container configuration was found.", containerResponseEntity.getConfigs().keySet());
    }

    @Test
    public void testUpdateContainerConfigSent() throws Exception {
        // The usual setup of the kie-server along with a container spec
        ServerTemplate serverTemplate = createServerTemplate();
        Map<Capability, ContainerConfig> containerConfigMap = new HashMap<>();
        ProcessConfig processConfig = new ProcessConfig("PER_PROCESS_INSTANCE", "kieBase", "kieSession", "MERGE_COLLECTION");
        containerConfigMap.put(Capability.PROCESS, processConfig);
        RuleConfig ruleConfig = new RuleConfig(500L, KieScannerStatus.STARTED);
        containerConfigMap.put(Capability.RULE, ruleConfig);
        ContainerSpec containerSpec = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STARTED, containerConfigMap);

        // Tell the controller to save the spec for the given template, which since the
        // container status is STARTED should also cause it to be deployed to the kie-server
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerSpec);
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        checkContainerConfigAgainstServer(processConfig,ruleConfig);

        // Update the rule configuration, turning off the scanner
        ruleConfig.setScannerStatus(KieScannerStatus.STOPPED);
        controllerClient.updateContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, Capability.RULE, ruleConfig);

        // Check the rule configuration
        KieServerSynchronization.waitForKieServerScannerStatus(client, CONTAINER_ID, KieScannerStatus.STOPPED);
        checkContainerConfigAgainstServer(ruleConfig);

        // Update the configuration
        processConfig = new ProcessConfig("SINGLETON", "defaultKieBase", "defaultKieSession", "OVERRIDE_ALL");
        controllerClient.updateContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, Capability.PROCESS, processConfig);

        // Reset the container, since the update process should not do that by itself
        controllerClient.stopContainer(containerSpec);
        controllerClient.startContainer(containerSpec);

        // Update the process configuration
        KieServerSynchronization.waitForKieServerConfig(client, CONTAINER_ID, KieServerConstants.PCFG_MERGE_MODE, "OVERRIDE_ALL");
        checkContainerConfigAgainstServer(processConfig);

        // Restart the scanner with the new interval
        ruleConfig.setScannerStatus(KieScannerStatus.STARTED);
        ruleConfig.setPollInterval(1000L);
        controllerClient.updateContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, Capability.RULE, ruleConfig);
        KieServerSynchronization.waitForKieServerScannerStatus(client, CONTAINER_ID, KieScannerStatus.STARTED, 1000L);
        checkContainerConfigAgainstServer(ruleConfig,processConfig);
    }

    @Test
    public void testDeleteContainerStopsContainer() throws Exception {
        ServerTemplate serverTemplate = createServerTemplate();
        Map<Capability, ContainerConfig> containerConfigMap = new HashMap<>();

        ContainerSpec containerSpec = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STARTED, containerConfigMap);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerSpec);
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);

        ServiceResponse<KieServerStateInfo> response = client.getServerState();
        KieServerAssert.assertSuccess(response);

        KieServerStateInfo serverState = response.getResult();
        assertThat(serverState).isNotNull();
        assertThat(serverState.getContainers() != null && serverState.getContainers().size() > 0).as("Expected to find containers, but none were found").isTrue();

        controllerClient.deleteContainerSpec(serverTemplate.getId(), CONTAINER_ID);
        KieServerSynchronization.waitForKieServerSynchronization(client, 0);

        response = client.getServerState();
        serverState = response.getResult();
        assertThat(serverState).isNotNull();
        assertThat(serverState.getContainers() != null && serverState.getContainers().size() > 0).as("Did not expect to find containers").isFalse();
    }

    protected void checkContainerConfigAgainstServer(ContainerConfig...configs) {
        ServiceResponse<KieContainerResource> containerResource = client.getContainerInfo(CONTAINER_ID);
        KieServerAssert.assertSuccess(containerResource);

        KieContainerResource kcr = containerResource.getResult();
        assertThat(kcr).isNotNull();
        for (ContainerConfig config: configs) {
            if (config instanceof ProcessConfig) {
                ProcessConfig pc = (ProcessConfig)config;
                Map<String, String> configMap = new HashMap<>();
                configMap.put("KBase", pc.getKBase());
                configMap.put("KSession", pc.getKSession());
                configMap.put("MergeMode", pc.getMergeMode());
                configMap.put("RuntimeStrategy", pc.getRuntimeStrategy());

                assertThat(kcr.getConfigItems()).as("No configuration items found for checking process configuration").isNotNull();
                List<KieServerConfigItem> kci = kcr.getConfigItems();
                for (KieServerConfigItem item: kci) {
                    String name = item.getName();
                    String value = item.getValue();
                    assertThat(value).isEqualTo(configMap.get(name));
                }
            } else if (config instanceof RuleConfig) {
                RuleConfig rc = (RuleConfig)config;
                KieScannerResource scanner = kcr.getScanner();
                assertThat(scanner).as("No scanner resource found").isNotNull();
                assertThat(scanner.getStatus()).isEqualTo(rc.getScannerStatus());
                // Only test the polling interval when starting the scanner
                // since it could be wrong at any other time
                if (rc.getScannerStatus() == KieScannerStatus.STARTED) {
                    assertThat(scanner.getPollInterval()).isEqualTo(rc.getPollInterval());
                }
            }
        }
    }
}
