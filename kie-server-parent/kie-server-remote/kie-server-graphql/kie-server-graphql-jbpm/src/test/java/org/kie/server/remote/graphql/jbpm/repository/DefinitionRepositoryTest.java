package org.kie.server.remote.graphql.jbpm.repository;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.jbpm.kie.services.impl.model.ProcessAssetDesc;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.model.ProcessDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.query.QueryContext;
import org.kie.server.services.jbpm.DefinitionServiceBase;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefinitionRepositoryTest {

    private static final int BATCH_SIZE = 1;
    private static final String CONTAINER_ID_ONE = "container-id-1";
    private static final String PROCESS_ID = "definitions";

    @Mock
    private DefinitionServiceBase definitionServiceBase;

    @Mock
    private RuntimeDataService runtimeDataService;

    private DefinitionRepository definitionRepository;
    private org.kie.server.api.model.definition.ProcessDefinition definition;
    private List<ProcessDefinition> jbpmDefinitions = new ArrayList<>();
    private QueryContext queryContext;

    @Before
    public void setUp() throws Exception {
        definitionRepository = new DefinitionRepository(definitionServiceBase, runtimeDataService);

        ProcessAssetDesc assetDesc = new ProcessAssetDesc();
        assetDesc.setId(PROCESS_ID);
        assetDesc.setDeploymentId(CONTAINER_ID_ONE);
        jbpmDefinitions.add(assetDesc);

        definition = org.kie.server.api.model.definition.ProcessDefinition
                .builder().id(PROCESS_ID).containerId(CONTAINER_ID_ONE).build();


        queryContext = new QueryContext();
        queryContext.setCount(1);
    }

    @Test
    public void getProcessDefinitions() {
        when(runtimeDataService.getProcesses(anyObject())).thenReturn(jbpmDefinitions);

        List<org.kie.server.api.model.definition.ProcessDefinition> result = definitionRepository.getProcessDefinitions(BATCH_SIZE);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(BATCH_SIZE);
    }

    @Test
    public void getProcessDefinitionsWithContainerId() {
        when(runtimeDataService.getProcessesByDeploymentId(eq(CONTAINER_ID_ONE), anyObject())).thenReturn(jbpmDefinitions);

        List<org.kie.server.api.model.definition.ProcessDefinition> result =
                definitionRepository.getProcessDefinitions(CONTAINER_ID_ONE, BATCH_SIZE);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(BATCH_SIZE);
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("containerId", CONTAINER_ID_ONE);
    }

    @Test
    public void getProcessDefinitionsWithProcessId() {
        when(runtimeDataService.getProcessesById(eq(PROCESS_ID))).thenReturn(jbpmDefinitions);

        List<org.kie.server.api.model.definition.ProcessDefinition> result =
                definitionRepository.getProcessDefinitions(PROCESS_ID);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasSize(BATCH_SIZE);
        Assertions.assertThat(result.get(0)).hasFieldOrPropertyWithValue("id", PROCESS_ID);
    }

    @Test
    public void getProcessDefinitionsWithProcessIdAndContainerId() {
        when(definitionServiceBase.getProcessDefinition(eq(CONTAINER_ID_ONE), eq(PROCESS_ID))).thenReturn(definition);

        org.kie.server.api.model.definition.ProcessDefinition result =
                definitionRepository.getProcessDefinition(PROCESS_ID, CONTAINER_ID_ONE);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).hasFieldOrPropertyWithValue("containerId", CONTAINER_ID_ONE);
        Assertions.assertThat(result).hasFieldOrPropertyWithValue("id", PROCESS_ID);
    }
}
