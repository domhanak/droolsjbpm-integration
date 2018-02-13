/*
 * Copyright 2016 JBoss by Red Hat.
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
package org.kie.server.integrationtests.jbpm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.TaskInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.integrationtests.category.Email;
import org.kie.server.integrationtests.category.Unstable;
import org.kie.server.integrationtests.config.TestConfig;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;

import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

@Category(Email.class)
public class UserTaskEscalationIntegrationTest extends JbpmKieServerBaseIntegrationTest {

    private static final String ESCALATION_TEXT = "Escalation text";
    private static final String FROM_EMAIL = "kie-server-test@domain.com";
    private static final String EMAIL_DOMAIN = "@domain.com";
    private static final int SMTP_PORT = 2525;
    private static final String SMTP_HOST = "localhost";

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");

    private final static Map<String, Object> params = new HashMap<String, Object>();

    static {
        params.put("actor", USER_YODA);
        params.put("reassignTo", USER_JOHN);
        params.put("escUser", USER_JOHN);
        params.put("escalation", ESCALATION_TEXT);
    }

    private Wiser wiser;

    @Parameterized.Parameters(name = "{index}: {0} {1}")
    public static Collection<Object[]> data() {
        KieServicesConfiguration configuration = createKieServicesRestConfiguration();

        Collection<Object[]> parameterData = new ArrayList<Object[]>(Arrays.asList(new Object[][] {
                                {MarshallingFormat.JAXB, configuration}
                        }
        ));

        return parameterData;
    }

    @Before
    public void startWiser() {
        wiser = new Wiser();
        wiser.setHostname(SMTP_HOST);
        wiser.setPort(SMTP_PORT);
        wiser.start();
    }

    @After
    public void stopWiser() {
        if (wiser != null) {
            wiser.stop();
        }
    }

    @BeforeClass
    public static void buildAndDeployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/definition-project").getFile());

        createContainer(CONTAINER_ID, releaseId);
    }

    @Test
    public void testEscalation() throws InterruptedException, MessagingException, Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK_ESCALATION, params);
        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId > 0).isTrue();
        try {
            List<TaskSummary> taskList = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertThat(taskList).isNotNull();
            assertThat(taskList).hasSize(1);
            TaskSummary taskSummary = taskList.get(0);
            assertThat(taskSummary.getName()).isEqualTo("User Task");
            Long taskId = taskSummary.getId();

            TaskInstance taskInstance = taskClient.findTaskById(taskId);
            assertThat(taskInstance).isNotNull();
            assertThat(taskInstance.getActualOwner()).isEqualTo(USER_YODA);

            changeUser(USER_JOHN);
            waitForAssign(taskId, USER_JOHN);

            taskList = taskClient.findTasksAssignedAsPotentialOwner(USER_JOHN, 0, 10);
            assertThat(taskList).isNotNull();
            assertThat(taskList).hasSize(1);
            taskId = taskList.get(0).getId();
            taskInstance = taskClient.findTaskById(taskId);
            assertThat(taskInstance).isNotNull();

            taskClient.startTask(CONTAINER_ID, taskId, USER_JOHN);

            taskInstance = taskClient.findTaskById(taskId);
            assertThat(taskInstance).isNotNull();
            assertThat(taskInstance.getActualOwner()).isEqualTo(USER_JOHN);

            waitForEmailsRecieve(wiser);
            taskClient.completeTask(CONTAINER_ID, taskId, USER_JOHN, new HashMap<String, Object>());

            ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getState().intValue()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);

            assertEmails("Escalation");
        } catch (Exception e) {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            throw e;
        } finally {
            changeUser(TestConfig.getUsername());
        }
    }

    @Test
    @Category(Unstable.class)
    public void testCompleteTaskBeforeEscalation() throws InterruptedException {
        // Unstable on slow DBs where starting of task is called after escalation timeout.
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK_ESCALATION, params);
        assertThat(processInstanceId).isNotNull();
        assertThat(processInstanceId > 0).isTrue();

        List<TaskSummary> taskList = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
        assertThat(taskList).isNotNull();
        assertThat(taskList).hasSize(1);
        TaskSummary taskSummary = taskList.get(0);
        assertThat(taskSummary.getName()).isEqualTo("User Task");
        Long taskId = taskSummary.getId();

        taskClient.startTask(CONTAINER_ID, taskId, USER_YODA);
        taskClient.completeTask(CONTAINER_ID, taskId, USER_YODA, new HashMap<String, Object>());

        ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getState().intValue()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);

        KieServerAssert.assertNullOrEmpty("Email recieved!", wiser.getMessages());

        //wait while, cause email is sent 6s after task start
        Thread.sleep(8000l);
        KieServerAssert.assertNullOrEmpty("Email recieved!", wiser.getMessages());
    }

    private void assertEmails(final String subj) throws MessagingException, IOException {
        //wiser shoudl catch 2 messages (one for John and one for Administrator)
        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).isNotNull();
        assertThat(messages).hasSize(2);

        for (WiserMessage message : messages) {
            MimeMessage receivedMessage = message.getMimeMessage();
            assertThat(receivedMessage).isNotNull();

            assertThat(receivedMessage.getSubject()).isEqualTo(subj);
            assertThat((String) receivedMessage.getContent()).isEqualTo(ESCALATION_TEXT);

            InternetAddress[] from = (InternetAddress[]) receivedMessage.getFrom();
            assertThat(from.length).isEqualTo(1);
            assertThat(from[0].getAddress()).isEqualTo(FROM_EMAIL);
            InternetAddress[] to = (InternetAddress[]) receivedMessage.getAllRecipients();
            assertThat(to.length).isEqualTo(2);
            if (to[0].getAddress().equals(USER_JOHN + EMAIL_DOMAIN)) {
                assertThat(to[0].getAddress()).isEqualTo(USER_JOHN + EMAIL_DOMAIN);
                assertThat(to[1].getAddress()).isEqualTo(USER_ADMINISTRATOR + EMAIL_DOMAIN);
            } else {
                assertThat(to[0].getAddress()).isEqualTo(USER_ADMINISTRATOR + EMAIL_DOMAIN);
                assertThat(to[1].getAddress()).isEqualTo(USER_JOHN + EMAIL_DOMAIN);
            }
        }
    }

    private static final long SERVICE_TIMEOUT = 30000;
    private static final long TIMEOUT_BETWEEN_CALLS = 200;

    protected void waitForEmailsRecieve(Wiser wiser) throws Exception {
        long timeoutTime = Calendar.getInstance().getTimeInMillis() + SERVICE_TIMEOUT;
        while (Calendar.getInstance().getTimeInMillis() < timeoutTime) {
            if (wiser.getMessages().isEmpty()) {
                Thread.sleep(TIMEOUT_BETWEEN_CALLS);
            } else {
                return;
            }
        }
        throw new TimeoutException("Timeout while waiting for process instance to finish.");
    }

    protected void waitForAssign(Long taskId, String potencialOwner) throws Exception {
        long timeoutTime = Calendar.getInstance().getTimeInMillis() + SERVICE_TIMEOUT;
        while (Calendar.getInstance().getTimeInMillis() < timeoutTime) {
            if (taskClient.findTasksAssignedAsPotentialOwner(potencialOwner, 0, 10).isEmpty()) {
                Thread.sleep(TIMEOUT_BETWEEN_CALLS);
            } else {
                return;
            }
        }
        throw new TimeoutException("Timeout while waiting for process instance to finish.");
    }
}
