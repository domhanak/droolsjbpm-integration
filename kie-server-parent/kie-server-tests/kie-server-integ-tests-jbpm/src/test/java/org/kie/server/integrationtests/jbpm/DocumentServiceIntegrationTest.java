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

package org.kie.server.integrationtests.jbpm;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.document.Document;
import org.jbpm.document.service.impl.DocumentImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.api.KieServices;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.DocumentInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.api.exception.KieServicesException;
import org.kie.server.integrationtests.category.Smoke;

import static org.assertj.core.api.Assertions.*;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.kie.server.integrationtests.shared.KieServerUtil;

public class DocumentServiceIntegrationTest extends JbpmKieServerBaseIntegrationTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");

    private static final String PERSON_CLASS_NAME = "org.jbpm.data.Person";

    @BeforeClass
    public static void buildAndDeployArtifacts() {

        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/definition-project").getFile());

        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);

        createContainer(CONTAINER_ID, releaseId);
    }

    private DocumentInstance document;
    private String content;
    private byte[] contentBytes;

    @Before
    public void createData() {

        KieServerUtil.deleteDocumentStorageFolder();

        content = "just text content";
        contentBytes = content.getBytes();

        document = DocumentInstance.builder()
                .name("first document")
                .size(contentBytes.length)
                .lastModified(new Date())
                .content(contentBytes)
                .build();
    }

    @Override
    protected void addExtraCustomClasses(Map<String, Class<?>> extraClasses) throws Exception {
        extraClasses.put(PERSON_CLASS_NAME, Class.forName(PERSON_CLASS_NAME, true, kieContainer.getClassLoader()));
        extraClasses.put(DocumentImpl.class.getName(), DocumentImpl.class);
    }

    @Test
    @Category(Smoke.class)
    public void testCreateLoadDeleteDocument() throws Exception {

        String documentId = documentClient.createDocument(document);
        assertThat(documentId).isNotNull();

        DocumentInstance fromServer = documentClient.getDocument(documentId);
        assertThat(fromServer.getIdentifier()).isEqualTo(documentId);
        assertDocumentInstances(document, fromServer, true);

        documentClient.deleteDocument(documentId);

        try {
            documentClient.getDocument(documentId);
            fail("Document with id " + documentId + " was deleted so should not be found anymore");
        } catch (KieServicesException e) {
            // expected
        }
    }


    @Test
    public void testCreateDocument() {
        String documentId = documentClient.createDocument(document);
        assertThat(documentId).isNotNull();

        DocumentInstance fromServer = documentClient.getDocument(documentId);
        assertThat(fromServer.getIdentifier()).isEqualTo(documentId);
        assertDocumentInstances(document, fromServer, true);
    }

    @Test
    public void testCreateEmptyDocument() {
        content = "";
        contentBytes = content.getBytes();
        document = DocumentInstance.builder()
                .name("first document")
                .size(contentBytes.length)
                .lastModified(new Date())
                .content(contentBytes)
                .build();

        String documentId = documentClient.createDocument(document);
        assertThat(documentId).isNotNull();

        DocumentInstance fromServer = documentClient.getDocument(documentId);
        assertThat(fromServer.getIdentifier()).isEqualTo(documentId);
        assertDocumentInstances(document, fromServer, true);
    }

    @Test(expected = KieServicesException.class)
    public void testGetNotExistingDocument() {

        documentClient.getDocument("not-existing");
    }

    @Test
    public void testUpdateDocument() {
        String documentId = documentClient.createDocument(document);
        assertThat(documentId).isNotNull();

        DocumentInstance fromServer = documentClient.getDocument(documentId);
        assertThat(fromServer.getIdentifier()).isEqualTo(documentId);
        assertDocumentInstances(document, fromServer, true);


        String udpatedDoc = "here comes the update";
        byte[] updateDocBytes = udpatedDoc.getBytes();
        fromServer.setContent(updateDocBytes);
        fromServer.setSize(updateDocBytes.length);
        fromServer.setLastModified(new Date());

        documentClient.updateDocument(fromServer);

        DocumentInstance updatedFromServer = documentClient.getDocument(documentId);
        assertThat(updatedFromServer.getIdentifier()).isEqualTo(documentId);
        assertDocumentInstances(fromServer, updatedFromServer, true);
    }

    @Test
    public void testDeleteDocument() throws Exception {

        String documentId = documentClient.createDocument(document);
        assertThat(documentId).isNotNull();

        DocumentInstance fromServer = documentClient.getDocument(documentId);
        assertThat(fromServer).isNotNull();

        documentClient.deleteDocument(documentId);

        try {
            documentClient.getDocument(documentId);
            fail("Document with id " + documentId + " was deleted so should not be found anymore");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test(expected = KieServicesException.class)
    public void testDeleteNotExistingDocument() {

        documentClient.deleteDocument("not-existing");
    }

    @Test(expected = KieServicesException.class)
    public void testUpdateNotExistingDocument() {

        document.setIdentifier("not-existing");
        documentClient.updateDocument(document);
    }

    @Test
    public void testListDocuments() {
        List<DocumentInstance> docs = documentClient.listDocuments(0, 10);
        assertThat(docs).isNotNull();
        assertThat(docs).isEmpty();

        String documentId = documentClient.createDocument(document);
        assertThat(documentId).isNotNull();

        docs = documentClient.listDocuments(0, 10);
        assertThat(docs).isNotNull();
        assertThat(docs).hasSize(1);

        DocumentInstance fromServer = docs.get(0);
        assertThat(fromServer.getIdentifier()).isEqualTo(documentId);
        assertDocumentInstances(document, fromServer, false);
    }

    @Test
    public void testDocumentProcess() {

        DocumentImpl docToTranslate = new DocumentImpl();
        docToTranslate.setContent(document.getContent());
        docToTranslate.setLastModified(document.getLastModified());
        docToTranslate.setName(document.getName());
        docToTranslate.setSize(document.getSize());


        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("original_document", docToTranslate);

        Long processInstanceId = null;
        try {
            processInstanceId = processClient.startProcess("definition-project", "xyz-translations", parameters);

            assertThat(processInstanceId).isNotNull();
            assertThat(processInstanceId.longValue() > 0).isTrue();

            List<DocumentInstance> docs = documentClient.listDocuments(0, 10);
            assertThat(docs).isNotNull();
            assertThat(docs).hasSize(1);

            Object docVar = processClient.getProcessInstanceVariable("definition-project", processInstanceId, "original_document");
            assertThat(docVar).isNotNull();
            assertThat(docVar instanceof Document).isTrue();

            Document doc = (Document) docVar;
            assertDocuments(docToTranslate, doc);

            List<TaskSummary> tasks = taskClient.findTasksAssignedAsPotentialOwner("yoda", 0, 10);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(1);
            // review task
            long taskId = tasks.get(0).getId();

            taskClient.claimTask("definition-project", taskId, "yoda");
            taskClient.startTask("definition-project", taskId, "yoda");

            Map<String, Object> taskInputs = taskClient.getTaskInputContentByTaskId("definition-project", taskId);
            assertThat(taskInputs).isNotNull();
            assertThat(taskInputs).hasSize(6);

            docVar = taskInputs.get("in_doc");
            assertThat(docVar).isNotNull();
            assertThat(docVar instanceof Document).isTrue();

            doc = (Document) docVar;
            assertDocuments(docToTranslate, doc);

            Map<String, Object> result = new HashMap<String, Object>();
            result.put("out_comments", "ready to translate");
            result.put("out_status", "OK");

            taskClient.completeTask("definition-project", taskId, "yoda", result);

            tasks = taskClient.findTasksAssignedAsPotentialOwner("yoda", 0, 10);
            assertThat(tasks).isNotNull();
            assertThat(tasks).hasSize(1);
            // translate task
            taskId = tasks.get(0).getId();

            taskClient.claimTask("definition-project", taskId, "yoda");
            taskClient.startTask("definition-project", taskId, "yoda");

            taskInputs = taskClient.getTaskInputContentByTaskId("definition-project", taskId);
            assertThat(taskInputs).isNotNull();
            assertThat(taskInputs).hasSize(8);

            docVar = taskInputs.get("in_doc");
            assertThat(docVar).isNotNull();
            assertThat(docVar instanceof Document).isTrue();

            doc = (Document) docVar;
            assertDocuments(docToTranslate, doc);

            result = new HashMap<String, Object>();
            DocumentImpl translated = new DocumentImpl();
            translated.setContent("translated document content".getBytes());
            translated.setLastModified(new Date());
            translated.setName("translated document");
            translated.setSize(translated.getContent().length);
            result.put("out_translated", translated);
            result.put("out_comments", "translated");
            result.put("out_status", "DONE");

            taskClient.completeTask("definition-project", taskId, "yoda", result);

            // now lets check if the document was updated
            docVar = processClient.getProcessInstanceVariable("definition-project", processInstanceId, "translated_document");
            assertThat(docVar).isNotNull();
            assertThat(docVar instanceof Document).isTrue();

            doc = (Document) docVar;
            assertDocuments(translated, doc);

            docs = documentClient.listDocuments(0, 10);
            assertThat(docs).isNotNull();
            assertThat(docs).hasSize(2);
        } finally {
            if (processInstanceId != null) {
                processClient.abortProcessInstance("definition-project", processInstanceId);
            }
        }

    }

    private void assertDocumentInstances(DocumentInstance expected, DocumentInstance actual, boolean assertContent) {
        assertThat(actual).isNotNull();
        assertThat(actual.getIdentifier()).isNotNull();
        assertThat(actual.getName()).isNotNull();
        assertThat(actual.getLastModified()).isNotNull();
        assertThat(actual.getSize()).isNotNull();
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getSize()).isEqualTo(expected.getSize());
        if (assertContent) {
            assertThat(new String(actual.getContent())).isEqualTo(new String(expected.getContent()));
        }
    }

    private void assertDocuments(Document expected, Document actual) {
        assertThat(actual).isNotNull();
        assertThat(actual.getIdentifier()).isNotNull();
        assertThat(actual.getName()).isNotNull();
        assertThat(actual.getLastModified()).isNotNull();
        assertThat(actual.getSize()).isNotNull();
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getSize()).isEqualTo(expected.getSize());
        assertThat(new String(actual.getContent())).isEqualTo(new String(expected.getContent()));
    }
}
