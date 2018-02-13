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

package org.kie.server.integrationtests.jbpm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.jbpm.services.api.TaskNotFoundException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.api.KieServices;
import org.kie.api.task.model.Status;
import org.kie.internal.KieInternalServices;
import org.kie.internal.process.CorrelationKey;
import org.kie.internal.process.CorrelationKeyFactory;
import org.kie.internal.task.api.model.TaskEvent;
import org.kie.server.api.exception.KieServicesHttpException;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.instance.NodeInstance;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.TaskEventInstance;
import org.kie.server.api.model.instance.TaskInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.api.model.instance.VariableInstance;
import org.kie.server.api.model.instance.WorkItemInstance;
import org.kie.server.api.exception.KieServicesException;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.integrationtests.category.Smoke;
import org.kie.server.integrationtests.config.TestConfig;

import static org.hamcrest.CoreMatchers.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.assumeFalse;

import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;



public class RuntimeDataServiceIntegrationTest extends JbpmKieServerBaseIntegrationTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");

    protected static final String SORT_BY_PROCESS_ID = "ProcessId";
    protected static final String SORT_BY_INSTANCE_PROCESS_ID = "Id";
    protected static final String SORT_BY_TASK_STATUS = "Status";
    protected static final String SORT_BY_TASK_EVENTS_TYPE = "Type";

    @BeforeClass
    public static void buildAndDeployArtifacts() {

        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/definition-project").getFile());

        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);

        createContainer(CONTAINER_ID, releaseId);
    }

    @Override
    protected void addExtraCustomClasses(Map<String, Class<?>> extraClasses) throws Exception {
        extraClasses.put(PERSON_CLASS_NAME, Class.forName(PERSON_CLASS_NAME, true, kieContainer.getClassLoader()));
    }

    @Test
    public void testGetProcessDefinitions() throws Exception {
        List<ProcessDefinition> definitions = queryClient.findProcesses(0, 20);
        assertThat(definitions).isNotNull();

        assertThat(definitions).hasSize(12);
        List<String> processIds = collectDefinitions(definitions);
        checkProcessDefinitions(processIds);

        // test paging of the result
        definitions = queryClient.findProcesses(0, 3);

        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(3);
        processIds = collectDefinitions(definitions);
        assertThat(processIds.contains(PROCESS_ID_ASYNC_SCRIPT)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_SIGNAL_START)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_TIMER)).isTrue();

        definitions = queryClient.findProcesses(1, 3);
        assertThat(definitions).isNotNull();

        assertThat(definitions).hasSize(3);
        processIds = collectDefinitions(definitions);
        assertThat(processIds.contains(PROCESS_ID_EVALUATION)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_CUSTOM_TASK)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_CALL_EVALUATION)).isTrue();

    }

    @Test
    public void testGetProcessDefinitionsSorted() throws Exception {
        List<ProcessDefinition> definitions = queryClient.findProcesses(0, 20, QueryServicesClient.SORT_BY_NAME, false);
        assertThat(definitions).isNotNull();

        assertThat(definitions).hasSize(12);
        List<String> processIds = collectDefinitions(definitions);
        checkProcessDefinitions(processIds);

        // test paging of the result
        definitions = queryClient.findProcesses(0, 3, QueryServicesClient.SORT_BY_NAME, true);

        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(3);
        processIds = collectDefinitions(definitions);
        assertThat(processIds.contains(PROCESS_ID_ASYNC_SCRIPT)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_SIGNAL_START)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_TIMER)).isTrue();

        definitions = queryClient.findProcesses(0, 3, QueryServicesClient.SORT_BY_NAME, false);
        assertThat(definitions).isNotNull();

        assertThat(definitions).hasSize(3);
        processIds = collectDefinitions(definitions);
        assertThat(processIds.contains(PROCESS_ID_XYZ_TRANSLATIONS)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_USERTASK)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_SIGNAL_PROCESS)).isTrue();

    }

    @Test
    public void testGetProcessDefinitionsWithFilter() throws Exception {
        List<ProcessDefinition> definitions = queryClient.findProcesses("evaluation", 0, 20);
        assertThat(definitions).isNotNull();

        assertThat(definitions).hasSize(2);
        List<String> processIds = collectDefinitions(definitions);
        assertThat(processIds.contains(PROCESS_ID_CALL_EVALUATION)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_EVALUATION)).isTrue();

        // test paging of the result
        definitions = queryClient.findProcesses("evaluation", 0, 1);

        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        processIds = collectDefinitions(definitions);
        assertThat(processIds.contains(PROCESS_ID_CALL_EVALUATION)).isTrue();

        definitions = queryClient.findProcesses("evaluation", 1, 1);
        assertThat(definitions).isNotNull();

        assertThat(definitions).hasSize(1);
        processIds = collectDefinitions(definitions);
        assertThat(processIds.contains(PROCESS_ID_EVALUATION)).isTrue();

    }

    @Test
    public void testGetProcessDefinitionsWithFilterSorted() throws Exception {
        List<ProcessDefinition> definitions = queryClient.findProcesses("evaluation", 0, 20, QueryServicesClient.SORT_BY_NAME, true);
        assertThat(definitions).isNotNull();

        Assertions.assertThat(definitions).hasSize(2);
        List<String> processIds = collectDefinitions(definitions);
        assertThat(processIds.get(0).equals(PROCESS_ID_CALL_EVALUATION)).isTrue();
        assertThat(processIds.get(1).equals(PROCESS_ID_EVALUATION)).isTrue();

        // test paging of the result
        definitions = queryClient.findProcesses("evaluation", 0, 20, QueryServicesClient.SORT_BY_NAME, false);

        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(2);
        processIds = collectDefinitions(definitions);
        assertThat(processIds.get(0).equals(PROCESS_ID_EVALUATION)).isTrue();
        assertThat(processIds.get(1).equals(PROCESS_ID_CALL_EVALUATION)).isTrue();

    }

    @Test
    public void testGetProcessDefinitionsByContainer() throws Exception {
        List<ProcessDefinition> definitions = queryClient.findProcessesByContainerId(CONTAINER_ID, 0, 20);
        assertThat(definitions).isNotNull();

        Assertions.assertThat(definitions).hasSize(12);
        List<String> processIds = collectDefinitions(definitions);
        checkProcessDefinitions(processIds);

        // test paging of the result
        definitions = queryClient.findProcessesByContainerId(CONTAINER_ID, 0, 3);

        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(3);
        processIds = collectDefinitions(definitions);
        assertThat(processIds.contains(PROCESS_ID_ASYNC_SCRIPT)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_SIGNAL_START)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_TIMER)).isTrue();

        definitions = queryClient.findProcessesByContainerId(CONTAINER_ID, 1, 3);
        assertThat(definitions).isNotNull();

        assertThat(definitions).hasSize(3);
        processIds = collectDefinitions(definitions);
        assertThat(processIds.contains(PROCESS_ID_EVALUATION)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_CUSTOM_TASK)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_CALL_EVALUATION)).isTrue();

        // last check if there are process def for not existing project
        definitions = queryClient.findProcessesByContainerId("not-existing-project", 0, 10);
        assertThat(definitions).isNotNull();

        assertThat(definitions).isEmpty();

    }

    @Test
    public void testGetProcessDefinitionsByContainerSorted() throws Exception {
        List<ProcessDefinition> definitions = queryClient.findProcessesByContainerId(CONTAINER_ID, 0, 20, QueryServicesClient.SORT_BY_NAME, true);
        assertThat(definitions).isNotNull();

        Assertions.assertThat(definitions).hasSize(12);
        List<String> processIds = collectDefinitions(definitions);
        checkProcessDefinitions(processIds);

        // test paging of the result
        definitions = queryClient.findProcessesByContainerId(CONTAINER_ID, 0, 3, QueryServicesClient.SORT_BY_NAME, true);

        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(3);
        processIds = collectDefinitions(definitions);
        assertThat(processIds.contains(PROCESS_ID_ASYNC_SCRIPT)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_SIGNAL_START)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_TIMER)).isTrue();

        definitions = queryClient.findProcessesByContainerId(CONTAINER_ID, 0, 3, QueryServicesClient.SORT_BY_NAME, false);
        assertThat(definitions).isNotNull();

        assertThat(definitions).hasSize(3);
        processIds = collectDefinitions(definitions);
        assertThat(processIds.contains(PROCESS_ID_XYZ_TRANSLATIONS)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_USERTASK)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_SIGNAL_PROCESS)).isTrue();
    }

    @Test
    public void testGetProcessDefinitionsById() throws Exception {
        List<ProcessDefinition> definitions = queryClient.findProcessesById(PROCESS_ID_USERTASK);
        assertThat(definitions).isNotNull();

        Assertions.assertThat(definitions).hasSize(1);
        List<String> processIds = collectDefinitions(definitions);
        assertThat(processIds.contains(PROCESS_ID_USERTASK)).isTrue();


         // last check if there are process def for not existing project
        definitions = queryClient.findProcessesById("not-existing-project");
        assertThat(definitions).isNotNull();

        assertThat(definitions).isEmpty();

    }

    @Test
    public void testGetProcessDefinitionByContainerAndId() throws Exception {
        ProcessDefinition definition = queryClient.findProcessByContainerIdProcessId(CONTAINER_ID, PROCESS_ID_USERTASK);
        assertThat(definition).isNotNull();
        assertThat(definition.getId()).isEqualTo(PROCESS_ID_USERTASK);
        assertThat(definition.getName()).isEqualTo("usertask");
        assertThat(definition.getVersion()).isEqualTo("1.0");
        assertThat(definition.getPackageName()).isEqualTo("org.jbpm");
        assertThat(definition.getContainerId()).isEqualTo(CONTAINER_ID);

    }

    @Test
    public void testGetProcessDefinitionByContainerAndNonExistingId() throws Exception {
        try {
            queryClient.findProcessByContainerIdProcessId(CONTAINER_ID, "non-existing");
            fail("KieServicesException should be thrown complaining about process definition not found.");

        } catch (KieServicesException e) {
            KieServerAssert.assertResultContainsString(e.getMessage(), "Could not find process definition \"non-existing\" in container \"definition-project\"");
        }
    }

    @Test
    public void testGetProcessInstances() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = queryClient.findProcessInstances(0, 10);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(5);

            List<Long> found = collectInstances(instances);
            assertThat(found).isEqualTo(processInstanceIds);

            instances = queryClient.findProcessInstances(0, 3);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(3);

            instances = queryClient.findProcessInstances(1, 3);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);

        } finally {
            abortProcessInstances(processInstanceIds);
        }

    }

    @Test
    public void testGetProcessInstancesSortedByName() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = queryClient.findProcessInstances(0, 3, SORT_BY_PROCESS_ID, true);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(3);
            for (ProcessInstance instance : instances) {
                assertThat(processInstanceIds.contains(instance.getId())).isTrue();
                assertThat(instance.getProcessId()).isEqualTo(PROCESS_ID_SIGNAL_PROCESS);
            }

            instances = queryClient.findProcessInstances(1, 3, SORT_BY_PROCESS_ID, true);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);
            for (ProcessInstance instance : instances) {
                assertThat(processInstanceIds.contains(instance.getId())).isTrue();
                assertThat(instance.getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
            }

            instances = queryClient.findProcessInstances(0, 10, SORT_BY_PROCESS_ID, false);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(5);
            for (int i = 0; i < instances.size(); i++) {
                if (i < 2) {
                    assertThat(instances.get(i).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
                } else {
                    assertThat(instances.get(i).getProcessId()).isEqualTo(PROCESS_ID_SIGNAL_PROCESS);
                }
            }
        } finally {
            abortProcessInstances(processInstanceIds);
        }
    }

    @Test
    public void testGetProcessInstancesByContainer() throws Exception {
        int offset = queryClient.findProcessInstancesByContainerId(CONTAINER_ID, Collections.singletonList(2), 0, 10).size();

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = queryClient.findProcessInstancesByContainerId(CONTAINER_ID, null, 0, 10);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(5);

            List<Long> found = collectInstances(instances);
            assertThat(found).isEqualTo(processInstanceIds);

            instances = queryClient.findProcessInstancesByContainerId(CONTAINER_ID, null, 0, 3);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(3);

            instances = queryClient.findProcessInstancesByContainerId(CONTAINER_ID, null, 1, 3);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);

            // search for completed only
            instances = queryClient.findProcessInstancesByContainerId(CONTAINER_ID, Collections.singletonList(2), 0, 10);
            assertThat(instances).isNotNull();
            assertThat(instances.size()).isEqualTo(0 + offset);

        } finally {
            abortProcessInstances(processInstanceIds);
        }

    }

    @Test
    public void testGetProcessInstancesByContainerSortedByName() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = queryClient.findProcessInstancesByContainerId(CONTAINER_ID, null, 0, 3, SORT_BY_PROCESS_ID, true);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(3);
            for (ProcessInstance instance : instances) {
                assertThat(processInstanceIds.contains(instance.getId())).isTrue();
                assertThat(instance.getProcessId()).isEqualTo(PROCESS_ID_SIGNAL_PROCESS);
            }

            instances = queryClient.findProcessInstancesByContainerId(CONTAINER_ID, null, 1, 3, SORT_BY_PROCESS_ID, true);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);
            for (ProcessInstance instance : instances) {
                assertThat(processInstanceIds.contains(instance.getId())).isTrue();
                assertThat(instance.getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
            }

            instances = queryClient.findProcessInstancesByContainerId(CONTAINER_ID, null, 0, 10, SORT_BY_PROCESS_ID, false);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(5);
            for (int i = 0; i < instances.size(); i++) {
                if (i < 2) {
                    assertThat(instances.get(i).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
                } else {
                    assertThat(instances.get(i).getProcessId()).isEqualTo(PROCESS_ID_SIGNAL_PROCESS);
                }
            }
        } finally {
            abortProcessInstances(processInstanceIds);
        }
    }

    @Test
    public void testGetProcessInstancesByProcessId() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = queryClient.findProcessInstancesByProcessId(PROCESS_ID_USERTASK, null, 0, 10);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);
            assertThat(instances.get(0).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
            assertThat(instances.get(1).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);

            instances = queryClient.findProcessInstancesByProcessId(PROCESS_ID_USERTASK, null, 0, 1);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(1);
            assertThat(instances.get(0).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);

            instances = queryClient.findProcessInstancesByProcessId(PROCESS_ID_USERTASK, null, 1, 1);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(1);
            assertThat(instances.get(0).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
        } finally {
            abortProcessInstances(processInstanceIds);
        }

    }

    @Test
    public void testGetProcessInstancesByProcessIdAndStatus() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Integer> abortedStatus = Collections.singletonList(org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED);
        List<Integer> activeStatus = Collections.singletonList(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);

        List<ProcessInstance> instances = queryClient.findProcessInstancesByProcessId(PROCESS_ID_USERTASK, abortedStatus, 0, 10000);
        assertThat(instances).isNotNull();
        int originalAborted = instances.size();

        instances = queryClient.findProcessInstancesByProcessId(PROCESS_ID_USERTASK, activeStatus, 0, 10000);
        assertThat(instances).isNotNull();
        assertThat(instances).isEmpty();

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        instances = queryClient.findProcessInstancesByProcessId(PROCESS_ID_USERTASK, activeStatus, 0, 10000);
        assertThat(instances).isNotNull();
        assertThat(instances).hasSize(1);
        assertThat(instances.get(0).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);

        processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);

        instances = queryClient.findProcessInstancesByProcessId(PROCESS_ID_USERTASK, abortedStatus, 0, 10000);
        assertThat(instances).isNotNull();
        assertThat(instances.size()).isEqualTo(originalAborted + 1);
    }

    @Test
    public void testGetProcessInstancesByProcessIdSortedByInstanceId() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = queryClient.findProcessInstancesByProcessId(PROCESS_ID_USERTASK, null, 0, 10, SORT_BY_INSTANCE_PROCESS_ID, false);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);
            assertThat(instances.get(0).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
            assertThat(instances.get(1).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
            assertThat(instances.get(0).getId() > instances.get(1).getId()).isTrue();

        } finally {
            abortProcessInstances(processInstanceIds);
        }
    }

    @Test
    public void testGetProcessInstancesByProcessName() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = queryClient.findProcessInstancesByProcessName("usertask", null, 0, 10);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);
            assertThat(instances.get(0).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
            assertThat(instances.get(1).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);

            instances = queryClient.findProcessInstancesByProcessName("usertask", null, 0, 1);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(1);
            assertThat(instances.get(0).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);

            instances = queryClient.findProcessInstancesByProcessName("usertask", null, 1, 1);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(1);
            assertThat(instances.get(0).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);

        } finally {
            abortProcessInstances(processInstanceIds);
        }
    }

    @Test
    public void testGetProcessInstancesByProcessNameAndStatus() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Integer> abortedStatus = Collections.singletonList(org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED);
        List<Integer> activeStatus = Collections.singletonList(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);

        List<ProcessInstance> instances = queryClient.findProcessInstancesByProcessName("usertask", abortedStatus, 0, 10000);
        assertThat(instances).isNotNull();
        int originalAborted = instances.size();

        instances = queryClient.findProcessInstancesByProcessName("usertask", activeStatus, 0, 10000);
        assertThat(instances).isNotNull();
        assertThat(instances).isEmpty();

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        instances = queryClient.findProcessInstancesByProcessName("usertask", activeStatus, 0, 10000);
        assertThat(instances).isNotNull();
        assertThat(instances).hasSize(1);
        assertThat(instances.get(0).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);

        processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);

        instances = queryClient.findProcessInstancesByProcessName("usertask", abortedStatus, 0, 10000);
        assertThat(instances).isNotNull();
        assertThat(instances.size()).isEqualTo(originalAborted + 1);
    }

    @Test
    public void testGetProcessInstancesByProcessNameSortedByInstanceId() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = queryClient.findProcessInstancesByProcessName("usertask", null, 0, 10, SORT_BY_INSTANCE_PROCESS_ID, false);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);
            assertThat(instances.get(0).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
            assertThat(instances.get(1).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
            assertThat(instances.get(0).getId() > instances.get(1).getId()).isTrue();

        } finally {
            abortProcessInstances(processInstanceIds);
        }
    }

    @Test
    public void testGetProcessInstancesByStatus() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Integer> abortedStatus = Collections.singletonList(org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED);
        List<Integer> activeStatus = Collections.singletonList(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);

        List<ProcessInstance> instances = queryClient.findProcessInstancesByStatus(abortedStatus, 0, 10000);
        assertThat(instances).isNotNull();
        int originalAborted = instances.size();

        instances = queryClient.findProcessInstancesByStatus(activeStatus, 0, 10000);
        assertThat(instances).isNotNull();
        assertThat(instances).isEmpty();

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        instances = queryClient.findProcessInstancesByStatus(activeStatus, 0, 10000);
        assertThat(instances).isNotNull();
        assertThat(instances).hasSize(1);

        processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);

        instances = queryClient.findProcessInstancesByStatus(abortedStatus, 0, 10000);
        assertThat(instances).isNotNull();
        assertThat(instances.size()).isEqualTo(originalAborted + 1);
    }

    @Test
    public void testGetProcessInstancesByStatusPaging() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<Integer> activeStatus = Collections.singletonList(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);

            List<ProcessInstance> instances = queryClient.findProcessInstancesByStatus(activeStatus, 0, 10);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(5);

            instances = queryClient.findProcessInstancesByStatus(activeStatus, 0, 3);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(3);

            instances = queryClient.findProcessInstancesByStatus(activeStatus, 1, 3);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);

        } finally {
            abortProcessInstances(processInstanceIds);
        }

    }

    @Test
    public void testGetProcessInstancesByStatusSortedByName() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = queryClient.findProcessInstancesByStatus(Collections.singletonList(1), 0, 3, SORT_BY_PROCESS_ID, true);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(3);
            for (ProcessInstance instance : instances) {
                assertThat(processInstanceIds.contains(instance.getId())).isTrue();
                assertThat(instance.getProcessId()).isEqualTo(PROCESS_ID_SIGNAL_PROCESS);
            }

            instances = queryClient.findProcessInstancesByStatus(Collections.singletonList(1), 1, 3, SORT_BY_PROCESS_ID, true);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);
            for (ProcessInstance instance : instances) {
                assertThat(processInstanceIds.contains(instance.getId())).isTrue();
                assertThat(instance.getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
            }

            instances = queryClient.findProcessInstancesByStatus(Collections.singletonList(1), 0, 10, SORT_BY_PROCESS_ID, false);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(5);
            for (int i = 0; i < instances.size(); i++) {
                if (i < 2) {
                    assertThat(instances.get(i).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
                } else {
                    assertThat(instances.get(i).getProcessId()).isEqualTo(PROCESS_ID_SIGNAL_PROCESS);
                }
            }
        } finally {
            abortProcessInstances(processInstanceIds);
        }
    }

    @Test
    public void testGetProcessInstancesByInitiator() throws Exception {
        int offset = queryClient.findProcessInstancesByInitiator(USER_YODA, Collections.singletonList(2), 0, 10).size();

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = queryClient.findProcessInstancesByInitiator(USER_YODA, null, 0, 10);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(5);

            instances = queryClient.findProcessInstancesByInitiator(USER_YODA, null, 0, 3);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(3);


            instances = queryClient.findProcessInstancesByInitiator(USER_YODA, null, 1, 3);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);

            // search for completed only
            instances = queryClient.findProcessInstancesByInitiator(USER_YODA, Collections.singletonList(2), 0, 10);
            assertThat(instances).isNotNull();
            assertThat(instances.size()).isEqualTo(0 + offset);

        } finally {
            abortProcessInstances(processInstanceIds);
        }

    }

    @Test
    public void testGetProcessInstancesByInitiatorSortedByName() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = queryClient.findProcessInstancesByInitiator(USER_YODA, null, 0, 3, SORT_BY_PROCESS_ID, true);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(3);
            for (ProcessInstance instance : instances) {
                assertThat(processInstanceIds.contains(instance.getId())).isTrue();
                assertThat(instance.getProcessId()).isEqualTo(PROCESS_ID_SIGNAL_PROCESS);
            }

            instances = queryClient.findProcessInstancesByInitiator(USER_YODA, null, 1, 3, SORT_BY_PROCESS_ID, true);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);
            for (ProcessInstance instance : instances) {
                assertThat(processInstanceIds.contains(instance.getId())).isTrue();
                assertThat(instance.getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
            }

            instances = queryClient.findProcessInstancesByInitiator(USER_YODA, null, 0, 10, SORT_BY_PROCESS_ID, false);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(5);
            for (int i = 0; i < instances.size(); i++) {
                if (i < 2) {
                    assertThat(instances.get(i).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
                } else {
                    assertThat(instances.get(i).getProcessId()).isEqualTo(PROCESS_ID_SIGNAL_PROCESS);
                }
            }
        } finally {
            abortProcessInstances(processInstanceIds);
        }
    }

    @Test
    @Category(Smoke.class)
    public void testGetProcessInstanceById() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        try {
            ProcessInstance instance = queryClient.findProcessInstanceById(processInstanceId);
            assertThat(instance).isNotNull();
            assertThat(instance.getId()).isEqualTo(processInstanceId);
            assertThat(instance.getProcessId()).isEqualTo(PROCESS_ID_EVALUATION);
            assertThat(instance.getProcessName()).isEqualTo("evaluation");
            assertThat(instance.getProcessVersion()).isEqualTo("1.0");
            assertThat(instance.getInitiator()).isEqualTo(USER_YODA);
            assertThat(instance.getContainerId()).isEqualTo(CONTAINER_ID);
            assertThat(instance.getCorrelationKey()).isEqualTo(processInstanceId.toString());
            assertThat(instance.getProcessInstanceDescription()).isEqualTo("evaluation");
            assertThat(instance.getParentId().longValue()).isEqualTo(-1);
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testGetProcessInstanceWithVariables() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        Object person = createPersonInstance(USER_JOHN);
        parameters.put("personData", person);

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters);
        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId.longValue() > 0).isTrue();

        try {
            ProcessInstance processInstance = queryClient.findProcessInstanceById(processInstanceId, true);
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getId()).isEqualTo(processInstanceId);
            assertThat(processInstance.getState().intValue()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);
            assertThat(processInstance.getProcessId()).isEqualTo(PROCESS_ID_SIGNAL_PROCESS);
            assertThat(processInstance.getProcessName()).isEqualTo("signalprocess");
            assertThat(processInstance.getProcessVersion()).isEqualTo("1.0");
            assertThat(processInstance.getContainerId()).isEqualTo(CONTAINER_ID);
            assertThat(processInstance.getProcessInstanceDescription()).isEqualTo("signalprocess");
            assertThat(processInstance.getInitiator()).isEqualTo(TestConfig.getUsername());
            assertThat(processInstance.getParentId().longValue()).isEqualTo(-1l);
            assertThat(processInstance.getCorrelationKey()).isNotNull();
            assertThat(processInstance.getDate()).isNotNull();

            Map<String, Object> variables = processInstance.getVariables();
            assertThat(variables).isNotNull();
            assertThat(variables).hasSize(4);

            assertThat(variables.containsKey("stringData")).isTrue();
            assertThat(variables.containsKey("personData")).isTrue();
            assertThat(variables.containsKey("initiator")).isTrue();
            assertThat(variables.containsKey("nullAccepted")).isTrue();

            String stringVar = (String) variables.get("stringData");
            Object personVar = variables.get("personData");
            String initiator = (String) variables.get("initiator");

            assertThat(personVar).isNotNull();
            assertThat(personVar).isEqualTo(person.toString());

            assertThat(stringVar).isNotNull();
            assertThat(stringVar).isEqualTo("waiting for signal");

            assertThat(initiator).isNotNull();
            assertThat(initiator).isEqualTo(TestConfig.getUsername());
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testGetProcessInstanceByNonExistingId() throws Exception {
        try {
            queryClient.findProcessInstanceById(-9999l);
            fail("KieServicesException should be thrown complaining about process instance not found.");

        } catch (KieServicesException e) {
            KieServerAssert.assertResultContainsString(e.getMessage(), "Could not find process instance with id");
        }
    }

    @Test
    public void testGetProcessInstanceByCorrelationKey() throws Exception {
        CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

        String businessKey = "simple-key";
        CorrelationKey key = correlationKeyFactory.newCorrelationKey(businessKey);

        Map<String, Object> parameters = new HashMap<String, Object>();
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, key, parameters);
        try {
            List<ProcessInstance> returnedProcessInstances = new ArrayList<ProcessInstance>();

            ProcessInstance instance = queryClient.findProcessInstanceById(processInstanceId);
            returnedProcessInstances.add(instance);

            instance = queryClient.findProcessInstanceByCorrelationKey(key);
            returnedProcessInstances.add(instance);

            List<ProcessInstance> processInstances = queryClient.findProcessInstancesByCorrelationKey(key, 0, 100);
            assertThat(processInstances).isNotNull();
            // Separate active instances as response contains also instances already closed or aborted.
            List<ProcessInstance> activeInstances = new ArrayList<ProcessInstance>();
            for (ProcessInstance processInstance : processInstances) {
                if (org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE == processInstance.getState().intValue()) {
                    activeInstances.add(processInstance);
                }
            }
            assertThat(activeInstances).hasSize(1);
            returnedProcessInstances.addAll(activeInstances);

            // All returned instances should contain all values
            for (ProcessInstance returnedProcessInstance : returnedProcessInstances) {
                assertThat(returnedProcessInstance).isNotNull();
                assertThat(returnedProcessInstance.getId()).isEqualTo(processInstanceId);
                assertThat(returnedProcessInstance.getProcessId()).isEqualTo(PROCESS_ID_EVALUATION);
                assertThat(returnedProcessInstance.getProcessName()).isEqualTo("evaluation");
                assertThat(returnedProcessInstance.getProcessVersion()).isEqualTo("1.0");
                assertThat(returnedProcessInstance.getInitiator()).isEqualTo(USER_YODA);
                assertThat(returnedProcessInstance.getContainerId()).isEqualTo(CONTAINER_ID);
                assertThat(returnedProcessInstance.getCorrelationKey()).isEqualTo(businessKey);
                assertThat(returnedProcessInstance.getProcessInstanceDescription()).isEqualTo("evaluation");
                assertThat(returnedProcessInstance.getParentId().longValue()).isEqualTo(-1);
                assertThat(returnedProcessInstance.getState().intValue()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);
            }
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testGetProcessInstancesByCorrelationKeySortedById() throws Exception {
        CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

        String firstBusinessKey = "my-simple-key-first";
        String secondBusinessKey = "my-simple-key-second";
        CorrelationKey firstKey = correlationKeyFactory.newCorrelationKey(firstBusinessKey);
        CorrelationKey secondKey = correlationKeyFactory.newCorrelationKey(secondBusinessKey);
        CorrelationKey partKey = correlationKeyFactory.newCorrelationKey("my-simple%");

        Long processInstanceEvalutionId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, firstKey);
        Long processInstanceSignalId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, secondKey);
        try {
            List<ProcessInstance> returnedProcessInstances = queryClient.findProcessInstancesByCorrelationKey(partKey, 0, 100, SORT_BY_INSTANCE_PROCESS_ID, false);
            assertThat(returnedProcessInstances).isNotNull();
            assertProcessInstancesOrderById(returnedProcessInstances, false);

            ProcessInstance returnedSignalProcess = findProcessInstance(returnedProcessInstances, processInstanceSignalId);
            assertThat(returnedSignalProcess.getProcessId()).isEqualTo(PROCESS_ID_SIGNAL_PROCESS);
            assertThat(returnedSignalProcess.getId()).isEqualTo(processInstanceSignalId);
            assertThat(returnedSignalProcess.getCorrelationKey()).isEqualTo(secondBusinessKey);
            ProcessInstance returnedEvaluation = findProcessInstance(returnedProcessInstances, processInstanceEvalutionId);
            assertThat(returnedEvaluation.getProcessId()).isEqualTo(PROCESS_ID_EVALUATION);
            assertThat(returnedEvaluation.getId()).isEqualTo(processInstanceEvalutionId);
            assertThat(returnedEvaluation.getCorrelationKey()).isEqualTo(firstBusinessKey);
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceEvalutionId);
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceSignalId);
        }
    }

    @Test
    public void testGetProcessInstanceByCorrelationKeyPaging() throws Exception {
        CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

        String businessKey = "simple-key";
        CorrelationKey key = correlationKeyFactory.newCorrelationKey(businessKey);

        // Start and abort 2 processes to be sure that there are processes to be returned.
        Map<String, Object> parameters = new HashMap<String, Object>();
        Long processInstanceId1 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, key, parameters);
        processClient.abortProcessInstance(CONTAINER_ID, processInstanceId1);
        Long processInstanceId2 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, key, parameters);
        processClient.abortProcessInstance(CONTAINER_ID, processInstanceId2);

        List<ProcessInstance> processInstancesPage0 = queryClient.findProcessInstancesByCorrelationKey(key, 0, 1);
        List<ProcessInstance> processInstancesPage1 = queryClient.findProcessInstancesByCorrelationKey(key, 1, 1);
        assertThat(processInstancesPage0).hasSize(1);
        assertThat(processInstancesPage1).hasSize(1);
        assertNotEquals("Process instances are same! Paging doesn't work.", processInstancesPage0.get(0).getId(), processInstancesPage1.get(0).getId());
    }

    @Test
    public void testGetProcessInstancesByVariableName() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = queryClient.findProcessInstancesByVariable("stringData", null, 0, 10);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(5);

            List<Long> found = collectInstances(instances);
            assertThat(found).isEqualTo(processInstanceIds);

            instances = queryClient.findProcessInstancesByVariable("stringData", null, 0, 3);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(3);

            instances = queryClient.findProcessInstancesByVariable("stringData", null, 1, 3);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);

        } finally {
            abortProcessInstances(processInstanceIds);
        }

    }

    @Test
    public void testGetProcessInstancesByVariableNameSortedByName() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = queryClient.findProcessInstancesByVariable("stringData", null, 0, 3, SORT_BY_PROCESS_ID, true);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(3);
            for (ProcessInstance instance : instances) {
                assertThat(processInstanceIds.contains(instance.getId())).isTrue();
                assertThat(instance.getProcessId()).isEqualTo(PROCESS_ID_SIGNAL_PROCESS);
            }

            instances = queryClient.findProcessInstancesByVariable("stringData", null, 1, 3, SORT_BY_PROCESS_ID, true);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);
            for (ProcessInstance instance : instances) {
                assertThat(processInstanceIds.contains(instance.getId())).isTrue();
                assertThat(instance.getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
            }

            instances = queryClient.findProcessInstancesByVariable("stringData", null, 0, 10, SORT_BY_PROCESS_ID, false);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(5);
            for (int i = 0; i < instances.size(); i++) {
                if (i < 2) {
                    assertThat(instances.get(i).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
                } else {
                    assertThat(instances.get(i).getProcessId()).isEqualTo(PROCESS_ID_SIGNAL_PROCESS);
                }
            }
        } finally {
            abortProcessInstances(processInstanceIds);
        }
    }

    @Test
    public void testGetProcessInstancesByVariableNameAndValue() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        for (Long processInstanceId : processInstanceIds) {
            processClient.setProcessVariable(CONTAINER_ID, processInstanceId, "stringData", "waiting for signal");
        }

        try {
            List<ProcessInstance> instances = queryClient.findProcessInstancesByVariableAndValue("stringData", "waiting%", null, 0, 50);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(5);

            List<Long> found = collectInstances(instances);
            assertThat(found).isEqualTo(processInstanceIds);

            instances = queryClient.findProcessInstancesByVariableAndValue("stringData", "waiting%", null, 0, 3);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(3);

            instances = queryClient.findProcessInstancesByVariableAndValue("stringData", "waiting%", null, 1, 3);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);

            processClient.setProcessVariable(CONTAINER_ID, processInstanceIds.get(0), "stringData", "updated value");

            instances = queryClient.findProcessInstancesByVariableAndValue("stringData", "waiting%", null, 0, 10);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(4);

            instances = queryClient.findProcessInstancesByVariableAndValue("stringData", "updated value", null, 0, 10);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(1);

        } finally {
            abortProcessInstances(processInstanceIds);
        }

    }

    @Test
    public void testGetProcessInstancesByVariableNameAndValueSortedByName() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        for (Long processInstanceId : processInstanceIds) {
            processClient.setProcessVariable(CONTAINER_ID, processInstanceId, "stringData", "waiting for signal");
        }

        try {
            List status = Arrays.asList(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);
            List<ProcessInstance> instances = queryClient.findProcessInstancesByVariableAndValue("stringData", "waiting%", status, 0, 3, SORT_BY_PROCESS_ID, true);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(3);
            for (ProcessInstance instance : instances) {
                assertThat(processInstanceIds.contains(instance.getId())).isTrue();
                assertThat(instance.getProcessId()).isEqualTo(PROCESS_ID_SIGNAL_PROCESS);
            }

            instances = queryClient.findProcessInstancesByVariableAndValue("stringData", "waiting%", status, 1, 3, SORT_BY_PROCESS_ID, true);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);
            for (ProcessInstance instance : instances) {
                assertThat(processInstanceIds.contains(instance.getId())).isTrue();
                assertThat(instance.getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
            }

            instances = queryClient.findProcessInstancesByVariableAndValue("stringData", "waiting%", status, 0, 10, SORT_BY_PROCESS_ID, false);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(5);
            for (int i = 0; i < instances.size(); i++) {
                if (i < 2) {
                    assertThat(instances.get(i).getProcessId()).isEqualTo(PROCESS_ID_USERTASK);
                } else {
                    assertThat(instances.get(i).getProcessId()).isEqualTo(PROCESS_ID_SIGNAL_PROCESS);
                }
            }
        } finally {
            abortProcessInstances(processInstanceIds);
        }
    }

    @Test
    public void testGetNodeInstances() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {
            List<NodeInstance> instances = queryClient.findActiveNodeInstances(processInstanceId, 0, 10);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(1);

            NodeInstance expectedFirstTask = NodeInstance
                    .builder()
                    .name("First task")
                    .containerId(CONTAINER_ID)
                    .nodeType("HumanTaskNode")
                    .completed(false)
                    .processInstanceId(processInstanceId)
                    .build();

            NodeInstance nodeInstance = instances.get(0);
            assertNodeInstance(expectedFirstTask, nodeInstance);
            assertThat(nodeInstance.getWorkItemId()).isNotNull();
            assertThat(nodeInstance.getDate()).isNotNull();

            nodeInstance = queryClient.findNodeInstanceByWorkItemId(processInstanceId, nodeInstance.getWorkItemId());
            assertNodeInstance(expectedFirstTask, nodeInstance);
            assertThat(nodeInstance.getWorkItemId()).isNotNull();
            assertThat(nodeInstance.getDate()).isNotNull();

            instances = queryClient.findCompletedNodeInstances(processInstanceId, 0, 10);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(1);

            NodeInstance expectedStart = NodeInstance
                    .builder()
                    .name("start")
                    .containerId(CONTAINER_ID)
                    .nodeType("StartNode")
                    .completed(true)
                    .processInstanceId(processInstanceId)
                    .build();

            nodeInstance = instances.get(0);
            assertNodeInstance(expectedStart, nodeInstance);
            assertThat(nodeInstance.getWorkItemId()).isNull();
            assertThat(nodeInstance.getDate()).isNotNull();

            instances = queryClient.findNodeInstances(processInstanceId, 0, 10);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(3);

            nodeInstance = instances.get(0);
            assertNodeInstance(expectedStart, nodeInstance);
            assertThat(nodeInstance.getWorkItemId()).isNull();
            assertThat(nodeInstance.getDate()).isNotNull();

            nodeInstance = instances.get(1);
            assertNodeInstance(expectedFirstTask, nodeInstance);
            assertThat(nodeInstance.getWorkItemId()).isNotNull();
            assertThat(nodeInstance.getDate()).isNotNull();

            nodeInstance = instances.get(2);
            expectedStart.setCompleted(false);
            assertNodeInstance(expectedStart, nodeInstance);
            assertThat(nodeInstance.getWorkItemId()).isNull();
            assertThat(nodeInstance.getDate()).isNotNull();
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testGetVariableInstance() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {

            List<VariableInstance> currentState = queryClient.findVariablesCurrentState(processInstanceId);
            assertThat(currentState).isNotNull();
            assertThat(currentState).hasSize(3);

            for (VariableInstance variableInstance : currentState) {

                if ("personData".equals(variableInstance.getVariableName())) {
                    assertThat(variableInstance).isNotNull();
                    assertThat(variableInstance.getProcessInstanceId()).isEqualTo(processInstanceId);
                    KieServerAssert.assertNullOrEmpty(variableInstance.getOldValue());
                    assertThat(variableInstance.getValue()).isEqualTo("Person{name='john'}");
                    assertThat(variableInstance.getVariableName()).isEqualTo("personData");
                } else if ("stringData".equals(variableInstance.getVariableName())) {
                    assertThat(variableInstance).isNotNull();
                    assertThat(variableInstance.getProcessInstanceId()).isEqualTo(processInstanceId);
                    KieServerAssert.assertNullOrEmpty(variableInstance.getOldValue());
                    assertThat(variableInstance.getValue()).isEqualTo("waiting for signal");
                    assertThat(variableInstance.getVariableName()).isEqualTo("stringData");
                } else if("initiator".equals(variableInstance.getVariableName())){
                    assertThat(variableInstance).isNotNull();
                    assertThat(variableInstance.getProcessInstanceId()).isEqualTo(processInstanceId);
                    assertThat(variableInstance.getValue()).isEqualTo(TestConfig.getUsername());
                    KieServerAssert.assertNullOrEmpty(variableInstance.getOldValue());
                } else {
                    fail("Got unexpected variable " + variableInstance.getVariableName());
                }
            }

            List<VariableInstance> varHistory = queryClient.findVariableHistory(processInstanceId, "stringData", 0, 10);
            assertThat(varHistory).isNotNull();
            assertThat(varHistory).hasSize(1);

            VariableInstance variableInstance = varHistory.get(0);
            assertThat(variableInstance).isNotNull();
            assertThat(variableInstance.getProcessInstanceId()).isEqualTo(processInstanceId);
            KieServerAssert.assertNullOrEmpty(variableInstance.getOldValue());
            assertThat(variableInstance.getValue()).isEqualTo("waiting for signal");
            assertThat(variableInstance.getVariableName()).isEqualTo("stringData");

            processClient.setProcessVariable(CONTAINER_ID, processInstanceId, "stringData", "updated value");

            currentState = queryClient.findVariablesCurrentState(processInstanceId);
            assertThat(currentState).isNotNull();
            assertThat(currentState).hasSize(3);

            for (VariableInstance variable : currentState) {
                if ("personData".equals(variable.getVariableName())) {
                    assertThat(variable).isNotNull();
                    assertThat(variable.getProcessInstanceId()).isEqualTo(processInstanceId);
                    KieServerAssert.assertNullOrEmpty(variable.getOldValue());
                    assertThat(variable.getValue()).isEqualTo("Person{name='john'}");
                    assertThat(variable.getVariableName()).isEqualTo("personData");
                } else if ("stringData".equals(variable.getVariableName())) {
                    assertThat(variable).isNotNull();
                    assertThat(variable.getProcessInstanceId()).isEqualTo(processInstanceId);
                    assertThat(variable.getOldValue()).isEqualTo("waiting for signal");
                    assertThat(variable.getValue()).isEqualTo("updated value");
                    assertThat(variable.getVariableName()).isEqualTo("stringData");
                } else if("initiator".equals(variable.getVariableName())){
                    assertThat(variable).isNotNull();
                    assertThat(variable.getProcessInstanceId()).isEqualTo(processInstanceId);
                    assertThat(variable.getValue()).isEqualTo(TestConfig.getUsername());
                    KieServerAssert.assertNullOrEmpty(variable.getOldValue());
                } else {
                    fail("Got unexpected variable " + variable.getVariableName());
                }
            }

            varHistory = queryClient.findVariableHistory(processInstanceId, "stringData", 0, 10);
            assertThat(varHistory).isNotNull();
            assertThat(varHistory).hasSize(2);

            variableInstance = varHistory.get(0);
            assertThat(variableInstance).isNotNull();
            assertThat(variableInstance.getProcessInstanceId()).isEqualTo(processInstanceId);
            assertThat(variableInstance.getOldValue()).isEqualTo("waiting for signal");
            assertThat(variableInstance.getValue()).isEqualTo("updated value");
            assertThat(variableInstance.getVariableName()).isEqualTo("stringData");

            variableInstance = varHistory.get(1);
            assertThat(variableInstance).isNotNull();
            assertThat(variableInstance.getProcessInstanceId()).isEqualTo(processInstanceId);
            KieServerAssert.assertNullOrEmpty(variableInstance.getOldValue());
            assertThat(variableInstance.getValue()).isEqualTo("waiting for signal");
            assertThat(variableInstance.getVariableName()).isEqualTo("stringData");

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }

    @Test (expected = KieServicesException.class)
    public void testNotExistingUserTaskFindByWorkItemId() throws Exception {
        taskClient.findTaskByWorkItemId(-9999l);
    }

    @Test (expected = KieServicesException.class)
    public void testNotExistingUserTaskFindById() throws Exception {
        taskClient.findTaskById(-9999l);
    }


    @Test
    public void testFindTasks() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {

            List<TaskSummary> tasks = taskClient.findTasks(USER_YODA, 0, 50);
            assertThat(tasks).isNotNull();

            TaskSummary taskSummary = null;
            for (TaskSummary t : tasks) {
                if (t.getProcessInstanceId().equals(processInstanceId)) {
                    taskSummary = t;
                    break;
                }
            }

            TaskSummary expectedTaskSummary = createDefaultTaskSummary(processInstanceId);

            assertTaskSummary(expectedTaskSummary, taskSummary);

            TaskInstance expecteTaskInstace = TaskInstance
                    .builder()
                    .name("First task")
                    .status(Status.Reserved.toString())
                    .priority(0)
                    .actualOwner(USER_YODA)
                    .createdBy(USER_YODA)
                    .processId(PROCESS_ID_USERTASK)
                    .containerId(CONTAINER_ID)
                    .processInstanceId(processInstanceId)
                    .build();

            TaskInstance taskById = taskClient.findTaskById(taskSummary.getId());
            assertTaskInstace(expecteTaskInstace, taskById);

            List<WorkItemInstance> workItems = processClient.getWorkItemByProcessInstance(CONTAINER_ID, processInstanceId);
            assertThat(workItems).isNotNull();
            assertThat(workItems).hasSize(1);

            taskById = taskClient.findTaskByWorkItemId(workItems.get(0).getId());
            assertTaskInstace(expecteTaskInstace, taskById);
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testFindTasksSortedByProcessInstanceId() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);
        Long processInstanceId2 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {
            List<TaskSummary> tasks = taskClient.findTasks(USER_YODA, 0, 50, "processInstanceId", false);
            assertThat(tasks).isNotNull();

            //latest task is from second process
            TaskSummary task = tasks.get(0);
            TaskSummary expectedTaskSummary;
            if (processInstanceId2 > processInstanceId) {
                expectedTaskSummary = createDefaultTaskSummary(processInstanceId2);
            } else {
                expectedTaskSummary = createDefaultTaskSummary(processInstanceId);
            }
            assertTaskSummary(expectedTaskSummary, task);

            task = tasks.get(1);
            expectedTaskSummary = null;
            if (processInstanceId2 > processInstanceId) {
                expectedTaskSummary = createDefaultTaskSummary(processInstanceId);
            } else {
                expectedTaskSummary = createDefaultTaskSummary(processInstanceId2);
            }
            assertTaskSummary(expectedTaskSummary, task);
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId2);
        }
    }

    @Test
    public void testFindTaskEvents() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {

            List<TaskSummary> tasks = taskClient.findTasksByStatusByProcessInstanceId(processInstanceId, null, 0, 10);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(1);

            TaskSummary taskInstance = tasks.get(0);

            List<TaskEventInstance> events = taskClient.findTaskEvents(taskInstance.getId(), 0, 10);
            assertThat(events).isNotNull();
            assertThat(events).hasSize(1);

            TaskEventInstance expectedTaskEventInstance = TaskEventInstance
                    .builder()
                    .type(TaskEvent.TaskEventType.ADDED.toString())
                    .processInstanceId(processInstanceId)
                    .taskId(taskInstance.getId())
                    .user(PROCESS_ID_USERTASK)      // is this really correct to set process id as user for added task
                    .build();

            TaskEventInstance event = events.get(0);
            assertTaskEventInstance(expectedTaskEventInstance, event);
            //assertThat(event.getUserId()).isEqualTo(PROCESS_ID_USERTASK);   // is this really correct to set process id as user for added task

            // now let's start it
            taskClient.startTask(CONTAINER_ID, taskInstance.getId(), USER_YODA);
            events = taskClient.findTaskEvents(taskInstance.getId(), 0, 10);
            assertThat(events).isNotNull();
            assertThat(events).hasSize(2);

            event = getTaskEventInstanceFromListByType(events, TaskEvent.TaskEventType.ADDED.toString());
            assertTaskEventInstance(expectedTaskEventInstance, event);

            event = getTaskEventInstanceFromListByType(events, TaskEvent.TaskEventType.STARTED.toString());
            expectedTaskEventInstance.setType(TaskEvent.TaskEventType.STARTED.toString());
            expectedTaskEventInstance.setUserId(USER_YODA);
            assertTaskEventInstance(expectedTaskEventInstance, event);

            // now let's stop it
            taskClient.stopTask(CONTAINER_ID, taskInstance.getId(), USER_YODA);

            events = taskClient.findTaskEvents(taskInstance.getId(), 0, 10);
            assertThat(events).isNotNull();
            assertThat(events).hasSize(3);

            event = getTaskEventInstanceFromListByType(events, TaskEvent.TaskEventType.ADDED.toString());
            expectedTaskEventInstance.setType(TaskEvent.TaskEventType.ADDED.toString());
            expectedTaskEventInstance.setUserId(PROCESS_ID_USERTASK);  // is this really correct to set process id as user for added task
            assertTaskEventInstance(expectedTaskEventInstance, event);

            event = getTaskEventInstanceFromListByType(events, TaskEvent.TaskEventType.STARTED.toString());
            expectedTaskEventInstance.setType(TaskEvent.TaskEventType.STARTED.toString());
            expectedTaskEventInstance.setUserId(USER_YODA);
            assertTaskEventInstance(expectedTaskEventInstance, event);

            event = getTaskEventInstanceFromListByType(events, TaskEvent.TaskEventType.STOPPED.toString());
            expectedTaskEventInstance.setType(TaskEvent.TaskEventType.STOPPED.toString());
            assertTaskEventInstance(expectedTaskEventInstance, event);
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    private TaskEventInstance getTaskEventInstanceFromListByType(List<TaskEventInstance> events, String type) {
        for (TaskEventInstance t : events) {
            if (t.getType().equals(type)) {
                return t;
            }
        }
        return null;
    }

    @Test
    public void testFindTaskEventsSortedByType() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {

            List<TaskSummary> tasks = taskClient.findTasksByStatusByProcessInstanceId(processInstanceId, null, 0, 10);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(1);

            TaskSummary taskInstance = tasks.get(0);

            // now let's start it
            taskClient.startTask(CONTAINER_ID, taskInstance.getId(), USER_YODA);

            // now let's stop it
            taskClient.stopTask(CONTAINER_ID, taskInstance.getId(), USER_YODA);

            // test paging of the result
            List<TaskEventInstance> events = taskClient.findTaskEvents(taskInstance.getId(), 0, 3, SORT_BY_TASK_EVENTS_TYPE, true);

            assertThat(events).isNotNull();
            assertThat(events).hasSize(3);

            TaskEventInstance event = events.get(0);
            assertThat(event).isNotNull();
            assertThat(event.getTaskId()).isEqualTo(taskInstance.getId());
            assertThat(event.getType()).isEqualTo(TaskEvent.TaskEventType.ADDED.toString());

            event = events.get(1);
            assertThat(event).isNotNull();
            assertThat(event.getTaskId()).isEqualTo(taskInstance.getId());
            assertThat(event.getType()).isEqualTo(TaskEvent.TaskEventType.STARTED.toString());

            event = events.get(2);
            assertThat(event).isNotNull();
            assertThat(event.getTaskId()).isEqualTo(taskInstance.getId());
            assertThat(event.getType()).isEqualTo(TaskEvent.TaskEventType.STOPPED.toString());

            try {
                events = taskClient.findTaskEvents(taskInstance.getId(), 1, 3, SORT_BY_TASK_EVENTS_TYPE, true);
                KieServerAssert.assertNullOrEmpty("Task events list is not empty.", events);
            } catch (TaskNotFoundException e) {
                assertThat(e.getMessage().contains( "No task found with id " + taskInstance.getId() )).isTrue();
            } catch (KieServicesException ee) {
                if(configuration.isRest()) {
                    KieServerAssert.assertResultContainsString(ee.getMessage(), "Could not find task instance with id " + taskInstance.getId());
                    KieServicesHttpException httpEx = (KieServicesHttpException) ee;
                    assertThat(httpEx.getHttpCode()).isEqualTo(Integer.valueOf(404));
                } else {
                    assertThat(ee.getMessage().contains( "No task found with id " + taskInstance.getId() )).isTrue();
                }
            }

            events = taskClient.findTaskEvents(taskInstance.getId(), 0, 10, SORT_BY_TASK_EVENTS_TYPE, false);
            assertThat(events).isNotNull();
            assertThat(events).hasSize(3);

            event = events.get(0);
            assertThat(event).isNotNull();
            assertThat(event.getTaskId()).isEqualTo(taskInstance.getId());
            assertThat(event.getType()).isEqualTo(TaskEvent.TaskEventType.STOPPED.toString());

            event = events.get(1);
            assertThat(event).isNotNull();
            assertThat(event.getTaskId()).isEqualTo(taskInstance.getId());
            assertThat(event.getType()).isEqualTo(TaskEvent.TaskEventType.STARTED.toString());

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testFindTasksOwned() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {

            List<TaskSummary> tasks = taskClient.findTasksOwned(USER_YODA, 0, 10);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(1);

            TaskSummary expectedTaskSummary = createDefaultTaskSummary(processInstanceId);

            TaskSummary taskInstance = tasks.get(0);
            assertTaskSummary(expectedTaskSummary, taskInstance);

            List<String> status = new ArrayList<String>();
            status.add(Status.InProgress.toString());

            tasks = taskClient.findTasksOwned(USER_YODA, status, 0, 10);
            assertThat(tasks).isNotNull();
            assertThat(tasks).isEmpty();

            taskClient.startTask(CONTAINER_ID, taskInstance.getId(), USER_YODA);
            tasks = taskClient.findTasksOwned(USER_YODA, status, 0, 10);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(1);

            taskInstance = tasks.get(0);
            expectedTaskSummary.setStatus(Status.InProgress.toString());
            assertTaskSummary(expectedTaskSummary, taskInstance);
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testFindTasksOwnedSortedByStatus() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);
        Long processInstanceId2 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {
            List<TaskSummary> tasks = taskClient.findTasksOwned(USER_YODA, 0, 10, SORT_BY_TASK_STATUS, true);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(2);
            Long someTaskId = tasks.get(0).getId();

            taskClient.startTask(CONTAINER_ID, someTaskId, USER_YODA);
            tasks = taskClient.findTasksOwned(USER_YODA, 0, 10, SORT_BY_TASK_STATUS, true);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(2);
            assertThat(tasks.get(0).getStatus()).isEqualTo(Status.InProgress.toString());
            assertThat(tasks.get(1).getStatus()).isEqualTo(Status.Reserved.toString());

            tasks = taskClient.findTasksOwned(USER_YODA, 0, 10, SORT_BY_TASK_STATUS, false);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(2);
            assertThat(tasks.get(0).getStatus()).isEqualTo(Status.Reserved.toString());
            assertThat(tasks.get(1).getStatus()).isEqualTo(Status.InProgress.toString());

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId2);
        }
    }

    @Test
    public void testFindTasksAssignedAsPotentialOwner() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {

            List<TaskSummary> tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(1);

            TaskSummary expectedTaskSummary = createDefaultTaskSummary(processInstanceId);

            TaskSummary taskInstance = tasks.get(0);
            assertTaskSummary(expectedTaskSummary, taskInstance);

            List<String> status = new ArrayList<String>();
            status.add(Status.InProgress.toString());

            tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, status, 0, 10);
            assertThat(tasks).isNotNull();
            assertThat(tasks).isEmpty();

            taskClient.startTask(CONTAINER_ID, taskInstance.getId(), USER_YODA);
            tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, status, 0, 10);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(1);

            taskInstance = tasks.get(0);
            expectedTaskSummary.setStatus(Status.InProgress.toString());
            assertTaskSummary(expectedTaskSummary, taskInstance);

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testFindTasksAssignedAsPotentialOwnerSortedByStatus() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);
        Long processInstanceId2 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {
            List<TaskSummary> tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10, SORT_BY_TASK_STATUS, true);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(2);
            Long someTaskId = tasks.get(0).getId();

            taskClient.startTask(CONTAINER_ID, someTaskId, USER_YODA);

            tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10, SORT_BY_TASK_STATUS, true);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(2);
            assertThat(tasks.get(0).getStatus()).isEqualTo(Status.InProgress.toString());
            assertThat(tasks.get(1).getStatus()).isEqualTo(Status.Reserved.toString());

            tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10, SORT_BY_TASK_STATUS, false);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(2);
            assertThat(tasks.get(0).getStatus()).isEqualTo(Status.Reserved.toString());
            assertThat(tasks.get(1).getStatus()).isEqualTo(Status.InProgress.toString());
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId2);
        }
    }

    @Test
    public void testFindTasksByStatusByProcessInstanceId() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {
            List<String> status = new ArrayList<String>();
            status.add(Status.Reserved.toString());
            status.add(Status.InProgress.toString());

            List<TaskSummary> tasks = taskClient.findTasksByStatusByProcessInstanceId(processInstanceId, status, 0, 10);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(1);

            TaskSummary expectedTaskSummary = createDefaultTaskSummary(processInstanceId);

            TaskSummary taskInstance = tasks.get(0);
            assertTaskSummary(expectedTaskSummary, taskInstance);

            status = new ArrayList<String>();
            status.add(Status.InProgress.toString());

            tasks = taskClient.findTasksByStatusByProcessInstanceId(processInstanceId, status, 0, 10);
            assertThat(tasks).isNotNull();
            assertThat(tasks).isEmpty();

            taskClient.startTask(CONTAINER_ID, taskInstance.getId(), USER_YODA);
            tasks = taskClient.findTasksByStatusByProcessInstanceId(processInstanceId, status, 0, 10);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(1);

            taskInstance = tasks.get(0);
            expectedTaskSummary.setStatus(Status.InProgress.toString());
            assertTaskSummary(expectedTaskSummary, taskInstance);

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testFindTasksByStatusByProcessInstanceIdMissingParam() throws Exception {
        // only applicable for REST
        assumeFalse(configuration.isJms());
        try {
            taskClient.findTasksByStatusByProcessInstanceId(null, Arrays.asList("Ready"), 0, 10);
            fail("Method should throw missing InstanceId exception");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Missing value for pInstanceId");
        }

    }

    @Test
    public void testFindTasksWithNameFilter() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {

            List<TaskSummary> tasks = taskClient.findTasksByStatusByProcessInstanceId(processInstanceId, null, 0, 10);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(1);

            tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, "First%", null, 0, 10);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(1);

            tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, "First%", null, 0, 10, "Status", false);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(1);

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testFindTaskEventsForNotExistingTask() {
        String invalidId = "-9,999";
        try {
            taskClient.findTaskEvents(-9999l, 0, 10);
            fail("KieServicesException should be thrown when task not found");
        } catch (KieServicesException e) {
            if(configuration.isRest()) {
                KieServerAssert.assertResultContainsString(e.getMessage(), "Could not find task instance with id \"" + invalidId + "\"");
                KieServicesHttpException httpEx = (KieServicesHttpException) e;
                assertThat(httpEx.getHttpCode()).isEqualTo(Integer.valueOf(404));
            } else {
                assertThat(e.getMessage().contains("No task found with id " + invalidId)).isTrue();
            }
        } catch (TaskNotFoundException tnfe) {
            assertThat(tnfe.getMessage().contains("No task found with id " + invalidId)).isTrue();
        }
    }


    private void checkProcessDefinitions(List<String> processIds) {
        assertThat(processIds.contains(PROCESS_ID_CALL_EVALUATION)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_EVALUATION)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_GROUPTASK)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_SIGNAL_PROCESS)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_USERTASK)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_CUSTOM_TASK)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_SIGNAL_START)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_ASYNC_SCRIPT)).isTrue();
        assertThat(processIds.contains(PROCESS_ID_TIMER)).isTrue();
    }

    private void assertNodeInstance(NodeInstance expected, NodeInstance actual) {
        assertThat(actual).isNotNull();
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getContainerId()).isEqualTo(expected.getContainerId());
        assertThat(actual.getNodeType()).isEqualTo(expected.getNodeType());
        assertThat(actual.getCompleted()).isEqualTo(expected.getCompleted());
        assertThat(actual.getProcessInstanceId()).isEqualTo(expected.getProcessInstanceId());
    }

    private void assertTaskSummary(TaskSummary expected, TaskSummary actual) {
        assertThat(actual).isNotNull();
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getProcessId()).isEqualTo(expected.getProcessId());
        KieServerAssert.assertNullOrEmpty(actual.getDescription());
        assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
        assertThat(actual.getPriority()).isEqualTo(expected.getPriority());
        assertThat(actual.getActualOwner()).isEqualTo(expected.getActualOwner());
        assertThat(actual.getCreatedBy()).isEqualTo(expected.getCreatedBy());
        assertThat(actual.getContainerId()).isEqualTo(expected.getContainerId());
        assertThat(actual.getParentId()).isEqualTo(expected.getParentId());
        assertThat(actual.getProcessInstanceId()).isEqualTo(expected.getProcessInstanceId());
    }

    private void assertTaskInstace(TaskInstance expected, TaskInstance actual) {
        assertThat(actual).isNotNull();
        assertThat(actual.getName()).isEqualTo(expected.getName());
        KieServerAssert.assertNullOrEmpty(actual.getDescription());
        assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
        assertThat(actual.getPriority()).isEqualTo(expected.getPriority());
        assertThat(actual.getActualOwner()).isEqualTo(expected.getActualOwner());
        assertThat(actual.getCreatedBy()).isEqualTo(expected.getCreatedBy());
        assertThat(actual.getProcessId()).isEqualTo(expected.getProcessId());
        assertThat(actual.getContainerId()).isEqualTo(expected.getContainerId());
        assertThat(actual.getProcessInstanceId()).isEqualTo(expected.getProcessInstanceId());
    }

    private void assertTaskEventInstance(TaskEventInstance expected, TaskEventInstance actual) {
        assertThat(actual).isNotNull();
        assertThat(actual.getType()).isEqualTo(expected.getType());
        assertThat(actual.getProcessInstanceId()).isEqualTo(expected.getProcessInstanceId());
        assertThat(actual.getTaskId()).isEqualTo(expected.getTaskId());
        assertThat(actual.getUserId()).isEqualTo(expected.getUserId());
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getLogTime()).isNotNull();
        assertThat(actual.getWorkItemId()).isNotNull();
    }

    private void assertProcessInstancesOrderById(List<ProcessInstance> processInstances, boolean ascending) {
        List<Long> pids = collectInstances(processInstances);
        List<Long> sortedPids = new ArrayList<Long>(pids);

        if(ascending) {
            Collections.sort(sortedPids);
        } else {
            sortedPids.sort(Collections.reverseOrder());
        }

        assertThat("Processes are returned in wrong order!", pids, is(sortedPids));
    }

    private TaskSummary createDefaultTaskSummary(long processInstanceId) {
        return TaskSummary
                .builder()
                .name("First task")
                .status(Status.Reserved.toString())
                .priority(0)
                .actualOwner(USER_YODA)
                .createdBy(USER_YODA)
                .processId(PROCESS_ID_USERTASK)
                .containerId(CONTAINER_ID)
                .taskParentId(-1l)
                .processInstanceId(processInstanceId)
                .build();
    }


    protected List<Long> createProcessInstances(Map<String, Object> parameters) {
        List<Long> processInstanceIds = new ArrayList<Long>();

        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters));

        Collections.sort(processInstanceIds);
        return processInstanceIds;
    }

    protected void abortProcessInstances(List<Long> processInstanceIds) {
        for (Long piId : processInstanceIds) {
            processClient.abortProcessInstance(CONTAINER_ID, piId);
        }
    }

    protected List<String> collectDefinitions(List<ProcessDefinition> definitions) {
        List<String> ids = new ArrayList<String>();

        for (ProcessDefinition definition : definitions) {
            ids.add(definition.getId());
        }
        return ids;
    }

    protected List<Long> collectInstances(List<ProcessInstance> instances) {
        List<Long> ids = new ArrayList<Long>();

        for (ProcessInstance instance : instances) {
            ids.add(instance.getId());
        }
        return ids;
    }

    private ProcessInstance findProcessInstance(List<ProcessInstance> processInstances, Long pid) {
        return processInstances.stream().filter(n -> n.getId().equals(pid)).findFirst().orElseThrow(() -> new RuntimeException("Process instance with id " + pid + " not found."));
    }
}
