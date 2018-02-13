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

package org.kie.server.integrationtests.jbpm.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.admin.ExecutionErrorInstance;
import org.kie.server.api.model.admin.ProcessNode;
import org.kie.server.api.model.admin.TimerInstance;
import org.kie.server.api.model.instance.NodeInstance;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.exception.KieServicesException;
import org.kie.server.integrationtests.jbpm.JbpmKieServerBaseIntegrationTest;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.kie.server.integrationtests.shared.KieServerSynchronization;

import static org.assertj.core.api.Assertions.*;

public class ProcessInstanceAdminServiceIntegrationTest extends JbpmKieServerBaseIntegrationTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");


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
    public void testCancelAndTrigger() throws Exception {

        Map<String, Object> parameters = new HashMap<String, Object>();

        Long processInstanceId = null;
        try {
            processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);

            assertThat(processInstanceId).isNotNull();
            assertThat(processInstanceId.longValue() > 0).isTrue();

            List<NodeInstance> activeNodeInstances = processAdminClient.getActiveNodeInstances(CONTAINER_ID, processInstanceId);
            assertThat(activeNodeInstances).isNotNull();
            assertThat(activeNodeInstances).hasSize(1);

            NodeInstance active = activeNodeInstances.get(0);
            assertThat(active.getName()).isEqualTo("Evaluate items?");

            processAdminClient.cancelNodeInstance(CONTAINER_ID, processInstanceId, active.getId());

            activeNodeInstances = processAdminClient.getActiveNodeInstances(CONTAINER_ID, processInstanceId);
            assertThat(activeNodeInstances).isNotNull();
            assertThat(activeNodeInstances).isEmpty();

            List<ProcessNode> processNodes = processAdminClient.getProcessNodes(CONTAINER_ID, processInstanceId);
            ProcessNode first = processNodes.stream().filter(pn -> pn.getNodeName().equals("Evaluate items?")).findFirst().orElse(null);
            assertThat(first).isNotNull();

            processAdminClient.triggerNode(CONTAINER_ID, processInstanceId, first.getNodeId());

            activeNodeInstances = processAdminClient.getActiveNodeInstances(CONTAINER_ID, processInstanceId);
            assertThat(activeNodeInstances).isNotNull();
            assertThat(activeNodeInstances).hasSize(1);

            NodeInstance activeTriggered = activeNodeInstances.get(0);
            assertThat(activeTriggered.getName()).isEqualTo("Evaluate items?");

            assertThat(activeTriggered.getId().longValue() == active.getId().longValue()).isFalse();

        } finally {
            if (processInstanceId != null) {
                processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            }
        }

    }

    @Test
    public void testRetrigger() throws Exception {

        Map<String, Object> parameters = new HashMap<String, Object>();

        Long processInstanceId = null;
        try {
            processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);

            assertThat(processInstanceId).isNotNull();
            assertThat(processInstanceId.longValue() > 0).isTrue();

            List<NodeInstance> activeNodeInstances = processAdminClient.getActiveNodeInstances(CONTAINER_ID, processInstanceId);
            assertThat(activeNodeInstances).isNotNull();
            assertThat(activeNodeInstances).hasSize(1);

            NodeInstance active = activeNodeInstances.get(0);
            assertThat(active.getName()).isEqualTo("Evaluate items?");

            processAdminClient.retriggerNodeInstance(CONTAINER_ID, processInstanceId, active.getId());

            activeNodeInstances = processAdminClient.getActiveNodeInstances(CONTAINER_ID, processInstanceId);
            assertThat(activeNodeInstances).isNotNull();
            assertThat(activeNodeInstances).hasSize(1);

            NodeInstance activeTriggered = activeNodeInstances.get(0);
            assertThat(activeTriggered.getName()).isEqualTo("Evaluate items?");

            assertThat(activeTriggered.getId().longValue() == active.getId().longValue()).isFalse();

        } finally {
            if (processInstanceId != null) {
                processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            }
        }

    }

    @Test(timeout = 60 * 1000)
    public void testUpdateTimer() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("timer", "1h");
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_TIMER, parameters);
        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId.longValue() > 0).isTrue();

        try {
            List<TimerInstance> timers = processAdminClient.getTimerInstances(CONTAINER_ID, processInstanceId);
            assertThat(timers).isNotNull();
            assertThat(timers).hasSize(1);

            TimerInstance timerInstance = timers.get(0);
            assertThat(timerInstance).isNotNull();
            assertThat(timerInstance.getTimerName()).isEqualTo("timer");

            processAdminClient.updateTimer(CONTAINER_ID, processInstanceId, timerInstance.getTimerId(), 3, 0, 0);

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
    public void testUpdateTimerRelative() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("timer", "1h");
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_TIMER, parameters);
        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId.longValue() > 0).isTrue();

        try {
            List<TimerInstance> timers = processAdminClient.getTimerInstances(CONTAINER_ID, processInstanceId);
            assertThat(timers).isNotNull();
            assertThat(timers).hasSize(1);

            TimerInstance timerInstance = timers.get(0);
            assertThat(timerInstance).isNotNull();
            assertThat(timerInstance.getTimerName()).isEqualTo("timer");

            processAdminClient.updateTimerRelative(CONTAINER_ID, processInstanceId, timerInstance.getTimerId(), 3, 0, 0);

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
    public void testErrorHandlingFailedToStart() throws Exception {

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("timer", "invalid value");

        try {
            processClient.startProcess(CONTAINER_ID, PROCESS_ID_TIMER, parameters);
            fail("Process instance should fail as it has invalid timer expression");
        } catch (KieServicesException e) {
            // expected as the variable to configure timer duration is invalid
        }
        List<ExecutionErrorInstance> errors = processAdminClient.getErrors(CONTAINER_ID, false, 0, 10);
        assertThat(errors).isNotNull();
        assertThat(errors).hasSize(1);

        ExecutionErrorInstance errorInstance = errors.get(0);
        assertThat(errorInstance.getErrorId()).isNotNull();
        assertThat(errorInstance.getError()).isNull();
        assertThat(errorInstance.getProcessInstanceId()).isNotNull();
        assertThat(errorInstance.getActivityId()).isNotNull();
        assertThat(errorInstance.getErrorDate()).isNotNull();

        assertThat(errorInstance.getContainerId()).isEqualTo(CONTAINER_ID);
        assertThat(errorInstance.getProcessId()).isEqualTo(PROCESS_ID_TIMER);
        assertThat(errorInstance.getActivityName()).isEqualTo("timer");

        assertThat(errorInstance.isAcknowledged()).isFalse();
        assertThat(errorInstance.getAcknowledgedAt()).isNull();
        assertThat(errorInstance.getAcknowledgedBy()).isNull();

        processAdminClient.acknowledgeError(CONTAINER_ID, errorInstance.getErrorId());

        errors = processAdminClient.getErrors(CONTAINER_ID, false, 0, 10);
        assertThat(errors).isNotNull();
        assertThat(errors).isEmpty();

        errorInstance = processAdminClient.getError(CONTAINER_ID, errorInstance.getErrorId());
        assertThat(errorInstance).isNotNull();
        assertThat(errorInstance.getErrorId()).isNotNull();
        assertThat(errorInstance.isAcknowledged()).isTrue();
        assertThat(errorInstance.getAcknowledgedAt()).isNotNull();
        assertThat(errorInstance.getAcknowledgedBy()).isEqualTo(USER_YODA);
    }

    @Test
    public void testErrorHandlingFailedToSignal() throws Exception {

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("nullAccepted", false);
        Long processInstanceId = null;
        try {
            processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters);

            List<ExecutionErrorInstance> errors = processAdminClient.getErrors(CONTAINER_ID, false, 0, 10);
            assertThat(errors).isNotNull();
            assertThat(errors).isEmpty();

            try {
                processClient.signalProcessInstance(CONTAINER_ID, processInstanceId, "Signal1", null);
                fail("Process instance signal fail as it provides null as event");
            } catch (KieServicesException e) {
                // expected
            }

            errors = processAdminClient.getErrorsByProcessInstance(CONTAINER_ID, processInstanceId, false, 0, 10);
            assertThat(errors).isNotNull();
            assertThat(errors).hasSize(1);
            ExecutionErrorInstance errorInstance = errors.get(0);
            assertThat(errorInstance.getErrorId()).isNotNull();
            assertThat(errorInstance.getError()).isNull();
            assertThat(errorInstance.getProcessInstanceId()).isNotNull();
            assertThat(errorInstance.getActivityId()).isNotNull();
            assertThat(errorInstance.getErrorDate()).isNotNull();

            assertThat(errorInstance.getContainerId()).isEqualTo(CONTAINER_ID);
            assertThat(errorInstance.getProcessId()).isEqualTo(PROCESS_ID_SIGNAL_PROCESS);
            assertThat(errorInstance.getActivityName()).isEqualTo("Signal 1 data");

            assertThat(errorInstance.isAcknowledged()).isFalse();
            assertThat(errorInstance.getAcknowledgedAt()).isNull();
            assertThat(errorInstance.getAcknowledgedBy()).isNull();

            errors = processAdminClient.getErrorsByProcessInstanceAndNode(CONTAINER_ID, processInstanceId, "Signal 1 data", false, 0, 10);
            assertThat(errors).isNotNull();
            assertThat(errors).hasSize(1);
            ExecutionErrorInstance errorInstance2 = errors.get(0);
            assertThat(errorInstance2.getErrorId()).isEqualTo(errorInstance.getErrorId());

            processAdminClient.acknowledgeError(CONTAINER_ID, errorInstance.getErrorId());

            errors = processAdminClient.getErrors(CONTAINER_ID, false, 0, 10);
            assertThat(errors).isNotNull();
            assertThat(errors).isEmpty();

            errorInstance = processAdminClient.getError(CONTAINER_ID, errorInstance.getErrorId());
            assertThat(errorInstance).isNotNull();
            assertThat(errorInstance.getErrorId()).isNotNull();
            assertThat(errorInstance.isAcknowledged()).isTrue();
            assertThat(errorInstance.getAcknowledgedAt()).isNotNull();
            assertThat(errorInstance.getAcknowledgedBy()).isEqualTo(USER_YODA);
        } catch (KieServicesException e) {
            logger.error("Unexpected error", e);
            fail(e.getMessage());
        } finally {
            if (processInstanceId != null) {
                processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            }
        }
    }

    @Test
    public void testErrorHandlingFailedToStartBulkAck() throws Exception {

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("timer", "invalid value");

        try {
            processClient.startProcess(CONTAINER_ID, PROCESS_ID_TIMER, parameters);
            fail("Process instance should fail as it has invalid timer expression");
        } catch (KieServicesException e) {
            // expected as the variable to configure timer duration is invalid
        }
        try {
            processClient.startProcess(CONTAINER_ID, PROCESS_ID_TIMER, parameters);
            fail("Process instance should fail as it has invalid timer expression");
        } catch (KieServicesException e) {
            // expected as the variable to configure timer duration is invalid
        }
        List<ExecutionErrorInstance> errors = processAdminClient.getErrors(CONTAINER_ID, false, 0, 10);
        assertThat(errors).isNotNull();
        assertThat(errors).hasSize(2);

        ExecutionErrorInstance errorInstance = errors.get(0);

        assertThat(errorInstance.isAcknowledged()).isFalse();
        assertThat(errorInstance.getAcknowledgedAt()).isNull();
        assertThat(errorInstance.getAcknowledgedBy()).isNull();

        ExecutionErrorInstance errorInstance2 = errors.get(1);

        assertThat(errorInstance2.isAcknowledged()).isFalse();
        assertThat(errorInstance2.getAcknowledgedAt()).isNull();
        assertThat(errorInstance2.getAcknowledgedBy()).isNull();

        processAdminClient.acknowledgeError(CONTAINER_ID, errorInstance.getErrorId(), errorInstance2.getErrorId());

        errors = processAdminClient.getErrors(CONTAINER_ID, false, 0, 10);
        assertThat(errors).isNotNull();
        assertThat(errors).isEmpty();

        errorInstance = processAdminClient.getError(CONTAINER_ID, errorInstance.getErrorId());
        assertThat(errorInstance).isNotNull();
        assertThat(errorInstance.getErrorId()).isNotNull();
        assertThat(errorInstance.isAcknowledged()).isTrue();
        assertThat(errorInstance.getAcknowledgedAt()).isNotNull();
        assertThat(errorInstance.getAcknowledgedBy()).isEqualTo(USER_YODA);
    }

}
