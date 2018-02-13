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

package org.kie.server.router;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

public class ConfigurationTest {


    @Test
    public void testRemoveServerWhenUnavailable() {

        Configuration config = new Configuration();

        config.addContainerHost("container1", "http://localhost:8080/server");
        config.addContainerHost("container2", "http://localhost:8180/server");

        config.addServerHost("server1", "http://localhost:8080/server");
        config.addServerHost("server2", "http://localhost:8180/server");

        ContainerInfo containerInfo = new ContainerInfo("test1.0", "test", "org.kie:test:1.0");
        config.addContainerInfo(containerInfo);

        assertThat(config.getHostsPerContainer()).hasSize(2);
        assertThat(config.getHostsPerServer()).hasSize(2);

        assertThat(config.getHostsPerContainer().get("container1")).hasSize(1);
        assertThat(config.getHostsPerContainer().get("container2")).hasSize(1);
        assertThat(config.getHostsPerServer().get("server1")).hasSize(1);
        assertThat(config.getHostsPerServer().get("server2")).hasSize(1);

        config.removeUnavailableServer("http://localhost:8180/server");

        assertThat(config.getHostsPerContainer()).hasSize(2);
        assertThat(config.getHostsPerServer()).hasSize(2);

        assertThat(config.getHostsPerContainer().get("container1")).hasSize(1);
        assertThat(config.getHostsPerContainer().get("container2")).isEmpty();
        assertThat(config.getHostsPerServer().get("server1")).hasSize(1);
        assertThat(config.getHostsPerServer().get("server2")).isEmpty();
    }

    @Test
    public void testRemoveServerWhenUnavailableRequestURL() {

        Configuration config = new Configuration();

        config.addContainerHost("container1", "http://localhost:8080/server");
        config.addContainerHost("container2", "http://localhost:8180/server");

        config.addServerHost("server1", "http://localhost:8080/server");
        config.addServerHost("server2", "http://localhost:8180/server");

        ContainerInfo containerInfo = new ContainerInfo("test1.0", "test", "org.kie:test:1.0");
        config.addContainerInfo(containerInfo);

        assertThat(config.getHostsPerContainer()).hasSize(2);
        assertThat(config.getHostsPerServer()).hasSize(2);

        assertThat(config.getHostsPerContainer().get("container1")).hasSize(1);
        assertThat(config.getHostsPerContainer().get("container2")).hasSize(1);
        assertThat(config.getHostsPerServer().get("server1")).hasSize(1);
        assertThat(config.getHostsPerServer().get("server2")).hasSize(1);

        config.removeUnavailableServer("http://localhost:8180/server/containers/instances/1");

        assertThat(config.getHostsPerContainer()).hasSize(2);
        assertThat(config.getHostsPerServer()).hasSize(2);

        assertThat(config.getHostsPerContainer().get("container1")).hasSize(1);
        assertThat(config.getHostsPerContainer().get("container2")).isEmpty();
        assertThat(config.getHostsPerServer().get("server1")).hasSize(1);
        assertThat(config.getHostsPerServer().get("server2")).isEmpty();
    }
    
    @Test
    public void testMultipleServersWithSameUrl() {

        Configuration config = new Configuration();

        // add two server with same host url for container
        config.addContainerHost("container1", "http://localhost:8080/server");
        config.addContainerHost("container1", "http://localhost:8080/server");
        // add two server with same host url for alias
        config.addContainerHost("container", "http://localhost:8080/server");
        config.addContainerHost("container", "http://localhost:8080/server");
        // add two server with same host url
        config.addServerHost("server1", "http://localhost:8080/server");
        config.addServerHost("server1", "http://localhost:8080/server");
        // add two containers info each for every server
        ContainerInfo containerInfo = new ContainerInfo("test1.0", "test", "org.kie:test:1.0");
        config.addContainerInfo(containerInfo);
        ContainerInfo containerInfo2 = new ContainerInfo("test1.0", "test", "org.kie:test:1.0");
        config.addContainerInfo(containerInfo2);

        assertThat(config.getHostsPerContainer()).hasSize(2);
        assertThat(config.getHostsPerServer()).hasSize(1);

        assertThat(config.getHostsPerContainer().get("container1")).hasSize(2);        
        assertThat(config.getHostsPerServer().get("server1")).hasSize(2);

        config.removeContainerHost("container1", "http://localhost:8080/server");
        config.removeContainerHost("container", "http://localhost:8080/server");
        config.removeServerHost("server1", "http://localhost:8080/server");

        config.removeContainerInfo(containerInfo);

        assertThat(config.getHostsPerContainer()).hasSize(2);
        assertThat(config.getHostsPerServer()).hasSize(1);

        assertThat(config.getHostsPerContainer().get("container1")).hasSize(1);       
        assertThat(config.getHostsPerServer().get("server1")).hasSize(1);
    }
    
    @Test
    public void testReloadFromConfigurationAddedServersAndContainers() {
        
        Configuration config = new Configuration();
        config.addContainerHost("container1", "http://localhost:8080/server");
        config.addServerHost("server1", "http://localhost:8080/server");
        ContainerInfo containerInfo = new ContainerInfo("test1.0", "test", "org.kie:test:1.0");
        config.addContainerInfo(containerInfo);

        assertThat(config.getHostsPerContainer()).hasSize(1);
        assertThat(config.getHostsPerServer()).hasSize(1);

        assertThat(config.getHostsPerContainer().get("container1")).hasSize(1);        
        assertThat(config.getHostsPerServer().get("server1")).hasSize(1);
        
        Configuration updated = new Configuration();
        updated.addContainerHost("container1", "http://localhost:8080/server");
        updated.addServerHost("server1", "http://localhost:8080/server");
        updated.addContainerHost("container2", "http://localhost:8081/server");
        updated.addServerHost("server2", "http://localhost:8081/server");
        ContainerInfo updatedContainerInfo = new ContainerInfo("test1.0", "test", "org.kie:test:1.0");
        updated.addContainerInfo(updatedContainerInfo);
        
        config.reloadFrom(updated);
        
        assertThat(config.getHostsPerContainer()).hasSize(2);
        assertThat(config.getHostsPerServer()).hasSize(2);

        assertThat(config.getHostsPerContainer().get("container1")).hasSize(1);        
        assertThat(config.getHostsPerServer().get("server1")).hasSize(1);
        assertThat(config.getHostsPerContainer().get("container2")).hasSize(1);        
        assertThat(config.getHostsPerServer().get("server2")).hasSize(1);
    }
    
    @Test
    public void testReloadFromConfigurationReplacedServersAndContainers() {
        
        Configuration config = new Configuration();
        config.addContainerHost("container1", "http://localhost:8080/server");
        config.addServerHost("server1", "http://localhost:8080/server");
        ContainerInfo containerInfo = new ContainerInfo("test1.0", "test", "org.kie:test:1.0");
        config.addContainerInfo(containerInfo);

        assertThat(config.getHostsPerContainer()).hasSize(1);
        assertThat(config.getHostsPerServer()).hasSize(1);

        assertThat(config.getHostsPerContainer().get("container1")).hasSize(1);        
        assertThat(config.getHostsPerServer().get("server1")).hasSize(1);
        
        Configuration updated = new Configuration();
        updated.addContainerHost("container2", "http://localhost:8081/server");
        updated.addServerHost("server2", "http://localhost:8081/server");
        ContainerInfo updatedContainerInfo = new ContainerInfo("test1.0", "test", "org.kie:test:1.0");
        updated.addContainerInfo(updatedContainerInfo);
        
        config.reloadFrom(updated);
        
        assertThat(config.getHostsPerContainer()).hasSize(1);
        assertThat(config.getHostsPerServer()).hasSize(1);

        assertThat(config.getHostsPerContainer().get("container1")).isNull();        
        assertThat(config.getHostsPerServer().get("server1")).isNull();
        assertThat(config.getHostsPerContainer().get("container2")).hasSize(1);        
        assertThat(config.getHostsPerServer().get("server2")).hasSize(1);
    }
    
    @Test
    public void testReloadFromConfigurationUpdatedUrls() {
        
        Configuration config = new Configuration();
        config.addContainerHost("container1", "http://localhost:8080/server");
        config.addServerHost("server1", "http://localhost:8080/server");
        ContainerInfo containerInfo = new ContainerInfo("test1.0", "test", "org.kie:test:1.0");
        config.addContainerInfo(containerInfo);

        assertThat(config.getHostsPerContainer()).hasSize(1);
        assertThat(config.getHostsPerServer()).hasSize(1);

        assertThat(config.getHostsPerContainer().get("container1")).hasSize(1);        
        assertThat(config.getHostsPerServer().get("server1")).hasSize(1);
        
        assertThat(config.getHostsPerContainer().get("container1").get(0)).isEqualTo("http://localhost:8080/server");        
        assertThat(config.getHostsPerServer().get("server1").get(0)).isEqualTo("http://localhost:8080/server");
               
        Configuration updated = new Configuration();
        updated.addContainerHost("container1", "http://localhost:8081/server");
        updated.addServerHost("server1", "http://localhost:8081/server");
        ContainerInfo updatedContainerInfo = new ContainerInfo("test1.0", "test", "org.kie:test:1.0");
        updated.addContainerInfo(updatedContainerInfo);
        
        config.reloadFrom(updated);
        
        assertThat(config.getHostsPerContainer()).hasSize(1);
        assertThat(config.getHostsPerServer()).hasSize(1);

        assertThat(config.getHostsPerContainer().get("container1")).hasSize(1);        
        assertThat(config.getHostsPerServer().get("server1")).hasSize(1);

        assertThat(config.getHostsPerContainer().get("container1").get(0)).isEqualTo("http://localhost:8081/server");        
        assertThat(config.getHostsPerServer().get("server1").get(0)).isEqualTo("http://localhost:8081/server");
    }
    
    @Test
    public void testReloadFromConfigurationUpdatedAndAddedUrls() {
        
        Configuration config = new Configuration();
        config.addContainerHost("container1", "http://localhost:8080/server");
        config.addServerHost("server1", "http://localhost:8080/server");
        ContainerInfo containerInfo = new ContainerInfo("test1.0", "test", "org.kie:test:1.0");
        config.addContainerInfo(containerInfo);

        assertThat(config.getHostsPerContainer()).hasSize(1);
        assertThat(config.getHostsPerServer()).hasSize(1);

        assertThat(config.getHostsPerContainer().get("container1")).hasSize(1);        
        assertThat(config.getHostsPerServer().get("server1")).hasSize(1);
        
        assertThat(config.getHostsPerContainer().get("container1").get(0)).isEqualTo("http://localhost:8080/server");        
        assertThat(config.getHostsPerServer().get("server1").get(0)).isEqualTo("http://localhost:8080/server");
               
        Configuration updated = new Configuration();
        updated.addContainerHost("container1", "http://localhost:8081/server");
        updated.addServerHost("server1", "http://localhost:8081/server");
        updated.addContainerHost("container1", "http://localhost:8080/server");
        updated.addServerHost("server1", "http://localhost:8080/server");
        ContainerInfo updatedContainerInfo = new ContainerInfo("test1.0", "test", "org.kie:test:1.0");
        updated.addContainerInfo(updatedContainerInfo);
        
        config.reloadFrom(updated);
        
        assertThat(config.getHostsPerContainer()).hasSize(1);
        assertThat(config.getHostsPerServer()).hasSize(1);

        assertThat(config.getHostsPerContainer().get("container1")).hasSize(2);        
        assertThat(config.getHostsPerServer().get("server1")).hasSize(2);

        assertThat(config.getHostsPerContainer().get("container1").get(0)).isEqualTo("http://localhost:8080/server");        
        assertThat(config.getHostsPerServer().get("server1").get(0)).isEqualTo("http://localhost:8080/server");
        assertThat(config.getHostsPerContainer().get("container1").get(1)).isEqualTo("http://localhost:8081/server");        
        assertThat(config.getHostsPerServer().get("server1").get(1)).isEqualTo("http://localhost:8081/server");
    }
}
