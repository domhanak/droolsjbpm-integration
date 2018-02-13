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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.api.KieServices;
import org.kie.api.task.model.Status;
import org.kie.internal.KieInternalServices;
import org.kie.internal.executor.api.STATUS;
import org.kie.internal.process.CorrelationKey;
import org.kie.internal.process.CorrelationKeyFactory;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.NodeInstance;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.RequestInfoInstance;
import org.kie.server.api.model.instance.TaskInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.api.model.instance.WorkItemInstance;
import org.kie.server.api.exception.KieServicesException;
import org.kie.server.integrationtests.category.Smoke;
import org.kie.server.integrationtests.config.TestConfig;

import static org.assertj.core.api.Assertions.*;
import org.kie.server.api.model.instance.VariableInstance;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.kie.server.integrationtests.shared.KieServerReflections;
import org.kie.server.integrationtests.shared.KieServerSynchronization;


public class ProcessServiceIntegrationTest extends JbpmKieServerBaseIntegrationTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");

    protected static final String SORT_BY_PROCESS_ID = "ProcessId";

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
    public void testStartCheckVariablesAndAbortProcess() throws Exception {
        Class<?> personClass = Class.forName(PERSON_CLASS_NAME, true, kieContainer.getClassLoader());

        Object person = createPersonInstance(USER_JOHN);

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("test", USER_MARY);
        parameters.put("number", new Integer(12345));

        List<Object> list = new ArrayList<Object>();
        list.add("item");

        parameters.put("list", list);
        parameters.put("person", person);
        Long processInstanceId = null;
        try {
            processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);

            assertThat(processInstanceId).isNotNull();
            assertThat(processInstanceId.longValue() > 0).isTrue();

            Object personVariable = processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "person");
            assertThat(personVariable).isNotNull();
            assertThat(personClass.isAssignableFrom(personVariable.getClass())).isTrue();

            personVariable = processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "person");
            assertThat(personVariable).isNotNull();
            assertThat(personClass.isAssignableFrom(personVariable.getClass())).isTrue();

            Map<String, Object> variables = processClient.getProcessInstanceVariables(CONTAINER_ID, processInstanceId);
            assertThat(variables).isNotNull();
            assertThat(variables).hasSize(5);
            assertThat(variables.containsKey("test")).isTrue();
            assertThat(variables.containsKey("number")).isTrue();
            assertThat(variables.containsKey("list")).isTrue();
            assertThat(variables.containsKey("person")).isTrue();
            assertThat(variables.containsKey("initiator")).isTrue();

            assertThat(variables.get("test")).isNotNull();
            assertThat(variables.get("number")).isNotNull();
            assertThat(variables.get("list")).isNotNull();
            assertThat(variables.get("person")).isNotNull();
            assertThat(variables.get("initiator")).isNotNull();

            assertThat(String.class.isAssignableFrom(variables.get("test").getClass())).isTrue();
            assertThat(Integer.class.isAssignableFrom(variables.get("number").getClass())).isTrue();
            assertThat(List.class.isAssignableFrom(variables.get("list").getClass())).isTrue();
            assertThat(personClass.isAssignableFrom(variables.get("person").getClass())).isTrue();
            assertThat(String.class.isAssignableFrom(variables.get("initiator").getClass())).isTrue();

            assertThat(variables.get("test")).isEqualTo(USER_MARY);
            assertThat(variables.get("number")).isEqualTo(12345);
            assertThat(((List) variables.get("list"))).hasSize(1);
            assertThat(((List) variables.get("list")).get(0)).isEqualTo("item");
            assertThat("name")).isEqualTo(USER_JOHN, KieServerReflections.valueOf(variables.get("person"));
            assertThat(variables.get("initiator")).isEqualTo(TestConfig.getUsername());
        } finally {
            if (processInstanceId != null) {
                processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            }
        }



    }

    @Test(expected = KieServicesException.class)
    public void testStartNotExistingProcess() {
        processClient.startProcess(CONTAINER_ID, "not-existing", (Map)null);
    }

    @Test()
    public void testAbortExistingProcess() {
        Map<String, Object> parameters = new HashMap<String, Object>();

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        try {
            assertThat(processInstanceId).isNotNull();
            assertThat(processInstanceId.longValue() > 0).isTrue();

            // Process instance is running and is active.
            ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getState().intValue()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);

            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);

            // Process instance is now aborted.
            processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getState().intValue()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED);
        } catch (Exception e) {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }
    }

    @Test(expected = KieServicesException.class)
    public void testAbortNonExistingProcess() {
        processClient.abortProcessInstance(CONTAINER_ID, 9999l);
    }

    @Test(expected = KieServicesException.class)
    public void testStartCheckNonExistingVariables() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        try {
            processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "person");
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }

    @Test
    public void testAbortMultipleProcessInstances() throws Exception {

        Long processInstanceId1 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION);
        Long processInstanceId2 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION);
        Long processInstanceId3 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION);
        Long processInstanceId4 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION);

        List<Long> processInstances = new ArrayList<Long>();
        processInstances.add(processInstanceId1);
        processInstances.add(processInstanceId2);
        processInstances.add(processInstanceId3);
        processInstances.add(processInstanceId4);

        processClient.abortProcessInstances(CONTAINER_ID, processInstances);
    }

    @Test
    public void testSignalProcessInstance() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId.longValue() > 0).isTrue();

        try {
            checkAvailableSignals(CONTAINER_ID, processInstanceId);

            Object person = createPersonInstance(USER_JOHN);
            processClient.signalProcessInstance(CONTAINER_ID, processInstanceId, "Signal1", person);

            processClient.signalProcessInstance(CONTAINER_ID, processInstanceId, "Signal2", "My custom string event");
        } catch (Exception e){
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    @Test
    public void testSignalProcessInstanceNullEvent() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);
        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId.longValue() > 0).isTrue();

        try {
            checkAvailableSignals(CONTAINER_ID, processInstanceId);

            processClient.signalProcessInstance(CONTAINER_ID, processInstanceId, "Signal1", null);

            processClient.signalProcessInstance(CONTAINER_ID, processInstanceId, "Signal2", null);
        } catch (Exception e){
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    @Test
    public void testSignalProcessInstances() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId.longValue() > 0).isTrue();

        Long processInstanceId2 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);
        assertThat(processInstanceId2).isNotNull();
        assertThat(processInstanceId2.longValue() > 0).isTrue();

        List<Long> processInstanceIds = new ArrayList<Long>();
        processInstanceIds.add(processInstanceId);
        processInstanceIds.add(processInstanceId2);

        try {
            checkAvailableSignals(CONTAINER_ID, processInstanceId);
            checkAvailableSignals(CONTAINER_ID, processInstanceId2);

            Object person = createPersonInstance(USER_JOHN);
            processClient.signalProcessInstances(CONTAINER_ID, processInstanceIds, "Signal1", person);

            processClient.signalProcessInstances(CONTAINER_ID, processInstanceIds, "Signal2", "My custom string event");
        } catch (Exception e){
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId2);
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    @Test
    public void testManipulateProcessVariable() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId.longValue() > 0).isTrue();

        try {
            Object personVar = null;
            try {
                personVar = processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "personData");
                fail("Should fail as there is no process variable personData set yet");
            } catch (KieServicesException e) {
                // expected
            }
            assertThat(personVar).isNull();

            personVar = createPersonInstance(USER_JOHN);
            processClient.setProcessVariable(CONTAINER_ID, processInstanceId, "personData", personVar);

            personVar = processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "personData");
            assertThat(personVar).isNotNull();
            assertThat("name")).isEqualTo(USER_JOHN, KieServerReflections.valueOf(personVar);


            processClient.setProcessVariable(CONTAINER_ID, processInstanceId, "stringData", "custom value");

            String stringVar = (String) processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "stringData");
            assertThat(personVar).isNotNull();
            assertThat(stringVar).isEqualTo("custom value");

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }

    @Test
    public void testManipulateProcessVariables() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);
        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId.longValue() > 0).isTrue();

        try {
            Object personVar = null;
            String stringVar = null;
            try {
                personVar = processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "personData");
                fail("Should fail as there is no process variable personData set yet");
            } catch (KieServicesException e) {
                // expected
            }
            assertThat(personVar).isNull();

            try {
                stringVar = (String) processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "stringData");
                fail("Should fail as there is no process variable personData set yet");
            } catch (KieServicesException e) {
                // expected
            }
            assertThat(personVar).isNull();
            assertThat(stringVar).isNull();

            personVar = createPersonInstance(USER_JOHN);
            stringVar = "string variable test";

            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("personData", personVar);
            variables.put("stringData", stringVar);

            processClient.setProcessVariables(CONTAINER_ID, processInstanceId, variables);

            personVar = processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "personData");
            assertThat(personVar).isNotNull();
            assertThat("name")).isEqualTo(USER_JOHN, KieServerReflections.valueOf(personVar);

            stringVar = (String) processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "stringData");
            assertThat(personVar).isNotNull();
            assertThat(stringVar).isEqualTo("string variable test");

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }

    @Test
    @Category(Smoke.class)
    public void testGetProcessInstance() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId.longValue() > 0).isTrue();

        try {
            ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            ProcessInstance expectedInstance = createSignalProcessInstance(processInstanceId);
            assertProcessInstance(expectedInstance, processInstance);

            Map<String, Object> variables = processInstance.getVariables();
            assertThat(variables).isNull();
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }

    @Test
    public void testGetProcessInstanceWithVariables() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters);
        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId.longValue() > 0).isTrue();

        try {
            ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId, true);
            ProcessInstance expectedInstance = createSignalProcessInstance(processInstanceId);
            assertProcessInstance(expectedInstance, processInstance);

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
            assertThat("name")).isEqualTo(USER_JOHN, KieServerReflections.valueOf(personVar);

            assertThat(personVar).isNotNull();
            assertThat(stringVar).isEqualTo("waiting for signal");

            assertThat(initiator).isNotNull();
            assertThat(initiator).isEqualTo(TestConfig.getUsername());

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }

    @Test(expected = KieServicesException.class)
    public void testGetNonExistingProcessInstance() {
        processClient.getProcessInstance(CONTAINER_ID, 9999l);
    }

    @Test
    public void testWorkItemOperations() throws Exception {

        Map<String, Object> parameters = new HashMap<String, Object>();

        parameters.put("person", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        try {
            assertThat(processInstanceId).isNotNull();
            assertThat(processInstanceId.longValue() > 0).isTrue();

            // Completing human task so we can move in process flow to work item.
            // User task shouldn't be handled as work item because in such case it doesn't behave consistently:
            // i.e. leaving open tasks after finishing process instance.
            List<String> status = Arrays.asList(Status.Ready.toString());
            List<TaskSummary> taskList = taskClient.findTasksByStatusByProcessInstanceId(processInstanceId, status, 0, 10);

            assertThat(taskList).hasSize(1);
            TaskSummary taskSummary = taskList.get(0);
            taskClient.startTask(CONTAINER_ID, taskSummary.getId(), USER_YODA);
            taskClient.completeTask(CONTAINER_ID, taskSummary.getId(), USER_YODA, null);

            TaskInstance userTask = taskClient.findTaskById(taskSummary.getId());
            assertThat(userTask).isNotNull();
            assertThat(userTask.getName()).isEqualTo("Evaluate items?");
            assertThat(userTask.getStatus()).isEqualTo(Status.Completed.toString());

            List<WorkItemInstance> workItems = processClient.getWorkItemByProcessInstance(CONTAINER_ID, processInstanceId);
            assertThat(workItems).isNotNull();
            assertThat(workItems).hasSize(1);

            WorkItemInstance workItemInstance = workItems.get(0);
            assertThat(workItemInstance).isNotNull();
            assertThat(workItemInstance.getProcessInstanceId()).isEqualTo(processInstanceId);
            assertThat(workItemInstance.getName()).isEqualTo("Email");
            assertThat(workItemInstance.getContainerId()).isEqualTo(CONTAINER_ID);
            assertThat(workItemInstance.getState().intValue()).isEqualTo(0);
            assertThat(workItemInstance.getParameters()).hasSize(5);

            assertThat(workItemInstance.getId()).isNotNull();
            assertThat(workItemInstance.getNodeId()).isNotNull();
            assertThat(workItemInstance.getProcessInstanceId()).isNotNull();


            workItemInstance = processClient.getWorkItem(CONTAINER_ID, processInstanceId, workItemInstance.getId());
            assertThat(workItemInstance).isNotNull();
            assertThat(workItemInstance.getProcessInstanceId()).isEqualTo(processInstanceId);
            assertThat(workItemInstance.getName()).isEqualTo("Email");
            assertThat(workItemInstance.getContainerId()).isEqualTo(CONTAINER_ID);
            assertThat(workItemInstance.getState().intValue()).isEqualTo(0);
            assertThat(workItemInstance.getParameters()).hasSize(5);

            assertThat(workItemInstance.getId()).isNotNull();
            assertThat(workItemInstance.getNodeId()).isNotNull();
            assertThat(workItemInstance.getProcessInstanceId()).isNotNull();

            processClient.abortWorkItem(CONTAINER_ID, processInstanceId, workItemInstance.getId());

            ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getState().intValue()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);

        } catch (Exception e){
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            throw e;
        } finally {
            changeUser(TestConfig.getUsername());
        }
    }

    @Test
    public void testWorkItemOperationComplete() throws Exception {

        Map<String, Object> parameters = new HashMap<String, Object>();

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        try {
            assertThat(processInstanceId).isNotNull();
            assertThat(processInstanceId.longValue() > 0).isTrue();

            // Completing human task so we can move in process flow to work item.
            // User task shouldn't be handled as work item because in such case it doesn't behave consistently:
            // i.e. leaving open tasks after finishing process instance.
            List<String> status = Arrays.asList(Status.Ready.toString());
            List<TaskSummary> taskList = taskClient.findTasksByStatusByProcessInstanceId(processInstanceId, status, 0, 10);

            assertThat(taskList).hasSize(1);
            TaskSummary taskSummary = taskList.get(0);
            taskClient.startTask(CONTAINER_ID, taskSummary.getId(), USER_YODA);
            taskClient.completeTask(CONTAINER_ID, taskSummary.getId(), USER_YODA, null);

            TaskInstance userTask = taskClient.findTaskById(taskSummary.getId());
            assertThat(userTask).isNotNull();
            assertThat(userTask.getName()).isEqualTo("Evaluate items?");
            assertThat(userTask.getStatus()).isEqualTo(Status.Completed.toString());

            List<WorkItemInstance> workItems = processClient.getWorkItemByProcessInstance(CONTAINER_ID, processInstanceId);
            assertThat(workItems).isNotNull();
            assertThat(workItems).hasSize(1);

            WorkItemInstance workItemInstance = workItems.get(0);
            assertThat(workItemInstance).isNotNull();

            processClient.completeWorkItem(CONTAINER_ID, processInstanceId, workItemInstance.getId(), parameters);

            ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getState().intValue()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);

        } catch (Exception e){
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            throw e;
        } finally {
            changeUser(TestConfig.getUsername());
        }
    }

    @Test
    public void testStartCheckProcessWithCorrelationKey() throws Exception {
        String firstSimpleKey = "first-simple-key";
        String secondSimpleKey = "second-simple-key";
        String stringVarName = "stringData";
        String stringVarValue = "string variable test";

        CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

        CorrelationKey firstKey = correlationKeyFactory.newCorrelationKey(firstSimpleKey);
        CorrelationKey secondKey = correlationKeyFactory.newCorrelationKey(secondSimpleKey);

        Long firstProcessInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, firstKey);
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(stringVarName, stringVarValue);
        Long secondProcessInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, secondKey, parameters);
        try {
            ProcessInstance instance = processClient.getProcessInstance(CONTAINER_ID, firstProcessInstanceId);
            ProcessInstance expected = createEvaluationProcessInstace(firstProcessInstanceId);
            assertProcessInstance(expected, instance);
            assertThat(instance.getCorrelationKey()).isEqualTo(firstSimpleKey);

            instance = processClient.getProcessInstance(CONTAINER_ID, secondProcessInstanceId, true);
            expected = createEvaluationProcessInstace(secondProcessInstanceId);
            assertProcessInstance(expected, instance);
            assertThat(instance.getCorrelationKey()).isEqualTo(secondSimpleKey);
            assertThat(instance.getVariables().containsKey(stringVarName)).isTrue();
            assertThat(instance.getVariables().get(stringVarName)).isEqualTo(stringVarValue);
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, firstProcessInstanceId);
            processClient.abortProcessInstance(CONTAINER_ID, secondProcessInstanceId);
        }
    }

    @Test
    public void testStartProcessWithCustomTask() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("id", "custom id");
        Long processInstanceId = null;
        try {
            processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_CUSTOM_TASK);

            assertThat(processInstanceId).isNotNull();
            assertThat(processInstanceId.longValue() > 0).isTrue();

            ProcessInstance pi = queryClient.findProcessInstanceById(processInstanceId);
            assertThat(pi).isNotNull();
            assertThat(pi.getState().intValue()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);

        } catch(Exception e) {
            if (processInstanceId != null) {
                processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            }
            fail("Exception " + e.getMessage());
        }
    }

    @Test
    public void testSignalContainer() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId.longValue() > 0).isTrue();

        try {
            checkAvailableSignals(CONTAINER_ID, processInstanceId);

            Object person = createPersonInstance(USER_JOHN);
            processClient.signal(CONTAINER_ID, "Signal1", person);

            processClient.signal(CONTAINER_ID, "Signal2", "My custom string event");

            ProcessInstance pi = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertThat(pi).isNotNull();
            assertThat(pi.getState().intValue()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);
        } catch (Exception e){
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    @Test
    public void testSignalStartProcess() throws Exception {
        try {

            List<Integer> status = new ArrayList<Integer>();
            status.add(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);
            status.add(org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED);
            status.add(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);

            List<ProcessInstance> processInstances = queryClient.findProcessInstancesByProcessId(PROCESS_ID_SIGNAL_START, status, 0, 10);
            int initial = processInstances.size();

            Object person = createPersonInstance(USER_JOHN);
            processClient.signal(CONTAINER_ID, "start-process", person);

            processInstances = queryClient.findProcessInstancesByProcessId(PROCESS_ID_SIGNAL_START, status, 0, 10);
            assertThat(processInstances).isNotNull();
            assertThat(processInstances.size()).isEqualTo(initial + 1);

        } catch (Exception e){
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    @Test(timeout = 60 * 1000)
    public void testStartProcessInstanceWithAsyncNodes() throws Exception {
        List<String> status = new ArrayList<String>();
        status.add(STATUS.QUEUED.toString());
        status.add(STATUS.RUNNING.toString());
        status.add(STATUS.DONE.toString());
        int originalJobCount = jobServicesClient.getRequestsByStatus(status, 0, 1000).size();

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_ASYNC_SCRIPT);
        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId.longValue() > 0).isTrue();

        try {

            // async node is executed as a job
            List<RequestInfoInstance> jobs = jobServicesClient.getRequestsByStatus(status, 0, 1000);
            assertThat(jobs).isNotNull();
            assertThat(jobs.size()).isEqualTo(originalJobCount + 1);

            // wait for process instance to be completed
            KieServerSynchronization.waitForProcessInstanceToFinish(processClient, CONTAINER_ID, processInstanceId);

            ProcessInstance pi = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertThat(pi).isNotNull();
            assertThat(pi.getState().intValue()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);

        } catch (Exception e){
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }
    }

    @Test(timeout = 60 * 1000)
    public void testProcessInstanceWithTimer() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("timer", "1s");
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_TIMER, parameters);
        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId.longValue() > 0).isTrue();

        try {
            KieServerSynchronization.waitForProcessInstanceToFinish(processClient, CONTAINER_ID, processInstanceId);

            ProcessInstance pi = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertThat(pi).isNotNull();
            assertThat(pi.getState().intValue()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);

        } catch (Exception e){
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetNodeInstances() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {
            List<NodeInstance> instances = processClient.findActiveNodeInstances(CONTAINER_ID, processInstanceId, 0, 10);
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

            instances = processClient.findCompletedNodeInstances(CONTAINER_ID, processInstanceId, 0, 10);
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

            instances = processClient.findNodeInstances(CONTAINER_ID, processInstanceId, 0, 10);
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
    public void testCallActivityProcess() {
        Map<String, Object> parameters = new HashMap<String, Object>();

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_CALL_EVALUATION, parameters);
        try {
            assertThat(processInstanceId).isNotNull();
            assertThat(processInstanceId.longValue() > 0).isTrue();

            // Process instance is running and is active.
            ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getState().intValue()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);

            List<TaskSummary> tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertThat(tasks).hasSize(1);

            taskClient.completeAutoProgress(CONTAINER_ID, tasks.get(0).getId(), USER_YODA, null);

            List<ProcessInstance> instances = processClient.findProcessInstancesByParent(CONTAINER_ID, processInstanceId, 0, 10);
            assertThat(instances).hasSize(1);

            ProcessInstance childInstance = instances.get(0);
            assertThat(childInstance).isNotNull();
            assertThat(childInstance.getProcessId()).isEqualTo(PROCESS_ID_EVALUATION);
            assertThat(childInstance.getParentId()).isEqualTo(processInstanceId);

            List<NodeInstance> activeNodes = queryClient.findActiveNodeInstances(processInstanceId, 0, 10);
            assertThat(activeNodes).hasSize(1);

            NodeInstance active = activeNodes.get(0);
            assertThat(active.getName()).isEqualTo("Call Evaluation");
            assertThat(active.getNodeType()).isEqualTo("SubProcessNode");
            assertThat(active.getReferenceId()).isEqualTo(childInstance.getId());

            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);

            // Process instance is now aborted.
            processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getState().intValue()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED);

            processInstance = processClient.getProcessInstance(CONTAINER_ID, childInstance.getId());
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getState().intValue()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED);

            // no more active instances
            instances = processClient.findProcessInstancesByParent(CONTAINER_ID, processInstanceId, 0, 10);
            assertThat(instances).isEmpty();

            instances = processClient.findProcessInstancesByParent(CONTAINER_ID, processInstanceId, Arrays.asList(3), 0, 10);
            assertThat(instances).hasSize(1);
        } catch (Exception e) {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }
    }

    @Test
    public void testFindVariableInstances() {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {
            List<VariableInstance> currentState = processClient.findVariablesCurrentState(CONTAINER_ID, processInstanceId);
            assertThat(currentState).isNotNull();
            assertThat(currentState).hasSize(3);

            for (VariableInstance variableInstance : currentState) {
                if ("personData".equals(variableInstance.getVariableName())) {
                    assertVariableInstance(variableInstance, processInstanceId, "personData", "Person{name='john'}");
                } else if ("stringData".equals(variableInstance.getVariableName())) {
                    assertVariableInstance(variableInstance, processInstanceId, "stringData", "waiting for signal");
                } else if ("initiator".equals(variableInstance.getVariableName())) {
                    assertVariableInstance(variableInstance, processInstanceId, "initiator", TestConfig.getUsername());
                } else {
                    fail("Got unexpected variable " + variableInstance.getVariableName());
                }
            }

            List<VariableInstance> varHistory = processClient.findVariableHistory(CONTAINER_ID, processInstanceId, "stringData", 0, 10);
            assertThat(varHistory).isNotNull();
            assertThat(varHistory).hasSize(1);

            VariableInstance variableInstance = varHistory.get(0);
            assertVariableInstance(variableInstance, processInstanceId, "stringData", "waiting for signal");

            processClient.setProcessVariable(CONTAINER_ID, processInstanceId, "stringData", "updated value");

            currentState = processClient.findVariablesCurrentState(CONTAINER_ID, processInstanceId);
            assertThat(currentState).isNotNull();
            assertThat(currentState).hasSize(3);

            for (VariableInstance variable : currentState) {
                if ("personData".equals(variable.getVariableName())) {
                    assertVariableInstance(variable, processInstanceId, "personData", "Person{name='john'}");
                } else if ("stringData".equals(variable.getVariableName())) {
                    assertVariableInstance(variable, processInstanceId, "stringData", "updated value", "waiting for signal");
                } else if ("initiator".equals(variable.getVariableName())) {
                    assertVariableInstance(variable, processInstanceId, "initiator", TestConfig.getUsername());
                } else {
                    fail("Got unexpected variable " + variable.getVariableName());
                }
            }

            varHistory = processClient.findVariableHistory(CONTAINER_ID, processInstanceId, "stringData", 0, 10);
            assertThat(varHistory).isNotNull();
            assertThat(varHistory).hasSize(2);

            variableInstance = varHistory.get(0);
            assertVariableInstance(variableInstance, processInstanceId, "stringData", "updated value", "waiting for signal");

            variableInstance = varHistory.get(1);
            assertVariableInstance(variableInstance, processInstanceId, "stringData", "waiting for signal");

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }

    @Test
    public void testGetProcessInstances() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = processClient.findProcessInstances(CONTAINER_ID, 0, 10);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(5);

            instances = processClient.findProcessInstances(CONTAINER_ID, 0, 3);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(3);

            instances = processClient.findProcessInstances(CONTAINER_ID, 1, 3);
            assertThat(instances).isNotNull();
            assertThat(instances).hasSize(2);
        } finally {
            processClient.abortProcessInstances(CONTAINER_ID, processInstanceIds);
        }
    }

    @Test
    public void testGetProcessInstancesSortedByName() {
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
            processClient.abortProcessInstances(CONTAINER_ID, processInstanceIds);
        }
    }

    protected List<Long> createProcessInstances(Map<String, Object> parameters) {
        List<Long> processInstanceIds = new ArrayList<>();

        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters));

        Collections.sort(processInstanceIds);
        return processInstanceIds;
    }

    private void assertVariableInstance(VariableInstance variable, Long processInstanceId, String name, String value) {
        assertThat(variable).isNotNull();
        assertThat(variable.getProcessInstanceId()).isEqualTo(processInstanceId);
        KieServerAssert.assertNullOrEmpty(variable.getOldValue());
        assertThat(variable.getValue()).isEqualTo(value);
        assertThat(variable.getVariableName()).isEqualTo(name);
    }

    private void assertVariableInstance(VariableInstance variable, Long processInstanceId, String name, String value, String oldValue) {
        assertThat(variable).isNotNull();
        assertThat(variable.getProcessInstanceId()).isEqualTo(processInstanceId);
        assertThat(variable.getOldValue()).isEqualTo(oldValue);
        assertThat(variable.getValue()).isEqualTo(value);
        assertThat(variable.getVariableName()).isEqualTo(name);
    }

    private ProcessInstance createSignalProcessInstance(Long processInstanceId) {
        return ProcessInstance.builder()
                .id(processInstanceId)
                .state(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE)
                .processId(PROCESS_ID_SIGNAL_PROCESS)
                .processName("signalprocess")
                .processVersion("1.0")
                .containerId(CONTAINER_ID)
                .processInstanceDescription("signalprocess")
                .initiator(TestConfig.getUsername())
                .parentInstanceId(-1l)
                .build();
    }

    private ProcessInstance createEvaluationProcessInstace(Long proccesInstanceId) {
        return ProcessInstance.builder()
                .id(proccesInstanceId)
                .state(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE)
                .processId(PROCESS_ID_EVALUATION)
                .processName("evaluation")
                .processVersion("1.0")
                .initiator(USER_YODA)
                .containerId(CONTAINER_ID)
                .processInstanceDescription("evaluation")
                .parentInstanceId(-1l)
                .build();
    }

    private void assertProcessInstance(ProcessInstance expected, ProcessInstance actual) {
        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(expected.getId());
        assertThat(actual.getState()).isEqualTo(expected.getState());
        assertThat(actual.getProcessId()).isEqualTo(expected.getProcessId());
        assertThat(actual.getProcessName()).isEqualTo(expected.getProcessName());
        assertThat(actual.getProcessVersion()).isEqualTo(expected.getProcessVersion());
        assertThat(actual.getContainerId()).isEqualTo(expected.getContainerId());
        assertThat(actual.getProcessInstanceDescription()).isEqualTo(expected.getProcessInstanceDescription());
        assertThat(actual.getInitiator()).isEqualTo(expected.getInitiator());
        assertThat(actual.getParentId()).isEqualTo(expected.getParentId());
        assertThat(actual.getCorrelationKey()).isNotNull();
        assertThat(actual.getDate()).isNotNull();
    }

    private void assertNodeInstance(NodeInstance expected, NodeInstance actual) {
        assertThat(actual).isNotNull();
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getContainerId()).isEqualTo(expected.getContainerId());
        assertThat(actual.getNodeType()).isEqualTo(expected.getNodeType());
        assertThat(actual.getCompleted()).isEqualTo(expected.getCompleted());
        assertThat(actual.getProcessInstanceId()).isEqualTo(expected.getProcessInstanceId());
    }

    private void checkAvailableSignals(String containerId, Long processInstanceId) {
        List<String> availableSignals = processClient.getAvailableSignals(containerId, processInstanceId);
        assertThat(availableSignals).isNotNull();
        assertThat(availableSignals).hasSize(2);
        assertThat(availableSignals.contains("Signal1")).isTrue();
        assertThat(availableSignals.contains("Signal2")).isTrue();
    }
}
