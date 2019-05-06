package org.kie.server.remote.graphql.jbpm;

import org.assertj.core.api.Assertions;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.services.jbpm.DefinitionServiceBase;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JbpmGraphQLResourceTest {

    private JbpmGraphQLResource resource;

    @Mock
    private JbpmGraphQLServiceProvider serviceProvider;

    @Mock
    private DefinitionServiceBase definitionServiceBase;

    @Mock
    private ProcessService processService;

    @Mock
    private RuntimeDataService runtimeDataService;

    @Before
    public void setUp() throws Exception {
        when(serviceProvider.getDefinitionServiceBase()).thenReturn(definitionServiceBase);
        when(serviceProvider.getProcessService()).thenReturn(processService);
        when(serviceProvider.getRuntimeDataService()).thenReturn(runtimeDataService);
    }

    @Test
    public void testGraphQLResourceCanInitialize() {
        try {
            resource = new JbpmGraphQLResource(serviceProvider);
        } catch (Exception ex) {
            Assertions.fail("JbpmGraphQLResource thrown an exception while initializing itself. Error: {}", ex);
        }
    }
}
