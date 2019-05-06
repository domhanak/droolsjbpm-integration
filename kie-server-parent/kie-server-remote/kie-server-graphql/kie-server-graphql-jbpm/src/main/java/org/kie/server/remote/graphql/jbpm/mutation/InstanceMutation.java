package org.kie.server.remote.graphql.jbpm.mutation;

import java.util.List;
import java.util.Map;

import com.coxautodev.graphql.tools.GraphQLMutationResolver;
import graphql.language.SelectionSet;
import graphql.schema.DataFetchingEnvironment;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.WorkItemInstance;
import org.kie.server.remote.graphql.jbpm.repository.InstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Arguments.CORRELATION_KEY;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Arguments.PROCESS_VARIABLES;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.CONTAINER_ID;
import static org.kie.server.remote.graphql.jbpm.constants.GraphQLConstants.Fields.VARIABLES;

/**
 * {@link GraphQLMutationResolver} for {@link ProcessInstance} data objects.
 *
 * Groups all mutations related to ProcessInstances Runtime.
 */
public class InstanceMutation implements GraphQLMutationResolver {

    private static final Logger logger = LoggerFactory.getLogger(InstanceMutation.class);

    /**
     * Repository for processInstances and related data. Serves as a connector
     * to the service layer for our GraphQL resolvers.
     * You need to initialize this via constructor.
     */
    private final InstanceRepository instanceRepository;

    public InstanceMutation(InstanceRepository instanceRepository) {
        this.instanceRepository = instanceRepository;
    }

    /**
     * Starts the process with process definition id and containerId and returns the instance of the started process.
     * Optionally correlationKey and processVariables can be provided. Both or only one of them, method can handle all
     * cases.
     *
     * @param id id of the process definition fo which to create instance
     * @param containerId  id of the container holding the instance
     * @param correlationKey  correlationKey as string, can be null
     * @param processVariables  map of process variables, can be null
     * @param environment injected {@link DataFetchingEnvironment} used to access query arguments and fields
     * @return {@link ProcessInstance} object for given parameters either with variables or without them.
     *          Depends on the query's {@link SelectionSet}
     */
    public ProcessInstance startProcess(String id,
                                        String containerId,
                                        String correlationKey,
                                        Map<String, Object> processVariables,
                                        DataFetchingEnvironment environment) {
        boolean withVars = environment.getSelectionSet().contains(VARIABLES);
        if (environment.getArgument(CORRELATION_KEY) != null
                && environment.getArgument(PROCESS_VARIABLES) != null) {
            return instanceRepository.startProcess(id,
                                                   containerId,
                                                   correlationKey,
                                                   processVariables,
                                                   withVars);
        } else if (environment.getArgument(CORRELATION_KEY) != null
                && environment.getArgument(PROCESS_VARIABLES) == null) {
            return instanceRepository.startProcess(id,
                                                   containerId,
                                                   correlationKey,
                                                   withVars);
        } else if (environment.getArgument(CORRELATION_KEY) == null
                && environment.getArgument(PROCESS_VARIABLES) != null) {
            return instanceRepository.startProcess(id,
                                                   containerId,
                                                   null,
                                                   processVariables,
                                                   withVars);
        } else  {
            return instanceRepository.startProcess(id,
                                                   containerId,
                                                   withVars);
        }
    }

    /**
     * Starts a given number of process with process definition id and containerId and returns the instances of the started process.
     * Optionally correlationKey and processVariables can be provided. Both or only one of them, method can handle all
     * cases.
     *
     * @param id id of the process definition fo which to create instance
     * @param containerId  id of the container holding the instance
     * @param batchSize size of the resulting list, number of instances to get
     * @param correlationKey  correlationKey as string, can be null
     * @param processVariables  map of process variables, can be null
     * @param environment injected {@link DataFetchingEnvironment} used to access query arguments and fields
     * @return {@link ProcessInstance} object for given parameters either with variables or without them.
     *          Depends on the query's {@link SelectionSet}
     */
    public List<ProcessInstance> startProcesses(String id,
                                                String containerId,
                                                String correlationKey,
                                                Map<String, Object> processVariables,
                                                int batchSize,
                                                DataFetchingEnvironment environment) {
        boolean withVars = environment.getSelectionSet().contains(VARIABLES);
        if (environment.getArgument(CORRELATION_KEY) != null
                && environment.getArgument(PROCESS_VARIABLES) != null) {
            return instanceRepository.startProcesses(id,
                                                     containerId,
                                                     correlationKey,
                                                     processVariables,
                                                     batchSize,
                                                     withVars);
        } else if (environment.getArgument(CORRELATION_KEY) != null
                && environment.getArgument(PROCESS_VARIABLES) == null) {
            return instanceRepository.startProcesses(id,
                                                     containerId,
                                                     correlationKey,
                                                     batchSize,
                                                     withVars);
        } else if (environment.getArgument(CORRELATION_KEY) == null
                && environment.getArgument(PROCESS_VARIABLES) != null) {
            return instanceRepository.startProcesses(id,
                                                     containerId,
                                                     null,
                                                     processVariables,
                                                     batchSize,
                                                     withVars);
        } else  {
            return instanceRepository.startProcesses(id,
                                                     containerId,
                                                     batchSize,
                                                     withVars);
        }
    }

    /**
     * Aborts process instances with given ids and containerId (can be null).
     * If containerId is null it will not be used when aborting process instances.
     *
     * @param instanceIds list of ID of the process instances
     * @param containerId containerId process instances should belong tond fi
     * @param environment injected {@link DataFetchingEnvironment} that is use to access arguments aelds from gql query
     * @return
     */
    public List<ProcessInstance> abortProcessInstances(List<Long> instanceIds, String containerId,
                                                       DataFetchingEnvironment environment) {
        if (environment.getArgument(CONTAINER_ID) == null) {
            return instanceRepository.abortProcessInstances(instanceIds, null);
        } else {
            return instanceRepository.abortProcessInstances(instanceIds, containerId);
        }
    }

    /**
     * Signals process instances with given ids and containerId (can be null).
     * If containerId is null it will not be used when signalling.
     *
     * @param containerId id of the container, can be null
     * @param processInstanceIds id of the process instance
     * @param signalName name of the signal
     * @param event event represented as object
     * @return
     */
    public List<ProcessInstance> signalProcessInstances(String containerId, List<Long> processInstanceIds,
                                                        String signalName, Object event) {
        if (containerId == null) {
            return instanceRepository.signalProcessInstances(processInstanceIds, signalName, event);
        } else {
            return instanceRepository.signalProcessInstances(containerId, processInstanceIds, signalName, event);
        }
    }
}