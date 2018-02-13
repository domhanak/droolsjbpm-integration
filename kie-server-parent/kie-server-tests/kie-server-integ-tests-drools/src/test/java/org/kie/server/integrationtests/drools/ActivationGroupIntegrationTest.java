package org.kie.server.integrationtests.drools;

import java.util.ArrayList;
import java.util.List;

import org.drools.core.command.runtime.rule.ClearActivationGroupCommand;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.ExecutionResults;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;

import static org.assertj.core.api.Assertions.*;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;

public class ActivationGroupIntegrationTest extends DroolsKieServerBaseIntegrationTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "activation-group",
            "1.0.0.Final");

    private static final String CONTAINER_ID = "activation";
    private static final String LIST_NAME = "list";
    private static final String LIST_OUTPUT_NAME = "output-list";
    private static final String KIE_SESSION = "ksession1";
    private static final String ACTIVATION_GROUP = "first-group";

    @BeforeClass
    public static void buildAndDeployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/activation-group").getFile());

        createContainer(CONTAINER_ID, releaseId);
    }

    /**
     * First rule in activation group "First group" is executed, second rule skipped.
     */
    @Test
    public void testActivationGroup() {
        List<Command<?>> commands = new ArrayList<Command<?>>();

        BatchExecutionCommand batchExecution = commandsFactory.newBatchExecution(commands, KIE_SESSION);

        commands.add(commandsFactory.newSetGlobal(LIST_NAME, new ArrayList<String>(), LIST_OUTPUT_NAME));
        commands.add(commandsFactory.newFireAllRules());
        commands.add(commandsFactory.newGetGlobal(LIST_NAME, LIST_OUTPUT_NAME));

        ServiceResponse<ExecutionResults> response = ruleClient.executeCommandsWithResults(CONTAINER_ID, batchExecution);
        KieServerAssert.assertSuccess(response);
        ExecutionResults result = response.getResult();

        List<?> outcome = (List<?>) result.getValue(LIST_OUTPUT_NAME);
        assertThat(outcome).isNotNull();
        assertThat(outcome).hasSize(2);

        assertThat(outcome.get(0)).isEqualTo("First rule in first activation group executed");
        assertThat(outcome.get(1)).isEqualTo("Rule without activation group executed");

    }

    /**
     * Activation group "First group" is cleared, so it isn't executed.
     */
    @Test
    public void testClearActivationGroup() {
        List<Command<?>> commands = new ArrayList<Command<?>>();

        BatchExecutionCommand batchExecution = commandsFactory.newBatchExecution(commands, KIE_SESSION);

        commands.add(commandsFactory.newSetGlobal(LIST_NAME, new ArrayList<String>(), LIST_OUTPUT_NAME));
        // Replace if/after Clear command is added to command factory.
        // commands.add(commandsFactory.newClearActivationGroup(ACTIVATION_GROUP));
        commands.add(new ClearActivationGroupCommand(ACTIVATION_GROUP));
        commands.add(commandsFactory.newFireAllRules());
        commands.add(commandsFactory.newGetGlobal(LIST_NAME, LIST_OUTPUT_NAME));

        ServiceResponse<ExecutionResults> response = ruleClient.executeCommandsWithResults(CONTAINER_ID, batchExecution);
        KieServerAssert.assertSuccess(response);
        ExecutionResults result = response.getResult();

        List<?> outcome = (List<?>) result.getValue(LIST_OUTPUT_NAME);
        assertThat(outcome).isNotNull();
        assertThat(outcome).hasSize(1);

        assertThat(outcome.get(0)).isEqualTo("Rule without activation group executed");

    }
}
