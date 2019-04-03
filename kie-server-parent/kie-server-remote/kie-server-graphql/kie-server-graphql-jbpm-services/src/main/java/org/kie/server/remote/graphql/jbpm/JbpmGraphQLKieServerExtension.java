package org.kie.server.remote.graphql.jbpm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;

import org.jbpm.services.api.DefinitionService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.kie.server.api.KieServerConstants;
import org.kie.server.services.api.KieContainerInstance;
import org.kie.server.services.api.KieServerApplicationComponentsService;
import org.kie.server.services.api.KieServerExtension;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.api.SupportedTransports;
import org.kie.server.services.impl.KieServerImpl;
import org.kie.server.services.jbpm.JbpmKieServerExtension;

public class JbpmGraphQLKieServerExtension implements KieServerExtension {
    public static final String EXTENSION_NAME = "JBPM GraphQL";

    private static final Boolean JBPM_DISABLED = Boolean.parseBoolean(System.getProperty(KieServerConstants.KIE_JBPM_SERVER_EXT_DISABLED, "false"));

    private boolean initialized = false;

    private KieServerRegistry registry;

    private ProcessService processService;
    private ExecutorService executorService;
    private UserTaskService userTaskService;
    private RuntimeDataService runtimeDataService;
    private DefinitionService definitionService;

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public boolean isActive() {
        return JBPM_DISABLED == false;
    }

    @Override
    public void init(KieServerImpl kieServer, KieServerRegistry registry) {
        KieServerExtension jbpmExtension = registry.getServerExtension(JbpmKieServerExtension.EXTENSION_NAME);
        if (jbpmExtension == null) {
            // jBPM extension not found, disabling itself
            initialized = false;
            return;
        }

        List<Object> jbpmServices = jbpmExtension.getServices();

        for (Object service : jbpmServices) {
            if (DefinitionService.class.isAssignableFrom(service.getClass())) {
                definitionService = (DefinitionService) service;
                continue;
            }
            if (ProcessService.class.isAssignableFrom(service.getClass())) {
                processService = (ProcessService) service;
                continue;
            }
            if (ExecutorService.class.isAssignableFrom(service.getClass())) {
                executorService = (ExecutorService) service;
                continue;
            }
            if (UserTaskService.class.isAssignableFrom(service.getClass())) {
                userTaskService = (UserTaskService) service;
                continue;
            }
            if (RuntimeDataService.class.isAssignableFrom(service.getClass())) {
                runtimeDataService = (RuntimeDataService) service;
                continue;
            }
        }
        this.registry = registry;
        initialized = true;
    }



    @Override
    public void destroy(KieServerImpl kieServer, KieServerRegistry registry) {
    }

    @Override
    public void createContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
    }

    @Override
    public void updateContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
    }

    @Override
    public boolean isUpdateContainerAllowed(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
        return true;
    }

    @Override
    public void disposeContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
    }

    @Override
    public List<Object> getAppComponents(SupportedTransports type) {
        List<Object> appComponentsList = new ArrayList<>();
        if (!initialized) {
            return appComponentsList;
        }

        ServiceLoader<KieServerApplicationComponentsService> appComponentsServices = ServiceLoader.load(KieServerApplicationComponentsService.class);

        Object[] services = {registry};

        for (KieServerApplicationComponentsService appComponentsService : appComponentsServices) {
            appComponentsList.addAll(appComponentsService.getAppComponents(EXTENSION_NAME, type, services));
        }

        return appComponentsList;
    }

    @Override
    public <T> T getAppComponents(Class<T> serviceType) {
        if (serviceType.isAssignableFrom(definitionService.getClass())) {
            return (T) definitionService;
        } else if (serviceType.isAssignableFrom(executorService.getClass())) {
            return (T) executorService;
        } else if (serviceType.isAssignableFrom(processService.getClass())) {
            return (T) processService;
        } else if (serviceType.isAssignableFrom(userTaskService.getClass())) {
            return (T) userTaskService;
        } else if (serviceType.isAssignableFrom(runtimeDataService.getClass())) {
            return (T) runtimeDataService;
        }
        return null;
    }

    @Override
    public String getImplementedCapability() {
        return "GraphQL jBPM capability";
    }

    @Override
    public List<Object> getServices() {
        List<Object> services = new ArrayList<>();
        services.add(processService);
        services.add(definitionService);
        services.add(userTaskService);
        services.add(runtimeDataService);
        return services;
    }

    @Override
    public String getExtensionName() {
        return EXTENSION_NAME;
    }

    @Override
    public Integer getStartOrder() {
        // To be started after Drools
        return 12;
    }

    @Override
    public String toString() {
        return EXTENSION_NAME + " KIE Server extension";
    }
}