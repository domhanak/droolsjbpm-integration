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

package org.kie.server.integrationtests.common;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.server.api.KieServerEnvironment;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieScannerResource;
import org.kie.server.api.model.KieScannerStatus;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.KieServiceResponse.ResponseType;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.kie.server.integrationtests.shared.basetests.RestJmsSharedBaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class KieServerIntegrationTest extends RestJmsSharedBaseIntegrationTest {
    private static ReleaseId releaseId1 = new ReleaseId("foo.bar", "baz", "2.1.0.GA");

    private static final String CONTAINER_ID = "kie1";

    @BeforeClass
    public static void initialize() throws Exception {
        KieServerDeployer.createAndDeployKJar(releaseId1);
    }

    @Test
    public void testGetServerInfo() throws Exception {
        ServiceResponse<KieServerInfo> reply = client.getServerInfo();
        assertThat(reply.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);

        KieServerInfo info = reply.getResult();
        assertThat(info.getVersion()).isEqualTo(getServerVersion());

        // Kie server has all extensions disabled, available just default capability.
        assertThat(info.getCapabilities()).hasSize(2);
        assertThat(info.getCapabilities().get(0)).isEqualTo("KieServer");
        assertThat(info.getCapabilities().get(1)).isEqualTo("Swagger");
    }

    private String getServerVersion() {
        // use the property if specified and fallback to KieServerEnvironment if no property set
        return System.getProperty("kie.server.version", KieServerEnvironment.getVersion().toString());
    }

    @Before
    public void setupKieServer() {
        disposeAllContainers();
        createContainer(CONTAINER_ID, releaseId1);
    }

    @Test
    public void testScanner() throws Exception {
        ServiceResponse<KieScannerResource> si = client.getScannerInfo(CONTAINER_ID);
        Assert.assertThat(si.getType() ).isEqualTo(ResponseType.SUCCESS);
        KieScannerResource info = si.getResult();
        Assert.assertThat(info.getStatus() ).isEqualTo(KieScannerStatus.DISPOSED);
        
        si = client.updateScanner(CONTAINER_ID, new KieScannerResource(KieScannerStatus.STARTED, 10000L));
        Assert.assertThat(ResponseType.SUCCESS).isCloseTo(si.getMsg(), within(si.getType() ));
        info = si.getResult();
        Assert.assertThat(info.getStatus() ).isEqualTo(KieScannerStatus.STARTED);
        
        si = client.getScannerInfo(CONTAINER_ID);
        Assert.assertThat(ResponseType.SUCCESS).isCloseTo(si.getMsg(), within(si.getType() ));
        info = si.getResult();
        Assert.assertThat(info.getStatus() ).isEqualTo(KieScannerStatus.STARTED);
        
        si = client.updateScanner(CONTAINER_ID, new KieScannerResource(KieScannerStatus.STOPPED, 10000L));
        Assert.assertThat(ResponseType.SUCCESS).isCloseTo(si.getMsg(), within(si.getType() ));
        info = si.getResult();
        Assert.assertThat(info.getStatus() ).isEqualTo(KieScannerStatus.STOPPED);
        
        si = client.getScannerInfo(CONTAINER_ID);
        Assert.assertThat(ResponseType.SUCCESS).isCloseTo(si.getMsg(), within(si.getType() ));
        info = si.getResult();
        Assert.assertThat(info.getStatus() ).isEqualTo(KieScannerStatus.STOPPED);
        
        si = client.updateScanner(CONTAINER_ID, new KieScannerResource(KieScannerStatus.DISPOSED, 10000L));
        Assert.assertThat(ResponseType.SUCCESS).isCloseTo(si.getMsg(), within(si.getType() ));
        info = si.getResult();
        Assert.assertThat(info.getStatus() ).isEqualTo(KieScannerStatus.DISPOSED);
        
        si = client.getScannerInfo(CONTAINER_ID);
        Assert.assertThat(ResponseType.SUCCESS).isCloseTo(si.getMsg(), within(si.getType() ));
        info = si.getResult();
        Assert.assertThat(info.getStatus() ).isEqualTo(KieScannerStatus.DISPOSED);
    }

    @Test
    public void testScannerScanNow() throws Exception {
        ServiceResponse<KieScannerResource> si = client.getScannerInfo(CONTAINER_ID);
        Assert.assertThat(si.getType() ).isEqualTo(ResponseType.SUCCESS);
        KieScannerResource info = si.getResult();
        Assert.assertThat(info.getStatus() ).isEqualTo(KieScannerStatus.DISPOSED);

        si = client.updateScanner(CONTAINER_ID, new KieScannerResource(KieScannerStatus.SCANNING, 0L));
        Assert.assertThat(ResponseType.SUCCESS).isCloseTo(si.getMsg(), within(si.getType() ));
        info = si.getResult();
        Assert.assertThat(info.getStatus() ).isEqualTo(KieScannerStatus.STOPPED);

        si = client.getScannerInfo(CONTAINER_ID);
        Assert.assertThat(ResponseType.SUCCESS).isCloseTo(si.getMsg(), within(si.getType() ));
        info = si.getResult();
        Assert.assertThat(info.getStatus() ).isEqualTo(KieScannerStatus.STOPPED);

        si = client.updateScanner(CONTAINER_ID, new KieScannerResource(KieScannerStatus.DISPOSED, 10000L));
        Assert.assertThat(ResponseType.SUCCESS).isCloseTo(si.getMsg(), within(si.getType() ));
        info = si.getResult();
        Assert.assertThat(info.getStatus() ).isEqualTo(KieScannerStatus.DISPOSED);

        si = client.getScannerInfo(CONTAINER_ID);
        Assert.assertThat(ResponseType.SUCCESS).isCloseTo(si.getMsg(), within(si.getType() ));
        info = si.getResult();
        Assert.assertThat(info.getStatus() ).isEqualTo(KieScannerStatus.DISPOSED);
    }

    @Test
    public void testScannerStatusOnContainerInfo() throws Exception {
        ServiceResponse<KieContainerResource> reply = client.getContainerInfo(CONTAINER_ID);
        Assert.assertThat(reply.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);

        KieContainerResource kci = reply.getResult();
        Assert.assertThat(kci.getScanner().getStatus() ).isEqualTo(KieScannerStatus.DISPOSED);

        ServiceResponse<KieScannerResource> si = client.updateScanner(CONTAINER_ID, new KieScannerResource(KieScannerStatus.STARTED, 10000L));
        Assert.assertThat(ResponseType.SUCCESS).isCloseTo(si.getMsg(), within(si.getType() ));
        KieScannerResource info = si.getResult();
        Assert.assertThat(info.getStatus() ).isEqualTo(KieScannerStatus.STARTED);

        kci = client.getContainerInfo( CONTAINER_ID ).getResult();
        Assert.assertThat(kci.getScanner().getStatus() ).isEqualTo(KieScannerStatus.STARTED);
        Assert.assertThat(kci.getScanner().getPollInterval().longValue() ).isEqualTo(10000);

        si = client.updateScanner(CONTAINER_ID, new KieScannerResource(KieScannerStatus.STOPPED, 10000L));
        Assert.assertThat(ResponseType.SUCCESS).isCloseTo(si.getMsg(), within(si.getType() ));
        info = si.getResult();
        Assert.assertThat(info.getStatus() ).isEqualTo(KieScannerStatus.STOPPED);

        kci = client.getContainerInfo( CONTAINER_ID ).getResult();
        Assert.assertThat(kci.getScanner().getStatus() ).isEqualTo(KieScannerStatus.STOPPED);

        si = client.updateScanner(CONTAINER_ID, new KieScannerResource(KieScannerStatus.DISPOSED, 10000L));
        Assert.assertThat(ResponseType.SUCCESS).isCloseTo(si.getMsg(), within(si.getType() ));
        info = si.getResult();
        Assert.assertThat(info.getStatus() ).isEqualTo(KieScannerStatus.DISPOSED);

        kci = client.getContainerInfo( CONTAINER_ID ).getResult();
        Assert.assertThat(kci.getScanner().getStatus() ).isEqualTo(KieScannerStatus.DISPOSED);
    }

    @Test
    public void testConversationIdHandling() throws Exception {
        client.getContainerInfo(CONTAINER_ID);
        String conversationId = client.getConversationId();
        assertThat(conversationId).isNotNull();

        client.getContainerInfo(CONTAINER_ID);
        String afterNextCallConversationId = client.getConversationId();
        assertThat(afterNextCallConversationId).isEqualTo(conversationId);

        // complete conversation to start with new one
        client.completeConversation();

        client.getContainerInfo(CONTAINER_ID);
        afterNextCallConversationId = client.getConversationId();
        assertNotEquals(conversationId, afterNextCallConversationId);
    }
}
