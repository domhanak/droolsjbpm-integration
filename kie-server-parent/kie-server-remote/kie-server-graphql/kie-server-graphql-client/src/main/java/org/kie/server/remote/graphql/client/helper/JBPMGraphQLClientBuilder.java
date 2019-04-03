package org.kie.server.remote.graphql.client.helper;

import java.util.HashMap;
import java.util.Map;

import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.helper.KieServicesClientBuilder;
import org.kie.server.remote.graphql.client.JBPMGraphQLClient;
import org.kie.server.remote.graphql.client.impl.JBPMGraphQLClientImpl;

public class JBPMGraphQLClientBuilder implements KieServicesClientBuilder {

    @Override
    public String getImplementedCapability() {
        return "GraphQL jBPM capability";
    }

    @Override
    public Map<Class<?>, Object> build(KieServicesConfiguration configuration, ClassLoader classLoader) {

        Map<Class<?>, Object> services = new HashMap<>();
        services.put(JBPMGraphQLClient.class, new JBPMGraphQLClientImpl(configuration, classLoader));
        return services;
    }
}
