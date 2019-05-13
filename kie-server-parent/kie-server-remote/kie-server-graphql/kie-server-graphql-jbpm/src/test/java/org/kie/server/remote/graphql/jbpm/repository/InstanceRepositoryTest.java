package org.kie.server.remote.graphql.jbpm.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.jbpm.kie.services.impl.model.UserTaskInstanceDesc;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.jbpm.services.task.query.TaskSummaryImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.task.model.Status;
import org.kie.internal.KieInternalServices;
import org.kie.internal.process.CorrelationKey;
import org.kie.internal.process.CorrelationKeyFactory;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.TaskInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.remote.graphql.jbpm.filter.ProcessInstanceFilter;
import org.kie.server.remote.graphql.jbpm.filter.TaskInstanceFilter;
import org.kie.server.services.api.KieServerRegistry;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InstanceRepositoryTest {

    private final static long ID = 1L;
    private final static int ACTIVE_STATE = 1;
    private final static int DUMMY_BATCH_SIZE = 1;
    private final static int PRIORITY = 1;
    private final static int STATE = 1;
    private final static String STATUS = "Created";
    private final static String CONTAINER_ID_ONE = "container-id-1";
    private final static String CONTAINER_ID_TWO = "container-id-2";
    private final static String PROCESS_ID = "dummyId";
    private final static String PROCESS_NAME = "dummyName";
    private final static String SUBJECT = "dummySubject";
    private final static String NAME = "name";
    private final static String DESCRIPTION = "description";
    private final static String CORRELATION_KEY = "ABC:DEF";
    private final static String VERSION = "1:0:0";
    private final static Date DATE = new Date();
    private final static String USER_ID = "tester";
    private final static boolean SKIPPABLE = true;
    private final static String DUMMY_USER_ID = "dummy-user-id";
    private final static String FORM_NAME = "form-name";
    private final static String DUMMY_VARIABLES_KEY = "dummy-variables-key";
    private final static String DUMMY_VARIABLES_VALUE = "dummy-variables-value";
    private final static Map<String, Object> DUMMY_VARIABLES =  Collections.singletonMap(DUMMY_VARIABLES_KEY, DUMMY_VARIABLES_VALUE);


    private CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

    @Mock
    private KieServerRegistry context;

    @Mock
    private ProcessService processService;

    @Mock
    private RuntimeDataService runtimeDataService;

    private ProcessInstance expectedInstance;
    private ProcessInstanceDesc processInstanceDesc;
    private org.kie.api.task.model.TaskSummary taskSummary = new TaskSummaryImpl(ID,
                                                                                 PROCESS_NAME,
                                                                                 SUBJECT,
                                                                                 DESCRIPTION,
                                                                                 Status.Created,
                                                                                 PRIORITY,
                                                                                 USER_ID,
                                                                                 USER_ID,
                                                                                 DATE,
                                                                                 DATE,
                                                                                 DATE,
                                                                                 PROCESS_ID,
                                                                                 ID,
                                                                                 ID,
                                                                                 CONTAINER_ID_ONE,
                                                                                 SKIPPABLE);

    private UserTaskInstanceDesc userTaskInstanceDesc = new UserTaskInstanceDesc(ID,
                                                                                 NAME,
                                                                                 DESCRIPTION,
                                                                                 PRIORITY,
                                                                                 DATE,
                                                                                 FORM_NAME);

    private Map<String, Object> variables;
    private CorrelationKey actualCorrelationKey;

    InstanceRepository instanceRepository;

    @Before
    public void setUp() throws Exception {
        expectedInstance = createDummyProcessInstances().get(0);
        String[] correlationProperties = CORRELATION_KEY.split(":");
        actualCorrelationKey = correlationKeyFactory.newCorrelationKey(Arrays.asList(correlationProperties));


        processInstanceDesc = new org.jbpm.kie.services.impl.model.ProcessInstanceDesc(ID,
                                                                                       PROCESS_ID,
                                                                                       PROCESS_NAME,
                                                                                       VERSION,
                                                                                       STATE,
                                                                                       CONTAINER_ID_ONE,
                                                                                       DATE,
                                                                                       DUMMY_USER_ID,
                                                                                       CORRELATION_KEY);

        instanceRepository = new InstanceRepository(runtimeDataService, processService, context);
    }

    @Test
    public void getProcessInstanceWithVars() {
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        ProcessInstance processInstance = instanceRepository.getProcessInstance(1L, CONTAINER_ID_ONE, true);

        assertProcessInstanceDesc(processInstance, variables);
    }

    @Test
    public void getProcessInstanceWithoutVars() {
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        ProcessInstance processInstance = instanceRepository.getProcessInstance(1L, CONTAINER_ID_ONE, false);

        assertProcessInstanceDesc(processInstance, null);
    }

    @Test
    public void getProcessInstanceVariables() {
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);

        Map<String, Object> resultVariables = instanceRepository.getProcessInstanceVariables(1L, CONTAINER_ID_ONE);
        Assertions.assertThat(resultVariables).isEqualTo(variables);
    }

    @Test
    public void getAllProcessInstancesFilterIsNull() {
        when(runtimeDataService.getProcessInstances(anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(DUMMY_BATCH_SIZE, null);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertProcessInstanceDesc(result.get(0), null);

        verify(runtimeDataService, times(1)).getProcessInstances(anyObject());
    }

    @Test
    public void getAllProcessInstancesFilterStatesAndInitiator() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setStatesIn(Arrays.asList(1, 2, 3, 4));
        filter.setInitiator(USER_ID);

        when(runtimeDataService.getProcessInstances(eq(filter.getStatesIn()),
                                                    eq(filter.getInitiator()),
                                                    anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(DUMMY_BATCH_SIZE, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertProcessInstanceDesc(result.get(0), null);
        verify(runtimeDataService,
               times(1)).getProcessInstances(eq(filter.getStatesIn()),
                                                                   eq(filter.getInitiator()),
                                                                   anyObject());
    }

    @Test
    public void getAllProcessInstancesFilterStatesAndInitiatorAndProcessId() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setStatesIn(Arrays.asList(1, 2, 3, 4));
        filter.setInitiator(USER_ID);
        filter.setProcessId(PROCESS_ID);

        when(runtimeDataService.getProcessInstancesByProcessId(eq(filter.getStatesIn()),
                                                               eq(filter.getProcessId()),
                                                               eq(filter.getInitiator()),
                                                               anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(DUMMY_BATCH_SIZE, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertProcessInstanceDesc(result.get(0), null);

        verify(runtimeDataService,
               times(1)).getProcessInstancesByProcessId(eq(filter.getStatesIn()),
                                                                              eq(filter.getProcessId()),
                                                                              eq(filter.getInitiator()),
                                                                              anyObject());
    }

    @Test
    public void getAllProcessInstancesFilterStatesAndInitiatorAndProcessName() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setStatesIn(Arrays.asList(1, 2, 3, 4));
        filter.setInitiator(USER_ID);
        filter.setProcessName(PROCESS_NAME);

        when(runtimeDataService.getProcessInstancesByProcessName(eq(filter.getStatesIn()),
                                                                 eq(filter.getProcessName()),
                                                                 eq(filter.getInitiator()),
                                                                 anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(DUMMY_BATCH_SIZE, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertProcessInstanceDesc(result.get(0), null);

        verify(runtimeDataService,
               times(1)).getProcessInstancesByProcessName(eq(filter.getStatesIn()),
                                                                                eq(filter.getProcessName()),
                                                                                eq(filter.getInitiator()),
                                                                                anyObject());
    }

    @Test
    public void getAllProcessInstancesFilterStatesAndProcessName() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setStatesIn(Arrays.asList(1, 2, 3, 4));
        filter.setContainerId(CONTAINER_ID_ONE);

        when(runtimeDataService.getProcessInstancesByDeploymentId(eq(filter.getContainerId()),
                                                                  eq(filter.getStatesIn()),
                                                                  anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(DUMMY_BATCH_SIZE, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertProcessInstanceDesc(result.get(0), null);

        verify(runtimeDataService,
               times(1)).getProcessInstancesByDeploymentId(eq(filter.getContainerId()), eq(filter.getStatesIn()),
                                                           anyObject());
    }

    @Test
    public void getAllProcessInstancesFilterStatesAndVariableName() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setStatesIn(Arrays.asList(1, 2, 3, 4));
        filter.setVariableName("var1");
        filter.setVariableValue(null);

        when(runtimeDataService.getProcessInstancesByVariable(eq(filter.getVariableName()),
                                                              eq(filter.getStatesIn()),
                                                              anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(DUMMY_BATCH_SIZE, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertProcessInstanceDesc(result.get(0), null);

        verify(runtimeDataService,
               times(1)).getProcessInstancesByVariable(eq(filter.getVariableName()), eq(filter.getStatesIn()),
                                                       anyObject());
    }

    @Test
    public void getAllProcessInstancesFilterStatesAndVariableNameAndVariableValue() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setStatesIn(Arrays.asList(1, 2, 3, 4));
        filter.setVariableName("var2");
        filter.setVariableValue("1");

        when(runtimeDataService.getProcessInstancesByVariableAndValue(eq(filter.getVariableName()),
                                                                      eq(filter.getVariableValue()),
                                                                      eq(filter.getStatesIn()),
                                                                      anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(DUMMY_BATCH_SIZE, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertProcessInstanceDesc(result.get(0), null);

        verify(runtimeDataService,
               times(1)).getProcessInstancesByVariableAndValue(eq(filter.getVariableName()),
                                                               eq(filter.getVariableValue()),
                                                               eq(filter.getStatesIn()),
                                                               anyObject());
    }

    @Test
    public void getAllProcessInstancesFilterStatesAndCorrelationKey() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setStatesIn(Arrays.asList(1, 2, 3, 4));
        filter.setCorrelationKey(CORRELATION_KEY);

        String[] correlationProperties = filter.getCorrelationKey().split(":");
        CorrelationKey actualCorrelationKey = correlationKeyFactory.newCorrelationKey(Arrays.asList(correlationProperties));

        when(runtimeDataService.getProcessInstancesByCorrelationKeyAndStatus(eq(actualCorrelationKey),
                                                                             eq(filter.getStatesIn()),
                                                                      anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(DUMMY_BATCH_SIZE, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertProcessInstanceDesc(result.get(0), null);

        verify(runtimeDataService,
               times(1)).getProcessInstancesByCorrelationKeyAndStatus(eq(actualCorrelationKey),
                                                                       eq(filter.getStatesIn()),
                                                                       anyObject());
    }

    @Test
    public void getAllProcessInstancesFilterCorrelationKey() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setCorrelationKey(CORRELATION_KEY);

        String[] correlationProperties = filter.getCorrelationKey().split(":");
        CorrelationKey actualCorrelationKey = correlationKeyFactory.newCorrelationKey(Arrays.asList(correlationProperties));

        when(runtimeDataService.getProcessInstancesByCorrelationKey(eq(actualCorrelationKey),
                                                                             anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(DUMMY_BATCH_SIZE, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertProcessInstanceDesc(result.get(0), null);

        verify(runtimeDataService,
               times(1)).getProcessInstancesByCorrelationKey(eq(actualCorrelationKey),
                                                             anyObject());
    }

    @Test
    public void getAllProcessInstances_filterProcessIdOnly() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setProcessId(PROCESS_ID);

        when(runtimeDataService.getProcessInstancesByProcessDefinition(eq(filter.getProcessId()),
                                                                 anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(DUMMY_BATCH_SIZE, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertProcessInstanceDesc(result.get(0), null);

        verify(runtimeDataService,
               times(1)).getProcessInstancesByProcessDefinition(eq(filter.getProcessId()),
                                                                anyObject());
    }

    @Test
    public void getAllProcessInstancesUnsupportedFilterCombination() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setProcessId(PROCESS_ID);
        filter.setContainerId(CONTAINER_ID_ONE);

        Assertions.assertThatThrownBy(() -> instanceRepository.getAllProcessInstances(DUMMY_BATCH_SIZE, filter))
                .hasMessageContaining("Selected filter properties do not match any know combinations.");
    }

    @Test
    public void getAllTasksFilterStatesAndBusinessAdminId() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setStatesIn(Collections.singletonList("Created"));
        filter.setBusinessAdminId(USER_ID);

        List<Status> taskStatusList = getSelectedStatusList(filter.getStatesIn());

        when(runtimeDataService.getTasksAssignedAsBusinessAdministratorByStatus(eq(filter.getBusinessAdminId()),
                                                                                eq(taskStatusList),
                                                                       anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(DUMMY_BATCH_SIZE, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertTaskSummary(result.get(0));

        verify(runtimeDataService,
               times(1)).getTasksAssignedAsBusinessAdministratorByStatus(eq(filter.getBusinessAdminId()), eq(taskStatusList),
                                                                          anyObject());
    }

    @Test
    public void getAllTasksFilterStatesAndProcessInstanceId() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setStatesIn(Collections.singletonList(STATUS));
        filter.setProcessInstanceId(1L);

        List<Status> taskStatusList = getSelectedStatusList(filter.getStatesIn());

        when(runtimeDataService.getTasksByStatusByProcessInstanceId(eq(filter.getProcessInstanceId()), eq(taskStatusList),
                                                                                anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(DUMMY_BATCH_SIZE, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertTaskSummary(result.get(0));

        verify(runtimeDataService,
               times(1)).getTasksByStatusByProcessInstanceId(eq(filter.getProcessInstanceId()), eq(taskStatusList),
                                                             anyObject());
    }


    @Test
    public void getAllTasksFilterStatesAndOwnerAndVariableName() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setStatesIn(Collections.singletonList(STATUS));
        filter.setOwnerId(USER_ID);
        filter.setVariableName(USER_ID);

        List<Status> taskStatusList = getSelectedStatusList(filter.getStatesIn());

        when(runtimeDataService.getTasksByVariable(eq(filter.getOwnerId()), eq(filter.getVariableName()),eq(taskStatusList),
                                                                    anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(1, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertTaskSummary(result.get(0));

        verify(runtimeDataService,
               times(1)).getTasksByVariable(eq(filter.getOwnerId()), eq(filter.getVariableName()), eq(taskStatusList),
                                            anyObject());
    }

    @Test
    public void getAllTasksFilterStatesAndOwnerAndVariableNameAndVariableValue() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setStatesIn(Collections.singletonList("Created"));
        filter.setOwnerId(USER_ID);
        filter.setVariableName(USER_ID);
        filter.setVariableValue("test-value");

        List<Status> taskStatusList = getSelectedStatusList(filter.getStatesIn());

        when(runtimeDataService.getTasksByVariableAndValue(eq(filter.getOwnerId()), eq(filter.getVariableName()),
                                                           eq(filter.getVariableValue()),eq(taskStatusList),
                                                   anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(1, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertTaskSummary(result.get(0));

        verify(runtimeDataService,
               times(1)).getTasksByVariableAndValue(eq(filter.getOwnerId()), eq(filter.getVariableName()),
                                                    eq(filter.getVariableValue()),eq(taskStatusList),
                                                    anyObject());
    }

    @Test
    public void getAllTasksFilterPotentialOwnerIdAndGroupIds() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setPotentialOwnerId(USER_ID);
        filter.setGroupIds(Collections.singletonList("admin"));


        when(runtimeDataService.getTasksAssignedAsPotentialOwner(eq(filter.getPotentialOwnerId()), eq(filter.getGroupIds()),
                                                           anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(1, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertTaskSummary(result.get(0));
        verify(runtimeDataService,
               times(1)).getTasksAssignedAsPotentialOwner(eq(filter.getPotentialOwnerId()), eq(filter.getGroupIds()),
                                                         anyObject());
    }

    @Test
    public void getAllTasksFilterOwnerId() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setOwnerId(USER_ID);

        when(runtimeDataService.getTasksOwned(eq(filter.getOwnerId()),
                                                                 anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(1, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertTaskSummary(result.get(0));

        verify(runtimeDataService,
               times(1)).getTasksOwned(eq(filter.getOwnerId()),
                                                          anyObject());
    }


    @Test
    public void getAllTasksFilterStatesAndPotOwnerIdAndGroupId() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setStatesIn(Collections.singletonList(STATUS));
        filter.setPotentialOwnerId(USER_ID);
        filter.setGroupIds(Collections.singletonList("admin"));

        List<Status> taskStatusList = getSelectedStatusList(filter.getStatesIn());

        when(runtimeDataService.getTasksAssignedAsPotentialOwner(eq(filter.getPotentialOwnerId()), eq(filter.getGroupIds()), eq(taskStatusList),
                                                                                anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(1, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertTaskSummary(result.get(0));

        verify(runtimeDataService,
               times(1)).getTasksAssignedAsPotentialOwner(eq(filter.getPotentialOwnerId()), eq(filter.getGroupIds()), eq(taskStatusList),
                                                          anyObject());
    }

    @Test
    public void getAllTasksFilterStatesAndPotOwnerIdAndFromDate() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setStatesIn(Collections.singletonList(STATUS));
        filter.setFromExpirationalDate(DATE);
        filter.setPotentialOwnerId(USER_ID);

        List<Status> taskStatusList = getSelectedStatusList(filter.getStatesIn());

        when(runtimeDataService.getTasksAssignedAsPotentialOwnerByExpirationDateOptional(eq(filter.getPotentialOwnerId()),
                                                                                         eq(taskStatusList),
                                                                                         eq(filter.getFromExpirationalDate()),
                                                                 anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(1, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertTaskSummary(result.get(0));
        verify(runtimeDataService,
               times(1)).getTasksAssignedAsPotentialOwnerByExpirationDateOptional(eq(filter.getPotentialOwnerId()), eq(taskStatusList),
                                                                                  eq(filter.getFromExpirationalDate()),
                                                                                  anyObject());
    }

    @Test
    public void getAllTasksFilterStatesAndPotOwnerId() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setStatesIn(Collections.singletonList(STATUS));
        filter.setPotentialOwnerId(USER_ID);

        List<Status> taskStatusList = getSelectedStatusList(filter.getStatesIn());

        when(runtimeDataService.getTasksAssignedAsPotentialOwnerByStatus(eq(filter.getPotentialOwnerId()), eq(taskStatusList),
                                                                                         anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(DUMMY_BATCH_SIZE, filter);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        });

        assertTaskSummary(result.get(0));

        verify(runtimeDataService,
               times(1)).getTasksAssignedAsPotentialOwnerByStatus(eq(filter.getPotentialOwnerId()), eq(taskStatusList),
                                                                  anyObject());
    }

    @Test
    public void getAllTasksFilterCombinationUnsupported() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setBusinessAdminId("admin");

        Assertions.assertThatThrownBy(() -> instanceRepository.getAllTasks(DUMMY_BATCH_SIZE, filter))
                .hasMessageContaining("Selected filter properties do not match any know combinations.");
    }

    @Test
    public void getTaskInstance() {
        when(runtimeDataService.getTaskById(1L)).thenReturn(userTaskInstanceDesc);
        TaskInstance result = instanceRepository.getTaskInstance(1L);
        Assertions.assertThat(result).isNotNull();

        verify(runtimeDataService, times(1)).getTaskById(1L);
    }

    @Test
    public void startProcessWithIdAndContainerIdWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);

        ProcessInstance result = instanceRepository.startProcess(PROCESS_ID, CONTAINER_ID_ONE, true);
        Assertions.assertThat(result).isNotNull();
        assertProcessInstanceDesc(result, variables);
    }

    @Test
    public void startProcessWithIdAndContainerIdWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        ProcessInstance result = instanceRepository.startProcess(PROCESS_ID, CONTAINER_ID_ONE, false);
        Assertions.assertThat(result).isNotNull();
        assertProcessInstanceDesc(result, null);
    }

    @Test
    public void startProcessWithIdAndContainerIdAndCorrelationKeyWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID, actualCorrelationKey)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);

        ProcessInstance result = instanceRepository.startProcess(PROCESS_ID, CONTAINER_ID_ONE, CORRELATION_KEY, true);
        Assertions.assertThat(result).isNotNull();
        assertProcessInstanceDesc(result, variables);
    }

    @Test
    public void startProcessWithIdAndContainerIdAndCorrelationKeyWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID, actualCorrelationKey)).thenReturn(ID);
        when(runtimeDataService.getProcessInstanceById(ID)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        ProcessInstance result = instanceRepository.startProcess(PROCESS_ID, CONTAINER_ID_ONE, CORRELATION_KEY, false);
        Assertions.assertThat(result).isNotNull();
        assertProcessInstanceDesc(result, null);
    }

    @Test
    public void startProcessWithIdAndContainerIdAndCorrelationKeyAndVariablesWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID, actualCorrelationKey, variables)).thenReturn(ID);
        when(runtimeDataService.getProcessInstanceById(ID)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        ProcessInstance result = instanceRepository.startProcess(PROCESS_ID, CONTAINER_ID_ONE, CORRELATION_KEY, variables, false);
        Assertions.assertThat(result).isNotNull();
        assertProcessInstanceDesc(result, null);
    }

    @Test
    public void startProcessWithIdAndContainerIdAndCorrelationKeyAndVariablesWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID, actualCorrelationKey, variables)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);

        ProcessInstance result = instanceRepository.startProcess(PROCESS_ID, CONTAINER_ID_ONE, CORRELATION_KEY, variables, true);
        Assertions.assertThat(result).isNotNull();
        assertProcessInstanceDesc(result, variables);
    }

    @Test
    public void startProcessWithIdAndContainerIdAndVariablesWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID,  variables)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        ProcessInstance result = instanceRepository.startProcess(PROCESS_ID, CONTAINER_ID_ONE, null, variables, false);
        Assertions.assertThat(result).isNotNull();
        assertProcessInstanceDesc(result, null);
    }

    @Test
    public void startProcessWithIdAndContainerIdAndVariablesWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID, variables)).thenReturn(ID);
        when(runtimeDataService.getProcessInstanceById(ID)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, ID)).thenReturn(variables);

        ProcessInstance result = instanceRepository.startProcess(PROCESS_ID, CONTAINER_ID_ONE, null, variables, true);
        Assertions.assertThat(result).isNotNull();
        assertProcessInstanceDesc(result, variables);
    }

    @Test
    public void startProcessesWithIdAndContainerIdWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID)).thenReturn(ID);
        when(runtimeDataService.getProcessInstanceById(ID)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, ID)).thenReturn(variables);

        List<ProcessInstance> result = instanceRepository.startProcesses(PROCESS_ID, CONTAINER_ID_ONE, DUMMY_BATCH_SIZE,true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        assertProcessInstanceDesc(result.get(0), variables);
    }

    @Test
    public void startProcessesWithIdAndContainerIdWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID)).thenReturn(ID);
        when(runtimeDataService.getProcessInstanceById(ID)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        List<ProcessInstance> result = instanceRepository.startProcesses(PROCESS_ID, CONTAINER_ID_ONE,DUMMY_BATCH_SIZE,
                                                                         false);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        assertProcessInstanceDesc(result.get(0), null);
    }

    @Test
    public void startProcessesWithIdAndContainerIdAndCorrelationKeyWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID, actualCorrelationKey)).thenReturn(ID);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, ID)).thenReturn(variables);

        List<ProcessInstance> result = instanceRepository.startProcesses(PROCESS_ID, CONTAINER_ID_ONE, CORRELATION_KEY,
                                                                         DUMMY_BATCH_SIZE,true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        assertProcessInstanceDesc(result.get(0), variables);
    }

    @Test
    public void startProcessesWithIdAndContainerIdAndCorrelationKeyWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID, actualCorrelationKey)).thenReturn(ID);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        List<ProcessInstance> result = instanceRepository.startProcesses(PROCESS_ID, CONTAINER_ID_ONE,
                                                                         CORRELATION_KEY,DUMMY_BATCH_SIZE, false);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        assertProcessInstanceDesc(result.get(0), null);
    }

    @Test
    public void startProcessesWithIdAndContainerIdAndCorrelationKeyAndVariablesWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID, actualCorrelationKey, variables)).thenReturn(ID);
        when(runtimeDataService.getProcessInstanceById(ID)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, ID)).thenReturn(variables);

        List<ProcessInstance> result = instanceRepository.startProcesses(PROCESS_ID, CONTAINER_ID_ONE,
                                                                         CORRELATION_KEY, variables,DUMMY_BATCH_SIZE,true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        assertProcessInstanceDesc(result.get(0), variables);
    }

    @Test
    public void startProcessesWithIdAndContainerIdAndCorrelationKeyAndVariablesWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID, actualCorrelationKey, variables)).thenReturn(ID);
        when(runtimeDataService.getProcessInstanceById(ID)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        List<ProcessInstance> result = instanceRepository.startProcesses(PROCESS_ID, CONTAINER_ID_ONE,
                                                                         CORRELATION_KEY, variables,
                                                                         DUMMY_BATCH_SIZE, false);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        assertProcessInstanceDesc(result.get(0), null);
    }

    @Test
    public void startProcessesWithIdAndContainerIdVariablesWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID, variables)).thenReturn(ID);
        when(runtimeDataService.getProcessInstanceById(ID)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, ID)).thenReturn(variables);

        List<ProcessInstance> result = instanceRepository.startProcesses(PROCESS_ID, CONTAINER_ID_ONE,
                                                                         null, variables,DUMMY_BATCH_SIZE,true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        assertProcessInstanceDesc(result.get(0), variables);
    }

    @Test
    public void startProcessesWithIdAndContainerIdAndVariablesWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, PROCESS_ID, variables)).thenReturn(ID);
        when(runtimeDataService.getProcessInstanceById(ID)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        List<ProcessInstance> result = instanceRepository.startProcesses(PROCESS_ID, CONTAINER_ID_ONE,
                                                                         null, variables,
                                                                         DUMMY_BATCH_SIZE, false);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        assertProcessInstanceDesc(result.get(0), null);
    }

    @Test
    public void abortProcessInstancesUsingIds() {
        List<Long> ids = Collections.singletonList(ID);
        doNothing().when(processService).abortProcessInstances(ids);
        when(runtimeDataService.getProcessInstanceById(ID)).thenReturn(processInstanceDesc);

        List<ProcessInstance> result = instanceRepository.abortProcessInstances(ids, null, false);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        assertProcessInstanceDesc(result.get(0), null);

        verify(processService, times(1)).abortProcessInstances(ids);
        verify(processService, times(0)).getProcessInstanceVariables(ids.get(0));
    }

    @Test
    public void abortProcessInstancesUsingIdsAndContainerIdWithVars() {
        List<Long> ids = Collections.singletonList(1L);
        doNothing().when(processService).abortProcessInstances(CONTAINER_ID_ONE, ids);
        when(runtimeDataService.getProcessInstanceById(ID)).thenReturn(processInstanceDesc);
        when(processService.getProcessInstanceVariables(ID)).thenReturn(variables);

        List<ProcessInstance> result = instanceRepository.abortProcessInstances(ids, CONTAINER_ID_ONE, true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        assertProcessInstanceDesc(result.get(0), variables);

        verify(processService, times(1)).abortProcessInstances(CONTAINER_ID_ONE, ids);
        verify(processService, times(1)).getProcessInstanceVariables(ids.get(0));
    }

    @Test
    public void testSignalProcessInstancesWithVars() {
        List<Long> ids = Collections.singletonList(1L);
        doNothing().when(processService).signalProcessInstances(ids, NAME, "event");
        when(runtimeDataService.getProcessInstanceById(ID)).thenReturn(processInstanceDesc);
        when(processService.getProcessInstanceVariables(ID)).thenReturn(variables);

        List<ProcessInstance> result = instanceRepository.signalProcessInstances(ids, NAME, "event", true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        assertProcessInstanceDesc(result.get(0), variables);

        verify(processService, times(1)).getProcessInstanceVariables(ids.get(0));
    }

    @Test
    public void testSignalProcessInstancesWithContainerIdWithVars() {
        List<Long> ids = Collections.singletonList(1L);
        doNothing().when(processService).signalProcessInstances(CONTAINER_ID_ONE, ids, NAME, "event");
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(processService.getProcessInstanceVariables(ID)).thenReturn(variables);

        List<ProcessInstance> result = instanceRepository.signalProcessInstances(CONTAINER_ID_ONE, ids,
                                                                                 NAME, "event", true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(DUMMY_BATCH_SIZE);
        assertProcessInstanceDesc(result.get(0), variables);

        verify(processService, times(1)).getProcessInstanceVariables(ids.get(0));
    }

    private void assertProcessInstanceDesc(ProcessInstance processInstance, Map<String, Object> variables) {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(processInstance.getId()).isEqualTo(ID);
            softly.assertThat(processInstance.getProcessId()).isEqualTo(PROCESS_ID);
            softly.assertThat(processInstance.getProcessName()).isEqualTo(PROCESS_NAME);
            softly.assertThat(processInstance.getProcessVersion()).isEqualTo(VERSION);
            softly.assertThat(processInstance.getState()).isEqualTo(STATE);
            softly.assertThat(processInstance.getContainerId()).isEqualTo(CONTAINER_ID_ONE);
            softly.assertThat(processInstance.getDate()).isEqualTo(DATE);
            softly.assertThat(processInstance.getInitiator()).isEqualTo(DUMMY_USER_ID);
            softly.assertThat(processInstance.getCorrelationKey()).isEqualTo(CORRELATION_KEY);
            softly.assertThat(processInstance.getVariables()).isEqualTo(variables);
            // no need to call assertAll, it is done by assertSoftly.
        });
    }

    private void assertTaskSummary(TaskSummary taskSummary) {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(taskSummary.getId()).isEqualTo(ID);
            softly.assertThat(taskSummary.getProcessId()).isEqualTo(PROCESS_ID);
            softly.assertThat(taskSummary.getSubject()).isEqualTo(SUBJECT);
            softly.assertThat(taskSummary.getDescription()).isEqualTo(DESCRIPTION);
            softly.assertThat(taskSummary.getStatus()).isEqualTo(STATUS);
            softly.assertThat(taskSummary.getContainerId()).isEqualTo(CONTAINER_ID_ONE);
            softly.assertThat(taskSummary.getCreatedBy()).isEqualTo(USER_ID);
            softly.assertThat(taskSummary.getActualOwner()).isEqualTo(USER_ID);
            softly.assertThat(taskSummary.getSkipable()).isEqualTo(SKIPPABLE);
            softly.assertThat(taskSummary.getCreatedOn()).isEqualTo(DATE);
            softly.assertThat(taskSummary.getExpirationTime()).isEqualTo(DATE);
            softly.assertThat(taskSummary.getActivationTime()).isEqualTo(DATE);
            // no need to call assertAll, it is done by assertSoftly.
        });
    }

    private List<ProcessInstance> createDummyProcessInstances() {
        variables = new HashMap<>();
        variables.put("var1", "hello");
        variables.put("var2", 1);

        List<ProcessInstance> processInstances = new ArrayList<>();
        processInstances.add(ProcessInstance.builder().id(1L).containerId(CONTAINER_ID_ONE).variables(variables).build());
        processInstances.add(ProcessInstance.builder().id(2L).containerId(CONTAINER_ID_TWO).variables(variables).build());

        return processInstances;
    }

    private List<Status> getSelectedStatusList(List<String> selectedStatuses) {
        ArrayList<Status> result = new ArrayList<>();
        for (String statusAsString : selectedStatuses) {
            result.add(Status.valueOf(statusAsString));
        }
        return result;
    }
}
