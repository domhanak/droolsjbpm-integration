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
            .containerId(DUMMY_CONTAINER_ID)
            .state(ACTIVE_STATE)
            .build();


    @Before
    public void setUp() throws Exception {
        instanceMutation = new InstanceMutation(instanceRepository);
    }

    @Test
    public void testStartProcess() {
        when(instanceRepository.startProcess(DUMMY_ID, DUMMY_CONTAINER_ID, true))
                .thenReturn(processInstanceWithVariables);
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(true);
        when(environment.getArgument(CORRELATION_KEY)).thenReturn(null);
        when(environment.getArgument(PROCESS_VARIABLES)).thenReturn(null);

        ProcessInstance result = instanceMutation.startProcess(DUMMY_ID, DUMMY_CONTAINER_ID,
                                                               null, null, environment);

        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result.getProcessId()).isEqualTo(DUMMY_ID);
            softAssertions.assertThat(result.getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
            softAssertions.assertThat(result.getVariables()).isEqualTo(DUMMY_VARIABLES);
        }));
    }

    @Test
    public void testStartProcessWithCorrelationKey() {
        when(instanceRepository.startProcess(DUMMY_ID, DUMMY_CONTAINER_ID, DUMMY_CORRELATION_KEY, true))
                .thenReturn(processInstanceWithVariables);
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(true);
        when(environment.getArgument(CORRELATION_KEY)).thenReturn(DUMMY_CORRELATION_KEY);
        when(environment.getArgument(PROCESS_VARIABLES)).thenReturn(null);

        ProcessInstance result = instanceMutation.startProcess(DUMMY_ID, DUMMY_CONTAINER_ID,
                                                               DUMMY_CORRELATION_KEY, null, environment);

        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result.getProcessId()).isEqualTo(DUMMY_ID);
            softAssertions.assertThat(result.getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
            softAssertions.assertThat(result.getCorrelationKey()).isEqualTo(DUMMY_CORRELATION_KEY);
            softAssertions.assertThat(result.getVariables()).isEqualTo(DUMMY_VARIABLES);
        }));
    }

    @Test
    public void testStartProcessWithProcessVariables() {
        when(instanceRepository.startProcess(DUMMY_ID, DUMMY_CONTAINER_ID, null, DUMMY_VARIABLES, true))
                .thenReturn(processInstanceWithVariables);
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(true);
        when(environment.getArgument(CORRELATION_KEY)).thenReturn(null);
        when(environment.getArgument(PROCESS_VARIABLES)).thenReturn(DUMMY_VARIABLES);

        ProcessInstance result = instanceMutation.startProcess(DUMMY_ID, DUMMY_CONTAINER_ID,
                                                               null, DUMMY_VARIABLES , environment);

        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result.getProcessId()).isEqualTo(DUMMY_ID);
            softAssertions.assertThat(result.getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
            softAssertions.assertThat(result.getCorrelationKey()).isEqualTo(DUMMY_CORRELATION_KEY);
            softAssertions.assertThat(result.getVariables()).isEqualTo(DUMMY_VARIABLES);
        }));
    }

    @Test
    public void testStartProcessWithCorrelationKeyWithProcessVariables() {
        when(instanceRepository.startProcess(DUMMY_ID, DUMMY_CONTAINER_ID, DUMMY_CORRELATION_KEY, DUMMY_VARIABLES, true))
                .thenReturn(processInstanceWithVariables);
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(true);
        when(environment.getArgument(CORRELATION_KEY)).thenReturn(CORRELATION_KEY);
        when(environment.getArgument(PROCESS_VARIABLES)).thenReturn(DUMMY_VARIABLES);

        ProcessInstance result = instanceMutation.startProcess(DUMMY_ID, DUMMY_CONTAINER_ID,
                                                               DUMMY_CORRELATION_KEY, DUMMY_VARIABLES , environment);

        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result.getProcessId()).isEqualTo(DUMMY_ID);
            softAssertions.assertThat(result.getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
            softAssertions.assertThat(result.getCorrelationKey()).isEqualTo(DUMMY_CORRELATION_KEY);
            softAssertions.assertThat(result.getVariables()).isEqualTo(DUMMY_VARIABLES);
        }));
    }

    @Test
    public void testStartProcesses() {
        when(instanceRepository.startProcesses(DUMMY_ID, DUMMY_CONTAINER_ID, DUMMY_BATCH_SIZE, true))
                .thenReturn(Collections.singletonList(processInstanceWithVariables));
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(true);
        when(environment.getArgument(CORRELATION_KEY)).thenReturn(null);
        when(environment.getArgument(PROCESS_VARIABLES)).thenReturn(null);

        List<ProcessInstance> result = instanceMutation.startProcesses(DUMMY_ID, DUMMY_CONTAINER_ID,
                                                               null, null, DUMMY_BATCH_SIZE, environment);

        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
            softAssertions.assertThat(result.get(0).getProcessId()).isEqualTo(DUMMY_ID);
            softAssertions.assertThat(result.get(0).getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
            softAssertions.assertThat(result.get(0).getVariables()).isEqualTo(DUMMY_VARIABLES);
        }));
    }

    @Test
    public void testStartProcessesWithCorrelationKey() {
        when(instanceRepository.startProcesses(DUMMY_ID, DUMMY_CONTAINER_ID, DUMMY_CORRELATION_KEY, DUMMY_BATCH_SIZE, true))
                .thenReturn(Collections.singletonList(processInstanceWithVariables));
        when(environment.getSelectionSet()).thenReturn(selectionSet);
        when(selectionSet.contains(VARIABLES)).thenReturn(true);
        when(environment.getArgument(CORRELATION_KEY)).thenReturn(DUMMY_CORRELATION_KEY);
        when(environment.getArgument(PROCESS_VARIABLES)).thenReturn(null);

        List<ProcessInstance> result = instanceMutation.startProcesses(DUMMY_ID, DUMMY_CONTAINER_ID,
                                                               DUMMY_CORRELATION_KEY, null, DUMMY_BATCH_SIZE, environment);

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
        when(environment.getArgument(CORRELATION_KEY)).thenReturn(null);
        when(environment.getArgument(PROCESS_VARIABLES)).thenReturn(DUMMY_VARIABLES);

        List<ProcessInstance> result = instanceMutation.startProcesses(DUMMY_ID, DUMMY_CONTAINER_ID,
                                                               null, DUMMY_VARIABLES, DUMMY_BATCH_SIZE, environment);

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
        when(environment.getArgument(CORRELATION_KEY)).thenReturn(CORRELATION_KEY);
        when(environment.getArgument(PROCESS_VARIABLES)).thenReturn(DUMMY_VARIABLES);

        List<ProcessInstance> result = instanceMutation.startProcesses(DUMMY_ID, DUMMY_CONTAINER_ID,
                                                                       DUMMY_CORRELATION_KEY, DUMMY_VARIABLES, DUMMY_BATCH_SIZE, environment);

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
        when(instanceRepository.abortProcessInstances(Collections.singletonList(ID), DUMMY_CONTAINER_ID))
                .thenReturn(Collections.singletonList(processInstanceWithoutVariables));
        when(environment.getArgument(CONTAINER_ID)).thenReturn(DUMMY_CONTAINER_ID);

        List<ProcessInstance> result = instanceMutation.abortProcessInstances(Collections.singletonList(ID), DUMMY_CONTAINER_ID, environment);

        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result).isNotNull();
            softAssertions.assertThat(result.get(0).getId()).isEqualTo(ID);
            softAssertions.assertThat(result.get(0).getContainerId()).isEqualTo(DUMMY_CONTAINER_ID);
        }));
    }

    @Test
    public void testAbortProcessInstancesWithoutContainerId() {
        when(instanceRepository.abortProcessInstances(Collections.singletonList(ID), null))
                .thenReturn(Collections.singletonList(processInstanceWithoutVariables));
        when(environment.getArgument(CONTAINER_ID)).thenReturn(null);

        List<ProcessInstance> result = instanceMutation.abortProcessInstances(Collections.singletonList(ID), null, environment);

        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result).isNotNull();
            softAssertions.assertThat(result.get(0).getId()).isEqualTo(ID);
        }));
    }

    @Test
    public void testSignalProcessInstancesWithContainerId() {
        when(instanceRepository.signalProcessInstances(DUMMY_CONTAINER_ID,
                                                       Collections.singletonList(ID),
                                                       DUMMY_SIGNAL_NAME, DUMMY_EVENT))
        .thenReturn(Collections.singletonList(processInstanceWithoutVariables));

        List<ProcessInstance> result = instanceMutation.signalProcessInstances(DUMMY_CONTAINER_ID,
                                                                               Collections.singletonList(ID),
                                                                               DUMMY_SIGNAL_NAME,
                                                                               DUMMY_EVENT);
        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result).isNotNull();
            softAssertions.assertThat(result.get(0).getId()).isEqualTo(ID);
        }));
    }

    @Test
    public void testSignalProcessInstancesWithoutContainerId() {
        when(instanceRepository.signalProcessInstances(Collections.singletonList(ID),
                                                       DUMMY_SIGNAL_NAME, DUMMY_EVENT))
                .thenReturn(Collections.singletonList(processInstanceWithoutVariables));
        List<ProcessInstance> result = instanceMutation.signalProcessInstances(null,
                                                                               Collections.singletonList(ID),
                                                                               DUMMY_SIGNAL_NAME,
                                                                               DUMMY_EVENT);
        SoftAssertions.assertSoftly((softAssertions -> {
            softAssertions.assertThat(result).isNotNull();
            softAssertions.assertThat(result.get(0).getId()).isEqualTo(ID);
        }));
    }
}
