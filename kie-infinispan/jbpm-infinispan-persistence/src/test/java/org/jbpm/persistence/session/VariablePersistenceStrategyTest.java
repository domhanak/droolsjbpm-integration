/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.jbpm.persistence.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import junit.framework.Assert;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.impl.KnowledgeBaseFactory;
import org.drools.core.impl.KnowledgeBaseImpl;
import org.drools.core.io.impl.ClassPathResource;
import org.drools.core.marshalling.impl.ClassObjectMarshallingStrategyAcceptor;
import org.drools.core.marshalling.impl.SerializablePlaceholderResolverStrategy;
import org.drools.persistence.infinispan.marshaller.InfinispanPlaceholderResolverStrategy;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.jbpm.persistence.session.objects.MyEntity;
import org.jbpm.persistence.session.objects.MyEntityMethods;
import org.jbpm.persistence.session.objects.MyEntityOnlyFields;
import org.jbpm.persistence.session.objects.MySubEntity;
import org.jbpm.persistence.session.objects.MySubEntityMethods;
import org.jbpm.persistence.session.objects.MyVariableExtendingSerializable;
import org.jbpm.persistence.session.objects.MyVariableSerializable;
import org.jbpm.persistence.session.objects.TestWorkItemHandler;
import org.jbpm.persistence.util.PersistenceUtil;
import org.jbpm.process.core.Work;
import org.jbpm.process.core.context.variable.Variable;
import org.jbpm.process.core.datatype.impl.type.ObjectDataType;
import org.jbpm.process.core.impl.WorkImpl;
import org.jbpm.process.instance.impl.Action;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.workflow.core.DroolsAction;
import org.jbpm.workflow.core.Node;
import org.jbpm.workflow.core.impl.ConnectionImpl;
import org.jbpm.workflow.core.impl.DroolsConsequenceAction;
import org.jbpm.workflow.core.node.ActionNode;
import org.jbpm.workflow.core.node.EndNode;
import org.jbpm.workflow.core.node.StartNode;
import org.jbpm.workflow.core.node.WorkItemNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.process.ProcessContext;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkflowProcessInstance;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderConfiguration;
import org.kie.internal.builder.KnowledgeBuilderError;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.persistence.infinispan.InfinispanKnowledgeService;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jbpm.persistence.util.PersistenceUtil.*;
import static org.assertj.core.api.Assertions.*;

public class VariablePersistenceStrategyTest {

    private static Logger logger = LoggerFactory.getLogger( VariablePersistenceStrategyTest.class );
    
    private HashMap<String, Object> context;
    private DefaultCacheManager cm;

    @Before
    public void setUp() throws Exception {
        context = setupWithPoolingDataSource(JBPM_PERSISTENCE_UNIT_NAME);
        cm = (DefaultCacheManager) context.get(EnvironmentName.ENTITY_MANAGER_FACTORY);
    }

    @After
    public void tearDown() throws Exception {
        cleanUp(context);
    }

    @Test
    public void testExtendingInterfaceVariablePersistence() throws Exception {
        // Setup
        Environment env = createEnvironment();
        String processId = "extendingInterfaceVariablePersistence";
        String variableText = "my extending serializable variable text";
        KieBase kbase = getKnowledgeBaseForExtendingInterfaceVariablePersistence(processId,
                                                                                       variableText);
        StatefulKnowledgeSession ksession = createSession( kbase , env );
        Map<String, Object> initialParams = new HashMap<String, Object>();
        initialParams.put( "x", new MyVariableExtendingSerializable( variableText ) );
        
        // Start process and execute workItem
        long processInstanceId = ksession.startProcess( processId, initialParams ).getId();
        
        ksession = reloadSession( ksession, kbase, env );
        
        long workItemId = TestWorkItemHandler.getInstance().getWorkItem().getId();
        ksession.getWorkItemManager().completeWorkItem( workItemId, null );
        
        // Test
        Assert.assertThat(ksession.getProcessInstance( processInstanceId ) ).isNull();
    }

    private KieBase getKnowledgeBaseForExtendingInterfaceVariablePersistence(String processId, final String variableText) {
        RuleFlowProcess process = new RuleFlowProcess();
        process.setId( processId );
        
        List<Variable> variables = new ArrayList<Variable>();
        Variable variable = new Variable();
        variable.setName("x");
        ObjectDataType extendingSerializableDataType = new ObjectDataType();
        extendingSerializableDataType.setClassName(MyVariableExtendingSerializable.class.getName());
        variable.setType(extendingSerializableDataType);
        variables.add(variable);
        process.getVariableScope().setVariables(variables);

        StartNode startNode = new StartNode();
        startNode.setName( "Start" );
        startNode.setId(1);

        WorkItemNode workItemNode = new WorkItemNode();
        workItemNode.setName( "workItemNode" );
        workItemNode.setId( 2 );
        Work work = new WorkImpl();
        work.setName( "MyWork" );
        workItemNode.setWork( work );
        
        ActionNode actionNode = new ActionNode();
        actionNode.setName( "Print" );
        DroolsAction action = new DroolsConsequenceAction( "java" , null);
        action.setMetaData( "Action" , new Action() {
            public void execute(ProcessContext context) throws Exception {
                Assert.assertThat(((MyVariableExtendingSerializable) context.getVariable( "x" )).getText()).isEqualTo(variableText ); ;
            }
        });
        actionNode.setAction(action);
        actionNode.setId( 3 );
        
        EndNode endNode = new EndNode();
        endNode.setName("EndNode");
        endNode.setId(4);
        
        connect( startNode, workItemNode );
        connect( workItemNode, actionNode );
        connect( actionNode, endNode );

        process.addNode( startNode );
        process.addNode( workItemNode );
        process.addNode( actionNode );
        process.addNode( endNode );
        
        KieBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        ((KnowledgeBaseImpl) kbase).addProcess(process);
        return kbase;
    }
    
    @Test
    public void testPersistenceVariables() throws NamingException, NotSupportedException, SystemException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        Cache<String, Object> cache = cm.getCache("jbpm-configured-cache");
        UserTransaction utx = (UserTransaction) cache.getAdvancedCache().getTransactionManager();
        if( utx.getStatus() == Status.STATUS_NO_TRANSACTION ) { 
            utx.begin();
        }
        
        int origNumMyEntities = getEntitiesFromCache(cache, "myEntity", MyEntity.class).size();
        int origNumMyEntityMethods = getEntitiesFromCache(cache, "myEntityMethods", MyEntityMethods.class).size();
        int origNumMyEntityOnlyFields = getEntitiesFromCache(cache, "myEntityOnlyFields", MyEntityOnlyFields.class).size();
        if( utx.getStatus() == Status.STATUS_ACTIVE ) { 
            utx.commit();
        }
       
        // Setup entities
        MyEntity myEntity = new MyEntity("This is a test Entity with annotation in fields");
        MyEntityMethods myEntityMethods = new MyEntityMethods("This is a test Entity with annotations in methods");
        MyEntityOnlyFields myEntityOnlyFields = new MyEntityOnlyFields("This is a test Entity with annotations in fields and without accesors methods");
        MyVariableSerializable myVariableSerializable = new MyVariableSerializable("This is a test SerializableObject");

        // persist entities
        utx = (UserTransaction) cache.getAdvancedCache().getTransactionManager();
        utx.begin();
        cache.put(generateId(cache, myEntity), myEntity);
        cache.put(generateId(cache, myEntityMethods), myEntityMethods);
        cache.put(generateId(cache, myEntityOnlyFields), myEntityOnlyFields);
        utx.commit();
        
        // More setup
        Environment env =  createEnvironment();
        KieBase kbase = createKnowledgeBase( "VariablePersistenceStrategyProcess.rf" );
        StatefulKnowledgeSession ksession = createSession( kbase, env );

        logger.debug("### Starting process ###");
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("x", "SomeString");
        parameters.put("y", myEntity);
        parameters.put("m", myEntityMethods);
        parameters.put("f", myEntityOnlyFields);
        parameters.put("z", myVariableSerializable);
        
        // Start process
        long processInstanceId = ksession.startProcess( "com.sample.ruleflow", parameters ).getId();

        TestWorkItemHandler handler = TestWorkItemHandler.getInstance();
        WorkItem workItem = handler.getWorkItem();
        assertThat(workItem ).isNotNull();
        // Test results
        List<?> result = getEntitiesFromCache(cache, "myEntity", MyEntity.class);
        assertThat(result.size()).isEqualTo(origNumMyEntities + 1);
        result = getEntitiesFromCache(cache, "myEntityMethods", MyEntityMethods.class);
        assertThat(result.size()).isEqualTo(origNumMyEntityMethods + 1);
        result = getEntitiesFromCache(cache, "myEntityOnlyFields", MyEntityOnlyFields.class);
        assertThat(result.size()).isEqualTo(origNumMyEntityOnlyFields + 1);

        logger.debug("### Retrieving process instance ###");
        ksession = reloadSession( ksession, kbase, env );
        WorkflowProcessInstance processInstance = (WorkflowProcessInstance)
        	ksession.getProcessInstance( processInstanceId );
        assertThat(processInstance ).isNotNull();
        assertThat(processInstance.getVariable("x")).isEqualTo("SomeString");
        assertThat(((MyEntity) processInstance.getVariable("y")).getTest()).isEqualTo("This is a test Entity with annotation in fields");
        assertThat(((MyEntityMethods) processInstance.getVariable("m")).getTest()).isEqualTo("This is a test Entity with annotations in methods");
        assertThat(((MyEntityOnlyFields) processInstance.getVariable("f")).test).isEqualTo("This is a test Entity with annotations in fields and without accesors methods");
        assertThat(((MyVariableSerializable) processInstance.getVariable("z")).getText()).isEqualTo("This is a test SerializableObject");
        assertThat(processInstance.getVariable("a")).isNull();
        assertThat(processInstance.getVariable("b")).isNull();
        assertThat(processInstance.getVariable("c")).isNull();
        logger.debug("### Completing first work item ###");
        ksession.getWorkItemManager().completeWorkItem( workItem.getId(), null );

        workItem = handler.getWorkItem();
        assertThat(workItem ).isNotNull();
        
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), null);
        
        logger.debug("### Retrieving process instance ###");
        ksession = reloadSession( ksession, kbase, env);
        processInstance = (WorkflowProcessInstance)
        	ksession.getProcessInstance(processInstanceId);
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getVariable("x")).isEqualTo("SomeString");
        assertThat(((MyEntity) processInstance.getVariable("y")).getTest()).isEqualTo("This is a test Entity with annotation in fields");
        assertThat(((MyEntityMethods) processInstance.getVariable("m")).getTest()).isEqualTo("This is a test Entity with annotations in methods");
        assertThat(((MyEntityOnlyFields) processInstance.getVariable("f")).test).isEqualTo("This is a test Entity with annotations in fields and without accesors methods");
        assertThat(((MyVariableSerializable) processInstance.getVariable("z")).getText()).isEqualTo("This is a test SerializableObject");
        assertThat(processInstance.getVariable("a")).isEqualTo("Some changed String");
        assertThat(((MyEntity) processInstance.getVariable("b")).getTest()).isEqualTo("This is a changed test Entity");
        assertThat(((MyVariableSerializable) processInstance.getVariable("c")).getText()).isEqualTo("This is a changed test SerializableObject");
        logger.debug("### Completing third work item ###");
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), null);

        workItem = handler.getWorkItem();
        assertThat(workItem).isNotNull();
        
        logger.debug("### Retrieving process instance ###");
        ksession = reloadSession( ksession, kbase, env);
        processInstance = (WorkflowProcessInstance)
        	ksession.getProcessInstance(processInstanceId);
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getVariable("x")).isEqualTo("SomeString");
        assertThat(((MyEntity) processInstance.getVariable("y")).getTest()).isEqualTo("This is a test Entity with annotation in fields");
        assertThat(((MyEntityMethods) processInstance.getVariable("m")).getTest()).isEqualTo("This is a test Entity with annotations in methods");
        assertThat(((MyEntityOnlyFields) processInstance.getVariable("f")).test).isEqualTo("This is a test Entity with annotations in fields and without accesors methods");
        assertThat(((MyVariableSerializable) processInstance.getVariable("z")).getText()).isEqualTo("This is a test SerializableObject");
        assertThat(processInstance.getVariable("a")).isEqualTo("Some changed String");
        assertThat(((MyEntity) processInstance.getVariable("b")).getTest()).isEqualTo("This is a changed test Entity");
        assertThat(((MyVariableSerializable) processInstance.getVariable("c")).getText()).isEqualTo("This is a changed test SerializableObject");
        logger.debug("### Completing third work item ###");
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), null);
        
        workItem = handler.getWorkItem();
        assertThat(workItem).isNull();
        
        ksession = reloadSession( ksession, kbase, env );
        processInstance = (WorkflowProcessInstance)
			ksession.getProcessInstance(processInstanceId);
        assertThat(processInstance).isNull();
    }
    
    private List<?> getEntitiesFromCache(Cache<String, Object> cache, String keyPrefix, Class<?> clazz) {
    	List<Object> retval = new ArrayList<Object>();
    	for (Map.Entry<String, Object> entry : cache.entrySet()) {
    		if (entry.getKey().startsWith(keyPrefix) && clazz.isInstance(entry.getValue())) {
    			retval.add(cache.get(entry.getValue()));
    		}
    	}
		return retval;
	}

	private static long MYENTITYMETHODS_KEY = 1;
    private static long MYENTITYONLYFIELDS_KEY = 1;
    private static long MYENTITY_KEY = 1;
    private static final Object syncObject = new Object();
    
    private String generateId(Cache<String, Object> cache, MyEntityOnlyFields myEntityOnlyFields) {
    	synchronized (syncObject) {
    		while (cache.containsKey("myEntityOnlyFields" + MYENTITYONLYFIELDS_KEY)) {
    			MYENTITYONLYFIELDS_KEY++;
    		}
    		myEntityOnlyFields.id = MYENTITYONLYFIELDS_KEY;
		}
		return "myEntityOnlyFields" + MYENTITYONLYFIELDS_KEY;
    }
    
    private String generateId(Cache<String, Object> cache, MyEntityMethods myEntityMethods) {
    	synchronized (syncObject) {
    		while (cache.containsKey("myEntityMethods" + MYENTITYMETHODS_KEY)) {
    			MYENTITYMETHODS_KEY++;
    		}
    		myEntityMethods.setId(MYENTITYMETHODS_KEY);
		}
		return "myEntityMethods" + MYENTITYMETHODS_KEY;
	}

	private String generateId(Cache<String, Object> cache, MyEntity myEntity) {
    	synchronized (syncObject) {
    		while (cache.containsKey("myEntity" + MYENTITY_KEY)) {
    			MYENTITY_KEY++;
    		}
    		myEntity.setId(MYENTITY_KEY);
		}
		return "myEntity" + MYENTITY_KEY;
	}

	@Test
    public void testPersistenceVariablesWithTypeChange() throws NamingException, NotSupportedException, SystemException, RollbackException, HeuristicMixedException, HeuristicRollbackException {

        MyEntity myEntity = new MyEntity("This is a test Entity with annotation in fields");
        MyEntityMethods myEntityMethods = new MyEntityMethods("This is a test Entity with annotations in methods");
        MyEntityOnlyFields myEntityOnlyFields = new MyEntityOnlyFields("This is a test Entity with annotations in fields and without accesors methods");
        MyVariableSerializable myVariableSerializable = new MyVariableSerializable("This is a test SerializableObject");

        Cache<String, Object> cache = cm.getCache("jbpm-configured-cache");
        UserTransaction utx = (UserTransaction) cache.getAdvancedCache().getTransactionManager();
        int s = utx.getStatus();
        if( utx.getStatus() == Status.STATUS_NO_TRANSACTION ) { 
            utx.begin();
        }
        cache.put(generateId(cache, myEntity), myEntity);
        cache.put(generateId(cache, myEntityMethods), myEntityMethods);
        cache.put(generateId(cache, myEntityOnlyFields), myEntityOnlyFields);
        if( utx.getStatus() == Status.STATUS_ACTIVE ) { 
            utx.commit();
        }
        Environment env = createEnvironment();
        KieBase kbase = createKnowledgeBase( "VariablePersistenceStrategyProcessTypeChange.rf" );
        StatefulKnowledgeSession ksession = createSession( kbase, env );
        
        
        
        
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("x", "SomeString");
        parameters.put("y", myEntity);
        parameters.put("m", myEntityMethods );
        parameters.put("f", myEntityOnlyFields);
        parameters.put("z", myVariableSerializable);
        long processInstanceId = ksession.startProcess( "com.sample.ruleflow", parameters ).getId();

        TestWorkItemHandler handler = TestWorkItemHandler.getInstance();
        WorkItem workItem = handler.getWorkItem();
        assertThat(workItem ).isNotNull();

        ksession = reloadSession( ksession, kbase, env );
        ProcessInstance processInstance = ksession.getProcessInstance( processInstanceId );
        assertThat(processInstance ).isNotNull();
        ksession.getWorkItemManager().completeWorkItem( workItem.getId(), null );

        workItem = handler.getWorkItem();
        assertThat(workItem ).isNotNull();

        ksession = reloadSession( ksession, kbase, env );
        processInstance = ksession.getProcessInstance( processInstanceId );
        assertThat(processInstance ).isNotNull();
        ksession.getWorkItemManager().completeWorkItem( workItem.getId(), null );

        workItem = handler.getWorkItem();
        assertThat(workItem ).isNull();

        ksession = reloadSession( ksession, kbase, env );
        processInstance = ksession.getProcessInstance( processInstanceId );
        assertThat(processInstance ).isNull();
    }
    
    @Test
    public void testPersistenceVariablesSubProcess() throws NamingException, NotSupportedException, SystemException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        
        MyEntity myEntity = new MyEntity("This is a test Entity with annotation in fields");
        MyEntityMethods myEntityMethods = new MyEntityMethods("This is a test Entity with annotations in methods");
        MyEntityOnlyFields myEntityOnlyFields = new MyEntityOnlyFields("This is a test Entity with annotations in fields and without accesors methods");
        MyVariableSerializable myVariableSerializable = new MyVariableSerializable("This is a test SerializableObject");
        Cache<String, Object> cache = cm.getCache("jbpm-configured-cache");
        UserTransaction utx = (UserTransaction) cache.getAdvancedCache().getTransactionManager();
        utx.begin();
        cache.put(generateId(cache, myEntity), myEntity);
        cache.put(generateId(cache, myEntityMethods), myEntityMethods);
        cache.put(generateId(cache, myEntityOnlyFields), myEntityOnlyFields);
        utx.commit();
        Environment env = createEnvironment();
        KieBase kbase = createKnowledgeBase( "VariablePersistenceStrategySubProcess.rf" );
        StatefulKnowledgeSession ksession = createSession( kbase, env );
       
        
        
        
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("x", "SomeString");
        parameters.put("y", myEntity);
        parameters.put("m", myEntityMethods);
        parameters.put("f", myEntityOnlyFields);
        parameters.put("z", myVariableSerializable);
        long processInstanceId = ksession.startProcess( "com.sample.ruleflow", parameters ).getId();

        TestWorkItemHandler handler = TestWorkItemHandler.getInstance();
        WorkItem workItem = handler.getWorkItem();
        assertThat(workItem ).isNotNull();

        ksession = reloadSession( ksession, kbase, env );
        ProcessInstance processInstance = ksession.getProcessInstance( processInstanceId );
        assertThat(processInstance ).isNotNull();
        ksession.getWorkItemManager().completeWorkItem( workItem.getId(), null );

        workItem = handler.getWorkItem();
        assertThat(workItem ).isNotNull();

        ksession = reloadSession( ksession, kbase, env );
        processInstance = ksession.getProcessInstance( processInstanceId );
        assertThat(processInstance ).isNotNull();
        ksession.getWorkItemManager().completeWorkItem( workItem.getId(), null );

        workItem = handler.getWorkItem();
        assertThat(workItem ).isNotNull();

        ksession = reloadSession( ksession, kbase, env );
        processInstance = ksession.getProcessInstance( processInstanceId );
        assertThat(processInstance ).isNotNull();
        ksession.getWorkItemManager().completeWorkItem( workItem.getId(), null );

        workItem = handler.getWorkItem();
        assertThat(workItem ).isNull();

        ksession = reloadSession( ksession, kbase, env );
        processInstance = ksession.getProcessInstance( processInstanceId );
        assertThat(processInstance ).isNull();
    }
    
    @Test
    public void testWorkItemWithVariablePersistence() throws Exception{
        MyEntity myEntity = new MyEntity("This is a test Entity");
        MyVariableSerializable myVariableSerializable = new MyVariableSerializable("This is a test SerializableObject");
        Cache<String, Object> cache = cm.getCache("jbpm-configured-cache");
        UserTransaction utx = (UserTransaction) cache.getAdvancedCache().getTransactionManager();
        utx.begin();
        
        cache.put(generateId(cache, myEntity), myEntity);
        utx.commit();
        Environment env = createEnvironment();
        KieBase kbase = createKnowledgeBase( "VPSProcessWithWorkItems.rf" );
        StatefulKnowledgeSession ksession = createSession( kbase , env);
        
        
       
       
        
        logger.debug("### Starting process ###");
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("x", "SomeString");
        parameters.put("y", myEntity);
        parameters.put("z", myVariableSerializable);
        long processInstanceId = ksession.startProcess( "com.sample.ruleflow", parameters ).getId();

        TestWorkItemHandler handler = TestWorkItemHandler.getInstance();
        WorkItem workItem = handler.getWorkItem();
        assertThat(workItem ).isNotNull();

        logger.debug("### Retrieving process instance ###");
        ksession = reloadSession( ksession, kbase , env);
        WorkflowProcessInstance processInstance = (WorkflowProcessInstance)
        	ksession.getProcessInstance( processInstanceId );
        assertThat(processInstance ).isNotNull();
        assertThat(processInstance.getVariable("x")).isEqualTo("SomeString");
        assertThat(((MyEntity) processInstance.getVariable("y")).getTest()).isEqualTo("This is a test Entity");
        assertThat(((MyVariableSerializable) processInstance.getVariable("z")).getText()).isEqualTo("This is a test SerializableObject");
        assertThat(processInstance.getVariable("a")).isNull();
        assertThat(processInstance.getVariable("b")).isNull();
        assertThat(processInstance.getVariable("c")).isNull();

        logger.debug("### Completing first work item ###");
        Map<String, Object> results = new HashMap<String, Object>();
        results.put("zeta", processInstance.getVariable("z"));
        results.put("equis", processInstance.getVariable("x")+"->modifiedResult");

        ksession.getWorkItemManager().completeWorkItem( workItem.getId(),  results );

        workItem = handler.getWorkItem();
        assertThat(workItem ).isNotNull();

        logger.debug("### Retrieving process instance ###");
        ksession = reloadSession( ksession, kbase, env );
		processInstance = (WorkflowProcessInstance)
			ksession.getProcessInstance(processInstanceId);
		assertThat(processInstance).isNotNull();
        logger.debug("######## Getting the already Persisted Variables #########");
        assertThat(processInstance.getVariable("x")).isEqualTo("SomeString->modifiedResult");
        assertThat(((MyEntity) processInstance.getVariable("y")).getTest()).isEqualTo("This is a test Entity");
        assertThat(((MyVariableSerializable) processInstance.getVariable("z")).getText()).isEqualTo("This is a test SerializableObject");
        assertThat(processInstance.getVariable("a")).isEqualTo("Some new String");
        assertThat(((MyEntity) processInstance.getVariable("b")).getTest()).isEqualTo("This is a new test Entity");
        assertThat(((MyVariableSerializable) processInstance.getVariable("c")).getText()).isEqualTo("This is a new test SerializableObject");
        logger.debug("### Completing second work item ###");
        results = new HashMap<String, Object>();
        results.put("zeta", processInstance.getVariable("z"));
        results.put("equis", processInstance.getVariable("x"));
        ksession.getWorkItemManager().completeWorkItem( workItem.getId(),  results );


        workItem = handler.getWorkItem();
        assertThat(workItem).isNotNull();

        logger.debug("### Retrieving process instance ###");
        ksession = reloadSession( ksession, kbase, env );
        processInstance = (WorkflowProcessInstance)
        	ksession.getProcessInstance(processInstanceId);
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getVariable("x")).isEqualTo("SomeString->modifiedResult");
        assertThat(((MyEntity) processInstance.getVariable("y")).getTest()).isEqualTo("This is a test Entity");
        assertThat(((MyVariableSerializable) processInstance.getVariable("z")).getText()).isEqualTo("This is a test SerializableObject");
        assertThat(processInstance.getVariable("a")).isEqualTo("Some changed String");
        assertThat(((MyEntity) processInstance.getVariable("b")).getTest()).isEqualTo("This is a changed test Entity");
        assertThat(((MyVariableSerializable) processInstance.getVariable("c")).getText()).isEqualTo("This is a changed test SerializableObject");
        logger.debug("### Completing third work item ###");
        results = new HashMap<String, Object>();
        results.put("zeta", processInstance.getVariable("z"));
        results.put("equis", processInstance.getVariable("x"));
        ksession.getWorkItemManager().completeWorkItem( workItem.getId(),  results );

        workItem = handler.getWorkItem();
        assertThat(workItem).isNull();


        ksession = reloadSession( ksession, kbase, env );
        processInstance = (WorkflowProcessInstance)
			ksession.getProcessInstance(processInstanceId);
        assertThat(processInstance).isNull();
    }

    @Test
    public void testEntityWithSuperClassAnnotationField() throws Exception {
    	MySubEntity subEntity = new MySubEntity();
    	subEntity.setId(3L);
    	assertThat(InfinispanPlaceholderResolverStrategy.getClassIdValue(subEntity)).isEqualTo(3L);
    }
    
    @Test
    public void testEntityWithSuperClassAnnotationMethod() throws Exception {
    	MySubEntityMethods subEntity = new MySubEntityMethods();
    	subEntity.setId(3L);
    	assertThat(InfinispanPlaceholderResolverStrategy.getClassIdValue(subEntity)).isEqualTo(3L);
    }
    
    @Test
    public void testAbortWorkItemWithVariablePersistence() throws Exception{
        MyEntity myEntity = new MyEntity("This is a test Entity");
        MyVariableSerializable myVariableSerializable = new MyVariableSerializable("This is a test SerializableObject");
        Cache<String, Object> cache = cm.getCache("jbpm-configured-cache");
        UserTransaction utx = (UserTransaction) cache.getAdvancedCache().getTransactionManager();
        utx.begin();
        
        cache.put(generateId(cache, myEntity), myEntity);
        utx.commit();
        Environment env = createEnvironment();
        KieBase kbase = createKnowledgeBase( "VPSProcessWithWorkItems.rf" );
        StatefulKnowledgeSession ksession = createSession( kbase , env);
        
        logger.debug("### Starting process ###");
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("x", "SomeString");
        parameters.put("y", myEntity);
        parameters.put("z", myVariableSerializable);
        long processInstanceId = ksession.startProcess( "com.sample.ruleflow", parameters ).getId();
    
        TestWorkItemHandler handler = TestWorkItemHandler.getInstance();
        WorkItem workItem = handler.getWorkItem();
        assertThat(workItem ).isNotNull();
    
        logger.debug("### Retrieving process instance ###");
        ksession = reloadSession( ksession, kbase , env);
        WorkflowProcessInstance processInstance = (WorkflowProcessInstance)
               ksession.getProcessInstance( processInstanceId );
        assertThat(processInstance ).isNotNull();
        assertThat(processInstance.getVariable("x")).isEqualTo("SomeString");
        assertThat(((MyEntity) processInstance.getVariable("y")).getTest()).isEqualTo("This is a test Entity");
        assertThat(((MyVariableSerializable) processInstance.getVariable("z")).getText()).isEqualTo("This is a test SerializableObject");
        assertThat(processInstance.getVariable("a")).isNull();
        assertThat(processInstance.getVariable("b")).isNull();
        assertThat(processInstance.getVariable("c")).isNull();
    
        logger.debug("### Completing first work item ###");
        Map<String, Object> results = new HashMap<String, Object>();
        results.put("zeta", processInstance.getVariable("z"));
        results.put("equis", processInstance.getVariable("x")+"->modifiedResult");
    
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), results);
        
        workItem = handler.getWorkItem();
        assertThat(workItem ).isNotNull();
        
        // we simulate a failure here, aborting the work item
        ksession.getWorkItemManager().abortWorkItem( workItem.getId() );
    
        workItem = handler.getWorkItem();
        assertThat(workItem ).isNotNull();
    
        logger.debug("### Retrieving process instance ###");
        ksession = reloadSession( ksession, kbase, env );
               processInstance = (WorkflowProcessInstance)
                       ksession.getProcessInstance(processInstanceId);
               assertThat(processInstance).isNotNull();
        logger.debug("######## Getting the already Persisted Variables #########");
        // we expect the variables to be unmodifed
        assertThat(processInstance.getVariable("x")).isEqualTo("SomeString->modifiedResult");
        assertThat(((MyEntity) processInstance.getVariable("y")).getTest()).isEqualTo("This is a test Entity");
        assertThat(((MyVariableSerializable) processInstance.getVariable("z")).getText()).isEqualTo("This is a test SerializableObject");
        assertThat(processInstance.getVariable("a")).isEqualTo("Some changed String");
        assertThat(((MyEntity) processInstance.getVariable("b")).getTest()).isEqualTo("This is a changed test Entity");
        assertThat(((MyVariableSerializable) processInstance.getVariable("c")).getText()).isEqualTo("This is a changed test SerializableObject");
    }    
    
    private StatefulKnowledgeSession createSession(KieBase kbase, Environment env){
        return InfinispanKnowledgeService.newStatefulKnowledgeSession( kbase, null, env );
    }
    
    private StatefulKnowledgeSession reloadSession(StatefulKnowledgeSession ksession, KieBase kbase, Environment env){
        long sessionId = ksession.getIdentifier();
        ksession.dispose();
        return InfinispanKnowledgeService.loadStatefulKnowledgeSession( sessionId, kbase, null, env);
    }

    private KieBase createKnowledgeBase(String flowFile) {
        KnowledgeBuilderConfiguration conf = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration();
        conf.setProperty("drools.dialect.java.compiler", "JANINO");
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(conf);
        kbuilder.add( new ClassPathResource( flowFile ), ResourceType.DRF );
        if(kbuilder.hasErrors()){
            StringBuilder errorMessage = new StringBuilder();
            for (KnowledgeBuilderError error: kbuilder.getErrors()) {
                errorMessage.append( error.getMessage() );
                errorMessage.append( System.getProperty( "line.separator" ) );
            }
            fail( errorMessage.toString());
        }
        
        InternalKnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addPackages( kbuilder.getKnowledgePackages() );
        return kbase;
    }

    private Environment createEnvironment() {
        Environment env = PersistenceUtil.createEnvironment(context);
        env.set(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES, new ObjectMarshallingStrategy[]{
                                    new InfinispanPlaceholderResolverStrategy(env),
                                    new SerializablePlaceholderResolverStrategy( ClassObjectMarshallingStrategyAcceptor.DEFAULT  )
                                     });
        return env;
    }
    
    private void connect(Node sourceNode,
                         Node targetNode) {
        new ConnectionImpl (sourceNode, Node.CONNECTION_DEFAULT_TYPE,
                            targetNode, Node.CONNECTION_DEFAULT_TYPE);
    }

}
