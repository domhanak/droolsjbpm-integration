/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.router.proxy.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.kie.server.router.proxy.aggragate.JSONResponseAggregator;

public class JSONAggregatorTest extends AbstractAggregateTest {

    private static final Logger logger = Logger.getLogger(JSONAggregatorTest.class);

    @Test
    public void testAggregateProcessDefinitions() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/process-def-1.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/process-def-2.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        JSONObject aggregated = new JSONObject(result);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("processes");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(8);
    }

    @Test
    public void testAggregateProcessDefinitionsTargetEmpty() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/process-def-1.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/process-def-empty.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        JSONObject aggregated = new JSONObject(result);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("processes");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(5);
    }

    @Test
    public void testAggregateProcessDefinitionsSourceEmpty() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/process-def-empty.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/process-def-2.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        JSONObject aggregated = new JSONObject(result);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("processes");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(3);
    }

    @Test
    public void testAggregateProcessDefinitionsEmpty() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/process-def-empty.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/process-def-empty.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        JSONObject aggregated = new JSONObject(result);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("processes");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(0);
    }


    @Test
    public void testAggregateProcessInstances() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/process-instance-1.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/process-instance-2.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        JSONObject aggregated = new JSONObject(result);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("process-instance");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(5);
    }

    @Test
    public void testAggregateProcessInstancesTargetEmpty() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/process-instance-1.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/process-instance-empty.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        JSONObject aggregated = new JSONObject(result);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("process-instance");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(3);
    }

    @Test
    public void testAggregateProcessInstancesSourceEmpty() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/process-instance-empty.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/process-instance-2.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        JSONObject aggregated = new JSONObject(result);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("process-instance");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(2);
    }

    @Test
    public void testAggregateProcessInstancesEmpty() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/process-instance-empty.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/process-instance-empty.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        JSONObject aggregated = new JSONObject(result);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("process-instance");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(0);
    }

    @Test
    public void testAggregateTaskSummaries() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/task-summary-1.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/task-summary-2.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        JSONObject aggregated = new JSONObject(result);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("task-summary");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(7);
    }

    @Test
    public void testAggregateTaskSummariesTargetEmpty() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/task-summary-1.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/task-summary-empty.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        JSONObject aggregated = new JSONObject(result);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("task-summary");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(3);
    }

    @Test
    public void testAggregateTaskSummariesSourceEmpty() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/task-summary-empty.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/task-summary-2.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        JSONObject aggregated = new JSONObject(result);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("task-summary");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(4);
    }

    @Test
    public void testAggregateTaskSummariesEmpty() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/task-summary-empty.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/task-summary-empty.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        JSONObject aggregated = new JSONObject(result);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("task-summary");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(0);
    }

    @Test
    public void testSortProcessDefinitions() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/process-def-1.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/process-def-2.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String sorted = aggregate.aggregate(data, "ProcessId", true, 0, 2);
        logger.debug(sorted);

        JSONObject aggregated = new JSONObject(sorted);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("processes");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(2);
        // make sure it's properly sorted and paged
        String value1 = ((JSONObject)processDefs.get(0)).getString("process-id");
        assertThat(value1).isEqualTo("1");
        String value2 = ((JSONObject)processDefs.get(1)).getString("process-id");
        assertThat(value2).isEqualTo("2");
    }

    @Test
    public void testSortProcessDefinitionsDescending() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/process-def-1.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/process-def-2.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String sorted = aggregate.aggregate(data, "ProcessId", false, 0, 2);
        logger.debug(sorted);

        JSONObject aggregated = new JSONObject(sorted);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("processes");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(2);
        // make sure it's properly sorted and paged
        String value1 = ((JSONObject)processDefs.get(0)).getString("process-id");
        assertThat(value1).isEqualTo("8");
        String value2 = ((JSONObject)processDefs.get(1)).getString("process-id");
        assertThat(value2).isEqualTo("7");
    }

    @Test
    public void testSortProcessDefinitionsNextPage() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/process-def-1.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/process-def-2.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String sorted = aggregate.aggregate(data, "ProcessId", true, 1, 2);
        logger.debug(sorted);

        JSONObject aggregated = new JSONObject(sorted);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("processes");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(2);
        // make sure it's properly sorted and paged
        String value1 = ((JSONObject)processDefs.get(0)).getString("process-id");
        assertThat(value1).isEqualTo("3");
        String value2 = ((JSONObject)processDefs.get(1)).getString("process-id");
        assertThat(value2).isEqualTo("4");
    }

    @Test
    public void testSortProcessDefinitionsNextPageDescending() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/process-def-1.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/process-def-2.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String sorted = aggregate.aggregate(data, "ProcessId", false, 1, 2);
        logger.debug(sorted);

        JSONObject aggregated = new JSONObject(sorted);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("processes");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(2);
        // make sure it's properly sorted and paged
        String value1 = ((JSONObject)processDefs.get(0)).getString("process-id");
        assertThat(value1).isEqualTo("6");
        String value2 = ((JSONObject)processDefs.get(1)).getString("process-id");
        assertThat(value2).isEqualTo("5");
    }

    @Test
    public void testSortProcessDefinitionsOutOfPage() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/process-def-1.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/process-def-2.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String sorted = aggregate.aggregate(data, "ProcessId", true, 5, 2);
        logger.debug(sorted);

        JSONObject aggregated = new JSONObject(sorted);
        assertThat(aggregated).isNotNull();

        Object processes = aggregated.get("processes");
        assertThat(processes).isNotNull();
        assertThat(processes instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) processes;
        assertThat(processDefs.length()).isEqualTo(0);

    }

    @Test
    public void testAggregateContainers() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/containers-1.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/containers-2.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        JSONObject aggregated = new JSONObject(result);
        assertThat(aggregated).isNotNull();

        JSONObject content = (JSONObject)((JSONObject) aggregated.get("result")).get("kie-containers");

        Object containers = content.get("kie-container");
        assertThat(containers).isNotNull();
        assertThat(containers instanceof JSONArray).isTrue();

        JSONArray processDefs = (JSONArray) containers;
        assertThat(processDefs.length()).isEqualTo(6);
    }

    @Test
    public void testAggregateRawList() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/raw-list-1.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/raw-list-2.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        JSONArray aggregated = new JSONArray(result);
        assertThat(aggregated).isNotNull();
        assertThat(aggregated.length()).isEqualTo(5);
    }

    @Test
    public void testAggregateRawListWithPaging() throws Exception {
        String json1 = read(this.getClass().getResourceAsStream("/json/raw-list-1.json"));
        String json2 = read(this.getClass().getResourceAsStream("/json/raw-list-2.json"));

        JSONResponseAggregator aggregate = new JSONResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(json1);
        data.add(json2);

        String result = aggregate.aggregate(data, null, true, 1, 2);
        logger.debug(result);

        JSONArray aggregated = new JSONArray(result);
        assertThat(aggregated).isNotNull();
        assertThat(aggregated.length()).isEqualTo(2);
    }
}

