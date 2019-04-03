package org.kie.server.remote.graphql.jbpm;

import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.jbpm.DefinitionServiceBase;

/**
 * Service provider for JBPM GraphQL extensions.
 * Holds every service JBPM GraphQL extension needs
 * These services are final and once set they can't be altered.
 */
public final class JbpmGraphQLServiceProvider {

    private final DefinitionServiceBase definitionServiceBase;
    private final ProcessService processService;
    private final RuntimeDataService runtimeDataService;
    private final KieServerRegistry kieServerRegistry;

    public JbpmGraphQLServiceProvider(DefinitionServiceBase definitionServiceBase,
                                      ProcessService processService,
                                      RuntimeDataService runtimeDataService,
                                      KieServerRegistry kieServerRegistry) {
        this.definitionServiceBase = definitionServiceBase;
        this.processService = processService;
        this.runtimeDataService = runtimeDataService;
        this.kieServerRegistry = kieServerRegistry;
    }

    public DefinitionServiceBase getDefinitionServiceBase() {
        return definitionServiceBase;
    }

    public ProcessService getProcessService() {
        return processService;
    }

    public RuntimeDataService getRuntimeDataService() {
        return runtimeDataService;
    }

    public KieServerRegistry getKieServerRegistry() {
        return kieServerRegistry;
    }
}
