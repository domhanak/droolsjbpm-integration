package org.kie.server.integrationtests.graphql.jbpm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.kie.api.KieServices;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.client.KieServicesClient;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.kie.server.integrationtests.shared.basetests.RestJmsSharedBaseIntegrationTest;
import org.kie.server.remote.graphql.client.JBPMGraphQLClient;

public class JbpmGraphQLExtensionIntegrationTest extends RestJmsSharedBaseIntegrationTest {

    @ClassRule
    public static ExternalResource StaticResource = new DBExternalResource();

    private static final String CONTAINER_ID = "definition-project";
    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", CONTAINER_ID,
                                                       "1.0.0.Final");

    private JBPMGraphQLClient graphQLClient;
    private String query;
    private String mutation;

    @BeforeClass
    public static void deployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProjectFromResource("/kjars-sources/definition-project");

        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);

        createContainer(CONTAINER_ID, releaseId);
    }

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(configuration.isRest());
        Assume.assumeTrue(configuration.getMarshallingFormat().equals(MarshallingFormat.JSON));


        ClassLoader classLoader = getClass().getClassLoader();
        File queryFile = new File(Objects.requireNonNull(classLoader.getResource("processDefinitionsQuery.graphql")).getFile());
        File mutationFile = new File(Objects.requireNonNull(classLoader.getResource("processInstancesMutation.graphql")).getFile());

        query = readFile(queryFile.getAbsolutePath(), Charset.defaultCharset());
        mutation = readFile(mutationFile.getAbsolutePath(), Charset.defaultCharset());

    }

    @AfterClass
    public static void disposeContainers() {

        disposeAllContainers();
    }

    @Override
    protected void addExtraCustomClasses(Map<String, Class<?>> extraClasses) throws Exception {
       //extraClasses.put(PERSON_CLASS_NAME, Class.forName(PERSON_CLASS_NAME, true, kieContainer.getClassLoader()));
    }

    @Override
    protected void setupClients(KieServicesClient client) {
        graphQLClient = client.getServicesClient(JBPMGraphQLClient.class);
    }

    static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    @Test
    public void testExecuteQuery() {
        String operationName = "";
        Map<String, Object> variables = new HashMap<>();
        variables.put("processDefinitionId", "definition-project.UserTaskWithSLAOnTask");
        variables.put("containerId", "definition-project");

        Assertions.assertThat(graphQLClient).isNotNull();

        Map<String, Object> result = graphQLClient.executeQuery(query,
                                                                operationName,
                                                                variables);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get("data").toString()).contains("\"id\":\"definition-project.UserTaskWithSLAOnTask");
    }

    @Test
    public void testExecuteMutation() {
        String operationName = "";
        Map<String, Object> body = new HashMap<>();

        Map<String, Object> variables = new HashMap<>();
        variables.put("id", "AsyncScriptTask");
        variables.put("containerId", "definition-project");
        variables.put("batchSize", 1);

        Assertions.assertThat(graphQLClient).isNotNull();

        Map<String, Object> result = graphQLClient.executeMutation(mutation, operationName, variables);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get("data").toString()).contains("\"processId\":\"AsyncScriptTask");
    }
}
