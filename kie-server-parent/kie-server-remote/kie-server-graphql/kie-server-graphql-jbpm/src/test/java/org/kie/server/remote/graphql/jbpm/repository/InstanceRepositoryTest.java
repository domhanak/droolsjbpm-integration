package org.kie.server.remote.graphql.jbpm.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.jbpm.kie.services.impl.model.UserTaskInstanceDesc;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.jbpm.services.task.query.TaskSummaryImpl;
import org.junit.After;
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

    private static final String CONTAINER_ID_ONE = "container-id-1";
    private static final String CONTAINER_ID_TWO = "container-id-2";
    private static final String PROCESS_ID = "dummyId";
    private static final String PROCESS_NAME = "dummyName";
    private static final String USER_ID = "tester";

    private CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

    @Mock
    private KieServerRegistry context;

    @Mock
    private ProcessService processService;

    @Mock
    private RuntimeDataService runtimeDataService;

    private ProcessInstance expectedInstance;
    private ProcessInstanceDesc processInstanceDesc;
    private org.kie.api.task.model.TaskSummary taskSummary;
    private UserTaskInstanceDesc userTaskInstanceDesc;
    private Map<String, Object> variables;
    private CorrelationKey actualCorrelationKey;

    InstanceRepository instanceRepository;

    @Before
    public void setUp() throws Exception {
        expectedInstance = createDummyProcessInstances().get(0);
        String[] correlationProperties = "ABC:DEF".split(":");
        actualCorrelationKey = correlationKeyFactory.newCorrelationKey(Arrays.asList(correlationProperties));


        processInstanceDesc = new org.jbpm.kie.services.impl.model.ProcessInstanceDesc(1L,
                                                                                       PROCESS_ID,
                                                                                       PROCESS_NAME,
                                                                                       "",
                                                                                       1,
                                                                                       CONTAINER_ID_ONE,
                                                                                       new Date(),
                                                                                       "tester",
                                                                                       "");

        taskSummary = new TaskSummaryImpl(1L,
                                          "dummyName",
                                          "dummySubject",
                                          "",
                                          Status.Created,
                                          1,
                                          USER_ID,
                                          USER_ID,
                                          new Date(),
                                          new Date(),
                                          new Date(),
                                          PROCESS_ID,
                                          1L,
                                          1L,
                                          CONTAINER_ID_ONE,
                                          true);

        userTaskInstanceDesc = new UserTaskInstanceDesc(1L, "name", "here", 1, new Date(), "form");

        instanceRepository = new InstanceRepository(runtimeDataService, processService, context);
    }

    @Test
    public void getProcessInstanceWithVars() {
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        ProcessInstance processInstance = instanceRepository.getProcessInstance(1L, CONTAINER_ID_ONE, true);
        Assertions.assertThat(processInstance.getContainerId()).isEqualTo(expectedInstance.getContainerId());
        Assertions.assertThat(processInstance.getVariables()).isNotNull();
        Assertions.assertThat(processInstance.getVariables()).isEqualTo(expectedInstance.getVariables());
    }

    @Test
    public void getProcessInstanceWithoutVars() {
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        ProcessInstance processInstance = instanceRepository.getProcessInstance(1L, CONTAINER_ID_ONE, false);
        Assertions.assertThat(processInstance.getContainerId()).isEqualTo(expectedInstance.getContainerId());
        Assertions.assertThat(processInstance.getVariables()).isNull();
    }

    @Test
    public void getProcessInstanceVariables() {
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);

        Map<String, Object> resultVariables = instanceRepository.getProcessInstanceVariables(1L, CONTAINER_ID_ONE);
        Assertions.assertThat(resultVariables).isEqualTo(variables);
    }

    @Test
    public void getAllProcessInstances_filterIsNull() {
        when(runtimeDataService.getProcessInstances(anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(1, null);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("containerId", CONTAINER_ID_ONE);

        verify(runtimeDataService, times(1)).getProcessInstances(anyObject());
    }

    @Test
    public void getAllProcessInstances_filterStatesAndInitiator() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setStatesIn(Arrays.asList(1, 2, 3, 4));
        filter.setInitiator("tester");

        when(runtimeDataService.getProcessInstances(eq(filter.getStatesIn()), eq(filter.getInitiator()),
                                                    anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("containerId", CONTAINER_ID_ONE);
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("initiator", "tester");
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("state", 1);
        verify(runtimeDataService,
               times(1)).getProcessInstances(eq(filter.getStatesIn()), eq(filter.getInitiator()),
                                             anyObject());
    }

    @Test
    public void getAllProcessInstances_filterStatesAndInitiatorAndProcessId() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setStatesIn(Arrays.asList(1, 2, 3, 4));
        filter.setInitiator("tester");
        filter.setProcessId("dummyId");

        when(runtimeDataService.getProcessInstancesByProcessId(eq(filter.getStatesIn()), eq(filter.getProcessId()), eq(filter.getInitiator()),
                                                               anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("containerId", CONTAINER_ID_ONE);
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("initiator", "tester");
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("processId", "dummyId");
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("state", 1);

        verify(runtimeDataService,
               times(1)).getProcessInstancesByProcessId(eq(filter.getStatesIn()), eq(filter.getProcessId()), eq(filter.getInitiator()),
                                                        anyObject());
    }

    @Test
    public void getAllProcessInstances_filterStatesAndInitiatorAndProcessName() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setStatesIn(Arrays.asList(1, 2, 3, 4));
        filter.setInitiator("tester");
        filter.setProcessName("dummyName");

        when(runtimeDataService.getProcessInstancesByProcessName(eq(filter.getStatesIn()), eq(filter.getProcessName()), eq(filter.getInitiator()),
                                                                 anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("containerId", CONTAINER_ID_ONE);
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("initiator", "tester");
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("processName", "dummyName");
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("state", 1);

        verify(runtimeDataService,
               times(1)).getProcessInstancesByProcessName(eq(filter.getStatesIn()), eq(filter.getProcessName()), eq(filter.getInitiator()),
                                                          anyObject());
    }

    @Test
    public void getAllProcessInstances_filterStatesAndProcessName() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setStatesIn(Arrays.asList(1, 2, 3, 4));
        filter.setContainerId(CONTAINER_ID_ONE);

        when(runtimeDataService.getProcessInstancesByDeploymentId(eq(filter.getContainerId()), eq(filter.getStatesIn()),
                                                                  anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("containerId", CONTAINER_ID_ONE);
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("state", 1);

        verify(runtimeDataService,
               times(1)).getProcessInstancesByDeploymentId(eq(filter.getContainerId()), eq(filter.getStatesIn()),
                                                           anyObject());
    }

    @Test
    public void getAllProcessInstances_filterStatesAndVariableName() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setStatesIn(Arrays.asList(1, 2, 3, 4));
        filter.setVariableName("var1");
        filter.setVariableValue(null);

        when(runtimeDataService.getProcessInstancesByVariable(eq(filter.getVariableName()), eq(filter.getStatesIn()),
                                                              anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("containerId", CONTAINER_ID_ONE);

        verify(runtimeDataService,
               times(1)).getProcessInstancesByVariable(eq(filter.getVariableName()), eq(filter.getStatesIn()),
                                                       anyObject());
    }

    @Test
    public void getAllProcessInstances_filterStatesAndVariableNameAndVariableValue() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setStatesIn(Arrays.asList(1, 2, 3, 4));
        filter.setVariableName("var2");
        filter.setVariableValue("1");

        when(runtimeDataService.getProcessInstancesByVariableAndValue(eq(filter.getVariableName()),
                                                                      eq(filter.getVariableValue()), eq(filter.getStatesIn()),
                                                              anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("containerId", CONTAINER_ID_ONE);

        verify(runtimeDataService,
               times(1)).getProcessInstancesByVariableAndValue(eq(filter.getVariableName()),
                                                               eq(filter.getVariableValue()), eq(filter.getStatesIn()),
                                                               anyObject());
    }

    @Test
    public void getAllProcessInstances_filterStatesAndCorrelationKey() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setStatesIn(Arrays.asList(1, 2, 3, 4));
        filter.setCorrelationKey("ABC:FED");

        String[] correlationProperties = filter.getCorrelationKey().split(":");
        CorrelationKey actualCorrelationKey = correlationKeyFactory.newCorrelationKey(Arrays.asList(correlationProperties));

        when(runtimeDataService.getProcessInstancesByCorrelationKeyAndStatus(eq(actualCorrelationKey),
                                                                             eq(filter.getStatesIn()),
                                                                      anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("containerId", CONTAINER_ID_ONE);

        verify(runtimeDataService,
               times(1)).getProcessInstancesByCorrelationKeyAndStatus(eq(actualCorrelationKey),
                                                                       eq(filter.getStatesIn()),
                                                                       anyObject());
    }

    @Test
    public void getAllProcessInstances_filterCorrelationKey() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setCorrelationKey("ABC:FED");

        String[] correlationProperties = filter.getCorrelationKey().split(":");
        CorrelationKey actualCorrelationKey = correlationKeyFactory.newCorrelationKey(Arrays.asList(correlationProperties));

        when(runtimeDataService.getProcessInstancesByCorrelationKey(eq(actualCorrelationKey),
                                                                             anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("containerId", CONTAINER_ID_ONE);

        verify(runtimeDataService,
               times(1)).getProcessInstancesByCorrelationKey(eq(actualCorrelationKey),
                                                             anyObject());
    }

    @Test
    public void getAllProcessInstances_filterProcessIdOnly() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setProcessId("dummyId");

        when(runtimeDataService.getProcessInstancesByProcessDefinition(eq(filter.getProcessId()),
                                                                 anyObject())).thenReturn(Collections.singletonList(processInstanceDesc));

        List<ProcessInstance> result = instanceRepository.getAllProcessInstances(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("processId", "dummyId");

        verify(runtimeDataService,
               times(1)).getProcessInstancesByProcessDefinition(eq(filter.getProcessId()),
                                                                anyObject());
    }

    @Test
    public void getAllProcessInstances_unsupportedFilterCombination() {
        ProcessInstanceFilter filter = new ProcessInstanceFilter();
        filter.setProcessId("dummyId");
        filter.setContainerId(CONTAINER_ID_ONE);

        Assertions.assertThatThrownBy(() -> instanceRepository.getAllProcessInstances(1, filter))
                .hasMessageContaining("Selected filter properties do not match any know combinations.");
    }
/*
    @Test
    public void getAllTasks_filterIsNull() {
        Assertions.assertThatThrownBy(() -> instanceRepository.getAllTasks(1, null))
                .hasMessageContaining("Filter for getAllTasks can't be null");
    }*/

    @Test
    public void getAllTasks_filterStatesAndBusinessAdminId() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setStatesIn(Collections.singletonList("Created"));
        filter.setBusinessAdminId("tester");

        List<Status> taskStatusList = getSelectedStatusList(filter.getStatesIn());

        when(runtimeDataService.getTasksAssignedAsBusinessAdministratorByStatus(eq(filter.getBusinessAdminId()), eq(taskStatusList),
                                                                       anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("status", "Created");
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("actualOwner", "tester");

        verify(runtimeDataService,
               times(1)).getTasksAssignedAsBusinessAdministratorByStatus(eq(filter.getBusinessAdminId()), eq(taskStatusList),
                                                                          anyObject());
    }

    @Test
    public void getAllTasks_filterStatesAndProcessInstanceId() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setStatesIn(Collections.singletonList("Created"));
        filter.setProcessInstanceId(1L);

        List<Status> taskStatusList = getSelectedStatusList(filter.getStatesIn());

        when(runtimeDataService.getTasksByStatusByProcessInstanceId(eq(filter.getProcessInstanceId()), eq(taskStatusList),
                                                                                anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("status", "Created");
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("processInstanceId", 1L);

        verify(runtimeDataService,
               times(1)).getTasksByStatusByProcessInstanceId(eq(filter.getProcessInstanceId()), eq(taskStatusList),
                                                             anyObject());
    }


    @Test
    public void getAllTasks_filterStatesAndOwnerAndVariableName() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setStatesIn(Collections.singletonList("Created"));
        filter.setOwnerId("tester");
        filter.setVariableName("tester");

        List<Status> taskStatusList = getSelectedStatusList(filter.getStatesIn());

        when(runtimeDataService.getTasksByVariable(eq(filter.getOwnerId()), eq(filter.getVariableName()),eq(taskStatusList),
                                                                    anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("status", "Created");
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("actualOwner", "tester");

        verify(runtimeDataService,
               times(1)).getTasksByVariable(eq(filter.getOwnerId()), eq(filter.getVariableName()), eq(taskStatusList),
                                            anyObject());
    }

    @Test
    public void getAllTasks_filterStatesAndOwnerAndVariableNameAndVariableValue() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setStatesIn(Collections.singletonList("Created"));
        filter.setOwnerId("tester");
        filter.setVariableName("tester");
        filter.setVariableValue("test-value");

        List<Status> taskStatusList = getSelectedStatusList(filter.getStatesIn());

        when(runtimeDataService.getTasksByVariableAndValue(eq(filter.getOwnerId()), eq(filter.getVariableName()),
                                                           eq(filter.getVariableValue()),eq(taskStatusList),
                                                   anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("status", "Created");
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("actualOwner", "tester");

        verify(runtimeDataService,
               times(1)).getTasksByVariableAndValue(eq(filter.getOwnerId()), eq(filter.getVariableName()),
                                                    eq(filter.getVariableValue()),eq(taskStatusList),
                                                    anyObject());
    }

    @Test
    public void getAllTasks_filterPotentialOwnerIdAndGroupIds() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setPotentialOwnerId("tester");
        filter.setGroupIds(Collections.singletonList("admin"));


        when(runtimeDataService.getTasksAssignedAsPotentialOwner(eq(filter.getPotentialOwnerId()), eq(filter.getGroupIds()),
                                                           anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("actualOwner", "tester");
        verify(runtimeDataService,
               times(1)).getTasksAssignedAsPotentialOwner(eq(filter.getPotentialOwnerId()), eq(filter.getGroupIds()),
                                                         anyObject());
    }

    @Test
    public void getAllTasks_filterOwnerId() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setOwnerId("tester");

        when(runtimeDataService.getTasksOwned(eq(filter.getOwnerId()),
                                                                 anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(1, filter);
        Assertions.assertThat(result).isNotNull();

        verify(runtimeDataService,
               times(1)).getTasksOwned(eq(filter.getOwnerId()),
                                                          anyObject());
    }


    @Test
    public void getAllTasks_filterStatesAndPotOwnerIdAndGroupId() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setStatesIn(Collections.singletonList("Created"));
        filter.setPotentialOwnerId("tester");
        filter.setGroupIds(Collections.singletonList("admin"));

        List<Status> taskStatusList = getSelectedStatusList(filter.getStatesIn());

        when(runtimeDataService.getTasksAssignedAsPotentialOwner(eq(filter.getPotentialOwnerId()), eq(filter.getGroupIds()), eq(taskStatusList),
                                                                                anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("status", "Created");
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("actualOwner", "tester");

        verify(runtimeDataService,
               times(1)).getTasksAssignedAsPotentialOwner(eq(filter.getPotentialOwnerId()), eq(filter.getGroupIds()), eq(taskStatusList),
                                                          anyObject());
    }

    @Test
    public void getAllTasks_filterStatesAndPotOwnerIdAndFromDate() {
        Date date = new Date();
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setStatesIn(Collections.singletonList("Created"));
        filter.setFromExpirationalDate(date);
        filter.setPotentialOwnerId("tester");

        List<Status> taskStatusList = getSelectedStatusList(filter.getStatesIn());

        when(runtimeDataService.getTasksAssignedAsPotentialOwnerByExpirationDateOptional(eq(filter.getPotentialOwnerId()), eq(taskStatusList),
                                                                                         eq(filter.getFromExpirationalDate()),
                                                                 anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("status", "Created");
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("actualOwner", "tester");

        verify(runtimeDataService,
               times(1)).getTasksAssignedAsPotentialOwnerByExpirationDateOptional(eq(filter.getPotentialOwnerId()), eq(taskStatusList),
                                                                                  eq(filter.getFromExpirationalDate()),
                                                                                  anyObject());
    }

    @Test
    public void getAllTasks_filterStatesAndPotOwnerId() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setStatesIn(Collections.singletonList("Created"));
        filter.setPotentialOwnerId("tester");

        List<Status> taskStatusList = getSelectedStatusList(filter.getStatesIn());

        when(runtimeDataService.getTasksAssignedAsPotentialOwnerByStatus(eq(filter.getPotentialOwnerId()), eq(taskStatusList),
                                                                                         anyObject())).thenReturn(Collections.singletonList(taskSummary));

        List<TaskSummary> result = instanceRepository.getAllTasks(1, filter);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("status", "Created");
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("actualOwner", "tester");

        verify(runtimeDataService,
               times(1)).getTasksAssignedAsPotentialOwnerByStatus(eq(filter.getPotentialOwnerId()), eq(taskStatusList),
                                                                  anyObject());
    }

    @Test
    public void getAllTasks_filterCombinationUnsupported() {
        TaskInstanceFilter filter = new TaskInstanceFilter();
        filter.setBusinessAdminId("admin");

        Assertions.assertThatThrownBy(() -> instanceRepository.getAllTasks(1, filter))
                .hasMessageContaining("Selected filter properties do not match any know combinations.");
    }

    @Test
    public void getTaskInstanceBothArgumentsNotNull() {
       Assertions.assertThatThrownBy(() -> instanceRepository.getTaskInstance(1L, 1L))
               .hasMessageContaining("Only one of taskId or workItemId can be selected.");
    }

/*    @Test
    public void getTaskInstanceWorkItemIdNull() {
        when(runtimeDataService.getTaskById(1L)).thenReturn(userTaskInstanceDesc);
        TaskInstance result = instanceRepository.getTaskInstance(1L, null);
        Assertions.assertThat(result).isNotNull();

        verify(runtimeDataService, times(1)).getTaskById(1L);
    }

    @Test
    public void getTaskInstanceTaskIdNull() {
        when(runtimeDataService.getTaskByWorkItemId(1L)).thenReturn(userTaskInstanceDesc);
        TaskInstance result = instanceRepository.getTaskInstance(null, 1L);
        Assertions.assertThat(result).isNotNull();

        verify(runtimeDataService, times(1)).getTaskByWorkItemId(1L);
    }*/

    @Test
    public void startProcessWithIdAndContainerIdWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId")).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);

        ProcessInstance result = instanceRepository.startProcess("dummyId", CONTAINER_ID_ONE, true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasFieldOrPropertyWithValue("variables", variables);
    }

    @Test
    public void startProcessWithIdAndContainerIdWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId")).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        ProcessInstance result = instanceRepository.startProcess("dummyId", CONTAINER_ID_ONE, false);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getVariables()).isNull();
    }

    @Test
    public void startProcessWithIdAndContainerIdAndCorrelationKeyWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId", actualCorrelationKey)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);

        ProcessInstance result = instanceRepository.startProcess("dummyId", CONTAINER_ID_ONE, "ABC:DEF", true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasFieldOrPropertyWithValue("variables", variables);
    }

    @Test
    public void startProcessWithIdAndContainerIdAndCorrelationKeyWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId", actualCorrelationKey)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        ProcessInstance result = instanceRepository.startProcess("dummyId", CONTAINER_ID_ONE, "ABC:DEF", false);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getVariables()).isNull();
    }

    @Test
    public void startProcessWithIdAndContainerIdAndCorrelationKeyAndVariablesWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId", actualCorrelationKey, variables)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        ProcessInstance result = instanceRepository.startProcess("dummyId", CONTAINER_ID_ONE, "ABC:DEF", variables, false);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getVariables()).isNull();
    }

    @Test
    public void startProcessWithIdAndContainerIdAndCorrelationKeyAndVariablesWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId", actualCorrelationKey, variables)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);

        ProcessInstance result = instanceRepository.startProcess("dummyId", CONTAINER_ID_ONE, "ABC:DEF", variables, true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasFieldOrPropertyWithValue("variables", variables);
    }

    @Test
    public void startProcessWithIdAndContainerIdAndVariablesWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId",  variables)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        ProcessInstance result = instanceRepository.startProcess("dummyId", CONTAINER_ID_ONE, null, variables, false);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getVariables()).isNull();
    }

    @Test
    public void startProcessWithIdAndContainerIdAndVariablesWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId", variables)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);

        ProcessInstance result = instanceRepository.startProcess("dummyId", CONTAINER_ID_ONE, null, variables, true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasFieldOrPropertyWithValue("variables", variables);
    }

    @Test
    public void startProcessesWithIdAndContainerIdWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId")).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);

        List<ProcessInstance> result = instanceRepository.startProcesses("dummyId", CONTAINER_ID_ONE, 1,true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("variables", variables);
    }

    @Test
    public void startProcessesWithIdAndContainerIdWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId")).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        List<ProcessInstance> result = instanceRepository.startProcesses("dummyId", CONTAINER_ID_ONE,1, false);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0).getVariables()).isNull();
    }

    @Test
    public void startProcessesWithIdAndContainerIdAndCorrelationKeyWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId", actualCorrelationKey)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);

        List<ProcessInstance> result = instanceRepository.startProcesses("dummyId", CONTAINER_ID_ONE, "ABC:DEF", 1,true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("variables", variables);
    }

    @Test
    public void startProcessesWithIdAndContainerIdAndCorrelationKeyWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId", actualCorrelationKey)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        List<ProcessInstance> result = instanceRepository.startProcesses("dummyId", CONTAINER_ID_ONE, "ABC:DEF",1, false);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0).getVariables()).isNull();
    }

    @Test
    public void startProcessesWithIdAndContainerIdAndCorrelationKeyAndVariablesWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId", actualCorrelationKey, variables)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);

        List<ProcessInstance> result = instanceRepository.startProcesses("dummyId", CONTAINER_ID_ONE, "ABC:DEF", variables,1,true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("variables", variables);
    }

    @Test
    public void startProcessesWithIdAndContainerIdAndCorrelationKeyAndVariablesWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId", actualCorrelationKey, variables)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        List<ProcessInstance> result = instanceRepository.startProcesses("dummyId", CONTAINER_ID_ONE, "ABC:DEF", variables,1, false);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0).getVariables()).isNull();
    }

    @Test
    public void startProcessesWithIdAndContainerIdVariablesWithVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId", variables)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);
        when(processService.getProcessInstanceVariables(CONTAINER_ID_ONE, 1L)).thenReturn(variables);

        List<ProcessInstance> result = instanceRepository.startProcesses("dummyId", CONTAINER_ID_ONE, null, variables,1,true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("variables", variables);
    }

    @Test
    public void startProcessesWithIdAndContainerIdAndVariablesWithoutVars() {
        when(processService.startProcess(CONTAINER_ID_ONE, "dummyId", variables)).thenReturn(1L);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);
        when(context.getContainerId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(CONTAINER_ID_ONE);

        List<ProcessInstance> result = instanceRepository.startProcesses("dummyId", CONTAINER_ID_ONE, null, variables,1, false);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get(0).getVariables()).isNull();
    }

    @Test
    public void abortProcessInstancesUsingIds() {
        List<Long> ids = Collections.singletonList(1L);
        doNothing().when(processService).abortProcessInstances(ids);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);

        List<ProcessInstance> result = instanceRepository.abortProcessInstances(ids, null);
        Assertions.assertThat(result).isNotNull();

        verify(processService, times(1)).abortProcessInstances(ids);
    }

    @Test
    public void abortProcessInstancesUsingIdsAndContainerId() {
        List<Long> ids = Collections.singletonList(1L);
        doNothing().when(processService).abortProcessInstances(CONTAINER_ID_ONE, ids);
        when(runtimeDataService.getProcessInstanceById(1L)).thenReturn(processInstanceDesc);

        List<ProcessInstance> result = instanceRepository.abortProcessInstances(ids, CONTAINER_ID_ONE);
        Assertions.assertThat(result).isNotNull();

        verify(processService, times(1)).abortProcessInstances(CONTAINER_ID_ONE, ids);
    }

    @After
    public void tearDown() throws Exception {

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
