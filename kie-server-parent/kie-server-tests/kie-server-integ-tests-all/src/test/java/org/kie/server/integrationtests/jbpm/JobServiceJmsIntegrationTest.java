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

package org.kie.server.integrationtests.jbpm;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.internal.executor.api.STATUS;
import org.kie.server.api.model.instance.JobRequestInstance;
import org.kie.server.api.model.instance.RequestInfoInstance;
import org.kie.server.integrationtests.category.JMSOnly;
import org.kie.server.integrationtests.config.TestConfig;
import org.kie.server.integrationtests.shared.KieServerSynchronization;

@Category(JMSOnly.class)
public class JobServiceJmsIntegrationTest extends JbpmKieServerBaseIntegrationTest {

    private static final long NUMBER_OF_JOBS = 10;
    private static final long MAXIMUM_PROCESSING_TIME = 20000;

    @Test
    public void testScheduleSeveralJobs() throws Exception {
        // Test is using JMS, isn't available for local execution.
        Assume.assumeFalse(TestConfig.isLocalServer());

        String businessKey = "test key";
        String command = "org.jbpm.executor.commands.PrintOutCommand";

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("businessKey", businessKey);

        JobRequestInstance jobRequestInstance = new JobRequestInstance();
        jobRequestInstance.setCommand(command);
        jobRequestInstance.setData(data);

        List<Long> jobIds = new ArrayList<Long>();
        long startTime = Calendar.getInstance().getTimeInMillis();

        // Start 10 jobs at once.
        for (int i=0; i<NUMBER_OF_JOBS; i++) {
            Long jobId = jobServicesClient.scheduleRequest(jobRequestInstance);
            assertThat(jobId).isNotNull();
            assertThat(jobId.longValue() > 0).isTrue();
            jobIds.add(jobId);
        }

        // All jobs are processed successfully.
        for (Long jobId : jobIds) {
            KieServerSynchronization.waitForJobToFinish(jobServicesClient, jobId);
            RequestInfoInstance jobRequest = jobServicesClient.getRequestById(jobId, false, false);

            assertThat(jobRequest).isNotNull();
            assertThat(jobRequest.getId()).isEqualTo(jobId);
            assertThat(jobRequest.getBusinessKey()).isEqualTo(businessKey);
            assertThat(jobRequest.getStatus()).isEqualTo(STATUS.DONE.toString());
            assertThat(jobRequest.getCommandName()).isEqualTo(command);
        }
        long durationTime = Calendar.getInstance().getTimeInMillis() - startTime;

        // All jobs should be processed and done in less than 20 s.
        assertThat(durationTime < MAXIMUM_PROCESSING_TIME).as("Job processing exceeded expected time! Actual time: " + durationTime + "ms").isTrue();
    }
}
