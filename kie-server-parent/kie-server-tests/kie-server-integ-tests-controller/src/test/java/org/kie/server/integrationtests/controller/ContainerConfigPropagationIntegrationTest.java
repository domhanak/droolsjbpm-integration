/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.KieScannerStatus;
import org.kie.server.api.model.KieServerConfigItem;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.controller.api.ModelFactory;
import org.kie.server.controller.api.model.spec.Capability;
import org.kie.server.controller.api.model.spec.ContainerConfig;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.controller.api.model.spec.ProcessConfig;
import org.kie.server.controller.api.model.spec.ServerTemplate;
import org.kie.server.controller.impl.storage.InMemoryKieServerTemplateStorage;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.kie.server.integrationtests.shared.KieServerSynchronization;

import static org.hamcrest.core.Is.is;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

public class ContainerConfigPropagationIntegrationTest extends KieControllerManagementBaseTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "1.0.0");
    private static ReleaseId releaseId101 = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "1.0.1");
    private static ReleaseId releaseIdLatest = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "LATEST");

    private static final String CONTAINER_ID = "kie-concurrent";
    private static final String CONTAINER_NAME = "containerName";

    private KieServerInfo kieServerInfo;

    @BeforeClass
    public static void initialize() throws Exception {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/stateless-session-kjar").getFile());
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/stateless-session-kjar101").getFile());
    }

    @Before
    public void getKieServerInfo() {
        InMemoryKieServerTemplateStorage.getInstance().clear();
        // Getting info from currently started kie server.
        ServiceResponse<KieServerInfo> reply = client.getServerInfo();
        assumeThat(reply.getType(), is(ServiceResponse.ResponseType.SUCCESS));
        kieServerInfo = reply.getResult();
    }

    @Test
    public void testPropagateProcessContainerConfig() throws Exception {
        ServerTemplate serverTemplate = createServerTemplate();

        Map<Capability, ContainerConfig> containerConfigMap = new HashMap<>();

        ProcessConfig processConfig = new ProcessConfig("PER_PROCESS_INSTANCE", "kieBase", "kieSession", "MERGE_COLLECTION");
        containerConfigMap.put(Capability.PROCESS, processConfig);

        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STARTED, containerConfigMap);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        KieServerSynchronization.waitForKieServerSynchronization(client, 1);

        ServiceResponse<KieContainerResource> containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertThat(containerInfo.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        assertThat(containerInfo.getResult().getContainerId()).isEqualTo(CONTAINER_ID);
        assertThat(containerInfo.getResult().getStatus()).isEqualTo(KieContainerStatus.STARTED);

        assertThat(containerInfo.getResult().getConfigItems()).hasSize(4);
        assertThat(containsConfigItem(containerInfo.getResult(), KieServerConstants.PCFG_RUNTIME_STRATEGY, "PER_PROCESS_INSTANCE", KieServerConstants.CAPABILITY_BPM)).isTrue();
        assertThat(containsConfigItem(containerInfo.getResult(), KieServerConstants.PCFG_KIE_BASE, "kieBase", KieServerConstants.CAPABILITY_BPM)).isTrue();
        assertThat(containsConfigItem(containerInfo.getResult(), KieServerConstants.PCFG_KIE_SESSION, "kieSession", KieServerConstants.CAPABILITY_BPM)).isTrue();
        assertThat(containsConfigItem(containerInfo.getResult(), KieServerConstants.PCFG_MERGE_MODE, "MERGE_COLLECTION", KieServerConstants.CAPABILITY_BPM)).isTrue();
    }

    private ServerTemplate createServerTemplate() {
        ServerTemplate serverTemplate = new ServerTemplate();
        serverTemplate.setId(kieServerInfo.getServerId());
        serverTemplate.setName(kieServerInfo.getName());

        serverTemplate.addServerInstance(ModelFactory.newServerInstanceKey(serverTemplate.getId(), kieServerInfo.getLocation()));
        controllerClient.saveServerTemplate(serverTemplate);

        return serverTemplate;
    }

    private boolean containsConfigItem(KieContainerResource containerResource, String itemName, String itemValue, String itemType) {
        Predicate<KieServerConfigItem> matchItem = (n -> n.getName().equals(itemName) && n.getValue().equals(itemValue) && n.getType().equals(itemType));
        return containerResource.getConfigItems().stream().anyMatch(matchItem);
    }

    private void assertContainerInfoWithScanner(ReleaseId releaseId, Long pollInterval) {
        ServiceResponse<KieContainerResource> containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertThat(containerInfo.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        assertThat(containerInfo.getResult().getContainerId()).isEqualTo(CONTAINER_ID);
        assertThat(containerInfo.getResult().getStatus()).isEqualTo(KieContainerStatus.STARTED);
        assertThat(containerInfo.getResult().getResolvedReleaseId()).isEqualTo(releaseId);

        assertThat(containerInfo.getResult().getScanner().getPollInterval()).isEqualTo(pollInterval);
        assertThat(containerInfo.getResult().getScanner().getStatus()).isEqualTo(KieScannerStatus.STARTED);
    }
}
