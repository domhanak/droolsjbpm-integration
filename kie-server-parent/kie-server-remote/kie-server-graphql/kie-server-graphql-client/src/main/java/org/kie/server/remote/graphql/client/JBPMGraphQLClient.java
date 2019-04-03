package org.kie.server.remote.graphql.client;

import java.util.Map;


public interface JBPMGraphQLClient {
    Map<String, Object> executeQuery(String query, String operationName, Map<String, Object> variables);

    Map<String, Object> executeMutation(String query, String operationName, Map<String, Object> variables);

    //subscriptions
}
