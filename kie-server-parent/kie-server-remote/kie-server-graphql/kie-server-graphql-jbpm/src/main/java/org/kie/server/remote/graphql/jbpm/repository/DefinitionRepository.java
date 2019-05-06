package org.kie.server.remote.graphql.jbpm.repository;

import java.util.List;

import org.jbpm.services.api.RuntimeDataService;
import org.kie.api.runtime.query.QueryContext;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.definition.TaskInputsDefinition;
import org.kie.server.api.model.definition.TaskOutputsDefinition;
import org.kie.server.api.model.definition.UserTaskDefinition;
import org.kie.server.remote.graphql.jbpm.query.DefinitionQuery;
import org.kie.server.services.jbpm.DefinitionServiceBase;

import static org.kie.server.services.jbpm.ConvertUtils.convertToProcessList;

/**
 * GraphQL connector for {@link DefinitionQuery}
 */
public class DefinitionRepository {

    private DefinitionServiceBase definitionServiceBase;
    private RuntimeDataService runtimeDataService;

    public DefinitionRepository(DefinitionServiceBase definitionServiceBase, RuntimeDataService runtimeDataService) {
        this.definitionServiceBase = definitionServiceBase;
        this.runtimeDataService = runtimeDataService;
    }

    /**
     * Gets all process definitions from runtime. Size of the result is determined by
     * the parameter batchSize
     *
     * @param batchSize size of the result
     * @return List of {@link ProcessDefinition}
     */
    public List<ProcessDefinition> getProcessDefinitions(int batchSize) {
        QueryContext queryContext = new QueryContext();
        queryContext.setCount(batchSize);

        return convertToProcessList(runtimeDataService.getProcesses(queryContext)).getItems();
    }

    /**
     * Gets all process definitions with specific process id from runtime.
     *
     * @param processId id of the process
     *
     * @return List of {@link ProcessDefinition}
     */
    public List<ProcessDefinition> getProcessDefinitions(String processId) {
        return convertToProcessList(runtimeDataService.getProcessesById(processId)).getItems();
    }

    /**
     * Gets all process definitions from runtime, belonging to specific container.
     *
     * @param containerId id of the container that definitions belong to
     * @param batchSize size of the result
     *
     * @return List of {@link ProcessDefinition}
     */
    public List<ProcessDefinition> getProcessDefinitions(String containerId, int batchSize) {
        QueryContext queryContext = new QueryContext();
        queryContext.setCount(batchSize);
        return convertToProcessList(runtimeDataService.getProcessesByDeploymentId(containerId, queryContext)).getItems();
    }

    /**
     * Gets a single ProcessDefinition with id and containerId
     *
     * @param id id of the process definition
     * @param containerId id of the container that definition belongs to
     *
     * @return {@link ProcessDefinition}
     */
    public ProcessDefinition getProcessDefinition(String id, String containerId) {
       return definitionServiceBase.getProcessDefinition(containerId, id);
    }

    /**
     * Gets task definitions for process with id and containerId
     *
     * @param id id of the process tasks definitions belong to
     * @param containerId id of the container that definition belongs to
     *
     * @return List of {@link UserTaskDefinition}
     */
    public List<UserTaskDefinition> getUserTaskDefinitions(String id, String containerId) {
        return definitionServiceBase.getTasksDefinitions(containerId, id).getItems();
    }

    /**
     * Gets task's input mappings for specific task of the process.
     *
     * @param id id of the process definition
     * @param containerId id of the container that definition belongs to
     * @param name name of the task
     *
     * @return {@link TaskInputsDefinition}
     */
    public TaskInputsDefinition taskInputMappings(String id, String containerId, String name) {
        return definitionServiceBase.getTaskInputMappings(containerId, id, name);
    }

    /**
     * Gets task's output mappings for specific task of the process.
     *
     * @param id id of the process definition
     * @param containerId id of the container that definition belongs to
     * @param name name of the task
     *
     * @return {@link TaskOutputsDefinition}
     */
    public TaskOutputsDefinition taskOutputMappings(String id, String containerId, String name) {
        return definitionServiceBase.getTaskOutputMappings(containerId, id, name);
    }
}
