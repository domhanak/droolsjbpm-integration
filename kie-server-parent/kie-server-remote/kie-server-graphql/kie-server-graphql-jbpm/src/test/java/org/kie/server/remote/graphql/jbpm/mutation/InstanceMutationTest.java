package org.kie.server.remote.graphql.jbpm.mutation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.remote.graphql.jbpm.inputs.AbortProcessInstancesInput;
import org.kie.server.remote.graphql.jbpm.inputs.SignalProcessInstancesInput;
import org.kie.server.remote.graphql.jbpm.inputs.StartProcessesInput;
import org.kie.server.remote.graphql.jbpm.repository.InstanceRepository;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Arguments.CORRELATION_KEY;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Arguments.PROCESS_VARIABLES;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.CONTAINER_ID;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.VARIABLES;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InstanceMutationTest {

    private final static long ID = 1L;
    private final static int ACTIVE_STATE = 1;
    private final static int DUMMY_BATCH_SIZE = 1;
    private final static String DUMMY_ID = "dummy-id";
    private final static String DUMMY_CONTAINER_ID = "dummy-container-id";
    private final static String DUMMY_CORRELATION_KEY = "ABC:DEF";
    private final static String DUMMY_PROCESS_ID = "dummy-process-id";
    private final static String DUMMY_SIGNAL_NAME = "dummy-signal-name";
    private final static String DUMMY_EVENT = "dummy-event";
    private final static String DUMMY_USER_ID = "dummy-user-id";
    private final static String DUMMY_VARIABLES_KEY = "dummy-variables-key";
    private final static String DUMMY_VARIABLES_VALUE = "dummy-variables-value";
    private final static Map<String, Object> DUMMY_VARIABLES =  Collections.singletonMap(DUMMY_VARIABLES_KEY, DUMMY_VARIABLES_VALUE);

    @Mock
    private DataFetchingEnvironment environment;

    @Mock
    private DataFetchingFieldSelectionSet selectionSet;

    @Mock
    private InstanceRepository instanceRepository;

    private InstanceMutation instanceMutation;

    private ProcessInstance processInstanceWithVariables = ProcessInstance.builder()
            .id(ID)
            .processId(DUMMY_ID)
            .correlationKey(DUMMY_CORRELATION_KEY)
            .containerId(DUMMY_CONTAINER_ID)
            .state(ACTIVE_STATE)
            .variables(DUMMY_VARIABLES)
            .build();

    private ProcessInstance processInstanceWithoutVariables = ProcessInstance.builder()
            .id(ID)
            .processId(DUMMY_ID)
            .containerId(DUMMY_CONTAINER_ID)
            .state(ACTIVE_STATE)
            .build();


    @Before
    public void setUp() throws Exception {
        instanceMutation = new InstanceMutation(instanceRepository);
    }

    @Test
    public void testStartProcesses() {
        when(instanceRepository.startProcesses(DUMMY_ID, DUMMY_CONTAINER_ID, DUMMY_BATCH_SIZE, true))
                .thenReturn(Collections.singletonList(processInstanceWithVariables));
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(true);
        StartProcessesInput input = createDummyStartProcessesInput(null, null);

        List<ProcessInstance> result = instanceMutation.startProcesses(input, environment);

        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
            softAssertions.assertThat(result.get(0).getProcessId()).isEqualTo(DUMMY_ID);
            softAssertions.assertThat(result.get(0).getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
            softAssertions.assertThat(result.get(0).getVariables()).isEqualTo(DUMMY_VARIABLES);
        }));
    }

    @Test
    public void testStartProcessesWithoutVars() {
        when(instanceRepository.startProcesses(DUMMY_ID, DUMMY_CONTAINER_ID, DUMMY_BATCH_SIZE, false))
                .thenReturn(Collections.singletonList(processInstanceWithoutVariables));
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(false);
        StartProcessesInput input = createDummyStartProcessesInput(null, null);

        List<ProcessInstance> result = instanceMutation.startProcesses(input, environment);

        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
            softAssertions.assertThat(result.get(0).getProcessId()).isEqualTo(DUMMY_ID);
            softAssertions.assertThat(result.get(0).getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
            softAssertions.assertThat(result.get(0).getVariables()).isEqualTo(null);
        }));
    }

    @Test
    public void testStartProcessesWithCorrelationKey() {
        when(instanceRepository.startProcesses(DUMMY_ID, DUMMY_CONTAINER_ID, DUMMY_CORRELATION_KEY, DUMMY_BATCH_SIZE, true))
                .thenReturn(Collections.singletonList(processInstanceWithVariables));
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(true);
        StartProcessesInput input = createDummyStartProcessesInput(DUMMY_CORRELATION_KEY, null);

        List<ProcessInstance> result = instanceMutation.startProcesses(input, environment);

        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
            softAssertions.assertThat(result.get(0).getProcessId()).isEqualTo(DUMMY_ID);
            softAssertions.assertThat(result.get(0).getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
            softAssertions.assertThat(result.get(0).getCorrelationKey()).isEqualTo(DUMMY_CORRELATION_KEY);
            softAssertions.assertThat(result.get(0).getVariables()).isEqualTo(DUMMY_VARIABLES);
        }));
    }

    @Test
    public void testStartProcessesWithProcessVariables() {
        when(instanceRepository.startProcesses(DUMMY_ID, DUMMY_CONTAINER_ID, null, DUMMY_VARIABLES, DUMMY_BATCH_SIZE, true))
                .thenReturn(Collections.singletonList(processInstanceWithVariables));
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(true);
        StartProcessesInput input = createDummyStartProcessesInput(null, DUMMY_VARIABLES);

        List<ProcessInstance> result = instanceMutation.startProcesses(input, environment);

        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
            softAssertions.assertThat(result.get(0).getProcessId()).isEqualTo(DUMMY_ID);
            softAssertions.assertThat(result.get(0).getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
            softAssertions.assertThat(result.get(0).getCorrelationKey()).isEqualTo(DUMMY_CORRELATION_KEY);
            softAssertions.assertThat(result.get(0).getVariables()).isEqualTo(DUMMY_VARIABLES);
        }));
    }

    @Test
    public void testStartProcessesWithCorrelationKeyWithProcessVariables() {
        when(instanceRepository.startProcesses(DUMMY_ID, DUMMY_CONTAINER_ID, DUMMY_CORRELATION_KEY, DUMMY_VARIABLES, DUMMY_BATCH_SIZE, true))
                .thenReturn(Collections.singletonList(processInstanceWithVariables));
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(true);
        StartProcessesInput input = createDummyStartProcessesInput(DUMMY_CORRELATION_KEY, DUMMY_VARIABLES);

        List<ProcessInstance> result = instanceMutation.startProcesses(input, environment);

        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
            softAssertions.assertThat(result.get(0).getProcessId()).isEqualTo(DUMMY_ID);
            softAssertions.assertThat(result.get(0).getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
            softAssertions.assertThat(result.get(0).getCorrelationKey()).isEqualTo(DUMMY_CORRELATION_KEY);
            softAssertions.assertThat(result.get(0).getVariables()).isEqualTo(DUMMY_VARIABLES);
        }));
    }

    @Test
    public void testAbortProcessInstancesWithContainerId() {
        when(instanceRepository.abortProcessInstances(Collections.singletonList(ID), DUMMY_CONTAINER_ID, true))
                .thenReturn(Collections.singletonList(processInstanceWithoutVariables));
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(true);
        AbortProcessInstancesInput instancesInput = new AbortProcessInstancesInput();
        instancesInput.setContainerId(DUMMY_CONTAINER_ID);
        instancesInput.setIds(Collections.singletonList(ID));

        List<ProcessInstance> result = instanceMutation.abortProcessInstances(instancesInput, environment);

        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result).isNotNull();
            softAssertions.assertThat(result.get(0).getId()).isEqualTo(ID);
            softAssertions.assertThat(result.get(0).getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
        }));
    }

    @Test
    public void testAbortProcessInstancesWithoutContainerId() {
        when(instanceRepository.abortProcessInstances(Collections.singletonList(ID), null, true))
                .thenReturn(Collections.singletonList(processInstanceWithoutVariables));
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(true);
        AbortProcessInstancesInput instancesInput = new AbortProcessInstancesInput();
        instancesInput.setIds(Collections.singletonList(ID));

        List<ProcessInstance> result = instanceMutation.abortProcessInstances(instancesInput, environment);

        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result).isNotNull();
            softAssertions.assertThat(result.get(0).getId()).isEqualTo(ID);
        }));
    }

    @Test
    public void testSignalProcessInstancesWithContainerId() {
        when(instanceRepository.signalProcessInstances(DUMMY_CONTAINER_ID,
                                                       Collections.singletonList(ID),
                                                       DUMMY_SIGNAL_NAME, DUMMY_EVENT,
                                                       true))
        .thenReturn(Collections.singletonList(processInstanceWithoutVariables));
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(true);

        SignalProcessInstancesInput input = createDummySignalProcessInstancesInput(DUMMY_CONTAINER_ID);

        List<ProcessInstance> result = instanceMutation.signalProcessInstances(input,environment);
        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result).isNotNull();
            softAssertions.assertThat(result.get(0).getId()).isEqualTo(ID);
            softAssertions.assertThat(result.get(0).getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
        }));
    }

    @Test
    public void testSignalProcessInstancesWithoutContainerId() {
        when(instanceRepository.signalProcessInstances(Collections.singletonList(ID),
                                                       DUMMY_SIGNAL_NAME, DUMMY_EVENT, false))
                .thenReturn(Collections.singletonList(processInstanceWithoutVariables));
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(false);

        SignalProcessInstancesInput input = createDummySignalProcessInstancesInput(null);

        List<ProcessInstance> result = instanceMutation.signalProcessInstances(input, environment);
        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result).isNotNull();
            softAssertions.assertThat(result.get(0).getId()).isEqualTo(ID);
        }));
    }

    private StartProcessesInput createDummyStartProcessesInput(String correlationKey, Map<String, Object> variables){
        StartProcessesInput result = new StartProcessesInput();
        result.setBatchSize(DUMMY_BATCH_SIZE);
        result.setId(DUMMY_ID);
        result.setContainerId(DUMMY_CONTAINER_ID);
        result.setCorrelationKey(correlationKey);
        result.setVariables(variables);

        return result;
    }

    private SignalProcessInstancesInput createDummySignalProcessInstancesInput(String containerId) {
        SignalProcessInstancesInput result = new SignalProcessInstancesInput();
        result.setIds(Collections.singletonList(ID));
        result.setSignalName(DUMMY_SIGNAL_NAME);
        result.setEvent(DUMMY_EVENT);
        result.setContainerId(containerId);

        return result;
    }
}
