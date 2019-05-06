package org.kie.server.remote.graphql.jbpm.mutation;

import com.coxautodev.graphql.tools.GraphQLMutationResolver;
import org.kie.server.remote.graphql.jbpm.JbpmGraphQLServiceProvider;
import org.kie.server.remote.graphql.jbpm.repository.InstanceRepository;

/**
 * Root mutation resolver.
 *
 *  Takes {@link JbpmGraphQLServiceProvider} and sets up repositories
 *  for mutation resolvers.
 *
 * Groups other mutation resolver to increase readability, testability and structure.
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

    /**
     * Resolver for instances mutation.
     *
     * @return Mutation resolver for instance - {@link InstanceMutation}
     */
    public InstanceMutation getInstances() {
        return new InstanceMutation(instanceRepository);
    }
}
