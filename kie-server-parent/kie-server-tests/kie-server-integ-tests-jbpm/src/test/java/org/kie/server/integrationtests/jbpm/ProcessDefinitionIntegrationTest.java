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

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.definition.AssociatedEntitiesDefinition;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.definition.ServiceTasksDefinition;
import org.kie.server.api.model.definition.SubProcessesDefinition;
import org.kie.server.api.model.definition.TaskInputsDefinition;
import org.kie.server.api.model.definition.TaskOutputsDefinition;
import org.kie.server.api.model.definition.UserTaskDefinition;
import org.kie.server.api.model.definition.UserTaskDefinitionList;
import org.kie.server.api.model.definition.VariablesDefinition;
import org.kie.server.api.exception.KieServicesException;

import static org.assertj.core.api.Assertions.*;
import org.kie.server.integrationtests.shared.KieServerDeployer;


public class ProcessDefinitionIntegrationTest extends JbpmKieServerBaseIntegrationTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");


    @BeforeClass
    public static void buildAndDeployArtifacts() {

        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/definition-project").getFile());

        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);

        createContainer(CONTAINER_ID, releaseId);
    }


    @Test
    public void testEvaluationProcessDefinition() {
        ProcessDefinition result = processClient.getProcessDefinition(CONTAINER_ID, PROCESS_ID_EVALUATION);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(PROCESS_ID_EVALUATION);
        assertThat(result.getName()).isEqualTo("evaluation");
        assertThat(result.getPackageName()).isEqualTo("org.jbpm");
        assertThat(result.getVersion()).isEqualTo("1.0");
        assertThat(result.getContainerId()).isEqualTo(CONTAINER_ID);

        // assert variable definitions
        Map<String, String> variables = result.getProcessVariables();
        assertThat(variables).isNotNull();
        assertThat(variables).hasSize(3);

        assertThat(variables.containsKey("name")).isTrue();
        assertThat(variables.containsKey("item")).isTrue();
        assertThat(variables.containsKey("outcome")).isTrue();

        assertThat(variables.get("name")).isEqualTo("String");
        assertThat(variables.get("item")).isEqualTo("java.util.List");
        assertThat(variables.get("outcome")).isEqualTo("Boolean");

        // assert associated entities - users and groups
        Map<String, String[]> entities = result.getAssociatedEntities();
        assertThat(entities).isNotNull();

        assertThat(entities.containsKey("Evaluate items?")).isTrue();

        String[] evaluateItemsEntities = entities.get("Evaluate items?");
        assertThat(evaluateItemsEntities.length).isEqualTo(2);
        assertThat(evaluateItemsEntities[0]).isEqualTo(USER_YODA);
        assertThat(evaluateItemsEntities[1]).isEqualTo("HR,PM");

        // assert reusable subprocesses
        assertThat(result.getReusableSubProcesses()).isEmpty();

        // assert services tasks
        assertThat(result.getServiceTasks()).hasSize(1);
        assertThat(result.getServiceTasks().containsKey("Email results")).isTrue();
        // assert type of the services task for 'Email results' name
        assertThat(result.getServiceTasks().get("Email results")).isEqualTo("Email");

    }

    @Test
    public void testCallEvaluationProcessDefinition() {
        ProcessDefinition result = processClient.getProcessDefinition(CONTAINER_ID, PROCESS_ID_CALL_EVALUATION);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(PROCESS_ID_CALL_EVALUATION);
        assertThat(result.getName()).isEqualTo("call-evaluation");
        assertThat(result.getPackageName()).isEqualTo("org.jbpm");
        assertThat(result.getVersion()).isEqualTo("1.0");
        assertThat(result.getContainerId()).isEqualTo(CONTAINER_ID);

        // assert variable definitions
        Map<String, String> variables = result.getProcessVariables();
        assertThat(variables).isNotNull();
        assertThat(variables).hasSize(1);

        assertThat(variables.containsKey("items")).isTrue();

        assertThat(variables.get("items")).isEqualTo("java.util.List");

        // assert associated entities - users and groups
        Map<String, String[]> entities = result.getAssociatedEntities();
        assertThat(entities).isNotNull();

        assertThat(entities.containsKey("Prepare")).isTrue();

        String[] evaluateItemsEntities = entities.get("Prepare");
        assertThat(evaluateItemsEntities.length).isEqualTo(1);
        assertThat(evaluateItemsEntities[0]).isEqualTo(USER_YODA);

        // assert reusable subprocesses
        assertThat(result.getReusableSubProcesses()).hasSize(1);
        assertThat(result.getReusableSubProcesses().iterator().next()).isEqualTo("definition-project.evaluation");

        // assert services tasks
        assertThat(result.getServiceTasks()).isEmpty();

    }


    @Test(expected = KieServicesException.class)
    public void testNonExistingProcessDefinition() {
        processClient.getProcessDefinition(CONTAINER_ID, "non-existing-process");
    }


    @Test
    public void testReusableSubProcessDefinition() {
        SubProcessesDefinition result = processClient.getReusableSubProcessDefinitions(CONTAINER_ID, PROCESS_ID_CALL_EVALUATION);

        assertThat(result).isNotNull();
        // assert reusable subprocesses
        assertThat(result.getSubProcesses()).hasSize(1);
        assertThat(result.getSubProcesses().iterator().next()).isEqualTo(PROCESS_ID_EVALUATION);

    }


    @Test
    public void testProcessVariableDefinitions() {
        // assert variable definitions
        VariablesDefinition variablesDefinition = processClient.getProcessVariableDefinitions(CONTAINER_ID, PROCESS_ID_EVALUATION);

        Map<String, String> variables = variablesDefinition.getVariables();
        assertThat(variables).isNotNull();
        assertThat(variables).hasSize(3);

        assertThat(variables.containsKey("name")).isTrue();
        assertThat(variables.containsKey("item")).isTrue();
        assertThat(variables.containsKey("outcome")).isTrue();

        assertThat(variables.get("name")).isEqualTo("String");
        assertThat(variables.get("item")).isEqualTo("java.util.List");
        assertThat(variables.get("outcome")).isEqualTo("Boolean");
    }

    @Test
    public void testServiceTasksDefinition() {
        ServiceTasksDefinition result = processClient.getServiceTaskDefinitions(CONTAINER_ID, PROCESS_ID_EVALUATION);
        // assert services tasks
        assertThat(result.getServiceTasks()).hasSize(1);
        assertThat(result.getServiceTasks().containsKey("Email results")).isTrue();
        // assert type of the services task for 'Email results' name
        assertThat(result.getServiceTasks().get("Email results")).isEqualTo("Email");
    }


    @Test
    public void testAssociatedEntitiesDefinition() {
        AssociatedEntitiesDefinition result = processClient.getAssociatedEntityDefinitions(CONTAINER_ID, PROCESS_ID_EVALUATION);

        // assert associated entities - users and groups
        Map<String, String[]> entities = result.getAssociatedEntities();
        assertThat(entities).isNotNull();

        assertThat(entities.containsKey("Evaluate items?")).isTrue();
        String[] evaluateItemsEntities = entities.get("Evaluate items?");

        assertThat(evaluateItemsEntities.length).isEqualTo(2);
        assertThat(evaluateItemsEntities[0]).isEqualTo(USER_YODA);
        assertThat(evaluateItemsEntities[1]).isEqualTo("HR,PM");
    }
 
    @Test
    public void testUserTasksDefinition() {
        UserTaskDefinitionList result = processClient.getUserTaskDefinitions(CONTAINER_ID, PROCESS_ID_EVALUATION);

        assertThat(result).isNotNull();
        UserTaskDefinition[] tasks = result.getTasks();

        // assert user tasks
        assertThat(tasks).isNotNull();
        assertThat(tasks.length).isEqualTo(1);

        UserTaskDefinition task = tasks[0];

        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("Evaluate items?");
        assertThat(task.getComment()).isEqualTo("");
        assertThat(task.getCreatedBy()).isEqualTo("");
        assertThat(task.getPriority().intValue()).isEqualTo(0);
        assertThat(task.isSkippable()).isEqualTo(true);
        assertThat(task.getId()).isEqualTo("2");
        assertThat(task.getFormName()).isEqualTo("");

        // assert associated entities - users and groups
        String[] evaluateItemsEntities = task.getAssociatedEntities();

        assertThat(evaluateItemsEntities.length).isEqualTo(2);
        assertThat(evaluateItemsEntities[0]).isEqualTo(USER_YODA);
        assertThat(evaluateItemsEntities[1]).isEqualTo("HR,PM");

        // assert task inputs and outputs

        Map<String, String> inputs = task.getTaskInputMappings();
        assertThat(inputs).isNotNull();
        assertThat(inputs).hasSize(4);

        assertThat(inputs.containsKey("name_in")).isTrue();
        assertThat(inputs.containsKey("list_in")).isTrue();
        assertThat(inputs.containsKey("GroupId")).isTrue();
        assertThat(inputs.containsKey("Skippable")).isTrue();


        assertThat(inputs.get("name_in")).isEqualTo("String");
        assertThat(inputs.get("list_in")).isEqualTo("java.util.List");
        assertThat(inputs.get("GroupId")).isEqualTo("java.lang.String");
        assertThat(inputs.get("Skippable")).isEqualTo("java.lang.String");

        Map<String, String> outputs = task.getTaskOutputMappings();
        assertThat(outputs).isNotNull();
        assertThat(outputs).hasSize(1);

        assertThat(outputs.containsKey("outcome")).isTrue();

        assertThat(outputs.get("outcome")).isEqualTo("Boolean");

    }

    @Test
    public void testUserTaskInputDefinition() {
        TaskInputsDefinition result = processClient.getUserTaskInputDefinitions(CONTAINER_ID, PROCESS_ID_EVALUATION, "Evaluate items?");

        assertThat(result).isNotNull();
        // assert task inputs and outputs

        Map<String, String> inputs = result.getTaskInputs();
        assertThat(inputs).isNotNull();
        assertThat(inputs).hasSize(4);

        assertThat(inputs.containsKey("name_in")).isTrue();
        assertThat(inputs.containsKey("list_in")).isTrue();
        assertThat(inputs.containsKey("GroupId")).isTrue();
        assertThat(inputs.containsKey("Skippable")).isTrue();


        assertThat(inputs.get("name_in")).isEqualTo("String");
        assertThat(inputs.get("list_in")).isEqualTo("java.util.List");
        assertThat(inputs.get("GroupId")).isEqualTo("java.lang.String");
        assertThat(inputs.get("Skippable")).isEqualTo("java.lang.String");
    }

    @Test
    public void testTaskOutputsDefinition() {
        TaskOutputsDefinition result = processClient.getUserTaskOutputDefinitions(CONTAINER_ID, PROCESS_ID_EVALUATION, "Evaluate items?");

        assertThat(result).isNotNull();
        // assert task inputs and outputs
        Map<String, String> outputs = result.getTaskOutputs();
        assertThat(outputs).isNotNull();
        assertThat(outputs).hasSize(1);

        assertThat(outputs.containsKey("outcome")).isTrue();

        assertThat(outputs.get("outcome")).isEqualTo("Boolean");

    }

    @Test
    public void testUserTasksDefinitionWithEmptyAssociatedEntities() {
        UserTaskDefinitionList result = processClient.getUserTaskDefinitions(CONTAINER_ID, PROCESS_ID_XYZ_TRANSLATIONS);

        assertThat(result).isNotNull();
        UserTaskDefinition[] tasks = result.getTasks();

        // assert user tasks
        assertThat(tasks).isNotNull();
        assertThat(tasks.length).isEqualTo(3);

        UserTaskDefinition task = tasks[0];

        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("review-incoming");

        // assert associated entities - users and groups
        String[] evaluateItemsEntities = task.getAssociatedEntities();

        assertThat(evaluateItemsEntities.length).isEqualTo(2);
        assertThat(evaluateItemsEntities[0]).isEqualTo("yoda");
        assertThat(evaluateItemsEntities[1]).isEqualTo("Reviewer");

        task = tasks[1];

        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("translate");

        // assert associated entities - users and groups
        evaluateItemsEntities = task.getAssociatedEntities();

        assertThat(evaluateItemsEntities.length).isEqualTo(2);
        assertThat(evaluateItemsEntities[0]).isEqualTo("yoda");
        assertThat(evaluateItemsEntities[1]).isEqualTo("Writer");

        task = tasks[2];

        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("review-translation");

        // assert associated entities - users and groups
        evaluateItemsEntities = task.getAssociatedEntities();

        assertThat(evaluateItemsEntities.length).isEqualTo(0);
    }

}
