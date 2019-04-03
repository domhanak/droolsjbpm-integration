package org.kie.server.remote.graphql.jbpm.query;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import org.kie.server.remote.graphql.jbpm.JbpmGraphQLServiceProvider;
import org.kie.server.remote.graphql.jbpm.repository.DefinitionRepository;
import org.kie.server.remote.graphql.jbpm.repository.InstanceRepository;


/**
 * Root query resolver.
 *
 * Takes {@link JbpmGraphQLServiceProvider} and sets up repositories,
 * for our query resolvers.
 *
 * Groups other query resolvers to increase readability, testability and structure.
 */
public class Query implements GraphQLQueryResolver {

    /**
     * DB connector for definitions.
     */
    private final DefinitionRepository definitionRepository;

    /**
     * DB connector for instances.
     */
    private final InstanceRepository instanceRepository;

    public Query(JbpmGraphQLServiceProvider serviceProvider) {
        this.definitionRepository = new DefinitionRepository(serviceProvider.getDefinitionServiceBase(),
                                                             serviceProvider.getRuntimeDataService());
        this.instanceRepository = new InstanceRepository(serviceProvider.getRuntimeDataService(),
                                                         serviceProvider.getProcessService(),
                                                         serviceProvider.getKieServerRegistry());
    }

    public DefinitionQuery getDefinitions() {
        return new DefinitionQuery(definitionRepository);
    }

    public InstanceQuery getInstances() {
        return new InstanceQuery(instanceRepository);
    }
}
