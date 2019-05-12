package org.kie.server.remote.graphql.jbpm.query;

import java.util.Collections;
import java.util.List;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.definition.TaskInputsDefinition;
import org.kie.server.api.model.definition.TaskOutputsDefinition;
import org.kie.server.api.model.definition.UserTaskDefinition;
import org.kie.server.remote.graphql.jbpm.repository.DefinitionRepository;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.CONTAINER_ID;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.DEFINITION_ID;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.TASK_INPUT_MAPPINGS;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.TASK_OUTPUT_MAPPINGS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefinitionQueryTest {

    private final static int DUMMY_BATCH_SIZE = 1;
    private final static String DUMMY_ID = "dummy-id";
    private final static String DUMMY_NAME = "dummy-name";
    private final static String DUMMY_CONTAINER_ID = "dummy-container-id";
    private final static String DUMMY_PROCESS_ID = "dummy-process-id";
    private final static String DUMMY_USER_ID = "dummy-user-id";
    private final static String DUMMY_VARIABLES_KEY = "dummy-variables-key";
    private final static String DUMMY_VARIABLES_VALUE = "dummy-variables-value";

    @Mock
    private DefinitionRepository definitionRepository;

    @Mock
    private DataFetchingEnvironment environment;

    @Mock
    DataFetchingFieldSelectionSet selectionSet;

    private DefinitionQuery definitionQuery;

    private ProcessDefinition processDefinition = ProcessDefinition.builder()
            .id(DUMMY_ID)
            .containerId(DUMMY_CONTAINER_ID)
            .build();

    private UserTaskDefinition userTaskDefinition = UserTaskDefinition.builder()
            .id(DUMMY_ID)
            .name(DUMMY_NAME)
            .createdBy(DUMMY_USER_ID)
            .build();

    private TaskInputsDefinition taskInputsDefinition;
    private TaskOutputsDefinition taskOutputsDefinition;


    @Before
    public void setUp() throws Exception {
        definitionQuery = new DefinitionQuery(definitionRepository);

        taskInputsDefinition = new TaskInputsDefinition();
        taskInputsDefinition.setTaskInputs(
                Collections.singletonMap(DUMMY_VARIABLES_KEY, DUMMY_VARIABLES_VALUE));

        taskOutputsDefinition = new TaskOutputsDefinition();
        taskOutputsDefinition.setTaskOutputs(
                Collections.singletonMap(DUMMY_VARIABLES_KEY, DUMMY_VARIABLES_VALUE));
    }

    @Test
    public void testAllProcessDefinitions() {
        when(definitionRepository.getProcessDefinitions(DUMMY_BATCH_SIZE))
                .thenReturn(Collections.singletonList(processDefinition));

        List<ProcessDefinition> result = definitionQuery.allProcessDefinitions(DUMMY_BATCH_SIZE);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        Assertions.assertThat(result.get(0).getId()).isEqualTo(DUMMY_ID);
        Assertions.assertThat(result.get(0).getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
    }

    @Test
    public void testProcessDefinition() {
        when(definitionRepository.getProcessDefinition(eq(DUMMY_ID), eq(DUMMY_CONTAINER_ID)))
                .thenReturn(processDefinition);

        ProcessDefinition result = definitionQuery.processDefinition(DUMMY_ID, DUMMY_CONTAINER_ID);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(DUMMY_ID);
        Assertions.assertThat(result.getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
    }

    @Test
    public void testProcessDefinitionsWithContainerId() {
        when(definitionRepository.getProcessDefinitions(DUMMY_CONTAINER_ID, 100))
                .thenReturn(Collections.singletonList(processDefinition));
        when(environment.getArgument(CONTAINER_ID)).thenReturn(DUMMY_CONTAINER_ID);
        when(environment.getArgument(DEFINITION_ID)).thenReturn(null);

        List<ProcessDefinition> result = definitionQuery.processDefinitions(DUMMY_PROCESS_ID, DUMMY_CONTAINER_ID, environment);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0).getId()).isEqualTo(DUMMY_ID);
        Assertions.assertThat(result.get(0).getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
        verify(definitionRepository, times(1)).getProcessDefinitions(DUMMY_CONTAINER_ID, 100);
    }

    @Test
    public void testProcessDefinitionsWithProcessId() {
        when(definitionRepository.getProcessDefinitions(DUMMY_ID))
                .thenReturn(Collections.singletonList(processDefinition));
        when(environment.getArgument(CONTAINER_ID)).thenReturn(null);
        when(environment.getArgument(DEFINITION_ID)).thenReturn(DUMMY_ID);

        List<ProcessDefinition> result = definitionQuery.processDefinitions(DUMMY_ID, DUMMY_CONTAINER_ID, environment);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0).getId()).isEqualTo(DUMMY_ID);
        Assertions.assertThat(result.get(0).getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
        verify(definitionRepository, times(1)).getProcessDefinitions(DUMMY_ID);
    }

    @Test
    public void testProcessDefinitionsWithProcessIdAndContainerId() {
        when(environment.getArgument(CONTAINER_ID)).thenReturn(CONTAINER_ID);
        when(environment.getArgument(DEFINITION_ID)).thenReturn(DUMMY_ID);

        Assertions.assertThatThrownBy(() -> definitionQuery.processDefinitions(DUMMY_ID, DUMMY_CONTAINER_ID, environment))
            .hasMessage("Incorrect combination of arguments. Only one of them can be specified.");
    }

    @Test
    public void testUserTaskDefinitions() {
        when(definitionRepository.getUserTaskDefinitions(DUMMY_ID, DUMMY_CONTAINER_ID))
                .thenReturn(Collections.singletonList(userTaskDefinition));
        when(definitionRepository.taskInputMappings(DUMMY_ID, DUMMY_CONTAINER_ID, DUMMY_NAME))
                .thenReturn(taskInputsDefinition);
        when(definitionRepository.taskOutputMappings(DUMMY_ID, DUMMY_CONTAINER_ID, DUMMY_NAME))
                .thenReturn(taskOutputsDefinition);
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(TASK_INPUT_MAPPINGS)).thenReturn(true);
        when(selectionSet.contains(TASK_OUTPUT_MAPPINGS)).thenReturn(true);

        List<UserTaskDefinition> result = definitionQuery.userTaskDefinitions(DUMMY_ID, DUMMY_CONTAINER_ID, environment);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).isNotNull();
        Assertions.assertThat(result.get(0).getName()).isEqualTo(DUMMY_NAME);
        Assertions.assertThat(result.get(0).getTaskInputMappings()).isNotNull();
        Assertions.assertThat(result.get(0).getTaskInputMappings()).isEqualTo(taskInputsDefinition.getTaskInputs());
        Assertions.assertThat(result.get(0).getTaskOutputMappings()).isNotNull();
        Assertions.assertThat(result.get(0).getTaskOutputMappings()).isEqualTo(taskOutputsDefinition.getTaskOutputs());
    }

    @Test
    public void testUserTaskDefinitionsWithoutInputsAndOutputs() {
        when(definitionRepository.getUserTaskDefinitions(DUMMY_ID, DUMMY_CONTAINER_ID))
                .thenReturn(Collections.singletonList(userTaskDefinition));
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(TASK_INPUT_MAPPINGS)).thenReturn(false);
        when(selectionSet.contains(TASK_OUTPUT_MAPPINGS)).thenReturn(false);

        List<UserTaskDefinition> result = definitionQuery.userTaskDefinitions(DUMMY_ID, DUMMY_CONTAINER_ID, environment);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).isNotNull();
        Assertions.assertThat(result.get(0).getName()).isEqualTo(DUMMY_NAME);
        Assertions.assertThat(result.get(0).getId()).isEqualTo(DUMMY_ID);
        Assertions.assertThat(result.get(0).getTaskInputMappings()).isNull();
        Assertions.assertThat(result.get(0).getTaskOutputMappings()).isNull();
    }
}
