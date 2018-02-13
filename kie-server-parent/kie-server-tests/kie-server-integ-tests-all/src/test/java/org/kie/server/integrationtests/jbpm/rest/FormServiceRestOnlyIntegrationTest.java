/*
 * Copyright 2016 JBoss by Red Hat.
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
package org.kie.server.integrationtests.jbpm.rest;

import static org.assertj.core.api.Assertions.*;
import static org.kie.server.api.rest.RestURI.*;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.server.api.marshalling.Marshaller;
import org.kie.server.api.marshalling.MarshallerFactory;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.api.model.instance.TaskSummaryList;
import org.kie.server.api.model.type.JaxbLong;
import org.kie.server.api.rest.RestURI;
import org.kie.server.integrationtests.config.TestConfig;
import org.kie.server.integrationtests.shared.KieServerDeployer;

public class FormServiceRestOnlyIntegrationTest extends RestJbpmBaseIntegrationTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");

    private static final String CONTAINER_ID = "definition-project";
    private static final String HIRING_PROCESS_ID = "hiring";

    private Response response = null;

    @BeforeClass
    public static void buildAndDeployArtifacts() {

        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/definition-project").getFile());

        createContainer(CONTAINER_ID, releaseId);
    }

    @After
    public void releaseConnection() {
        if (response != null) {
            response.close();
        }
    }

    @Test
    public void testGetProcessFormTest() throws Exception {
        Map<String, Object> valuesMap = new HashMap<String, Object>();
        valuesMap.put(RestURI.CONTAINER_ID, CONTAINER_ID);
        valuesMap.put(RestURI.PROCESS_ID, HIRING_PROCESS_ID);

        WebTarget clientRequest = newRequest(build(TestConfig.getKieServerHttpUrl(), FORM_URI + "/" + PROCESS_FORM_GET_URI, valuesMap));
        logger.info("[GET] " + clientRequest.getUri());

        response = clientRequest.request(getMediaType()).get();
        Assert.assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        String result = response.readEntity(String.class);
        logger.debug("Form content is '{}'", result);
        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isFalse();
    }

    @Test
    public void testGetTaskFormTest() throws Exception {
        Map<String, Object> valuesMap = new HashMap<String, Object>();
        valuesMap.put(RestURI.CONTAINER_ID, CONTAINER_ID);
        valuesMap.put(RestURI.PROCESS_ID, HIRING_PROCESS_ID);

        Marshaller marshaller = MarshallerFactory.getMarshaller(marshallingFormat, ClassLoader.getSystemClassLoader());

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", "john");

        // start process instance
        WebTarget clientRequest = newRequest(build(TestConfig.getKieServerHttpUrl(), PROCESS_URI + "/" + START_PROCESS_POST_URI, valuesMap));
        logger.info("[POST] " + clientRequest.getUri());
        response = clientRequest.request(getMediaType()).post(createEntity(marshaller.marshall(params)));
        Assert.assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());

        Long result = response.readEntity(JaxbLong.class).unwrap();
        assertThat(result).isNotNull();

        // find tasks by process instance id
        valuesMap.put(RestURI.PROCESS_INST_ID, result);

        clientRequest = newRequest(build(TestConfig.getKieServerHttpUrl(), QUERY_URI + "/" + TASK_BY_PROCESS_INST_ID_GET_URI, valuesMap));
        logger.info("[GET] " + clientRequest.getUri());

        response = clientRequest.request(getMediaType()).get();
        Assert.assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        TaskSummaryList taskSummaryList = marshaller.unmarshall(response.readEntity(String.class), TaskSummaryList.class);
        logger.debug("Form content is '{}'", taskSummaryList);

        assertThat(taskSummaryList).isNotNull();
        TaskSummary[] task = taskSummaryList.getTasks();
        assertThat(task.length).isEqualTo(1);

        Long taskId = task[0].getId();

        valuesMap.put(RestURI.TASK_INSTANCE_ID, taskId);

        clientRequest = newRequest(build(TestConfig.getKieServerHttpUrl(), FORM_URI + "/" + TASK_FORM_GET_URI, valuesMap));
        logger.info("[GET] " + clientRequest.getUri());

        response = clientRequest.request(getMediaType()).get();
        Assert.assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        String formdata = response.readEntity(String.class);
        logger.debug("Form content is '{}'", formdata);
        assertThat(formdata).isNotNull();
        assertThat(formdata.isEmpty()).isFalse();

        clientRequest = newRequest(build(TestConfig.getKieServerHttpUrl(), PROCESS_URI + "/" + ABORT_PROCESS_INST_DEL_URI, valuesMap));
        logger.info("[DELETE] " + clientRequest.getUri());

        response = clientRequest.request().delete();
        int noContentStatusCode = Response.Status.NO_CONTENT.getStatusCode();
        int okStatusCode = Response.Status.OK.getStatusCode();
        assertThat("Wrong status code returned: " + response.getStatus().isTrue(),
                response.getStatus() == noContentStatusCode || response.getStatus() == okStatusCode);
    }

    @Test
    public void testGetProcessDoesNotExistFormTest() throws Exception {
        Map<String, Object> valuesMap = new HashMap<String, Object>();
        valuesMap.put(RestURI.CONTAINER_ID, CONTAINER_ID);
        valuesMap.put(RestURI.PROCESS_ID, "not-existing");
        valuesMap.put(RestURI.TASK_INSTANCE_ID, 99999);

        WebTarget clientRequest = newRequest(build(TestConfig.getKieServerHttpUrl(), FORM_URI + "/" + TASK_FORM_GET_URI, valuesMap));
        logger.info("[GET] " + clientRequest.getUri());

        response = clientRequest.request(getMediaType()).get();
        Assert.assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testGetTaskDoesNotExistFormTest() throws Exception {
        Map<String, Object> valuesMap = new HashMap<String, Object>();
        valuesMap.put(RestURI.CONTAINER_ID, CONTAINER_ID);
        valuesMap.put(RestURI.PROCESS_ID, "not-existing");

        WebTarget clientRequest = newRequest(build(TestConfig.getKieServerHttpUrl(), FORM_URI + "/" + PROCESS_FORM_GET_URI, valuesMap));
        logger.info("[GET] " + clientRequest.getUri());

        response = clientRequest.request(getMediaType()).get();
        Assert.assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }
}
