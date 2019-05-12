package org.kie.server.remote.graphql.jbpm;

import java.util.Map;

import com.coxautodev.graphql.tools.GraphQLResolver;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.remote.graphql.jbpm.repository.InstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessInstanceResolver implements GraphQLResolver<ProcessInstance> {

    private static final Logger logger = LoggerFactory.getLogger(ProcessInstanceResolver.class);

    private InstanceRepository instanceRepository;

    public ProcessInstanceResolver(JbpmGraphQLServiceProvider serviceProvider) {
        this.instanceRepository = new InstanceRepository(serviceProvider.getRuntimeDataService(),
                                                         serviceProvider.getProcessService(),
                                                         serviceProvider.getKieServerRegistry());
    }

    public Map<String, Object> variables(ProcessInstance instance) {
        logger.info("Resolving variables for {} with container {}", instance.getId(), instance.getContainerId());
        return instanceRepository.getProcessInstanceVariables(instance.getId(), instance.getContainerId());
    }

}
