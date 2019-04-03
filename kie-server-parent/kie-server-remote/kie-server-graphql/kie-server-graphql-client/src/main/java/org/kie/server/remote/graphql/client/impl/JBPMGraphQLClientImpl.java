package org.kie.server.remote.graphql.client.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.kie.server.api.marshalling.Marshaller;
import org.kie.server.api.marshalling.MarshallerFactory;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.impl.AbstractKieServicesClientImpl;
import org.kie.server.common.rest.Authenticator;
import org.kie.server.remote.graphql.client.JBPMGraphQLClient;
import org.kie.server.remote.graphql.client.helper.JbpmGraphQLResponseData;

public class JBPMGraphQLClientImpl extends AbstractKieServicesClientImpl implements JBPMGraphQLClient {

    private  Marshaller marshaller;
    private  Client httpClient;

    public JBPMGraphQLClientImpl(KieServicesConfiguration config, ClassLoader classLoader) {
        super(config, classLoader);
        this.marshaller = MarshallerFactory.getMarshaller(config.getExtraClasses(), config.getMarshallingFormat(), classLoader);

        httpClient = new ResteasyClientBuilder()
                .establishConnectionTimeout(config.getTimeout(), TimeUnit.MILLISECONDS)
                .socketTimeout(config.getTimeout(), TimeUnit.MILLISECONDS)
                .register(new Authenticator(config.getUserName(), config.getPassword()))
                .build();
    }

    @Override
    public Map<String, Object> executeQuery(String query, String operationName, Map<String, Object> variables) {
            String uri = loadBalancer.getUrl()
                    + "/graphql"
                    + "?operationName=" + operationName + "&query=" + query + "&variables=" + serialize(variables);

            JbpmGraphQLResponseData jbpmGraphQLResponseData = makeHttpGetRequestAndCreateCustomResponse(uri, JbpmGraphQLResponseData.class);

            Map<String, Object> result = new HashMap<String, Object>();
            result.put("data", jbpmGraphQLResponseData.getData());
            result.put("extensions", jbpmGraphQLResponseData.getExtensions());
            return result;
    }

    @Override
    public Map<String, Object> executeMutation(String query, String operationName, Map<String, Object> variables) {
        String uri = loadBalancer.getUrl()
                + "/graphql";

        Map<String, Object> body = new HashMap<>();

        body.put("operationName",operationName);
        body.put("query", query);
        body.put("variables", variables);

        JbpmGraphQLResponseData jbpmGraphQLResponseData = makeHttpPostRequestAndCreateCustomResponse(uri, body, JbpmGraphQLResponseData.class);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("data", jbpmGraphQLResponseData.getData());
        result.put("extensions", jbpmGraphQLResponseData.getExtensions());
        return result;
    }
}
