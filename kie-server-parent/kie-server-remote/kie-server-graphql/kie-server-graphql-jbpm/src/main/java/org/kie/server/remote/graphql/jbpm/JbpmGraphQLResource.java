package org.kie.server.remote.graphql.jbpm;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.coxautodev.graphql.tools.SchemaParser;
import com.coxautodev.graphql.tools.SchemaParserOptions;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhokhov.graphql.datetime.GraphQLDate;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLSchema;
import org.kie.server.remote.graphql.jbpm.mutation.Mutation;
import org.kie.server.remote.graphql.jbpm.query.Query;
import org.kie.server.services.api.KieServerRegistry;

/**
 * GraphQL resource for jBPM.
 * Resource builds the GraphQL schema from a file - jbpm-schema.graphql -
 * at startup.
 * It exposes two methods to handle HTTP Get methods requests and
 * HTTP Post methods requests to follow best practices suggested by GraphQL
 * documentation.
 * Get method is implemented so it can handle GraphQL queries.
 * Post method is implemented so it can handle GraphQL mutations.
 */
@Path("/server")
public class JbpmGraphQLResource {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String JBPM_SCHEMA = "jbpm-schema.graphql";

    /**
     * GraphQL java implementation.
     * Initialized in constructor.
     */
    private GraphQL graphql;

    /**
     * Service provider that offers services needed by Query and Mutation resolvers.
     */
    private JbpmGraphQLServiceProvider serviceProvider;

    public JbpmGraphQLResource(JbpmGraphQLServiceProvider serviceProvider) {
        Instrumentation instrumentation = new TracingInstrumentation();

        // Initialize ServiceProvider before we build schema
        this.serviceProvider = serviceProvider;

        GraphQLSchema graphQLSchema = buildSchema();
        this.graphql = GraphQL.newGraphQL(graphQLSchema).instrumentation(instrumentation).build();
    }

    @Path(value = "/graphql")
    @GET
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Map<String, Object> graphqlGET(@QueryParam("query") String query,
                                          @QueryParam("operationName") String operationName,
                                          @QueryParam("variables") String variablesJson) throws IOException {
        if (query == null) {
            query = "";
        }

        if (operationName == null) {
            operationName = "";
        }

        Map<String, Object> variables = new LinkedHashMap<>();

        if (variablesJson != null) {
            variables = mapper.readValue(variablesJson, new TypeReference<Map<String, Object>>() {
            });
        }

        return executeGraphqlQuery(operationName, query, variables);
    }

    @SuppressWarnings("unchecked")
    @Path(value = "/graphql")
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Map<String, Object> graphql(@Context HttpHeaders headers, Map<String, Object> body) {
        String query = (String) body.get("query");

        if (query == null) {
            query = "";
        }

        String operationName = (String) body.get("operationName");
        if (operationName == null) {
            operationName = "";
        }
        Map<String, Object> variables = (Map<String, Object>) body.get("variables");

        if (variables == null) {
            variables = new LinkedHashMap<>();
        }

        return executeGraphqlQuery(operationName, query, variables);
    }

    /**
     * Executes the GraphQL query or mutation.
     *
     * @param operationName
     * @param query
     * @param variables
     * @return
     */
    private Map<String, Object> executeGraphqlQuery(String operationName,
                                                    String query, Map<String, Object> variables) {

        ExecutionInput executionInput;
        if (operationName == null) {
            executionInput = ExecutionInput.newExecutionInput()
                    .query(query)
                    .variables(variables)
                    .build();
        } else {
            executionInput = ExecutionInput.newExecutionInput()
                    .query(query)
                    .variables(variables)
                    .operationName(operationName)
                    .build();
        }
        return graphql.execute(executionInput).toSpecification();
    }

    /**
     * Builder for GraphQL schema for jBPM.
     *
     * @return graphql schema implementation {@link GraphQLSchema}
     */
    private GraphQLSchema buildSchema() {
        return SchemaParser
                .newParser()
                .options(SchemaParserOptions.newOptions().preferGraphQLResolver(true).build())
                .files(JBPM_SCHEMA)
                .scalars(new GraphQLDate(),
                         ExtendedScalars.DateTime,
                         ExtendedScalars.Object)
                .resolvers(new Query(serviceProvider),
                           new Mutation(serviceProvider))
                .build()
                .makeExecutableSchema();
    }
}
