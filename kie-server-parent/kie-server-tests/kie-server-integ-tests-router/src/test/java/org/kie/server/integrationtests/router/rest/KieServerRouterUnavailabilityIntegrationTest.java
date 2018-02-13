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

package org.kie.server.integrationtests.router.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.stream.Stream;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.kie.server.integrationtests.router.DBExternalResource;
import org.kie.server.integrationtests.router.client.KieServerRouterClient;
import org.kie.server.integrationtests.shared.basetests.RestOnlyBaseIntegrationTest;
import org.kie.server.router.Configuration;
import org.kie.server.router.ContainerInfo;
import org.kie.server.router.KieServerRouter;
import org.kie.server.router.KieServerRouterConstants;
import org.kie.server.router.repository.FileRepository;

public class KieServerRouterUnavailabilityIntegrationTest extends RestOnlyBaseIntegrationTest {

    @ClassRule
    public static ExternalResource StaticResource = new DBExternalResource();

    private static KieServerRouter router;
    private static File repository;

    private static String serverUrl;

    @Before
    public void startStandaloneRouter(){

        // setup repository for config of router
        repository = new File("target/unavailability-router-repo");
        repository.mkdirs();
        System.setProperty(KieServerRouterConstants.ROUTER_REPOSITORY_DIR, repository.getAbsolutePath());

        Configuration config = new Configuration();

        config.addContainerHost("container1", "http://localhost:56561");
        config.addContainerHost("container2", "http://localhost:56562");
        config.addContainerHost("container3", "http://invalid-host:8080");

        config.addServerHost("server1", "http://localhost:56561");
        config.addServerHost("server2", "http://localhost:56562");
        config.addServerHost("server3", "http://invalid-host:8080");

        ContainerInfo containerInfo = new ContainerInfo("container1", "test", "org.kie:test:1.0");
        config.addContainerInfo(containerInfo);

        ContainerInfo containerInfo2 = new ContainerInfo("container2", "test2", "org.kie:test:2.0");
        config.addContainerInfo(containerInfo2);

        ContainerInfo containerInfo3 = new ContainerInfo("container3", "test3", "org.kie:test:3.0");
        config.addContainerInfo(containerInfo3);

        FileRepository repo = new FileRepository(repository);
        repo.persist(config);

        // setup and start router
        Integer port = allocatePort();
        router = new KieServerRouter();
        router.start("localhost", port);

        serverUrl = "http://localhost:" + port;
    }

    @After
    public void stopStandaloneRouter(){
        // stop router and remove its config
        System.clearProperty(KieServerRouterConstants.ROUTER_REPOSITORY_DIR);
        router.stop(true);
        router = null;

        Stream.of(repository.listFiles()).forEach(f-> f.delete());
        repository.delete();
    }

    @Test
    public void testReactToUnavailableServersInRouterContainerBasedOperations() throws Exception {

        Response response = null;
        try (KieServerRouterClient routerClient = new KieServerRouterClient(serverUrl)) {
            Configuration initialConfig = routerClient.getRouterConfig();
            assertThat(initialConfig.getHostsPerContainer()).hasSize(3);
            assertThat(initialConfig.getHostsPerServer()).hasSize(3);

            assertThat(initialConfig.getHostsPerContainer().get("container1")).hasSize(1);
            assertThat(initialConfig.getHostsPerContainer().get("container2")).hasSize(1);
            assertThat(initialConfig.getHostsPerContainer().get("container3")).hasSize(1);
            assertThat(initialConfig.getHostsPerServer().get("server1")).hasSize(1);
            assertThat(initialConfig.getHostsPerServer().get("server2")).hasSize(1);
            assertThat(initialConfig.getHostsPerServer().get("server3")).hasSize(1);

            WebTarget clientRequest = newRequest(serverUrl + "/containers/container1/instances");
            logger.debug("[GET] " + clientRequest.getUri());

            response = clientRequest.request(getMediaType()).get();
            Assert.assertThat(response.getStatus()).isEqualTo(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
            response.close();

            Configuration config = routerClient.getRouterConfig();

            assertThat(config.getHostsPerContainer()).hasSize(3);
            assertThat(config.getHostsPerServer()).hasSize(3);

            assertThat(config.getHostsPerContainer().get("container1")).isEmpty();
            assertThat(config.getHostsPerContainer().get("container2")).hasSize(1);
            assertThat(config.getHostsPerContainer().get("container3")).hasSize(1);
            assertThat(config.getHostsPerServer().get("server1")).isEmpty();
            assertThat(config.getHostsPerServer().get("server2")).hasSize(1);
            assertThat(config.getHostsPerServer().get("server3")).hasSize(1);
        } finally {
            if(response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testReactToUnavailableServersInRouterQueryBasedOperations() throws Exception {

        Response response = null;
        try (KieServerRouterClient routerClient = new KieServerRouterClient(serverUrl)) {
            Configuration initialConfig = routerClient.getRouterConfig();
            assertThat(initialConfig.getHostsPerContainer()).hasSize(3);
            assertThat(initialConfig.getHostsPerServer()).hasSize(3);

            assertThat(initialConfig.getHostsPerContainer().get("container1")).hasSize(1);
            assertThat(initialConfig.getHostsPerContainer().get("container2")).hasSize(1);
            assertThat(initialConfig.getHostsPerContainer().get("container3")).hasSize(1);
            assertThat(initialConfig.getHostsPerServer().get("server1")).hasSize(1);
            assertThat(initialConfig.getHostsPerServer().get("server2")).hasSize(1);
            assertThat(initialConfig.getHostsPerServer().get("server3")).hasSize(1);

            WebTarget clientRequest = newRequest(serverUrl + "/containers");
            logger.debug( "[GET] " + clientRequest.getUri());

            response = clientRequest.request(getMediaType()).get();
            Assert.assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
            response.close();

            // since there are no servers connected and query operations are broadcasts all should be cleared
            Configuration config = routerClient.getRouterConfig();
            assertThat(config.getHostsPerContainer()).hasSize(3);
            assertThat(config.getHostsPerServer()).hasSize(3);

            assertThat(config.getHostsPerContainer().get("container1")).isEmpty();
            assertThat(config.getHostsPerContainer().get("container2")).isEmpty();
            assertThat(config.getHostsPerContainer().get("container3")).isEmpty();
            assertThat(config.getHostsPerServer().get("server1")).isEmpty();
            assertThat(config.getHostsPerServer().get("server2")).isEmpty();
            assertThat(config.getHostsPerServer().get("server3")).isEmpty();

        } finally {
            if(response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testReactToUnavailableServersInRouterContainerBasedOperationsInvalidHost() throws Exception {

        Response response = null;
        try (KieServerRouterClient routerClient = new KieServerRouterClient(serverUrl)) {
            Configuration initialConfig = routerClient.getRouterConfig();
            assertThat(initialConfig.getHostsPerContainer()).hasSize(3);
            assertThat(initialConfig.getHostsPerServer()).hasSize(3);

            assertThat(initialConfig.getHostsPerContainer().get("container1")).hasSize(1);
            assertThat(initialConfig.getHostsPerContainer().get("container2")).hasSize(1);
            assertThat(initialConfig.getHostsPerContainer().get("container3")).hasSize(1);
            assertThat(initialConfig.getHostsPerServer().get("server1")).hasSize(1);
            assertThat(initialConfig.getHostsPerServer().get("server2")).hasSize(1);
            assertThat(initialConfig.getHostsPerServer().get("server3")).hasSize(1);

            WebTarget clientRequest = newRequest(serverUrl + "/containers/container3/instances");
            logger.debug("[GET] " + clientRequest.getUri());
            try {
                response = clientRequest.request(getMediaType()).get();
                fail("Should fail as this is an invalid host");
            } catch (Exception e) {
                // expected
            } finally {
                if(response != null) {
                    response.close();
                }
            }

            Configuration config = routerClient.getRouterConfig();

            assertThat(config.getHostsPerContainer()).hasSize(3);
            assertThat(config.getHostsPerServer()).hasSize(3);

            assertThat(config.getHostsPerContainer().get("container1")).hasSize(1);
            assertThat(config.getHostsPerContainer().get("container2")).hasSize(1);
            assertThat(config.getHostsPerContainer().get("container3")).isEmpty();
            assertThat(config.getHostsPerServer().get("server1")).hasSize(1);
            assertThat(config.getHostsPerServer().get("server2")).hasSize(1);
            assertThat(config.getHostsPerServer().get("server3")).isEmpty();
        } finally {
            if(response != null) {
                response.close();
            }
        }
    }

    private static int allocatePort() {
        try {
            ServerSocket server = new ServerSocket(0);
            int port = server.getLocalPort();
            server.close();
            return port;
        } catch (IOException e) {
            // failed to dynamically allocate port, try to use hard coded one
            return 9783;
        }
    }
}
