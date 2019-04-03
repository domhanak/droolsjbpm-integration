package org.kie.server.remote.graphql.jbpm.mutation;

import com.coxautodev.graphql.tools.GraphQLMutationResolver;
import org.kie.server.remote.graphql.jbpm.JbpmGraphQLServiceProvider;
import org.kie.server.remote.graphql.jbpm.repository.InstanceRepository;

/**
 * Root mutation resolver.
 *
 * Groups other mutation reslover to increase readability, testability and structure
 */
public class Mutation implements GraphQLMutationResolver {

    /**
     * DB connector for instances.
     */
    private final InstanceRepository instanceRepository;

    public Mutation(JbpmGraphQLServiceProvider serviceProvider) {
        this.instanceRepository = new InstanceRepository(serviceProvider.getRuntimeDataService(),
                                                         serviceProvider.getProcessService(),
                                                         serviceProvider.getKieServerRegistry());
    }

    public InstanceMutation getProcessInstances() {
        return new InstanceMutation(instanceRepository);
    }
}
