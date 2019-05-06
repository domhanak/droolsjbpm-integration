package org.kie.server.remote.graphql.jbpm.query;

import java.util.List;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import com.coxautodev.graphql.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.definition.UserTaskDefinition;
import org.kie.server.remote.graphql.jbpm.exceptions.JbpmGraphQLException;
import org.kie.server.remote.graphql.jbpm.repository.DefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.CONTAINER_ID;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.PROCESS_DEFINITION_ID;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.PROCESS_INSTANCE_ID;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.TASK_INPUT_MAPPINGS;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.TASK_OUTPUT_MAPPINGS;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Values.DEFAULT_ALL_PROCESS_DEFINITION_BATCH_SIZE;

/**
 * {@link GraphQLResolver} for ProcessDefinition's and related data-objects.
 * Used to resolve queries for {@link ProcessDefinition} data
 *
 * Groups all ProcessDefinition related queries.
 */
public class DefinitionQuery implements GraphQLQueryResolver {

    private static final Logger logger = LoggerFactory.getLogger(DefinitionQuery.class);

    /**
     * Offers access to jBPM services API.
     * Provides data for resolvers.
     */
    private final DefinitionRepository definitionRepository;


    public DefinitionQuery(DefinitionRepository definitionRepository) {
        this.definitionRepository = definitionRepository;
    }

    /**
     * Gets all {@link ProcessDefinition} objects. The size of the result can be influenced by {@param batchSize}
     * @param batchSize size of the list that will be returned, by default it is 100
     * @return List of {@link ProcessDefinition} available
     */
    public List<ProcessDefinition> allProcessDefinitions(int batchSize) {
        logger.debug("allProcessDefinitions( {} )", batchSize);
        return definitionRepository.getProcessDefinitions(batchSize);
    }

    /**
     * Gets list of {@link ProcessDefinition} for given processDefinitionId or containerId.
     * Only one of the parameters can be selected.
     * @param processDefinitionId if provided in the query it is used to get results
     * @param containerId if provided in the query it is used to get results
     * @param environment injected {@link DataFetchingEnvironment} used to access query arguments and fields
     * @return List of {@link ProcessDefinition} for provided arguments
     */
    public List<ProcessDefinition> processDefinitions(String processDefinitionId,
                                                      String containerId,
                                                      DataFetchingEnvironment environment) {
        if (environment.getArgument(PROCESS_DEFINITION_ID) != null
                && environment.getArgument(CONTAINER_ID) == null) {
            return definitionRepository.getProcessDefinitions(processDefinitionId);
        } else if (environment.getArgument(CONTAINER_ID) != null
                && environment.getArgument(PROCESS_DEFINITION_ID) == null) {
            return definitionRepository.getProcessDefinitions(containerId, DEFAULT_ALL_PROCESS_DEFINITION_BATCH_SIZE);
        }
        throw new JbpmGraphQLException("Incorrect combination of arguments. Only one of them can be specified.");
    }

    /**
     * Gets single {@link ProcessDefinition} for given processDefinitionId and containerId.
     * @param processDefinitionId ID of the process definition
     * @param containerId Container ID of the container that ths definition is associated with
     * @return {@link ProcessDefinition} object
     */
    public ProcessDefinition processDefinition(String processDefinitionId,
                                               String containerId) {
        logger.info("processDefinition( {} , {} )", processDefinitionId, containerId);
        return definitionRepository.getProcessDefinition(processDefinitionId, containerId);
    }

    /**
     * Gets a list of {@link UserTaskDefinition} for process definition and container id.
     * @param processDefinitionId ID of the process definition
     * @param containerId Container ID of the container that ths definition is associated with
     * @param environment injected {@link DataFetchingEnvironment} used to access query arguments and fields
     * @return A list of {@link UserTaskDefinition} with taskInputsMapping & taskOutputMappings if they are selected
     */
    public List<UserTaskDefinition> userTaskDefinitions(String processDefinitionId,
                                                        String containerId,
                                                        DataFetchingEnvironment environment) {
        logger.debug("userTaskDefinitions( {} , {} )", processDefinitionId, containerId);
        String taskName;
        List<UserTaskDefinition> result = definitionRepository.getUserTaskDefinitions(processDefinitionId, containerId);
        if (environment.getSelectionSet().contains(TASK_INPUT_MAPPINGS)) {
            logger.debug("Getting taskInputMappings for task of process {} with container {} ", processDefinitionId, containerId);
            for (UserTaskDefinition userTaskDefinition: result) {
                taskName = userTaskDefinition.getName();
                userTaskDefinition.setTaskInputMappings(definitionRepository.taskInputMappings(processDefinitionId,
                                                                                               containerId,
                                                                                               taskName).getTaskInputs());
            }
        }
        if (environment.getSelectionSet().contains(TASK_OUTPUT_MAPPINGS)) {
            logger.debug("Getting taskOutputMappings for task of process {} with container {} ", processDefinitionId, containerId);
            for (UserTaskDefinition userTaskDefinition: result) {
                taskName = userTaskDefinition.getName();
                userTaskDefinition.setTaskOutputMappings(definitionRepository.taskOutputMappings(processDefinitionId,
                                                                                                 containerId,
                                                                                                 taskName).getTaskOutputs());
            }
        }
        return result;
    }
}
