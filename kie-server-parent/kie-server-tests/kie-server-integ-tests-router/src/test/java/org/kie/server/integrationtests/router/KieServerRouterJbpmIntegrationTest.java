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

package org.kie.server.integrationtests.router;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.internal.executor.api.STATUS;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.definition.QueryDefinition;
import org.kie.server.api.model.definition.QueryFilterSpec;
import org.kie.server.api.model.instance.JobRequestInstance;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.RequestInfoInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.api.util.QueryFilterSpecBuilder;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;

import static org.assertj.core.api.Assertions.*;

public class KieServerRouterJbpmIntegrationTest extends KieServerRouterBaseIntegrationTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");


    protected static final String CONTAINER_ALIAS = "project";
    protected static final String BUSINESS_KEY = "test key";
    protected static final String PRINT_OUT_COMMAND = "org.jbpm.executor.commands.PrintOutCommand";

    @BeforeClass
    public static void buildAndDeployArtifacts() {

        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/definition-project").getFile());
        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);
        createContainer(CONTAINER_ID, releaseId, CONTAINER_ALIAS);
    }

    @Override
    protected void addExtraCustomClasses(Map<String, Class<?>> extraClasses) throws Exception {
        extraClasses.put(PERSON_CLASS_NAME, Class.forName(PERSON_CLASS_NAME, true, kieContainer.getClassLoader()));
    }

    @Test
    public void testStartProcessWithContainerAlias() throws Exception {

        Object person = createPersonInstance(USER_JOHN);

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("test", USER_MARY);
        parameters.put("number", new Integer(12345));
        List<Object> list = new ArrayList<Object>();
        list.add("item");
        parameters.put("list", list);
        parameters.put("person", person);

        Long processInstanceIdV1 = processClient.startProcess(CONTAINER_ALIAS, PROCESS_ID_EVALUATION, parameters);

        assertThat(processInstanceIdV1).isNotNull();
        assertThat(processInstanceIdV1.longValue() > 0).isTrue();

        ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ALIAS, processInstanceIdV1);
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getContainerId()).isEqualTo(CONTAINER_ID);

        List<TaskSummary> tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
        assertThat(tasks).hasSize(1);

        ServiceResponse<KieContainerResourceList> containersResponse = client.listContainers();
        KieServerAssert.assertSuccess(containersResponse);

        List<KieContainerResource> containerResources = containersResponse.getResult().getContainers();
        assertThat(containerResources).hasSize(1);
    }

    @Test
    public void testStartProcessWithContainerId() throws Exception {

        Object person = createPersonInstance(USER_JOHN);

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("test", USER_MARY);
        parameters.put("number", new Integer(12345));
        List<Object> list = new ArrayList<Object>();
        list.add("item");
        parameters.put("list", list);
        parameters.put("person", person);

        Long processInstanceIdV1 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);

        assertThat(processInstanceIdV1).isNotNull();
        assertThat(processInstanceIdV1.longValue() > 0).isTrue();

        ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceIdV1);
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getContainerId()).isEqualTo(CONTAINER_ID);

        List<TaskSummary> tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
        assertThat(tasks).hasSize(1);

        processClient.abortProcessInstance(CONTAINER_ID, processInstanceIdV1);
    }

    @Test
    public void testStartProcessAndQueryViaAdvancedQueries() throws Exception {

        QueryDefinition query = new QueryDefinition();
        query.setName("allProcessInstances");
        query.setSource(System.getProperty("org.kie.server.persistence.ds", "jdbc/jbpm-ds"));
        query.setExpression("select * from ProcessInstanceLog where status = 1");
        query.setTarget("PROCESS");

        queryClient.registerQuery(query);
        try {
            Object person = createPersonInstance(USER_JOHN);

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("test", USER_MARY);
            parameters.put("number", new Integer(12345));
            List<Object> list = new ArrayList<Object>();
            list.add("item");
            parameters.put("list", list);
            parameters.put("person", person);

            Long processInstanceIdV1 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);

            assertThat(processInstanceIdV1).isNotNull();
            assertThat(processInstanceIdV1.longValue() > 0).isTrue();

            List<ProcessInstance> instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI, 0, 10, ProcessInstance.class);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(1);

            queryClient.replaceQuery(query);

            QueryFilterSpec filterSpec = new QueryFilterSpecBuilder()
                    .greaterThan("processinstanceid", 0)
                    .get();


            instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI, filterSpec, 0, 10, ProcessInstance.class);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(1);

            processClient.abortProcessInstance(CONTAINER_ID, processInstanceIdV1);
        } catch (Exception e){
          e.printStackTrace();
          fail(e.getMessage());
        } finally {
            queryClient.unregisterQuery(query.getName());
        }

    }

    @Test
    public void testStartProcessAndQueryViaAdvancedQueriesRawContent() throws Exception {

        QueryDefinition query = new QueryDefinition();
        query.setName("allProcessInstances");
        query.setSource(System.getProperty("org.kie.server.persistence.ds", "jdbc/jbpm-ds"));
        query.setExpression("select * from ProcessInstanceLog where status = 1");
        query.setTarget("PROCESS");

        queryClient.registerQuery(query);
        try {
            Object person = createPersonInstance(USER_JOHN);

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("test", USER_MARY);
            parameters.put("number", new Integer(12345));
            List<Object> list = new ArrayList<Object>();
            list.add("item");
            parameters.put("list", list);
            parameters.put("person", person);

            Long processInstanceIdV1 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);

            assertThat(processInstanceIdV1).isNotNull();
            assertThat(processInstanceIdV1.longValue() > 0).isTrue();

            List<List> instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_RAW, 0, 10, List.class);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(1);

            for (List row : instances) {
                assertThat(row).hasSize(16);
            }


            processClient.abortProcessInstance(CONTAINER_ID, processInstanceIdV1);
        } catch (Exception e){
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            queryClient.unregisterQuery(query.getName());
        }

    }
    
    @Test
    public void testScheduleViewAndCancelJob() {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);

        Map<String, Object> data = new HashMap<>();
        data.put("businessKey", BUSINESS_KEY);

        JobRequestInstance jobRequestInstance = new JobRequestInstance();
        jobRequestInstance.setCommand(PRINT_OUT_COMMAND);
        jobRequestInstance.setData(data);
        jobRequestInstance.setScheduledDate(tomorrow.getTime());

        Long jobId = jobServicesClient.scheduleRequest(CONTAINER_ID, jobRequestInstance);
        assertThat(jobId).isNotNull();
        assertThat(jobId.longValue() > 0).isTrue();

        RequestInfoInstance jobRequest = jobServicesClient.getRequestById(CONTAINER_ID, jobId, false, false);
        RequestInfoInstance expected = createExpectedRequestInfoInstance(jobId, STATUS.QUEUED);
        assertRequestInfoInstance(expected, jobRequest);
        assertThat(jobRequest.getScheduledDate()).isNotNull();

        jobServicesClient.cancelRequest(CONTAINER_ID, jobId);

        jobRequest = jobServicesClient.getRequestById(CONTAINER_ID, jobId, false, false);
        expected.setStatus(STATUS.CANCELLED.toString());
        assertRequestInfoInstance(expected, jobRequest);
    }
    
    private void assertRequestInfoInstance(RequestInfoInstance expected, RequestInfoInstance actual) {
        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(expected.getId());
        assertThat(actual.getBusinessKey()).isEqualTo(expected.getBusinessKey());
        assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
        assertThat(actual.getCommandName()).isEqualTo(expected.getCommandName());
    }
    
    private RequestInfoInstance createExpectedRequestInfoInstance(Long jobId, STATUS expected) {
        return RequestInfoInstance.builder()
                .id(jobId)
                .businessKey(BUSINESS_KEY)
                .status(expected.toString())
                .command(PRINT_OUT_COMMAND)
                .build();
    }
}
