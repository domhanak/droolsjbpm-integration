package org.kie.server.remote.graphql.jbpm.mutation;

import java.util.List;

import com.coxautodev.graphql.tools.GraphQLMutationResolver;
import graphql.language.SelectionSet;
import graphql.schema.DataFetchingEnvironment;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.remote.graphql.jbpm.inputs.AbortProcessInstancesInput;
import org.kie.server.remote.graphql.jbpm.inputs.SignalProcessInstancesInput;
import org.kie.server.remote.graphql.jbpm.inputs.StartProcessesInput;
import org.kie.server.remote.graphql.jbpm.repository.InstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Starts a given number of process with process definition id and containerId and returns the instances of the started process.
     * Optionally correlationKey and processVariables can be provided. Both or only one of them, method can handle all
     * cases.
     * Number of processes is defined by batchSize of {@link StartProcessesInput}. Can be also set to 1.
     *
     * @param input see {@link StartProcessesInput} for details
     * @param environment injected {@link DataFetchingEnvironment} used to access query arguments and fields
     * @return {@link ProcessInstance} object for given parameters either with variables or without them.
     *          Depends on the query's {@link SelectionSet}
     */
    public List<ProcessInstance> startProcesses(StartProcessesInput input,
                                                DataFetchingEnvironment environment) {
        logger.debug("Starting processes with id {} and container {}.", input.getId(), input.getContainerId());
        boolean withVars = environment.getSelectionSet().contains(VARIABLES);
        if (input.getCorrelationKey() != null && input.getVariables() != null) {
            return instanceRepository.startProcesses(input.getId(),
                                                     input.getContainerId(),
                                                     input.getCorrelationKey(),
                                                     input.getVariables(),
                                                     input.getBatchSize(),
                                                     withVars);
        } else if (input.getCorrelationKey() != null
                    && input.getVariables() == null) {
            return instanceRepository.startProcesses(input.getId(),
                                                     input.getContainerId(),
                                                     input.getCorrelationKey(),
                                                     input.getBatchSize(),
                                                     withVars);
        } else if (input.getVariables() != null
                    && input.getCorrelationKey() == null) {
            return instanceRepository.startProcesses(input.getId(),
                                                     input.getContainerId(),
                                                     null,
                                                     input.getVariables(),
                                                     input.getBatchSize(),
                                                     withVars);
        } else {
            return instanceRepository.startProcesses(input.getId(),
                                                     input.getContainerId(),
                                                     input.getBatchSize(),
                                                     withVars);
        }
    }

    /**
     * Aborts process instances with given input.
     *
     * @param input {@link AbortProcessInstancesInput}.
     *              Contains list of ids and optionally containerId
     *              GraphQL makes sure it is non-null for us
     * @return list of aborted {@link ProcessInstance}
     */
    public List<ProcessInstance> abortProcessInstances(AbortProcessInstancesInput input,
                                                       DataFetchingEnvironment environment) {

        boolean withVars = environment.getSelectionSet().contains(VARIABLES);
        if (input.getContainerId() == null) {
            return instanceRepository.abortProcessInstances(input.getIds(),
                                                            null,
                                                            withVars);
        } else {
            return instanceRepository.abortProcessInstances(input.getIds(),
                                                            input.getContainerId(),
                                                            withVars);
        }
    }

    /**
     * Signals process instances with given ids and containerId (can be null).
     * If containerId is null it will not be used when signalling.
     *
     * @param input {@link SignalProcessInstancesInput}
     * @return
     */
    public List<ProcessInstance> signalProcessInstances(SignalProcessInstancesInput input,
                                                        DataFetchingEnvironment environment) {
        boolean withVars = environment.getSelectionSet().contains(VARIABLES);
        if (input.getContainerId() == null) {
            return instanceRepository.signalProcessInstances(input.getIds(),
                                                             input.getSignalName(),
                                                             input.getEvent(),
                                                             withVars);
        } else {
            return instanceRepository.signalProcessInstances(input.getContainerId(), input.getIds(),
                                                             input.getSignalName(), input.getEvent(),
                                                             withVars);
        }
    }
}