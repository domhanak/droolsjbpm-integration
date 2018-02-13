/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.spring.tests.persistence;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.drools.compiler.builder.impl.KnowledgeBuilderImpl;
import org.drools.core.util.DroolsStreamUtils;
import org.h2.tools.DeleteDbFiles;
import org.h2.tools.Server;
import org.jbpm.compiler.ProcessBuilderImpl;
import org.jbpm.process.core.Work;
import org.jbpm.process.core.impl.WorkImpl;
import org.jbpm.process.core.timer.Timer;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.jbpm.workflow.core.Node;
import org.jbpm.workflow.core.impl.ConnectionImpl;
import org.jbpm.workflow.core.impl.DroolsConsequenceAction;
import org.jbpm.workflow.core.node.ActionNode;
import org.jbpm.workflow.core.node.EndNode;
import org.jbpm.workflow.core.node.StartNode;
import org.jbpm.workflow.core.node.SubProcessNode;
import org.jbpm.workflow.core.node.TimerNode;
import org.jbpm.workflow.core.node.WorkItemNode;
import org.jbpm.workflow.instance.node.SubProcessNodeInstance;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.definition.KiePackage;
import org.kie.api.persistence.jpa.KieStoreServices;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.spring.beans.persistence.TestWorkItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.*;

public class JPASingleSessionCommandServiceFactoryEnvTest {
    private static String TMPDIR = System.getProperty("java.io.tmpdir");
    private static final Logger log = LoggerFactory.getLogger(JPASingleSessionCommandServiceFactoryEnvTest.class);
    private static Server h2Server;

    private ApplicationContext ctx;

    @BeforeClass
    public static void startH2Database() throws Exception {
        DeleteDbFiles.execute("",
                "DroolsFlow",
                true);
        h2Server = Server.createTcpServer(new String[0]);
        h2Server.start();
        try {
            TMPDIR = JPASingleSessionCommandServiceFactoryEnvTest.class.getResource("/kb_persistence").getFile();
            log.debug("creating: {}",
                    TMPDIR + "/processWorkItems.pkg");
            writePackage(getProcessWorkItems(),
                    new File(TMPDIR + "/processWorkItems.pkg"));

            log.debug("creating: {}",
                    TMPDIR + "/processSubProcess.pkg");
            writePackage(getProcessSubProcess(),
                    new File(TMPDIR + "/processSubProcess.pkg"));

            log.debug("creating: {}",
                    TMPDIR + "/processTimer.pkg");
            writePackage(getProcessTimer(),
                    new File(TMPDIR + "/processTimer.pkg"));

            log.debug("creating: {}",
                    TMPDIR + "/processTimer2.pkg");
            writePackage(getProcessTimer2(),
                    new File(TMPDIR + "/processTimer2.pkg"));
        } catch (Exception e) {
            log.error("can't create packages!",
                    e);
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void stopH2Database() throws Exception {
        log.debug("stopping database");
        h2Server.stop();
        DeleteDbFiles.execute("",
                "DroolsFlow",
                true);
    }

    @Before
    public void createSpringContext() {
        try {
            log.debug("creating spring context");
            ctx = new ClassPathXmlApplicationContext("org/kie/spring/persistence/persistence_beans_env.xml");
        } catch (Exception e) {
            log.error("can't create spring context",
                    e);
            throw new RuntimeException(e);
        }
    }

    @After
    public void destroySpringContext() {
        log.debug("destroy spring context");
    }

    @Test
    public void testPersistenceWorkItems() throws Exception {
        log.debug("---> get bean jpaSingleSessionCommandService");
        KieSession service = (KieSession) ctx.getBean("jpaSingleSessionCommandService2");

        log.debug("---> create new SingleSessionCommandService");

        long sessionId = service.getIdentifier();
        log.debug("---> created SingleSessionCommandService id: " + sessionId);

        ProcessInstance processInstance = service.startProcess("org.drools.test.TestProcess");
        log.debug("Started process instance {}",
                processInstance.getId());

        TestWorkItemHandler handler = TestWorkItemHandler.getInstance();
        WorkItem workItem = handler.getWorkItem();
        assertThat(workItem).isNotNull();
        service.dispose();

        final Environment env = (Environment) ctx.getBean("env");
        /*Environment env = KnowledgeBaseFactory.newEnvironment();
        env.set( EnvironmentName.ENTITY_MANAGER_FACTORY,
                 ctx.getBean( "myEmf" ) );
        env.set( EnvironmentName.TRANSACTION_MANAGER,
                 ctx.getBean( "txManager" ) );
        */
        KieStoreServices kstore = (KieStoreServices) ctx.getBean("kstore1");
        KieBase kbase1 = (KieBase) ctx.getBean("kb_persistence");
        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        processInstance = service.getProcessInstance(processInstance.getId());
        assertThat(processInstance).isNotNull();
        service.dispose();

        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        service.getWorkItemManager().completeWorkItem(workItem.getId(),
                null);

        workItem = handler.getWorkItem();
        assertThat(workItem).isNotNull();
        service.dispose();

        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        processInstance = service.getProcessInstance(processInstance.getId());
        assertThat(processInstance).isNotNull();
        service.dispose();

        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        service.getWorkItemManager().completeWorkItem(workItem.getId(),
                null);

        workItem = handler.getWorkItem();
        assertThat(workItem).isNotNull();
        service.dispose();

        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        processInstance = service.getProcessInstance(processInstance.getId());
        service.dispose();

        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        service.getWorkItemManager().completeWorkItem(workItem.getId(),
                null);

        workItem = handler.getWorkItem();
        assertThat(workItem).isNull();
        service.dispose();

        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        processInstance = service.getProcessInstance(processInstance.getId());
        service.dispose();
    }

    @Test
    public void testPersistenceWorkItemsUserTransaction() throws Exception {

        KieSession service = (KieSession) ctx.getBean("jpaSingleSessionCommandService2");

        long sessionId = service.getIdentifier();
        ProcessInstance processInstance = service.startProcess("org.drools.test.TestProcess");
        log.debug("Started process instance {}",
                processInstance.getId());

        TestWorkItemHandler handler = TestWorkItemHandler.getInstance();
        WorkItem workItem = handler.getWorkItem();
        assertThat(workItem).isNotNull();
        service.dispose();

        final Environment env = (Environment) ctx.getBean("env");

        /*Environment env = KnowledgeBaseFactory.newEnvironment();
        env.set( EnvironmentName.ENTITY_MANAGER_FACTORY,
                 ctx.getBean( "myEmf" ) );
        env.set( EnvironmentName.TRANSACTION_MANAGER,
                 ctx.getBean( "txManager" ) );
        */
        KieStoreServices kstore = (KieStoreServices) ctx.getBean("kstore1");
        KieBase kbase1 = (KieBase) ctx.getBean("kb_persistence");
        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);

        processInstance = service.getProcessInstance(processInstance.getId());
        assertThat(processInstance).isNotNull();
        service.dispose();

        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        service.getWorkItemManager().completeWorkItem(workItem.getId(),
                null);

        workItem = handler.getWorkItem();
        assertThat(workItem).isNotNull();
        service.dispose();

        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        processInstance = service.getProcessInstance(processInstance.getId());
        assertThat(processInstance).isNotNull();
        service.dispose();

        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        service.getWorkItemManager().abortWorkItem(workItem.getId());

        workItem = handler.getWorkItem();
        assertThat(workItem).isNotNull();
        service.dispose();

        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        processInstance = service.getProcessInstance(processInstance.getId());
        assertThat(processInstance).isNotNull();
        service.dispose();

        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        service.getWorkItemManager().completeWorkItem(workItem.getId(),
                null);
        workItem = handler.getWorkItem();
        assertThat(workItem).isNull();
        service.dispose();

        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        processInstance = service.getProcessInstance(processInstance.getId());
        assertThat(processInstance).isNull();
        service.dispose();
    }

    private static KiePackage getProcessWorkItems() {
        RuleFlowProcess process = new RuleFlowProcess();
        process.setId("org.drools.test.TestProcess");
        process.setName("TestProcess");
        process.setPackageName("org.drools.test");
        StartNode start = new StartNode();
        start.setId(1);
        start.setName("Start");
        process.addNode(start);
        ActionNode actionNode = new ActionNode();
        actionNode.setId(2);
        actionNode.setName("Action");
        DroolsConsequenceAction action = new DroolsConsequenceAction();
        action.setDialect("java");
        action.setConsequence("System.out.println(\"Executed action\");");
        actionNode.setAction(action);
        process.addNode(actionNode);
        new ConnectionImpl(start,
                Node.CONNECTION_DEFAULT_TYPE,
                actionNode,
                Node.CONNECTION_DEFAULT_TYPE);
        WorkItemNode workItemNode = new WorkItemNode();
        workItemNode.setId(3);
        workItemNode.setName("WorkItem1");
        Work work = new WorkImpl();
        work.setName("MyWork");
        workItemNode.setWork(work);
        process.addNode(workItemNode);
        new ConnectionImpl(actionNode,
                Node.CONNECTION_DEFAULT_TYPE,
                workItemNode,
                Node.CONNECTION_DEFAULT_TYPE);
        WorkItemNode workItemNode2 = new WorkItemNode();
        workItemNode2.setId(4);
        workItemNode2.setName("WorkItem2");
        work = new WorkImpl();
        work.setName("MyWork");
        workItemNode2.setWork(work);
        process.addNode(workItemNode2);
        new ConnectionImpl(workItemNode,
                Node.CONNECTION_DEFAULT_TYPE,
                workItemNode2,
                Node.CONNECTION_DEFAULT_TYPE);
        WorkItemNode workItemNode3 = new WorkItemNode();
        workItemNode3.setId(5);
        workItemNode3.setName("WorkItem3");
        work = new WorkImpl();
        work.setName("MyWork");
        workItemNode3.setWork(work);
        process.addNode(workItemNode3);
        new ConnectionImpl(workItemNode2,
                Node.CONNECTION_DEFAULT_TYPE,
                workItemNode3,
                Node.CONNECTION_DEFAULT_TYPE);
        EndNode end = new EndNode();
        end.setId(6);
        end.setName("End");
        process.addNode(end);
        new ConnectionImpl(workItemNode3,
                Node.CONNECTION_DEFAULT_TYPE,
                end,
                Node.CONNECTION_DEFAULT_TYPE);

        KnowledgeBuilderImpl packageBuilder = new KnowledgeBuilderImpl();
        ProcessBuilderImpl processBuilder = new ProcessBuilderImpl(packageBuilder);
        processBuilder.buildProcess(process,
                null);

        return packageBuilder.getPackage("org.drools.test");
    }

    public static void writePackage(KiePackage pkg,
                                    File dest) {
        dest.deleteOnExit();
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(dest));
            DroolsStreamUtils.streamOut(out,
                    pkg);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Test
    public void testPersistenceSubProcess() {
        KieSession service = (KieSession) ctx.getBean("jpaSingleSessionCommandService2");

        long sessionId = service.getIdentifier();

        RuleFlowProcessInstance processInstance = (RuleFlowProcessInstance) service.startProcess("org.drools.test.ProcessSubProcess");
        log.debug("Started process instance {}",
                processInstance.getId());
        long processInstanceId = processInstance.getId();

        TestWorkItemHandler handler = TestWorkItemHandler.getInstance();
        WorkItem workItem = handler.getWorkItem();
        assertThat(workItem).isNotNull();
        service.dispose();

        final Environment env = (Environment) ctx.getBean("env");
        /*Environment env = KnowledgeBaseFactory.newEnvironment();
        env.set( EnvironmentName.ENTITY_MANAGER_FACTORY,
                 ctx.getBean( "myEmf" ) );
        env.set( EnvironmentName.TRANSACTION_MANAGER,
                 ctx.getBean( "txManager" ) );
        */
        KieStoreServices kstore = (KieStoreServices) ctx.getBean("kstore1");
        KieBase kbase1 = (KieBase) ctx.getBean("kb_persistence");
        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);

        processInstance = (RuleFlowProcessInstance) service.getProcessInstance(processInstanceId);
        assertThat(processInstance).isNotNull();

        Collection<NodeInstance> nodeInstances = processInstance.getNodeInstances();
        assertEquals(1,
                nodeInstances.size());
        SubProcessNodeInstance subProcessNodeInstance = (SubProcessNodeInstance) nodeInstances.iterator().next();
        long subProcessInstanceId = subProcessNodeInstance.getProcessInstanceId();
        RuleFlowProcessInstance subProcessInstance = (RuleFlowProcessInstance) service.getProcessInstance(subProcessInstanceId);
        assertThat(subProcessInstance).isNotNull();
        service.dispose();

        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        service.getWorkItemManager().completeWorkItem(workItem.getId(),
                null);
        service.dispose();

        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        subProcessInstance = (RuleFlowProcessInstance) service.getProcessInstance(subProcessInstanceId);
        assertThat(subProcessInstance).isNull();

        processInstance = (RuleFlowProcessInstance) service.getProcessInstance(processInstanceId);
        assertThat(processInstance).isNull();
        service.dispose();
    }

    @SuppressWarnings("unused")
    private static KiePackage getProcessSubProcess() {
        RuleFlowProcess process = new RuleFlowProcess();
        process.setId("org.drools.test.ProcessSubProcess");
        process.setName("ProcessSubProcess");
        process.setPackageName("org.drools.test");
        StartNode start = new StartNode();
        start.setId(1);
        start.setName("Start");
        process.addNode(start);
        ActionNode actionNode = new ActionNode();
        actionNode.setId(2);
        actionNode.setName("Action");
        DroolsConsequenceAction action = new DroolsConsequenceAction();
        action.setDialect("java");
        action.setConsequence("System.out.println(\"Executed action\");");
        actionNode.setAction(action);
        process.addNode(actionNode);
        new ConnectionImpl(start,
                Node.CONNECTION_DEFAULT_TYPE,
                actionNode,
                Node.CONNECTION_DEFAULT_TYPE);
        SubProcessNode subProcessNode = new SubProcessNode();
        subProcessNode.setId(3);
        subProcessNode.setName("SubProcess");
        subProcessNode.setProcessId("org.drools.test.SubProcess");
        process.addNode(subProcessNode);
        new ConnectionImpl(actionNode,
                Node.CONNECTION_DEFAULT_TYPE,
                subProcessNode,
                Node.CONNECTION_DEFAULT_TYPE);
        EndNode end = new EndNode();
        end.setId(4);
        end.setName("End");
        process.addNode(end);
        new ConnectionImpl(subProcessNode,
                Node.CONNECTION_DEFAULT_TYPE,
                end,
                Node.CONNECTION_DEFAULT_TYPE);

        KnowledgeBuilderImpl packageBuilder = new KnowledgeBuilderImpl();
        ProcessBuilderImpl processBuilder = new ProcessBuilderImpl(packageBuilder);
        processBuilder.buildProcess(process,
                null);

        process = new RuleFlowProcess();
        process.setId("org.drools.test.SubProcess");
        process.setName("SubProcess");
        process.setPackageName("org.drools.test");
        start = new StartNode();
        start.setId(1);
        start.setName("Start");
        process.addNode(start);
        actionNode = new ActionNode();
        actionNode.setId(2);
        actionNode.setName("Action");
        action = new DroolsConsequenceAction();
        action.setDialect("java");
        action.setConsequence("System.out.println(\"Executed action\");");
        actionNode.setAction(action);
        process.addNode(actionNode);
        new ConnectionImpl(start,
                Node.CONNECTION_DEFAULT_TYPE,
                actionNode,
                Node.CONNECTION_DEFAULT_TYPE);
        WorkItemNode workItemNode = new WorkItemNode();
        workItemNode.setId(3);
        workItemNode.setName("WorkItem1");
        Work work = new WorkImpl();
        work.setName("MyWork");
        workItemNode.setWork(work);
        process.addNode(workItemNode);
        new ConnectionImpl(actionNode,
                Node.CONNECTION_DEFAULT_TYPE,
                workItemNode,
                Node.CONNECTION_DEFAULT_TYPE);
        end = new EndNode();
        end.setId(6);
        end.setName("End");
        process.addNode(end);
        new ConnectionImpl(workItemNode,
                Node.CONNECTION_DEFAULT_TYPE,
                end,
                Node.CONNECTION_DEFAULT_TYPE);

        processBuilder.buildProcess(process,
                null);
        return packageBuilder.getPackage("org.drools.test");
    }

    @Test
    @Ignore("Test causes occational deadlock in service.getProcessInstance(procId)")
    public void testPersistenceTimer() throws Exception {
        log.debug("TMPDIR == " + TMPDIR);
        log.debug("---> get bean jpaSingleSessionCommandService2");
        KieSession service = (KieSession) ctx.getBean("jpaSingleSessionCommandService2");

        long sessionId = service.getIdentifier();
        log.debug("---> created SingleSessionCommandService id: " + sessionId);

        ProcessInstance processInstance = service.startProcess("org.drools.test.ProcessTimer");
        long procId = processInstance.getId();
        log.debug("---> Started ProcessTimer id: {}",
                procId);

        service.dispose();
        log.debug("---> session disposed");

        final Environment env = (Environment) ctx.getBean("env");
        /*
        Environment env = KnowledgeBaseFactory.newEnvironment();
        env.set( EnvironmentName.ENTITY_MANAGER_FACTORY,
                 ctx.getBean( "myEmf" ) );
        env.set( EnvironmentName.TRANSACTION_MANAGER,
                 ctx.getBean( "txManager" ) );
        */
        KieStoreServices kstore = (KieStoreServices) ctx.getBean("kstore1");
        KieBase kbase1 = (KieBase) ctx.getBean("kb_persistence");
        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);

        log.debug("---> load session: " + sessionId);
        processInstance = service.getProcessInstance(procId);
        log.debug("---> GetProcessInstanceCommand id: " + procId);
        assertThat(processInstance).isNotNull();

        waitForTimer();
        // wait for process to complete
        Thread.sleep(500);
        
        log.debug("---> session disposed");
        service.dispose();

        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);
        log.debug("---> load session: " + sessionId);

        log.debug("---> GetProcessInstanceCommand id: " + procId);
        processInstance = service.getProcessInstance(procId);
        log.debug("---> session disposed");
        assertThat(processInstance).isNull();
    }

    @SuppressWarnings("unused")
    private static KiePackage getProcessTimer() {
        RuleFlowProcess process = new RuleFlowProcess();
        process.setId("org.drools.test.ProcessTimer");
        process.setName("ProcessTimer");
        process.setPackageName("org.drools.test");
        StartNode start = new StartNode();
        start.setId(1);
        start.setName("Start");
        process.addNode(start);
        TimerNode timerNode = new TimerNode();
        timerNode.setId(2);
        timerNode.setName("Timer");
        Timer timer = new Timer();
        timer.setDelay("750");
        timerNode.setTimer(timer);
        process.addNode(timerNode);
        new ConnectionImpl(start,
                Node.CONNECTION_DEFAULT_TYPE,
                timerNode,
                Node.CONNECTION_DEFAULT_TYPE);
        ActionNode actionNode = new ActionNode();
        actionNode.setId(3);
        actionNode.setName("Action");
        DroolsConsequenceAction action = new DroolsConsequenceAction();
        action.setDialect("java");
        action.setConsequence("try { org.kie.spring.tests.persistence.JPASingleSessionCommandServiceFactoryEnvTest.waitForTest(); } catch (Throwable t) { t.printStackTrace(); } System.out.println(\"Executed timer action\");");
        actionNode.setAction(action);
        process.addNode(actionNode);
        new ConnectionImpl(timerNode,
                Node.CONNECTION_DEFAULT_TYPE,
                actionNode,
                Node.CONNECTION_DEFAULT_TYPE);
        EndNode end = new EndNode();
        end.setId(6);
        end.setName("End");
        process.addNode(end);
        new ConnectionImpl(actionNode,
                Node.CONNECTION_DEFAULT_TYPE,
                end,
                Node.CONNECTION_DEFAULT_TYPE);

        KnowledgeBuilderImpl packageBuilder = new KnowledgeBuilderImpl();
        ProcessBuilderImpl processBuilder = new ProcessBuilderImpl(packageBuilder);
        processBuilder.buildProcess(process,
                null);
        return packageBuilder.getPackage("org.drools.test");
    }

    @Test
    public void testPersistenceTimer2() throws Exception {
        KieSession service = (KieSession) ctx.getBean("jpaSingleSessionCommandService2");

        long sessionId = service.getIdentifier();

        ProcessInstance processInstance = service.startProcess("org.drools.test.ProcessTimer2");
        log.debug("Started process instance {}",
                processInstance.getId());

        waitForTimer();
        // wait for process to complete
        Thread.sleep(500);

        final Environment env = (Environment) ctx.getBean("env");
        /*
        Environment env = KnowledgeBaseFactory.newEnvironment();
        env.set( EnvironmentName.ENTITY_MANAGER_FACTORY,
                 ctx.getBean( "myEmf" ) );
        env.set( EnvironmentName.TRANSACTION_MANAGER,
                 ctx.getBean( "txManager" ) );
        */
        KieStoreServices kstore = (KieStoreServices) ctx.getBean("kstore1");
        KieBase kbase1 = (KieBase) ctx.getBean("kb_persistence");
        service = kstore.loadKieSession(sessionId,
                kbase1,
                null,
                env);

        processInstance = service.getProcessInstance(processInstance.getId());
        assertThat(processInstance).isNull();
    }

    @SuppressWarnings("unused")
    private static KiePackage getProcessTimer2() {
        RuleFlowProcess process = new RuleFlowProcess();
        process.setId("org.drools.test.ProcessTimer2");
        process.setName("ProcessTimer2");
        process.setPackageName("org.drools.test");
        StartNode start = new StartNode();
        start.setId(1);
        start.setName("Start");
        process.addNode(start);
        TimerNode timerNode = new TimerNode();
        timerNode.setId(2);
        timerNode.setName("Timer");
        Timer timer = new Timer();
        timer.setDelay("0");
        timerNode.setTimer(timer);
        process.addNode(timerNode);
        new ConnectionImpl(start,
                Node.CONNECTION_DEFAULT_TYPE,
                timerNode,
                Node.CONNECTION_DEFAULT_TYPE);
        ActionNode actionNode = new ActionNode();
        actionNode.setId(3);
        actionNode.setName("Action");
        DroolsConsequenceAction action = new DroolsConsequenceAction();
        action.setDialect("java");
        action.setConsequence("try { org.kie.spring.tests.persistence.JPASingleSessionCommandServiceFactoryEnvTest.waitForTest(); } catch (Throwable t) { t.printStackTrace(); } System.out.println(\"Executed timer action\");");
        actionNode.setAction(action);
        process.addNode(actionNode);
        new ConnectionImpl(timerNode,
                Node.CONNECTION_DEFAULT_TYPE,
                actionNode,
                Node.CONNECTION_DEFAULT_TYPE);
        EndNode end = new EndNode();
        end.setId(6);
        end.setName("End");
        process.addNode(end);
        new ConnectionImpl(actionNode,
                Node.CONNECTION_DEFAULT_TYPE,
                end,
                Node.CONNECTION_DEFAULT_TYPE);

        KnowledgeBuilderImpl packageBuilder = new KnowledgeBuilderImpl();
        ProcessBuilderImpl processBuilder = new ProcessBuilderImpl(packageBuilder);
        processBuilder.buildProcess(process,
                null);
        return packageBuilder.getPackage("org.drools.test");
    }
    
    private static CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
    
    public static void waitForTest() { 
        try {
            cyclicBarrier.await();
        } catch( InterruptedException ie ) {
            // do nothing
        } catch( BrokenBarrierException bbe ) {
            throw new IllegalStateException("The barrier is broken!", bbe);
        }
    }

    public static void waitForTimer() { 
        try {
            cyclicBarrier.await();
        } catch( InterruptedException ie ) {
            // do nothing
        } catch( BrokenBarrierException bbe ) {
            throw new IllegalStateException("The barrier is broken!", bbe);
        }
    }
}
