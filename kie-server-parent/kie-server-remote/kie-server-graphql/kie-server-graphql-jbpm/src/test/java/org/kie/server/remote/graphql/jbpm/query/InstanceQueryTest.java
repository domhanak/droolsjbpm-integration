package org.kie.server.remote.graphql.jbpm.query;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.TaskInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.remote.graphql.jbpm.filter.ProcessInstanceFilter;
import org.kie.server.remote.graphql.jbpm.filter.TaskInstanceFilter;
import org.kie.server.remote.graphql.jbpm.repository.InstanceRepository;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.VARIABLES;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InstanceQueryTest {

    private final static long ID = 1L;
    private final static int ACTIVE_STATE = 1;
    private final static int DUMMY_BATCH_SIZE = 1;
    private final static String DUMMY_ID = "dummy-id";
    private final static String DUMMY_CONTAINER_ID = "dummy-container-id";
    private final static String DUMMY_PROCESS_ID = "dummy-process-id";
    private final static String DUMMY_USER_ID = "dummy-user-id";
    private final static String DUMMY_VARIABLES_KEY = "dummy-variables-key";
    private final static String DUMMY_VARIABLES_VALUE = "dummy-variables-value";

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private DataFetchingEnvironment environment;

    @Mock
    private DataFetchingFieldSelectionSet selectionSet;

    private InstanceQuery instanceQuery;

    private ProcessInstanceFilter processInstanceFilter;
    private TaskInstanceFilter taskInstanceFilter;

    private ProcessInstance processInstanceWithVariables = ProcessInstance.builder()
            .id(ID)
            .containerId(DUMMY_CONTAINER_ID)
            .state(ACTIVE_STATE)
            .variables(Collections.singletonMap(DUMMY_VARIABLES_KEY, DUMMY_VARIABLES_VALUE))
            .build();

    private ProcessInstance processInstanceWithoutVariables = ProcessInstance.builder()
            .id(ID)
            .containerId(DUMMY_CONTAINER_ID)
            .state(ACTIVE_STATE)
            .build();

    private TaskSummary taskSummary = TaskSummary.builder()
            .id(ID)
            .processId(DUMMY_PROCESS_ID)
            .createdBy(DUMMY_USER_ID)
            .build();

    private TaskInstance taskInstance = TaskInstance.builder()
            .id(ID)
            .workItemId(ID)
            .build();

    @Before
    public void setUp() throws Exception {
        instanceQuery = new InstanceQuery(instanceRepository);

        processInstanceFilter = new ProcessInstanceFilter();
        taskInstanceFilter = new TaskInstanceFilter();
    }

    @Test
    public void testProcessInstanceWithVars() {
        when(instanceRepository.getProcessInstance(ID, DUMMY_CONTAINER_ID, true))
                .thenReturn(processInstanceWithVariables);
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains("variables")).thenReturn(true);

        ProcessInstance result = instanceQuery.processInstance(ID, DUMMY_CONTAINER_ID, environment);
        Assertions.assertThat(result.getId()).isEqualTo(1L);
        Assertions.assertThat(result.getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
        Assertions.assertThat(result.getVariables()).isNotNull();
        Assertions.assertThat(result.getVariables()).containsKey(DUMMY_VARIABLES_KEY);
        Assertions.assertThat(result.getVariables()).containsValue(DUMMY_VARIABLES_VALUE);
    }
    @Test
    public void testProcessInstanceWithoutVars() {
        when(instanceRepository.getProcessInstance(ID, DUMMY_CONTAINER_ID, false))
                .thenReturn(processInstanceWithoutVariables);
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains("variables")).thenReturn(false);

        ProcessInstance result = instanceQuery.processInstance(ID, DUMMY_CONTAINER_ID, environment);

        Assertions.assertThat(result.getId()).isEqualTo(1L);
        Assertions.assertThat(result.getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
        Assertions.assertThat(result.getVariables()).isNull();
    }

    @Test
    public void testAllProcessInstancesWithVariables() {
        when(instanceRepository.getAllProcessInstances(eq(1), any()))
                .thenReturn(Collections.singletonList(processInstanceWithVariables));
        when(instanceRepository.getProcessInstanceVariables(ID, DUMMY_CONTAINER_ID))
                .thenReturn(Collections.singletonMap(DUMMY_VARIABLES_KEY, DUMMY_VARIABLES_VALUE));
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains("variables")).thenReturn(true);

        List<ProcessInstance> result = instanceQuery.allProcessInstances(DUMMY_BATCH_SIZE, processInstanceFilter, environment);

        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        Assertions.assertThat(result.get(0).getId()).isEqualTo(1L);
        Assertions.assertThat(result.get(0).getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
        Assertions.assertThat(result.get(0).getVariables()).isNotNull();
        Assertions.assertThat(result.get(0).getVariables()).containsKey(DUMMY_VARIABLES_KEY);
        Assertions.assertThat(result.get(0).getVariables()).containsValue(DUMMY_VARIABLES_VALUE);

    }

    @Test
    public void testAllProcessInstancesWithoutVariables() {
        when(instanceRepository.getAllProcessInstances(eq(1), any()))
                .thenReturn(Collections.singletonList(processInstanceWithoutVariables));
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(false);

        List<ProcessInstance> result = instanceQuery.allProcessInstances(DUMMY_BATCH_SIZE, processInstanceFilter, environment);

        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        Assertions.assertThat(result.get(0).getId()).isEqualTo(1L);
        Assertions.assertThat(result.get(0).getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
        Assertions.assertThat(result.get(0).getVariables()).isNull();
    }
    @Test
    public void testAllProcessInstancesEmptyList() {
        when(instanceRepository.getAllProcessInstances(eq(1), any()))
                .thenReturn(Collections.EMPTY_LIST);

        List<ProcessInstance> result = instanceQuery.allProcessInstances(DUMMY_BATCH_SIZE, processInstanceFilter, environment);

        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void testVariables() {
        when(instanceRepository.getProcessInstanceVariables(ID, DUMMY_CONTAINER_ID))
                .thenReturn(Collections.singletonMap(DUMMY_VARIABLES_KEY, DUMMY_VARIABLES_VALUE));

        Map<String, Object> result = instanceQuery.variables(ID, DUMMY_CONTAINER_ID);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).containsKey(DUMMY_VARIABLES_KEY);
        Assertions.assertThat(result).containsValue(DUMMY_VARIABLES_VALUE);
    }

    @Test
    public void testAllTasks() {
        when(instanceRepository.getAllTasks(eq(1), any()))
                .thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceQuery.allTasks(DUMMY_BATCH_SIZE, taskInstanceFilter);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        Assertions.assertThat(result.get(0).getId()).isEqualTo(ID);
        Assertions.assertThat(result.get(0).getProcessId()).isEqualTo(DUMMY_PROCESS_ID);
        Assertions.assertThat(result.get(0).getCreatedBy()).isEqualTo(DUMMY_USER_ID);
    }

    @Test
    public void testTaskInstanceByTaskId() {
        when(instanceRepository.getTaskInstance(ID))
                .thenReturn(taskInstance);

        TaskInstance result = instanceQuery.taskInstance(ID);

        Assertions.assertThat(result.getId()).isEqualTo(ID);
    }
}
