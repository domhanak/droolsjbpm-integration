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

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.jboss.logging.Logger;
import org.junit.Test;
import org.kie.server.router.proxy.aggragate.JaxbXMLResponseAggregator;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class JAXBAggregatorTest extends AbstractAggregateTest {

    private static final Logger logger = Logger.getLogger(JAXBAggregatorTest.class);

    @Test
    public void testAggregateProcessDefinitions() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/process-def-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/process-def-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("process-definitions");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processDefs = xml.getElementsByTagName("processes");
        assertThat(processDefs).isNotNull();
        assertThat(processDefs.getLength()).isEqualTo(5);
    }

    @Test
    public void testAggregateProcessDefinitionsTargetEmpty() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/process-def-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/process-def-empty.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("process-definitions");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processDefs = xml.getElementsByTagName("processes");
        assertThat(processDefs).isNotNull();
        assertThat(processDefs.getLength()).isEqualTo(2);
    }

    @Test
    public void testAggregateProcessDefinitionsSourceEmpty() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/process-def-empty.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/process-def-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("process-definitions");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processDefs = xml.getElementsByTagName("processes");
        assertThat(processDefs).isNotNull();
        assertThat(processDefs.getLength()).isEqualTo(3);
    }

    @Test
    public void testAggregateProcessDefinitionsEmpty() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/process-def-empty.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/process-def-empty.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("process-definitions");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processDefs = xml.getElementsByTagName("processes");
        assertThat(processDefs).isNotNull();
        assertThat(processDefs.getLength()).isEqualTo(0);
    }

    @Test
    public void testAggregateProcessInstances() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/process-instance-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/process-instance-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("process-instance-list");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processInstances = xml.getElementsByTagName("process-instance");
        assertThat(processInstances).isNotNull();
        assertThat(processInstances.getLength()).isEqualTo(4);
    }

    @Test
    public void testAggregateProcessInstancesTargetEmpty() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/process-instance-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/process-instance-empty.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("process-instance-list");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processInstances = xml.getElementsByTagName("process-instance");
        assertThat(processInstances).isNotNull();
        assertThat(processInstances.getLength()).isEqualTo(1);
    }

    @Test
    public void testAggregateProcessInstancesSourceEmpty() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/process-instance-empty.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/process-instance-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("process-instance-list");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processInstances = xml.getElementsByTagName("process-instance");
        assertThat(processInstances).isNotNull();
        assertThat(processInstances.getLength()).isEqualTo(3);
    }

    @Test
    public void testAggregateProcessInstancesEmpty() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/process-instance-empty.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/process-instance-empty.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("process-instance-list");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processInstances = xml.getElementsByTagName("process-instance");
        assertThat(processInstances).isNotNull();
        assertThat(processInstances.getLength()).isEqualTo(0);
    }

    @Test
    public void testAggregateTaskSummaries() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/task-summary-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/task-summary-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("task-summary-list");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processInstances = xml.getElementsByTagName("task-summary");
        assertThat(processInstances).isNotNull();
        assertThat(processInstances.getLength()).isEqualTo(5);
    }

    @Test
    public void testAggregateTaskSummariesTargetEmpty() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/task-summary-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/task-summary-empty.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("task-summary-list");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processInstances = xml.getElementsByTagName("task-summary");
        assertThat(processInstances).isNotNull();
        assertThat(processInstances.getLength()).isEqualTo(3);
    }

    @Test
    public void testAggregateTaskSummariesSourceEmpty() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/task-summary-empty.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/task-summary-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("task-summary-list");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processInstances = xml.getElementsByTagName("task-summary");
        assertThat(processInstances).isNotNull();
        assertThat(processInstances.getLength()).isEqualTo(2);
    }

    @Test
    public void testAggregateTaskSummariesEmpty() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/task-summary-empty.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/task-summary-empty.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("task-summary-list");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processInstances = xml.getElementsByTagName("task-summary");
        assertThat(processInstances).isNotNull();
        assertThat(processInstances.getLength()).isEqualTo(0);
    }

    @Test
    public void testSortProcessDefinitions() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/process-def-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/process-def-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data, "ProcessId", true, 0, 2);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("process-definitions");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processDefs = xml.getElementsByTagName("processes");
        assertThat(processDefs).isNotNull();
        assertThat(processDefs.getLength()).isEqualTo(2);

        NodeList processDefIds = xml.getElementsByTagName("process-id");
        assertThat(processDefIds).isNotNull();
        assertThat(processDefIds.getLength()).isEqualTo(2);
        // make sure it's properly sorted and paged
        String value1 = processDefIds.item(0).getFirstChild().getNodeValue();
        assertThat(value1).isEqualTo("1");
        String value2 = processDefIds.item(1).getFirstChild().getNodeValue();
        assertThat(value2).isEqualTo("2");
    }

    @Test
    public void testSortProcessDefinitionsDescending() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/process-def-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/process-def-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data, "ProcessId", false, 0, 2);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("process-definitions");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processDefs = xml.getElementsByTagName("processes");
        assertThat(processDefs).isNotNull();
        assertThat(processDefs.getLength()).isEqualTo(2);

        NodeList processDefIds = xml.getElementsByTagName("process-id");
        assertThat(processDefIds).isNotNull();
        assertThat(processDefIds.getLength()).isEqualTo(2);
        // make sure it's properly sorted and paged
        String value1 = processDefIds.item(0).getFirstChild().getNodeValue();
        assertThat(value1).isEqualTo("5");
        String value2 = processDefIds.item(1).getFirstChild().getNodeValue();
        assertThat(value2).isEqualTo("4");
    }

    @Test
    public void testSortProcessDefinitionsNextPage() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/process-def-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/process-def-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data, "ProcessId", true, 1, 2);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("process-definitions");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processDefs = xml.getElementsByTagName("processes");
        assertThat(processDefs).isNotNull();
        assertThat(processDefs.getLength()).isEqualTo(2);

        NodeList processDefIds = xml.getElementsByTagName("process-id");
        assertThat(processDefIds).isNotNull();
        assertThat(processDefIds.getLength()).isEqualTo(2);
        // make sure it's properly sorted and paged
        String value1 = processDefIds.item(0).getFirstChild().getNodeValue();
        assertThat(value1).isEqualTo("3");
        String value2 = processDefIds.item(1).getFirstChild().getNodeValue();
        assertThat(value2).isEqualTo("4");
    }

    @Test
    public void testSortProcessDefinitionsNextPageDescending() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/process-def-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/process-def-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data, "ProcessId", false, 1, 2);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("process-definitions");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processDefs = xml.getElementsByTagName("processes");
        assertThat(processDefs).isNotNull();
        assertThat(processDefs.getLength()).isEqualTo(2);

        NodeList processDefIds = xml.getElementsByTagName("process-id");
        assertThat(processDefIds).isNotNull();
        assertThat(processDefIds.getLength()).isEqualTo(2);
        // make sure it's properly sorted and paged
        String value1 = processDefIds.item(0).getFirstChild().getNodeValue();
        assertThat(value1).isEqualTo("3");
        String value2 = processDefIds.item(1).getFirstChild().getNodeValue();
        assertThat(value2).isEqualTo("2");
    }

    @Test
    public void testSortProcessDefinitionsOutOPage() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/process-def-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/process-def-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data, "ProcessId", true, 5, 2);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("process-definitions");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList processDefs = xml.getElementsByTagName("processes");
        assertThat(processDefs).isNotNull();
        assertThat(processDefs.getLength()).isEqualTo(0);
    }

    @Test
    public void testAggregateContainers() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/containers-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/containers-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("response");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(1);

        NodeList wrapper = xml.getElementsByTagName("kie-containers");
        assertThat(wrapper).isNotNull();
        assertThat(wrapper.getLength()).isEqualTo(1);

        NodeList containers = xml.getElementsByTagName("kie-container");
        assertThat(containers).isNotNull();
        assertThat(containers.getLength()).isEqualTo(6);
    }

    @Test
    public void testAggregateRawList() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/raw-list-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/raw-list-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("value");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(5);

    }

    @Test
    public void testAggregateRawListNamespace() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/raw-list-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/raw-list-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList evaluate = (NodeList) xPath.evaluate("/list-type/items/items", xml, XPathConstants.NODESET);
        assertThat(evaluate.getLength()).isEqualTo(5);

        NamedNodeMap firstNodeAttributes = evaluate.item(0).getAttributes();
        assertThat(firstNodeAttributes.item(0).getNodeName()).isEqualTo("xmlns:xsi");
        assertThat(firstNodeAttributes.item(0).getNodeValue()).isEqualTo("http://www.w3.org/2001/XMLSchema-instance");
        assertThat(firstNodeAttributes.item(1).getNodeName()).isEqualTo("xsi:type");
        assertThat(firstNodeAttributes.item(1).getNodeValue()).isEqualTo("jaxbList");

    }

    @Test
    public void testAggregateRawListWithPaging() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/raw-list-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/raw-list-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data, null, true, 1, 2);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        NodeList processes = xml.getElementsByTagName("value");
        assertThat(processes).isNotNull();
        assertThat(processes.getLength()).isEqualTo(2);
    }

    @Test
    public void testAggregateRawListWithPagingNamespace() throws Exception {
        String xml1 = read(this.getClass().getResourceAsStream("/jaxb/raw-list-1.xml"));
        String xml2 = read(this.getClass().getResourceAsStream("/jaxb/raw-list-2.xml"));
        JaxbXMLResponseAggregator aggregate = new JaxbXMLResponseAggregator();

        List<String> data = new ArrayList<>();
        data.add(xml1);
        data.add(xml2);

        String result = aggregate.aggregate(data, null, true, 1, 2);
        logger.debug(result);

        Document xml = toXml(result);
        assertThat(xml).isNotNull();

        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList evaluate = (NodeList) xPath.evaluate("/list-type/items/items", xml, XPathConstants.NODESET);
        assertThat(evaluate.getLength()).isEqualTo(2);

        NamedNodeMap firstNodeAttributes = evaluate.item(0).getAttributes();
        assertThat(firstNodeAttributes.item(0).getNodeName()).isEqualTo("xmlns:xsi");
        assertThat(firstNodeAttributes.item(0).getNodeValue()).isEqualTo("http://www.w3.org/2001/XMLSchema-instance");
        assertThat(firstNodeAttributes.item(1).getNodeName()).isEqualTo("xsi:type");
        assertThat(firstNodeAttributes.item(1).getNodeValue()).isEqualTo("jaxbList");
    }
}

