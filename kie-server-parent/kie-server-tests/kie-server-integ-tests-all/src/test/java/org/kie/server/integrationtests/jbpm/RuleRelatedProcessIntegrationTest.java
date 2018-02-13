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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.ExecutionResults;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.instance.TaskSummary;

import static org.assertj.core.api.Assertions.*;
import org.kie.server.integrationtests.shared.KieServerDeployer;


public class RuleRelatedProcessIntegrationTest extends JbpmKieServerBaseIntegrationTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");

    private static final String CONTAINER_ID = "definition-project";
    private static final String PERSON_CLASS_NAME = "org.jbpm.data.Person";


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
    public void testProcessWithBusinessRuleTask() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, "business-rule-task");
        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId.longValue() > 0).isTrue();

        Object person = createPersonInstance(USER_YODA);

        try {
            List<TaskSummary> taskList = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertThat(taskList).isNotNull();

            assertThat(taskList).hasSize(1);
            TaskSummary taskSummary = taskList.get(0);
            assertThat(taskSummary.getName()).isEqualTo("Before rule");


            // let insert person as fact into working memory
            List<Command<?>> commands = new ArrayList<Command<?>>();
            BatchExecutionCommand executionCommand = commandsFactory.newBatchExecution(commands, CONTAINER_ID); // use container id as ksession id to use ksession from jBPM extension


            commands.add(commandsFactory.newSetGlobal("people", new ArrayList()));
            commands.add(commandsFactory.newInsert(person, "person-yoda"));


            ServiceResponse<ExecutionResults> reply = ruleClient.executeCommandsWithResults(CONTAINER_ID, executionCommand);
            assertThat(reply.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);

            // startTask and completeTask task
            taskClient.startTask(CONTAINER_ID, taskSummary.getId(), USER_YODA);

            Map<String, Object> taskOutcome = new HashMap<String, Object>();
            taskOutcome.put("string_", "my custom data");
            taskOutcome.put("person_", createPersonInstance(USER_MARY));

            taskClient.completeTask(CONTAINER_ID, taskSummary.getId(), USER_YODA, taskOutcome);

            // check if it was moved to another human task
            taskList = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertThat(taskList).isNotNull();

            assertThat(taskList).hasSize(1);
            taskSummary = taskList.get(0);
            assertThat(taskSummary.getName()).isEqualTo("After rule");

            // now let's check if the rule fired
            commands.clear();
            commands.add(commandsFactory.newGetGlobal("people"));

            executionCommand = commandsFactory.newBatchExecution(commands, CONTAINER_ID); // use container id as ksession id to use ksession from jBPM extension
            reply = ruleClient.executeCommandsWithResults(CONTAINER_ID, executionCommand);
            assertThat(reply.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);

            ExecutionResults actualData = reply.getResult();
            assertThat(actualData).isNotNull();

        } finally {
            List<Command<?>> commands = new ArrayList<Command<?>>();
            BatchExecutionCommand executionCommand = commandsFactory.newBatchExecution(commands, CONTAINER_ID); // use container id as ksession id to use ksession from jBPM extension

            commands.add(commandsFactory.newFireAllRules());

            ServiceResponse<ExecutionResults> reply = ruleClient.executeCommandsWithResults(CONTAINER_ID, executionCommand);
            assertThat(reply.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);

            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }


    @Test
    public void testProcessWithConditionalEvent() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, "conditionalevent");
        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId.longValue() > 0).isTrue();
        try {
            List<TaskSummary> taskList = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertThat(taskList).isNotNull();

            assertThat(taskList).hasSize(1);
            TaskSummary taskSummary = taskList.get(0);
            assertThat(taskSummary.getName()).isEqualTo("Before rule");

            // startTask and completeTask task
            taskClient.startTask(CONTAINER_ID, taskSummary.getId(), USER_YODA);

            Map<String, Object> taskOutcome = new HashMap<String, Object>();
            taskOutcome.put("string_", "my custom data");
            taskOutcome.put("person_", createPersonInstance(USER_MARY));

            taskClient.completeTask(CONTAINER_ID, taskSummary.getId(), USER_YODA, taskOutcome);

            taskList = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertThat(taskList).isNotNull();

            assertThat(taskList).isEmpty();


            // let insert person as fact into working memory
            List<Command<?>> commands = new ArrayList<Command<?>>();
            BatchExecutionCommand executionCommand = commandsFactory.newBatchExecution(commands, CONTAINER_ID); // use container id as ksession id to use ksession from jBPM extension

            Object person = createPersonInstance(USER_YODA);
            commands.add(commandsFactory.newInsert(person, "person-yoda"));


            ServiceResponse<ExecutionResults> reply = ruleClient.executeCommandsWithResults(CONTAINER_ID, executionCommand);
            assertThat(reply.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);

            // check if it was moved to another human task
            taskList = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertThat(taskList).isNotNull();

            assertThat(taskList).hasSize(1);
            taskSummary = taskList.get(0);
            assertThat(taskSummary.getName()).isEqualTo("After rule");


        } finally {

            List<Command<?>> commands = new ArrayList<Command<?>>();
            BatchExecutionCommand executionCommand = commandsFactory.newBatchExecution(commands, CONTAINER_ID); // use container id as ksession id to use ksession from jBPM extension

            commands.add(commandsFactory.newFireAllRules());

            ServiceResponse<ExecutionResults> reply = ruleClient.executeCommandsWithResults(CONTAINER_ID, executionCommand);
            assertThat(reply.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);

            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }
}
