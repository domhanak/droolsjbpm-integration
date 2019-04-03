package org.kie.server.remote.graphql.jbpm.repository;

import java.util.List;

import org.jbpm.services.api.RuntimeDataService;
import org.kie.api.runtime.query.QueryContext;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.definition.TaskInputsDefinition;
import org.kie.server.api.model.definition.TaskOutputsDefinition;
import org.kie.server.api.model.definition.UserTaskDefinition;
import org.kie.server.services.jbpm.DefinitionServiceBase;

import static org.kie.server.services.jbpm.ConvertUtils.convertToProcessList;

public class DefinitionRepository {

    private DefinitionServiceBase definitionServiceBase;
    private RuntimeDataService runtimeDataService;

    public DefinitionRepository(DefinitionServiceBase definitionServiceBase, RuntimeDataService runtimeDataService) {
        this.definitionServiceBase = definitionServiceBase;
        this.runtimeDataService = runtimeDataService;
    }

    public List<ProcessDefinition> getProcessDefinitions(int batchSize) {
        QueryContext queryContext = new QueryContext();
        queryContext.setCount(batchSize);

        return convertToProcessList(runtimeDataService.getProcesses(queryContext)).getItems();
    }


    public List<ProcessDefinition> getProcessDefinitions(String containerId, int batchSize) {
        QueryContext queryContext = new QueryContext();
        queryContext.setCount(batchSize);
        return convertToProcessList(runtimeDataService.getProcessesByDeploymentId(containerId, queryContext)).getItems();
    }

    public List<ProcessDefinition> getProcessDefinitions(String processId) {
        return convertToProcessList(runtimeDataService.getProcessesById(processId)).getItems();
    }

    public ProcessDefinition getProcessDefinition(String id, String containerId) {
       return definitionServiceBase.getProcessDefinition(containerId, id);
    }

    public List<UserTaskDefinition> getUserTaskDefinitions(String id, String containerId) {
        return definitionServiceBase.getTasksDefinitions(containerId, id).getItems();
    }

    public TaskInputsDefinition taskInputMappings(String id, String containerId, String name) {
        return definitionServiceBase.getTaskInputMappings(containerId, id, name);
    }

    public TaskOutputsDefinition taskOutputMappings(String id, String containerId, String name) {
        return definitionServiceBase.getTaskOutputMappings(containerId, id, name);
    }
}
