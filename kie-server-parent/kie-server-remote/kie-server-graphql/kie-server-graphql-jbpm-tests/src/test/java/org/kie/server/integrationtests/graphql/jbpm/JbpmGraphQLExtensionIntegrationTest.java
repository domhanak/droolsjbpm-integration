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
import org.kie.server.remote.graphql.jbpm.inputs.StartProcessesInput;

public class JbpmGraphQLExtensionIntegrationTest extends RestJmsSharedBaseIntegrationTest {

    @ClassRule
    public static ExternalResource StaticResource = new DBExternalResource();

    private static final String PROCESS_DEFINITION_ID = "definition-project.UserTaskWithSLAOnTask";
    private static final String CONTAINER_ID = "definition-project";
    private static final String GROUP_ID = "org.kie.server.testing";
    private static final String VERSION = "1.0.0.Final";

    private static ReleaseId releaseId = new ReleaseId(GROUP_ID,
                                                       CONTAINER_ID,
                                                       VERSION);

    private JBPMGraphQLClient graphQLClient;

    @BeforeClass
    public static void deployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProjectFromResource("/kjars-sources/definition-project");

        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);

        createContainer(CONTAINER_ID, releaseId);
    }

    @Before
    public void setUp() {
        Assume.assumeTrue(configuration.isRest());
        Assume.assumeTrue(configuration.getMarshallingFormat().equals(MarshallingFormat.JSON));
    }

    private String getStringFromFile(String fileName) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(Objects.requireNonNull(classLoader.getResource(fileName)).getFile());

        return readFile(file.getAbsolutePath(), Charset.defaultCharset());
    }

    @AfterClass
    public static void disposeContainers() {
        disposeAllContainers();
    }

    @Override
    protected void setupClients(KieServicesClient client) {
        graphQLClient = client.getServicesClient(JBPMGraphQLClient.class);
    }

    private static String readFile(String path, Charset encoding)
    {
        byte[] encoded = new byte[0];
        try {
            encoded = Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            Assertions.fail("Error while reading query file - [%s].", path);
        }
        return new String(encoded, encoding);
    }

    @Test
    public void testExecuteQuery() {
        String operationName = "";
        Map<String, Object> variables = new HashMap<>();
        variables.put("processDefinitionId", PROCESS_DEFINITION_ID);
        variables.put("containerId", CONTAINER_ID);

        Assertions.assertThat(graphQLClient).isNotNull();

        Map<String, Object> result = graphQLClient.executeQuery(getStringFromFile("processDefinitionsQuery.graphql"),
                                                                operationName,
                                                                variables);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get("data").toString()).contains("\"id\":\"definition-project.UserTaskWithSLAOnTask\"");
    }

    @Test
    public void testExecuteStartProcessMutation() {
        String operationName = "";

        StartProcessesInput input = new StartProcessesInput();
        input.setId("AsyncScriptTask");
        input.setContainerId(CONTAINER_ID);
        input.setBatchSize(1);

        Map<String, Object> variables = new HashMap<>();
        variables.put("input", input);

        Assertions.assertThat(graphQLClient).isNotNull();

        Map<String, Object> result = graphQLClient.executeMutation(getStringFromFile("processInstancesMutation.graphql"),
                                                                   operationName, variables);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.get("data").toString()).contains("\"processId\":\"AsyncScriptTask");
    }
}
