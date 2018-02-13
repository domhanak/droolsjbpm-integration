/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.integrationtests.controller;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.controller.api.ModelFactory;
import org.kie.server.controller.api.model.spec.Capability;
import org.kie.server.controller.api.model.spec.ContainerConfig;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.controller.api.model.spec.ServerTemplate;
import org.kie.server.controller.api.model.spec.ServerTemplateList;
import org.kie.server.integrationtests.config.TestConfig;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.kie.server.integrationtests.shared.KieServerExecutor;
import org.kie.server.integrationtests.shared.KieServerSynchronization;

public class KieControllerStartupIntegrationTest extends KieControllerManagementBaseTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "1.0.0");

    private static final String CONTAINER_ID = "kie-concurrent";

    @Override
    protected KieServicesClient createDefaultClient() {
        // For these tests we use embedded kie server as we need to control turning server off and on.
        KieServicesConfiguration config = KieServicesFactory.newRestConfiguration(TestConfig.getEmbeddedKieServerHttpUrl(), null, null);
        config.setMarshallingFormat(marshallingFormat);
        return KieServicesFactory.newKieServicesClient(config);
    }

    @BeforeClass
    public static void initialize() throws Exception {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/stateless-session-kjar").getFile());
    }

    @Before
    @Override
    public void setup() throws Exception {
        // Start embedded kie server to be correctly initialized and cleaned before tests.
        if (!TestConfig.isLocalServer()) {
            server = new KieServerExecutor();
            server.startKieServer();
        }
        super.setup();
    }

    @After
    public void cleanupEmbeddedKieServer() {
        // Turn off embedded kie server if running in container, turn on if running local tests.
        try {
            if (TestConfig.isLocalServer()) {
                server.startKieServer();
            } else {
                server.stopKieServer();
            }
        } catch (Exception e) {
            // Exception thrown if there is already kie server started or stopped respectively.
            // Don't need to do anything in such case.
        }
    }

    @Test
    public void testRegisterKieServerAfterStartup() {
        // Turn off embedded kie server.
        server.stopKieServer();

        // Check that there are no kie servers deployed in controller.
        ServerTemplateList instanceList = controllerClient.listServerTemplates();
        assertThat(instanceList).isNotNull();
        KieServerAssert.assertNullOrEmpty("Active kie server instance found!", instanceList.getServerTemplates());

        // Turn on new kie server.
        server.startKieServer();

        // Check that kie server is registered in controller.
        instanceList = controllerClient.listServerTemplates();
        assertThat(instanceList).isNotNull();
        assertThat(instanceList.getServerTemplates().length).isEqualTo(1);

        // Getting info from currently started kie server.
        ServiceResponse<KieServerInfo> reply = client.getServerInfo();
        assertThat(reply).isNotNull();
        assertThat(reply.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        assertThat(reply.getResult()).isNotNull();

        ServerTemplate deployedServerInstance = instanceList.getServerTemplates()[0];
        assertThat(deployedServerInstance).isNotNull();
        assertThat(deployedServerInstance.getId()).isEqualTo(reply.getResult().getServerId());
    }

    @Test
    public void testTurnOffKieServerAfterShutdown() {
        // Register kie server in controller.
        ServiceResponse<KieServerInfo> kieServerInfo = client.getServerInfo();
        assertThat(kieServerInfo.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        assertThat(kieServerInfo.getResult()).isNotNull();

        ServerTemplate serverTemplate = new ServerTemplate(kieServerInfo.getResult().getServerId(), kieServerInfo.getResult().getName());
        controllerClient.saveServerTemplate(serverTemplate);

        // Check that kie server is registered.
        ServerTemplateList instanceList = controllerClient.listServerTemplates();
        assertThat(instanceList.getServerTemplates().length).isEqualTo(1);
        assertThat(instanceList.getServerTemplates()[0].getId()).isEqualTo(kieServerInfo.getResult().getServerId()); //maybe change to avoid next -> null

        // Turn off embedded kie server.
        server.stopKieServer();

        // Check that kie server is down in controller.
        instanceList = controllerClient.listServerTemplates();
        assertThat(instanceList.getServerTemplates().length).isEqualTo(1);
        assertThat(instanceList.getServerTemplates()[0].getId()).isEqualTo(kieServerInfo.getResult().getServerId()); //maybe change to avoid next -> null
    }

    @Test
    public void testContainerCreatedAfterStartup() throws Exception {
        // Getting info from currently started kie server.
        ServiceResponse<KieServerInfo> kieServerInfo = client.getServerInfo();
        assertThat(kieServerInfo.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        assertThat(kieServerInfo.getResult()).isNotNull();

        // Check that there are no containers in kie server.
        ServiceResponse<KieContainerResourceList> containersList = client.listContainers();
        assertThat(containersList.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        KieServerAssert.assertNullOrEmpty("Active containers found!", containersList.getResult().getContainers());

        // Check that there are no kie servers deployed in controller.
        ServerTemplateList instanceList = controllerClient.listServerTemplates();
        KieServerAssert.assertNullOrEmpty("Active kie server instance found!", instanceList.getServerTemplates());

        // Turn kie server off, add embedded kie server to controller, create container and start kie server again.
        server.stopKieServer();

        ServerTemplate serverTemplate = new ServerTemplate(kieServerInfo.getResult().getServerId(), kieServerInfo.getResult().getName());
        controllerClient.saveServerTemplate(serverTemplate);

        ContainerSpec containerSpec = new ContainerSpec(CONTAINER_ID, CONTAINER_ID, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap<Capability, ContainerConfig>());
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerSpec);
        ContainerSpec deployedContainer = controllerClient.getContainerInfo(kieServerInfo.getResult().getServerId(), CONTAINER_ID);

        assertThat(deployedContainer).isNotNull();
        assertThat(deployedContainer.getId()).isEqualTo(CONTAINER_ID);
        assertThat(deployedContainer.getReleasedId()).isEqualTo(releaseId);
        assertThat(deployedContainer.getStatus()).isEqualTo(KieContainerStatus.STOPPED);

        controllerClient.startContainer(containerSpec);

        server.startKieServer();

        // Check that container is deployed on kie server.
        ServiceResponse<KieContainerResource> containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertThat(containerInfo.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        assertThat(containerInfo.getResult()).isNotNull();
        assertThat(containerInfo.getResult().getContainerId()).isEqualTo(CONTAINER_ID);
        assertThat(containerInfo.getResult().getStatus()).isEqualTo(KieContainerStatus.STARTED);
    }

    @Test
    public void testContainerDisposedAfterStartup() throws Exception {
        // Getting info from currently started kie server.
        ServiceResponse<KieServerInfo> kieServerInfo = client.getServerInfo();
        assertThat(kieServerInfo.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        assertThat(kieServerInfo.getResult()).isNotNull();

        // Create container.
        ServerTemplate serverTemplate = new ServerTemplate(kieServerInfo.getResult().getServerId(), kieServerInfo.getResult().getName());
        serverTemplate.addServerInstance(ModelFactory.newServerInstanceKey(serverTemplate.getId(), kieServerInfo.getResult().getLocation()));
        controllerClient.saveServerTemplate(serverTemplate);
        ContainerSpec containerSpec = new ContainerSpec(CONTAINER_ID, CONTAINER_ID, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap<Capability, ContainerConfig>());
        controllerClient.saveContainerSpec(kieServerInfo.getResult().getServerId(), containerSpec);
        controllerClient.startContainer(containerSpec);

        // Check that there is one container deployed.
        try {
            KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        } catch (TimeoutException e) {
            // Sometimes creating container fails in embedded server (unknown Socket timeout error, tends to happen here).
            // Retrigger container creation. These tests should be refactored to use more reliable container instead of embedded TJWSEmbeddedJaxrsServer.
            controllerClient.startContainer(containerSpec);
            KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        }
        ServiceResponse<KieContainerResourceList> containersList = client.listContainers();
        assertThat(containersList.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        assertThat(containersList.getResult().getContainers()).isNotNull();
        assertThat(containersList.getResult().getContainers()).hasSize(1);

        ServerTemplateList instanceList = controllerClient.listServerTemplates();
        assertThat(instanceList.getServerTemplates().length).isEqualTo(1);

        // Turn kie server off, dispose container and start kie server again.
        server.stopKieServer();

        controllerClient.stopContainer(containerSpec);
        controllerClient.deleteContainerSpec(serverTemplate.getId(), CONTAINER_ID);

        server.startKieServer();

        // Check that no container is deployed on kie server.
        containersList = client.listContainers();
        assertThat(containersList.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        KieServerAssert.assertNullOrEmpty("Active containers found!", containersList.getResult().getContainers());
    }
}
