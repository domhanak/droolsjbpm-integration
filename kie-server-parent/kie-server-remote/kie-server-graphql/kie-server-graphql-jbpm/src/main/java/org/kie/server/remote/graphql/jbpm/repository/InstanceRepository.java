package org.kie.server.remote.graphql.jbpm.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.kie.api.runtime.query.QueryContext;
import org.kie.api.task.model.Status;
import org.kie.internal.KieInternalServices;
import org.kie.internal.process.CorrelationKey;
import org.kie.internal.process.CorrelationKeyFactory;
import org.kie.internal.query.QueryFilter;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.TaskInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.remote.graphql.jbpm.exceptions.NoFilterMatchException;
import org.kie.server.remote.graphql.jbpm.filter.ProcessInstanceFilter;
import org.kie.server.remote.graphql.jbpm.filter.TaskInstanceFilter;
import org.kie.server.remote.graphql.jbpm.query.InstanceQuery;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.jbpm.locator.ByProcessInstanceIdContainerLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.server.services.jbpm.ConvertUtils.buildQueryFilter;
import static org.kie.server.services.jbpm.ConvertUtils.convertToProcessInstance;
import static org.kie.server.services.jbpm.ConvertUtils.convertToProcessInstanceList;
import static org.kie.server.services.jbpm.ConvertUtils.convertToTask;
import static org.kie.server.services.jbpm.ConvertUtils.convertToTaskSummaryList;

/**
 * GraphQL connector for {@link InstanceQuery}
 */
public class InstanceRepository {

    private static final Logger logger = LoggerFactory.getLogger(InstanceRepository.class);

    private KieServerRegistry context;
    private ProcessService processService;
    private RuntimeDataService runtimeDataService;

    private CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

    public InstanceRepository(RuntimeDataService runtimeDataService,
                              ProcessService processService,
                              KieServerRegistry context) {
       this.processService = processService;
       this.runtimeDataService = runtimeDataService;
       this.context = context;
    }

    /*
     * Queries for ProcessInstance
     */

    /**
     * Gets {@link ProcessInstance} for given id and containerId using {@link RuntimeDataService}.
     * @param id The id of {@link ProcessInstance} to be fetched.
     * @param containerId The containerId of {@link ProcessInstance} to be fetched.
     * @param withVariables Whether to fetch process variables for given instance. This produces additional DB access.
     * @return {@link ProcessInstance} instance representing the process instance with desired id and containerId.
     */
    public ProcessInstance getProcessInstance(long id, String containerId, boolean withVariables) {
        ProcessInstanceDesc processInstanceDesc = runtimeDataService.getProcessInstanceById(id);
        if (processInstanceDesc == null) {
            throw new IllegalStateException("Unable to find process instance with id " + id);
        }
        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(id));
        org.kie.server.api.model.instance.ProcessInstance processInstance = convertToProcessInstance(processInstanceDesc);

        if (withVariables && processInstance.getState().equals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE)) {
            Map<String, Object> variables = processService.getProcessInstanceVariables(containerId, id);
            processInstance.setVariables(variables);
        }

        return  processInstance;
    }

    /**
     * Gets {@link Map} with String and Object pair that contains variables of {@link ProcessInstance} for given id and containerId
     * @param id The id of {@link ProcessInstance} whose variables we want to be fetched.
     * @param containerId The containerId of {@link ProcessInstance} whose variables we want to be fetched.
     * @return {@link Map} where keys are {@link String} and values are {@link Object} where each entry of the Map represents variableName:variableValue
     */
    public Map<String, Object> getProcessInstanceVariables(long id, String containerId) {
        return processService.getProcessInstanceVariables(containerId, id);
    }

    /**
     * Gets {@link List} of {@link ProcessInstance} with size determined by batchSize argument and
     * instances are decided by filter.
     *
     * Method decides what DB query is used based on the combination of the filter.
     *
     * @param batchSize size of the resulting list
     * @param filter filter with selected combination
     * @return List of process instances with fields matching filter selection
     */
    public List<ProcessInstance> getAllProcessInstances(int batchSize, ProcessInstanceFilter filter) {
        QueryContext queryContext = new QueryContext();
        queryContext.setCount(batchSize);

        if (filter == null) {
            return convertToProcessInstanceList(
                    runtimeDataService.getProcessInstances(queryContext)).getItems();
        }

        String processId = filter.getProcessId();
        String containerId = filter.getContainerId();
        String processName = filter.getProcessName();
        String initiator = filter.getInitiator();
        List<Integer> states = filter.getStatesIn();
        String variableName = filter.getVariableName();
        String variableValue = filter.getVariableValue();
        String correlationKey = filter.getCorrelationKey();

        String[] correlationProperties;
        CorrelationKey actualCorrelationKey;
        switch (filter.getSelectedFilterCombination()) {
            case PROCESS_ID:
                logger.info("Getting instances with processId:{}", processId);
                return convertToProcessInstanceList(
                        runtimeDataService.getProcessInstancesByProcessDefinition(processId, queryContext)).getItems();
            case CORRELATION_KEY:
                logger.info("Getting instances  with correlationKey: {}", correlationKey);
                correlationProperties = filter.getCorrelationKey().split(":");
                actualCorrelationKey = correlationKeyFactory.newCorrelationKey(Arrays.asList(correlationProperties));
                return convertToProcessInstanceList(
                        runtimeDataService.getProcessInstancesByCorrelationKey(actualCorrelationKey, queryContext)).getItems();
            case STATES_AND_INITIATOR:
                logger.info("Getting instances with states:{} with initiator: {}", states, initiator);
                return convertToProcessInstanceList(
                        runtimeDataService.getProcessInstances(states, initiator, queryContext)).getItems();
            case STATES_AND_CONTAINER_ID:
                logger.info("Getting instances in states:{} and with containerId equal: {}",
                            states, containerId);
                return convertToProcessInstanceList(
                        runtimeDataService.getProcessInstancesByDeploymentId(containerId, states, queryContext)).getItems();
            case STATES_AND_CORRELATION_KEY:
                logger.info("Getting instances in states:{} and with correlationKey: {}",
                            states, correlationKey);
                correlationProperties = correlationKey.split(":");
                actualCorrelationKey = correlationKeyFactory.newCorrelationKey(Arrays.asList(correlationProperties));
                return convertToProcessInstanceList(
                        runtimeDataService.getProcessInstancesByCorrelationKeyAndStatus(actualCorrelationKey, states, queryContext)).getItems();
            case STATES_AND_VARIABLE_NAME:
                logger.info("Getting instances in states:{} and with variable equal: {}",
                            states, variableName);
                return convertToProcessInstanceList(
                        runtimeDataService.getProcessInstancesByVariable(variableName, states, queryContext)).getItems();
            case STATES_AND_VARIABLE_NAME_AND_VARIABLE_VALUE:
                logger.info("Getting instances in states:{} and with variableName: {} and variableValue: {}",
                            states, variableName, variableValue);
                return convertToProcessInstanceList(
                        runtimeDataService.getProcessInstancesByVariableAndValue(variableName, variableValue, states, queryContext)).getItems();
            case STATES_AND_INITIATOR_AND_PROCESS_ID:
                logger.info("Getting instances in states:{} with initiator: {} and with processId equal: {}",
                            states, initiator, processId);
                return convertToProcessInstanceList(
                        runtimeDataService.getProcessInstancesByProcessId(states, processId, initiator, queryContext)).getItems();
            case STATES_AND_INITIATOR_AND_PROCESS_NAME:
                logger.info("Getting instances in states:{} with initiator: {} and with processName equal: {}",
                            states, initiator, processName);
                return convertToProcessInstanceList(
                        runtimeDataService.getProcessInstancesByProcessName(states, processName, initiator, queryContext)).getItems();
            default:
                throw new NoFilterMatchException("No query available for selected filter combination. " +
                                                 "Allowed combinations are: " + Arrays.toString(ProcessInstanceFilter.AllowedCombinations.values()));
        }
    }

    /*
     * Queries for tasks
     */

    /**
     * Gets {@link TaskInstance} from database.
     * Method queris the DB either by using taskId only OR by using workItemId only. Not both.
     *
     * @param taskId id of the task, can be null
     * @param workItemId workItemId of the task, can be null
     * @return {@link TaskInstance} with taskId OR workItemId
     */
    public TaskInstance getTaskInstance(Long taskId, Long workItemId) {
        if (taskId != null && workItemId == null) {
            return convertToTask(runtimeDataService.getTaskById(taskId));
        } else if (workItemId != null && taskId == null) {
            return convertToTask(runtimeDataService.getTaskByWorkItemId(workItemId));
        } else {
            throw new IllegalArgumentException("Only one of taskId or workItemId can be selected.");
        }
    }

    /**
     * Gets {@link List} of {@link TaskSummary} with size determined by batchSize argument and
     * instances are decided by filter.
     *
     * Method decides what DB query is used based on the combination of the filter.
     *
     * @param batchSize size of the resulting list
     * @param filter {@link TaskInstanceFilter} filter with selected combination
     * @return List of {@link TaskSummary} with fields matching filter selection
     */
    public List<TaskSummary> getAllTasks(int batchSize, TaskInstanceFilter filter) {

        if (filter == null) {
            throw new IllegalArgumentException("Filter for getAllTasks can't be null");
        }


        QueryContext queryContext = new QueryContext();
        queryContext.setCount(batchSize);
        QueryFilter queryFilter = buildQueryFilter(1, batchSize);

        List<Status> taskStatusList;
        if (filter.getStatesIn() != null) {
            taskStatusList = getSelectedStatusList(filter.getStatesIn());
        }
        switch (filter.getSelectedFilterCombination()) {
            case OWNER_ID:
                logger.info("Getting summary for tasks with ownerId: {}", filter.getOwnerId());
                return convertToTaskSummaryList(
                        runtimeDataService.getTasksOwned(filter.getOwnerId(), queryFilter)).getItems();
            case POTENTIAL_OWNER_ID_AND_GROUP_IDS:
                logger.info("Getting summary for tasks with potentialOwnerId: {}", filter.getPotentialOwnerId());
                return convertToTaskSummaryList(
                        runtimeDataService.getTasksAssignedAsPotentialOwner(filter.getPotentialOwnerId(),
                                                                            filter.getGroupIds(),
                                                                            queryFilter)).getItems();
            case STATES_AND_POTENTIAL_OWNER_ID:
                logger.info("Getting summary for tasks with states in: {} and potentialOwner: {}", getSelectedStatusList(filter.getStatesIn()),
                            filter.getPotentialOwnerId());
                return convertToTaskSummaryList(
                        runtimeDataService.getTasksAssignedAsPotentialOwnerByStatus(filter.getPotentialOwnerId(),
                                                                                    getSelectedStatusList(filter.getStatesIn()),
                                                                                    queryFilter)).getItems();
            case STATES_AND_BUSINESS_ADMIN_ID:
                logger.info("Getting summary for tasks with states in: {} and businessAdmin: {}", getSelectedStatusList(filter.getStatesIn()),
                            filter.getBusinessAdminId());
                return convertToTaskSummaryList(
                        runtimeDataService.getTasksAssignedAsBusinessAdministratorByStatus(filter.getBusinessAdminId(),
                                                                                           getSelectedStatusList(filter.getStatesIn()),
                                                                                           queryFilter)).getItems();
            case STATES_AND_PROCESS_INSTANCE_ID:
                logger.info("Getting summary for tasks with states in: {} and processInstanceId: {}", getSelectedStatusList(filter.getStatesIn()),
                            filter.getProcessInstanceId());
                return convertToTaskSummaryList(
                        runtimeDataService.getTasksByStatusByProcessInstanceId(filter.getProcessInstanceId(),
                                                                               getSelectedStatusList(filter.getStatesIn()),
                                                                               queryFilter)).getItems();
            case STATES_AND_POTENTIAL_OWNER_ID_AND_FROM_DATE:
                logger.info("Getting summary for tasks with fromDate {}", filter.getFromExpirationalDate());
                return convertToTaskSummaryList(
                        runtimeDataService.getTasksAssignedAsPotentialOwnerByExpirationDateOptional(filter.getPotentialOwnerId(),
                                                                                                    getSelectedStatusList(filter.getStatesIn()),
                                                                                                    filter.getFromExpirationalDate(),
                                                                                                    queryFilter)).getItems();
            case STATES_AND_POTENTIAL_OWNER_ID_AND_GROUP_IDS:
                logger.info("With groupIds: {}", filter.getGroupIds());
                return convertToTaskSummaryList(
                        runtimeDataService.getTasksAssignedAsPotentialOwner(filter.getPotentialOwnerId(),
                                                                            filter.getGroupIds(),
                                                                            getSelectedStatusList(filter.getStatesIn()),
                                                                            queryFilter)).getItems();
            case STATES_AND_OWNER_ID_AND_VARIABLE_NAME:
                logger.info("Getting summary for tasks with states in: {} and ownerId: {} " +
                                    "and variableName: {}", getSelectedStatusList(filter.getStatesIn()),
                            filter.getOwnerId(),
                            filter.getVariableName());
                return convertToTaskSummaryList(
                        runtimeDataService.getTasksByVariable(filter.getOwnerId(),
                                                              filter.getVariableName(),
                                                              getSelectedStatusList(filter.getStatesIn()),
                                                              queryContext)).getItems();
            case STATES_AND_OWNER_ID_AND_VARIABLE_NAME_AND_VARIABLE_VALUE:
                logger.info("Getting summary for tasks with states in: {} and ownerId: {} " +
                                    "and variableName: {} and variableValue: {}", getSelectedStatusList(filter.getStatesIn()),
                            filter.getOwnerId(),
                            filter.getVariableName(),
                            filter.getVariableValue());
                return convertToTaskSummaryList(
                        runtimeDataService.getTasksByVariableAndValue(filter.getOwnerId(),
                                                                      filter.getVariableName(),
                                                                      filter.getVariableValue(),
                                                                      getSelectedStatusList(filter.getStatesIn()),
                                                                      queryContext)).getItems();
            default:
                throw new NoFilterMatchException("No query available for selected filter combination. " +
                                                         "Allowed combinations are: " + Arrays.toString(TaskInstanceFilter.AllowedCombinations.values()));

        }

/*
        if (filter.getStatesIn() != null) {
            List<Status> taskStatusList = getSelectedStatusList(filter.getStatesIn());
            logger.info("Getting summary for tasks in statuses: {}", taskStatusList);
            // States AND PotOwner
            if (filter.getPotentialOwnerId() != null && (filter.getBusinessAdminId() == null)) {
                logger.info("With potential owner: {}", filter.getPotentialOwnerId());
                // States AND PotOwner AND groupIds
                if (filter.getGroupIds() != null) {
                    logger.info("With groupIds: {}", filter.getGroupIds());
                    return convertToTaskSummaryList(
                            runtimeDataService.getTasksAssignedAsPotentialOwner(filter.getPotentialOwnerId(),
                                                                                filter.getGroupIds(),
                                                                                taskStatusList,
                                                                                queryFilter)
                    ).getItems();
                    // States AND PotOwner AND fromDate
                } else if (filter.getFromExpirationalDate() != null) {
                    logger.info("Getting summary for tasks with fromDate {}", filter.getFromExpirationalDate());
                    return convertToTaskSummaryList(
                            runtimeDataService.getTasksAssignedAsPotentialOwnerByExpirationDateOptional(filter.getPotentialOwnerId(),
                                                                                                        taskStatusList,
                                                                                                        filter.getFromExpirationalDate(),
                                                                                                        queryFilter)
                    ).getItems();
                    // States AND PotOwner
                }  else  {
                    logger.info("Getting summary for tasks with states in: {} and potentialOwner: {}", taskStatusList,
                                filter.getPotentialOwnerId());
                    return convertToTaskSummaryList(
                            runtimeDataService.getTasksAssignedAsPotentialOwnerByStatus(filter.getPotentialOwnerId(),
                                                                                        taskStatusList,
                                                                                        queryFilter)
                    ).getItems();
                }
                // States AND businessAdmin
            } else if (filter.getBusinessAdminId() != null) {
                logger.info("Getting summary for tasks with states in: {} and businessAdmin: {}", taskStatusList,
                            filter.getBusinessAdminId());
                return convertToTaskSummaryList(
                        runtimeDataService.getTasksAssignedAsBusinessAdministratorByStatus(filter.getBusinessAdminId(),
                                                                                           taskStatusList,
                                                                                           queryFilter)
                ).getItems();
                // States AND processInstanceId
            } else if (filter.getProcessInstanceId() != null) {
                logger.info("Getting summary for tasks with states in: {} and processInstanceId: {}", taskStatusList,
                            filter.getProcessInstanceId());
                return convertToTaskSummaryList(
                        runtimeDataService.getTasksByStatusByProcessInstanceId(filter.getProcessInstanceId(),
                                                                               taskStatusList,
                                                                               queryFilter)
                ).getItems();
                // By Owner AND variableName
            }  else if (filter.getVariableName() != null && filter.getOwnerId() != null) {
                // AND variableValue
                if (filter.getVariableValue() != null) {
                    logger.info("Getting summary for tasks with states in: {} and ownerId: {} " +
                                        "and variableName: {} and variableValue: {}", taskStatusList,
                                                                                      filter.getOwnerId(),
                                                                                      filter.getVariableName(),
                                                                                      filter.getVariableValue());
                    return convertToTaskSummaryList(
                            runtimeDataService.getTasksByVariableAndValue(filter.getOwnerId(),
                                                                          filter.getVariableName(),
                                                                          filter.getVariableValue(),
                                                                          taskStatusList,
                                                                          queryContext)
                    ).getItems();
                } else {
                    logger.info("Getting summary for tasks with states in: {} and ownerId: {} " +
                                        "and variableName: {}", taskStatusList,
                                                                filter.getOwnerId(),
                                                                filter.getVariableName());
                    return convertToTaskSummaryList(
                            runtimeDataService.getTasksByVariable(filter.getOwnerId(),
                                                                  filter.getVariableName(),
                                                                  taskStatusList,
                                                                  queryContext)
                    ).getItems();
                }
            }
            // Only Potential Owner
        } else if (filter.getPotentialOwnerId() != null && filter.getGroupIds() != null) {
            logger.info("Getting summary for tasks with potentialOwnerId: {}", filter.getPotentialOwnerId());
            return convertToTaskSummaryList(
                    runtimeDataService.getTasksAssignedAsPotentialOwner(filter.getPotentialOwnerId(),
                                                                        filter.getGroupIds(),
                                                                        queryFilter)).getItems();
        } else if (filter.getOwnerId() != null) {
            logger.info("Getting summary for tasks with ownerId: {}", filter.getOwnerId());
            return convertToTaskSummaryList(
                    runtimeDataService.getTasksOwned(filter.getOwnerId(), queryFilter)
            ).getItems();
        }
*/


    }

    /*
     * MUTATIONS
     */

    /**
     * Start a single process instance using processId and containerId.
     *
     * @param id processId of definition we want to start
     * @param containerId id of container
     * @param withVars determines if we query DB for variables or not
     * @return started process instance
     */
    public ProcessInstance startProcess(String id, String containerId, boolean withVars) {
        long instanceId =  processService.startProcess(containerId, id);

        ProcessInstanceDesc processInstanceDesc = runtimeDataService.getProcessInstanceById(instanceId);
        if (processInstanceDesc == null) {
            throw new IllegalStateException("Unable to find process instance with id " + id);
        }
        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(instanceId));
        org.kie.server.api.model.instance.ProcessInstance processInstance = convertToProcessInstance(processInstanceDesc);

        if (withVars && (processInstance.getState().equals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE))) {
            Map<String, Object> variables = processService.getProcessInstanceVariables(containerId, instanceId);
            processInstance.setVariables(variables);
        }

        return  processInstance;
    }

    /**
     * Start a single process instance using correlationKey and processVariables.
     *
     * @param id processId of definition we want to start
     * @param containerId id of container
     * @param correlationKey string representation of correlationkey
     * @param withVars determines if we query DB for variables or not
     * @return started process instance
     */
    public ProcessInstance startProcess(String id, String containerId, String correlationKey, boolean withVars) {
        String[] correlationProperties = correlationKey.split(":");
        CorrelationKey actualCorrelationKey = correlationKeyFactory.newCorrelationKey(Arrays.asList(correlationProperties));

        long instanceId =  processService.startProcess(containerId, id, actualCorrelationKey);

        ProcessInstanceDesc processInstanceDesc = runtimeDataService.getProcessInstanceById(instanceId);
        if (processInstanceDesc == null) {
            throw new IllegalStateException("Unable to find process instance with id " + id);
        }
        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(instanceId));
        org.kie.server.api.model.instance.ProcessInstance processInstance = convertToProcessInstance(processInstanceDesc);

        if (withVars && (processInstance.getState().equals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE))) {
            Map<String, Object> variables = processService.getProcessInstanceVariables(containerId, instanceId);
            processInstance.setVariables(variables);
        }

        return  processInstance;
    }

    /**
     * Start a single process instance using correlationKey and processVariables.
     *
     * @param id processId of definition we want to start
     * @param containerId id of container
     * @param correlationKey string representation of correlation key, if null the method starts process
     *                 with variables only
     * @param processVariables variables to start process with
     * @param withVars determines if we query DB for variables or not
     * @return started process instance
     */
    public ProcessInstance startProcess(String id, String containerId, String correlationKey,
                                        Map<String, Object> processVariables, boolean withVars) {
        long instanceId;
        if (correlationKey != null) {
            String[] correlationProperties = correlationKey.split(":");
            CorrelationKey actualCorrelationKey = correlationKeyFactory.newCorrelationKey(Arrays.asList(correlationProperties));
            instanceId =  processService.startProcess(containerId, id, actualCorrelationKey, processVariables);
        } else {
            instanceId =  processService.startProcess(containerId, id, processVariables);
        }

        ProcessInstanceDesc processInstanceDesc = runtimeDataService.getProcessInstanceById(instanceId);
        if (processInstanceDesc == null) {
            throw new IllegalStateException("Unable to find process instance with id " + id);
        }
        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(instanceId));
        org.kie.server.api.model.instance.ProcessInstance processInstance = convertToProcessInstance(processInstanceDesc);

        if (withVars && (processInstance.getState().equals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE))) {
            Map<String, Object> variables = processService.getProcessInstanceVariables(containerId, instanceId);
            processInstance.setVariables(variables);
        }

        return  processInstance;
    }

    /**
     * Starts batch of processes using processId and containerI and collects started instances.
     *
     * @param id processId of definition we want to start
     * @param containerId id of container
     * @param batchSize size of the batch
     * @param withVars determines if we query DB for variables or not
     * @return List started processInstances
     */
    public List<ProcessInstance> startProcesses(String id, String containerId, int batchSize, boolean withVars) {
        List<ProcessInstance> processInstances = new ArrayList<>();
        long instanceId;

        org.kie.server.api.model.instance.ProcessInstance processInstance;
        for (int i = 0; i< batchSize; ++i) {
            instanceId = processService.startProcess(containerId, id);
            containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(instanceId));
            processInstance = convertToProcessInstance(runtimeDataService.getProcessInstanceById(instanceId));

            if (withVars && (processInstance.getState().equals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE))) {
                Map<String, Object> variables = processService.getProcessInstanceVariables(containerId, instanceId);
                processInstance.setVariables(variables);
            }

            processInstances.add(processInstance);
        }

        return processInstances;
    }

    /**
     * Starts batch of processes using correlationKey  and collects started instances.
     *
     * @param id processId of definition we want to start
     * @param containerId id of container
     * @param correlationKey string representation of correlation key
     * @param batchSize size of the batch
     * @param withVars determines if we query DB for variables or not
     * @return List started processInstances
     */
    public List<ProcessInstance> startProcesses(String id, String containerId, String correlationKey, int batchSize,
                                                boolean withVars) {
        List<ProcessInstance> processInstances = new ArrayList<>();
        long instanceId;

        String[] correlationProperties = correlationKey.split(":");
        CorrelationKey actualCorrelationKey = correlationKeyFactory.newCorrelationKey(Arrays.asList(correlationProperties));

        org.kie.server.api.model.instance.ProcessInstance processInstance;
        for (int i = 0; i< batchSize; ++i) {
            instanceId = processService.startProcess(containerId, id, actualCorrelationKey);
            containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(instanceId));
            processInstance = convertToProcessInstance(runtimeDataService.getProcessInstanceById(instanceId));

            if (withVars && (processInstance.getState().equals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE))) {
                Map<String, Object> variables = processService.getProcessInstanceVariables(containerId, instanceId);
                processInstance.setVariables(variables);
            }

            processInstances.add(processInstance);
        }

        return processInstances;
    }

    /**
     * Starts batch of processes using correlationKey and variables and collects started instances.
     *
     * @param id processId of definition we want to start
     * @param containerId id of container
     * @param correlationKey string representation of correlation key, if null the method starts process
     *          with variables only
     * @param processVariables map of processVariables
     * @param batchSize size of the batch
     * @param withVars determines if we query DB for variables or not
     * @return List started processInstances
     */
    public List<ProcessInstance> startProcesses(String id, String containerId, String correlationKey,
                                                Map<String, Object> processVariables, int batchSize, boolean withVars) {
        List<ProcessInstance> processInstances = new ArrayList<>();
        long instanceId;
        org.kie.server.api.model.instance.ProcessInstance processInstance;
        if (correlationKey != null) {
            String[] correlationProperties = correlationKey.split(":");
            CorrelationKey actualCorrelationKey = correlationKeyFactory.newCorrelationKey(Arrays.asList(correlationProperties));

            for (int i = 0; i< batchSize; ++i) {
                instanceId = processService.startProcess(containerId, id, actualCorrelationKey, processVariables);
                containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(instanceId));
                processInstance = convertToProcessInstance(runtimeDataService.getProcessInstanceById(instanceId));

                if (withVars && (processInstance.getState().equals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE))) {
                    Map<String, Object> variables = processService.getProcessInstanceVariables(containerId, instanceId);
                    processInstance.setVariables(variables);
                }

                processInstances.add(processInstance);
            }
        } else {
            for (int i = 0; i< batchSize; ++i) {
                instanceId = processService.startProcess(containerId, id, processVariables);
                containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(instanceId));
                processInstance = convertToProcessInstance(runtimeDataService.getProcessInstanceById(instanceId));

                if (withVars && (processInstance.getState().equals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE))) {
                    Map<String, Object> variables = processService.getProcessInstanceVariables(containerId, instanceId);
                    processInstance.setVariables(variables);
                }

                processInstances.add(processInstance);
            }
        }
        return processInstances;
    }

    /**
     * Aborts process instances and gets the aborted instanced back from database.
     *
     * @param ids list of ID's of process instance to abort
     * @param containerId specific container to use when aborting instances, can
     *          be null to abort only using ids
     * @return list of aborted process instances
     */
    public List<ProcessInstance> abortProcessInstances(List<Long> ids, String containerId) {
        if (containerId == null) {
            processService.abortProcessInstances(ids);
        } else {
            processService.abortProcessInstances(containerId, ids);
        }
        List<ProcessInstance> result = new ArrayList<>();
        for (Long pid : ids) {
            result.add(convertToProcessInstance(runtimeDataService.getProcessInstanceById(pid)));
        }

        return result;
    }

    /**
     * Signals process instances with deploymentId using signal and gets signaled instances back.
     *
     * @param deploymentId
     * @param processInstanceIds
     * @param signalName
     * @param event
     * @return
     */
    public List<ProcessInstance> signalProcessInstances(String deploymentId, List<Long> processInstanceIds,
                                                        String signalName, Object event) {
        processService.signalProcessInstances(deploymentId, processInstanceIds, signalName, event);
        List<ProcessInstance> result = new ArrayList<>();
        for (Long id : processInstanceIds) {
            result.add(convertToProcessInstance(runtimeDataService.getProcessInstanceById(id)));
        }

        return result;
    }

    /**
     * Signals process instances with processInstanceIds using signal and gets signaled instances back.
     *
     * @param processInstanceIds
     * @param signalName
     * @param event
     * @return
     */
    public List<ProcessInstance> signalProcessInstances(List<Long> processInstanceIds,
                                                        String signalName, Object event) {
        processService.signalProcessInstances(processInstanceIds, signalName, event);
        List<ProcessInstance> result = new ArrayList<>();
        for (Long id : processInstanceIds) {
            result.add(convertToProcessInstance(runtimeDataService.getProcessInstanceById(id)));
        }

        return result;
    }


    /*
     * Helper methods
     */

    /**
     * Returns list of {@link Status} from list of strings.
     * @param selectedStatuses list of statues in Strings - "Created"
     * @return list of statuses in wrapped in {@link Status}
     */
    private List<Status> getSelectedStatusList(List<String> selectedStatuses) {
        ArrayList<Status> result = new ArrayList<>();
        for (String statusAsString : selectedStatuses) {
            result.add(Status.valueOf(statusAsString));
        }
        return result;
    }
}
