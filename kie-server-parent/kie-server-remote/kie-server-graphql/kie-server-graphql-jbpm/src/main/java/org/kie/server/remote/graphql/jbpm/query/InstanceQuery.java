package org.kie.server.remote.graphql.jbpm.query;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import com.coxautodev.graphql.tools.GraphQLResolver;
import graphql.language.SelectionSet;
import graphql.schema.DataFetchingEnvironment;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.TaskInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.remote.graphql.jbpm.filter.ProcessInstanceFilter;
import org.kie.server.remote.graphql.jbpm.filter.TaskInstanceFilter;
import org.kie.server.remote.graphql.jbpm.repository.InstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.BATCH_SIZE;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.VARIABLES;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Values.DEFAULT_ALL_TASKS_INSTANCE_BATCH_SIZE;

/**
 * {@link GraphQLResolver} for JBPM Instances and related data-objects.
 * Used to resolve queries for related to {@link ProcessInstance}
 *
 * Groups all ProcessInstance related queries.
 */
public class InstanceQuery implements GraphQLQueryResolver   {

    private static final Logger logger = LoggerFactory.getLogger(InstanceQuery.class);

    /**
     * Repository for processInstances and related data. Serves as a connector
     * to the service layer for our GraphQL resolvers.
     * You need to initialize this via constructor.
     */
    private final InstanceRepository instanceRepository;

    public InstanceQuery(InstanceRepository instanceRepository) {
        this.instanceRepository = instanceRepository;
    }

    /**
     * Gets {@link ProcessInstance} object.
     * Object contains variables if they are selected in query.
     * @param id of the process instance
     * @param containerId id of the container
     * @param environment injected {@link DataFetchingEnvironment} used to access query arguments and fields
     * @return {@link ProcessInstance} for selected arguments with or without variables - depends on queries {@link SelectionSet}
     */
    public ProcessInstance processInstance(long id,
                                           String containerId,
                                           DataFetchingEnvironment environment) {
        if (environment.getSelectionSet().contains(VARIABLES)) {
            logger.debug("Fetching variables for process instance with id={} and containerId={}", id, containerId);
            return instanceRepository.getProcessInstance(id, containerId, true);
        } else {
            logger.debug("Fetching process instance with id={} and containerId={}", id, containerId);
            return instanceRepository.getProcessInstance(id, containerId, false);
        }
    }

    /**
     * Gets all {@link ProcessInstance} objects. The size of the result can be influenced by {@param batchSize}
     * Resulting instances can be influenced by the {@link ProcessInstanceFilter}
     *
     * @param batchSize size of the list that will be returned, by default it is 100
     * @param environment injected {@link DataFetchingEnvironment} used to access query arguments and fields
     * @return List of all {@link ProcessInstance} available for selected filter combination
     */
    public List<ProcessInstance> allProcessInstances(int batchSize,
                                                     ProcessInstanceFilter filter,
                                                     DataFetchingEnvironment environment) {
        List<ProcessInstance> processInstances = instanceRepository.getAllProcessInstances(batchSize, filter);
        if (processInstances.isEmpty()) {
            // if the list is empty just return it
            return processInstances;
        }

        if (environment.getSelectionSet().contains(VARIABLES)) {
            logger.debug("Fetching variables for {} process instances", processInstances.size());
            for (ProcessInstance pi : processInstances) {
                if (pi.getState().equals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE))
                    pi.setVariables(instanceRepository.getProcessInstanceVariables(pi.getId(), pi.getContainerId()));
            }
        }
        return processInstances;
    }

    /**
     * Gets all variables for process instance and return the as a Map of string keys
     * and object values.
     *
     * @param id id of the process instance to fetch variables for
     * @param containerId if of the container of the process instance
     * @return Map of variables
     */
    public Map<String , Object> variables(Long id, String containerId) {
        logger.debug("Fetching variables for process instance with id= {}", id);
        return instanceRepository.getProcessInstanceVariables(id, containerId);
    }

    /**
     * Gets {@link TaskInstance} using taskId.
     *
     * @param taskId id of the task
     * @return {@link TaskInstance}
     */
    public TaskInstance taskInstance(Long taskId, DataFetchingEnvironment environment) {
        return instanceRepository.getTaskInstance(taskId, null);
    }

    /**
     * Gets {@link TaskInstance} using workItemId.
     *
     * @param workItemId workItemId of the task
     * @return {@link TaskInstance}
     */
    public TaskInstance taskInstance(Long workItemId) {
        return instanceRepository.getTaskInstance(null, workItemId);
    }

    /**
     * Gets {@link TaskSummary} list of all tasks matching selected filter.
     *
     * @param batchSize number of tasks to return
     * @param filter {@link TaskInstanceFilter} that is mapped by graphql
     * @return List of {@link TaskSummary} instances
     */
    public List<TaskSummary> allTasks(int batchSize,
                                      TaskInstanceFilter filter) {
        return instanceRepository.getAllTasks(batchSize, filter);
    }
}
