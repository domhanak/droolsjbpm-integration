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

package org.jbpm.simulation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.drools.core.command.runtime.rule.InsertElementsCommand;
import org.jbpm.simulation.impl.WorkingMemorySimulationRepository;
import org.jbpm.simulation.impl.events.ActivitySimulationEvent;
import org.jbpm.simulation.impl.events.AggregatedEndEventSimulationEvent;
import org.jbpm.simulation.impl.events.AggregatedProcessSimulationEvent;
import org.jbpm.simulation.impl.events.EndSimulationEvent;
import org.jbpm.simulation.impl.events.GenericSimulationEvent;
import org.jbpm.simulation.impl.events.HTAggregatedSimulationEvent;
import org.jbpm.simulation.impl.events.HumanTaskActivitySimulationEvent;
import org.jbpm.simulation.impl.events.ProcessInstanceEndSimulationEvent;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class SimulateProcessTest {

    @Before
    public void configure() {
        // enable logging
        //System.setProperty("jbpm.simulation.log.enabled", "true");
    }
    
    @Test
    public void testSimulationRunner() throws IOException {
        
        InputStreamReader in = new InputStreamReader(this.getClass().getResourceAsStream("/BPMN2-TwoUserTasks.bpmn2"));
        
        String out = new String();
        BufferedReader br = new BufferedReader(in);
        for(String line = br.readLine(); line != null; line = br.readLine()) 
          out += line;


        
        SimulationRepository repo = SimulationRunner.runSimulation("BPMN2-TwoUserTasks", out, 10, 2000, "default.simulation.rules.drl");
        assertThat(repo).isNotNull();
        
        WorkingMemorySimulationRepository wmRepo = (WorkingMemorySimulationRepository) repo;
        wmRepo.fireAllRules();
        
        assertThat(wmRepo.getAggregatedEvents()).hasSize(4);
        assertThat(wmRepo.getEvents()).hasSize(50);
        
        AggregatedSimulationEvent event = wmRepo.getAggregatedEvents().get(0);
        if (event instanceof AggregatedEndEventSimulationEvent) {
            assertThat(event.getProperty("minProcessDuration")).isNotNull();
            assertThat(event.getProperty("activityId").equals("")).isFalse();
        } 
        
        event = wmRepo.getAggregatedEvents().get(1);
        assertThat(event.getProperty("activityId").equals("")).isFalse();
        assertThat(event.getProperty("minExecutionTime")).isNotNull();
        event = wmRepo.getAggregatedEvents().get(2);
        assertThat(event.getProperty("activityId").equals("")).isFalse();
        assertThat(event.getProperty("minExecutionTime")).isNotNull();
        
        event = wmRepo.getAggregatedEvents().get(3);
        assertThat(event.getProperty("minExecutionTime")).isNotNull();
        wmRepo.close();
        
    }
    
    @Test
    public void testSimulationRunnerWithGateway() throws IOException {
        
        InputStreamReader in = new InputStreamReader(this.getClass().getResourceAsStream("/BPMN-SimpleExclusiveGatewayProcess.bpmn2"));
        
        String out = new String();
        BufferedReader br = new BufferedReader(in);
        for(String line = br.readLine(); line != null; line = br.readLine()) 
          out += line;


        
        SimulationRepository repo = SimulationRunner.runSimulation("defaultPackage.test", out, 10, 2000, "default.simulation.rules.drl");
        assertThat(repo).isNotNull();
        
        WorkingMemorySimulationRepository wmRepo = (WorkingMemorySimulationRepository) repo;
        wmRepo.fireAllRules();
        assertThat(wmRepo.getAggregatedEvents()).hasSize(5);
        assertThat(wmRepo.getEvents()).hasSize(70);
        
        List<AggregatedSimulationEvent> aggEvents = wmRepo.getAggregatedEvents();
        for (AggregatedSimulationEvent event : aggEvents) {
            if (event instanceof AggregatedProcessSimulationEvent) {
                Map<String, Integer> numberOfInstancePerPath = ((AggregatedProcessSimulationEvent) event).getPathNumberOfInstances();
                assertThat(numberOfInstancePerPath).isNotNull();
                assertThat(3 == numberOfInstancePerPath.get("Path800898475-0")).isTrue();
                assertThat(7 == numberOfInstancePerPath.get("Path-960633761-1")).isTrue();
            }
        }
        wmRepo.close();
    }

    @Test
    public void testSimulationRunnerWithGatewaySingleInstance() throws IOException {
        
        InputStreamReader in = new InputStreamReader(this.getClass().getResourceAsStream("/BPMN-SimpleExclusiveGatewayProcess.bpmn2"));
        
        String out = new String();
        BufferedReader br = new BufferedReader(in);
        for(String line = br.readLine(); line != null; line = br.readLine()) 
          out += line;


        
        SimulationRepository repo = SimulationRunner.runSimulation("defaultPackage.test", out, 1, 2000, "default.simulation.rules.drl");
        assertThat(repo).isNotNull();
        
        WorkingMemorySimulationRepository wmRepo = (WorkingMemorySimulationRepository) repo;
        wmRepo.fireAllRules();
        assertThat(wmRepo.getAggregatedEvents()).hasSize(4);
        assertThat(wmRepo.getEvents()).hasSize(7);
        wmRepo.close();
    }
    
    @Test
    public void testSimulationRunnerWithGatewayTwoInstances() throws IOException {
        
        InputStreamReader in = new InputStreamReader(this.getClass().getResourceAsStream("/BPMN-SimpleExclusiveGatewayProcess.bpmn2"));
        
        String out = new String();
        BufferedReader br = new BufferedReader(in);
        for(String line = br.readLine(); line != null; line = br.readLine()) 
          out += line;


        
        SimulationRepository repo = SimulationRunner.runSimulation("defaultPackage.test", out, 2, 2000, "default.simulation.rules.drl");
        assertThat(repo).isNotNull();
        
        WorkingMemorySimulationRepository wmRepo = (WorkingMemorySimulationRepository) repo;
        wmRepo.fireAllRules();
        assertThat(wmRepo.getAggregatedEvents()).hasSize(5);
        assertThat(wmRepo.getEvents()).hasSize(14);
        wmRepo.close();
    }
    
    @Test
    public void testSimulationRunnerWithGatewaySingleInstanceWithRunRulesOnEveryEvent() throws IOException {
        
        InputStreamReader in = new InputStreamReader(this.getClass().getResourceAsStream("/BPMN-SimpleExclusiveGatewayProcess.bpmn2"));
        
        String out = new String();
        BufferedReader br = new BufferedReader(in);
        for(String line = br.readLine(); line != null; line = br.readLine()) 
          out += line;


        
        SimulationRepository repo = SimulationRunner.runSimulation("defaultPackage.test", out, 5, 2000, true, "default.simulation.rules.drl");
        assertThat(repo).isNotNull();
        
        WorkingMemorySimulationRepository wmRepo = (WorkingMemorySimulationRepository) repo;

        assertThat(wmRepo.getAggregatedEvents()).hasSize(20);
        assertThat(wmRepo.getEvents()).hasSize(35);
        wmRepo.close();
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testSimulationRunnerWithRunRulesOnEveryEvent() throws IOException {
        
        InputStreamReader in = new InputStreamReader(this.getClass().getResourceAsStream("/BPMN-SimpleExclusiveGatewayProcess.bpmn2"));
        
        String out = new String();
        BufferedReader br = new BufferedReader(in);
        for(String line = br.readLine(); line != null; line = br.readLine()) 
          out += line;


        
        SimulationRepository repo = SimulationRunner.runSimulation("defaultPackage.test", out, 5, 2000, true, "onevent.simulation.rules.drl");
        assertThat(repo).isNotNull();
        
        WorkingMemorySimulationRepository wmRepo = (WorkingMemorySimulationRepository) repo;

        assertThat(wmRepo.getAggregatedEvents()).hasSize(20);
        assertThat(wmRepo.getEvents()).hasSize(35);

        for (SimulationEvent event : wmRepo.getEvents()) {
            if ((event instanceof EndSimulationEvent) || (event instanceof ActivitySimulationEvent)|| (event instanceof HumanTaskActivitySimulationEvent)) {
                assertThat(((GenericSimulationEvent) event).getAggregatedEvent()).isNotNull();
                assertThat(((GenericSimulationEvent) event).getAggregatedEvent() instanceof AggregatedProcessSimulationEvent).isTrue();
            } else if (event instanceof ProcessInstanceEndSimulationEvent) {
                assertThat(((GenericSimulationEvent) event).getAggregatedEvent()).isNull();
            }
        }
        wmRepo.getSession().execute(new InsertElementsCommand((Collection)wmRepo.getAggregatedEvents()));
        wmRepo.fireAllRules();
        List<AggregatedSimulationEvent> summary = (List<AggregatedSimulationEvent>) wmRepo.getGlobal("summary");
        assertThat(summary).isNotNull();
        assertThat(summary).hasSize(5);
        for (AggregatedSimulationEvent event : summary) {
            if (event instanceof AggregatedProcessSimulationEvent) {
                Map<String, Integer> numberOfInstancePerPath = ((AggregatedProcessSimulationEvent) event).getPathNumberOfInstances();
                assertThat(numberOfInstancePerPath).isNotNull();
                assertThat((int)numberOfInstancePerPath.get("Path800898475-0")).isEqualTo(1);
                assertThat((int)numberOfInstancePerPath.get("Path-960633761-1")).isEqualTo(4);
            }
        }
        
        SimulationInfo info = wmRepo.getSimulationInfo();
        
        assertThat(info).isNotNull();
        assertThat(info.getProcessId()).isEqualTo("defaultPackage.test");
        assertThat(info.getProcessName()).isEqualTo("test");
        assertThat(info.getNumberOfExecutions()).isEqualTo(5);
        assertThat(info.getInterval()).isEqualTo(2000);
        
        System.out.println("Start date is " + new Date(info.getStartTime()) + " end date is " + new Date(info.getEndTime()));
        wmRepo.close();
    }
    
    @Test
    public void testSimulationRunnerWithSinglePath() throws IOException {
        
        InputStreamReader in = new InputStreamReader(this.getClass().getResourceAsStream("/BPMN2-UserTaskWithSimulationMetaData.bpmn2"));
        
        String out = new String();
        BufferedReader br = new BufferedReader(in);
        for(String line = br.readLine(); line != null; line = br.readLine()) 
          out += line;


        
        SimulationRepository repo = SimulationRunner.runSimulation("UserTask", out, 5, 2000, true, "onevent.simulation.rules.drl");
        assertThat(repo).isNotNull();
        
        WorkingMemorySimulationRepository wmRepo = (WorkingMemorySimulationRepository) repo;

        assertThat(wmRepo.getAggregatedEvents()).hasSize(15);
        assertThat(wmRepo.getEvents()).hasSize(20);
        wmRepo.close();
    }
    
    @Test
    public void testSimulationRunnerWithSinglePathAndCatchingEvent() throws IOException {
        
        InputStreamReader in = new InputStreamReader(this.getClass().getResourceAsStream("/BPMN2-SinglePathWithCatchingEvent.bpmn2"));
        
        String out = new String();
        BufferedReader br = new BufferedReader(in);
        for(String line = br.readLine(); line != null; line = br.readLine()) 
          out += line;


        
        SimulationRepository repo = SimulationRunner.runSimulation("defaultPackage.test", out, 5, 2000, true, "onevent.simulation.rules.drl");
        assertThat(repo).isNotNull();
        
        WorkingMemorySimulationRepository wmRepo = (WorkingMemorySimulationRepository) repo;

        assertThat(wmRepo.getAggregatedEvents()).hasSize(25);
        assertThat(wmRepo.getEvents()).hasSize(30);
        wmRepo.close();
    }
    
    @Test
    public void testSimulationRunnerWithSinglePathAndThrowingEvent() throws IOException {
        
        InputStreamReader in = new InputStreamReader(this.getClass().getResourceAsStream("/BPMN2-SinglePathWithThrowingEvent.bpmn2"));
        
        String out = new String();
        BufferedReader br = new BufferedReader(in);
        for(String line = br.readLine(); line != null; line = br.readLine()) 
          out += line;


        
        SimulationRepository repo = SimulationRunner.runSimulation("defaultPackage.test", out, 5, 2000, true, "onevent.simulation.rules.drl");
        assertThat(repo).isNotNull();
        
        WorkingMemorySimulationRepository wmRepo = (WorkingMemorySimulationRepository) repo;

        assertThat(wmRepo.getAggregatedEvents()).hasSize(25);
        assertThat(wmRepo.getEvents()).hasSize(30);
        wmRepo.close();
    }
    
    @Test
    public void testSimulationRunnerWithBoundaryEvent() throws IOException {
        
        InputStreamReader in = new InputStreamReader(this.getClass().getResourceAsStream("/BPMN2-SimpleWithBoundaryEvent.bpmn2"));
        
        String out = new String();
        BufferedReader br = new BufferedReader(in);
        for(String line = br.readLine(); line != null; line = br.readLine()) 
          out += line;


        
        SimulationRepository repo = SimulationRunner.runSimulation("defaultPackage.test", out, 5, 2000, true, "onevent.simulation.rules.drl");
        assertThat(repo).isNotNull();
        
        WorkingMemorySimulationRepository wmRepo = (WorkingMemorySimulationRepository) repo;

        assertThat(wmRepo.getAggregatedEvents()).hasSize(25);
        assertThat(wmRepo.getEvents()).hasSize(30);
        wmRepo.close();
    }
    
    @Test
    public void testSimulationRunnerWithScriptRuleXor() throws IOException {
        
        InputStreamReader in = new InputStreamReader(this.getClass().getResourceAsStream("/BPMN2-ScriptRuleXor.bpmn2"));
        
        String out = new String();
        BufferedReader br = new BufferedReader(in);
        for(String line = br.readLine(); line != null; line = br.readLine()) 
          out += line;


        
        SimulationRepository repo = SimulationRunner.runSimulation("defaultPackage.demo", out, 5, 2000, true, "onevent.simulation.rules.drl");
        assertThat(repo).isNotNull();
        
        WorkingMemorySimulationRepository wmRepo = (WorkingMemorySimulationRepository) repo;

        assertThat(wmRepo.getAggregatedEvents()).hasSize(30);
        assertThat(wmRepo.getEvents()).hasSize(45);
        
        wmRepo.getSession().execute(new InsertElementsCommand((Collection)wmRepo.getAggregatedEvents()));
        wmRepo.fireAllRules();
        
        List<AggregatedSimulationEvent> summary = (List<AggregatedSimulationEvent>) wmRepo.getGlobal("summary");
        assertThat(summary).isNotNull();
        assertThat(summary).hasSize(7);
        wmRepo.close();
    }
    
    @Test
    public void testSimulationRunnerWithLoop() throws IOException {
        
        InputStreamReader in = new InputStreamReader(this.getClass().getResourceAsStream("/BPMN2-loop-sim.bpmn2"));
        
        String out = new String();
        BufferedReader br = new BufferedReader(in);
        for(String line = br.readLine(); line != null; line = br.readLine()) 
          out += line;


        
        SimulationRepository repo = SimulationRunner.runSimulation("defaultPackage.loop-sim", out, 5, 2000, true, "onevent.simulation.rules.drl");
        assertThat(repo).isNotNull();
        
        WorkingMemorySimulationRepository wmRepo = (WorkingMemorySimulationRepository) repo;

        assertThat(wmRepo.getAggregatedEvents()).hasSize(19);
        assertThat(wmRepo.getEvents()).hasSize(37);
        wmRepo.close();
    }

    @Test
    public void testSimulationRunnerEmbeddedSubprocessWithActivites() throws IOException {

        InputStreamReader in = new InputStreamReader(this.getClass().getResourceAsStream("/BPMN2-EmbeddedSubprocessWithActivites.bpmn2"));

        String out = new String();
        BufferedReader br = new BufferedReader(in);
        for(String line = br.readLine(); line != null; line = br.readLine())
            out += line;



        SimulationRepository repo = SimulationRunner.runSimulation("project.simulation", out, 10, 120000, true, "onevent.simulation.rules.drl");
        assertThat(repo).isNotNull();

        WorkingMemorySimulationRepository wmRepo = (WorkingMemorySimulationRepository) repo;

        assertThat(wmRepo.getAggregatedEvents()).hasSize(50);
        assertThat(wmRepo.getEvents()).hasSize(80);
        wmRepo.close();
    }

    @Test
    public void testSimulationRunnerWithNestedFork() throws IOException {

        InputStreamReader in = new InputStreamReader(this.getClass().getResourceAsStream("/fork-process.bpmn2"));

        String out = new String();
        BufferedReader br = new BufferedReader(in);
        for(String line = br.readLine(); line != null; line = br.readLine())
            out += line;

        Integer intervalInt = 8*1000*60*60;

        SimulationRepository repo = SimulationRunner.runSimulation("simulation.fork-process", out, 40, intervalInt, true, "onevent.simulation.rules.drl");
        assertThat(repo).isNotNull();

        WorkingMemorySimulationRepository wmRepo = (WorkingMemorySimulationRepository) repo;
        wmRepo.getSession().execute(new InsertElementsCommand((Collection)wmRepo.getAggregatedEvents()));
        wmRepo.fireAllRules();

        List<AggregatedSimulationEvent> aggEvents = (List<AggregatedSimulationEvent>) wmRepo.getGlobal("summary");

        for (AggregatedSimulationEvent event : aggEvents) {
            if (event instanceof HTAggregatedSimulationEvent) {
                assertThat(((HTAggregatedSimulationEvent) event).getAvgWaitTime()).isCloseTo(0.0, within(0));
            }
        }
        wmRepo.close();

    }
}
