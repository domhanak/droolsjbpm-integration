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
package org.kie.server.integrationtests.jbpm.cases;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.assertj.core.api.Assertions.fail;
import static org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED;
import static org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE;
import static org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.jbpm.casemgmt.api.model.CaseStatus;
import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.task.model.Status;
import org.kie.server.api.exception.KieServicesException;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.cases.CaseAdHocFragment;
import org.kie.server.api.model.cases.CaseComment;
import org.kie.server.api.model.cases.CaseDefinition;
import org.kie.server.api.model.cases.CaseFile;
import org.kie.server.api.model.cases.CaseFileDataItem;
import org.kie.server.api.model.cases.CaseInstance;
import org.kie.server.api.model.cases.CaseMilestone;
import org.kie.server.api.model.cases.CaseRoleAssignment;
import org.kie.server.api.model.cases.CaseStage;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.instance.NodeInstance;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.client.CaseServicesClient;
import org.kie.server.integrationtests.config.TestConfig;
import org.kie.server.integrationtests.jbpm.JbpmKieServerBaseIntegrationTest;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;

public class CaseRuntimeDataServiceIntegrationTest extends JbpmKieServerBaseIntegrationTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "case-insurance",
            "1.0.0.Final");

    private static final String CONTAINER_ID = "insurance";
    private static final String CONTAINER_ID2 = "insurance-second";
    private static final String PROPERTY_DAMAGE_REPORT_CLASS_NAME = "org.kie.server.testing.PropertyDamageReport";
    private static final String CLAIM_REPORT_CLASS_NAME = "org.kie.server.testing.ClaimReport";

    private static final String CASE_OWNER_ROLE = "owner";
    private static final String CASE_CONTACT_ROLE = "contact";

    private static final String CASE_INSURED_ROLE = "insured";
    private static final String CASE_INS_REP_ROLE = "insuranceRepresentative";
    private static final String CASE_ASSESSOR_ROLE = "assessor";

    private static final String CLAIM_CASE_ID_PREFIX = "CAR_INS";
    private static final String CLAIM_CASE_DEF_ID = "insurance-claims.CarInsuranceClaimCase";
    private static final String CLAIM_CASE_DESRIPTION = "CarInsuranceClaimCase";
    private static final String CLAIM_CASE_NAME = "CarInsuranceClaimCase";
    private static final String CLAIM_CASE_VERSION = "1.0";

    private static final String CASE_HR_ID_PREFIX = "HR";
    private static final String CASE_HR_DEF_ID = "UserTaskCase";
    private static final String CASE_HR_DESRIPTION = "Case first case started";
    private static final String CASE_HR_NAME = "Simple Case with User Tasks";
    private static final String CASE_HR_VERSION = "1.0";

    private static final String DATA_VERIFICATION_DEF_ID = "DataVerification";
    private static final String USER_TASK_DEF_ID = "UserTask";

    private static final String HELLO_1_TASK = "Hello1";
    private static final String HELLO_2_TASK = "Hello2";

    private static final String SUBMIT_POLICE_REPORT_TASK = "Submit police report";

    @BeforeClass
    public static void buildAndDeployArtifacts() {

        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/case-insurance").getFile());

        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);

        createContainer(CONTAINER_ID, releaseId);
    }

    @Override
    protected void addExtraCustomClasses(Map<String, Class<?>> extraClasses) throws Exception {
        extraClasses.put(CLAIM_REPORT_CLASS_NAME, Class.forName(CLAIM_REPORT_CLASS_NAME, true, kieContainer.getClassLoader()));
        extraClasses.put(PROPERTY_DAMAGE_REPORT_CLASS_NAME, Class.forName(PROPERTY_DAMAGE_REPORT_CLASS_NAME, true, kieContainer.getClassLoader()));
    }

    @After
    public void resetUser() throws Exception {
        changeUser(TestConfig.getUsername());
    }

    @Test
    public void testGetCaseDefinitionsByNotExistingContainer() {
        List<CaseDefinition> definitions = caseClient.getCaseDefinitionsByContainer("not-existing-container", 0, 10);
        assertThat(definitions).isNotNull();
        assertThat(definitions).isEmpty();
    }

    @Test
    public void testGetCaseDefinitionsByContainer() {
        List<CaseDefinition> definitions = caseClient.getCaseDefinitionsByContainer(CONTAINER_ID, 0, 10);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(2);

        List<String> mappedDefinitions = definitions.stream().map(CaseDefinition::getIdentifier).collect(Collectors.toList());
        assertThat(mappedDefinitions.contains(CASE_HR_DEF_ID)).isTrue();
        assertThat(mappedDefinitions.contains(CLAIM_CASE_DEF_ID)).isTrue();

        definitions = caseClient.getCaseDefinitionsByContainer(CONTAINER_ID, 0, 1);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertHrCaseDefinition(definitions.get(0));

        definitions = caseClient.getCaseDefinitionsByContainer(CONTAINER_ID, 1, 1);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertCarInsuranceCaseDefinition(definitions.get(0));
    }

    @Test
    public void testGetCaseDefinitionsByContainerSorting() {
        List<CaseDefinition> definitions = caseClient.getCaseDefinitionsByContainer(CONTAINER_ID, 0, 1, CaseServicesClient.SORT_BY_CASE_DEFINITION_ID, true);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertHrCaseDefinition(definitions.get(0));

        definitions = caseClient.getCaseDefinitionsByContainer(CONTAINER_ID, 1, 1, CaseServicesClient.SORT_BY_CASE_DEFINITION_ID, true);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertCarInsuranceCaseDefinition(definitions.get(0));

        definitions = caseClient.getCaseDefinitionsByContainer(CONTAINER_ID, 0, 10, CaseServicesClient.SORT_BY_CASE_DEFINITION_ID, false);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(2);
        assertCarInsuranceCaseDefinition(definitions.get(0));
        assertHrCaseDefinition(definitions.get(1));
    }

    @Test
    public void testGetCaseDefinitionNotExistingContainer() {
        try {
            caseClient.getCaseDefinition("not-existing-container", CASE_HR_DEF_ID);
            fail("Should have failed because of not existing container.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testGetCaseDefinitionNotExistingCase() {
        try {
            caseClient.getCaseDefinition(CONTAINER_ID, "not-existing-case");
            fail("Should have failed because of not existing case definition Id.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testGetCaseDefinition() {
        CaseDefinition hrCase = caseClient.getCaseDefinition(CONTAINER_ID, CASE_HR_DEF_ID);
        assertHrCaseDefinition(hrCase);
    }

    @Test
    public void testGetCaseDefinitions() {
        List<CaseDefinition> definitions = caseClient.getCaseDefinitions(0, 10);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(2);

        List<String> mappedDefinitions = definitions.stream().map(CaseDefinition::getIdentifier).collect(Collectors.toList());
        assertThat(mappedDefinitions.contains(CASE_HR_DEF_ID)).isTrue();
        assertThat(mappedDefinitions.contains(CLAIM_CASE_DEF_ID)).isTrue();

        definitions = caseClient.getCaseDefinitions(0, 1);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertHrCaseDefinition(definitions.get(0));

        definitions = caseClient.getCaseDefinitions(1, 1);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertCarInsuranceCaseDefinition(definitions.get(0));
    }

    @Test
    public void testGetCaseDefinitionsSorting() {
        List<CaseDefinition> definitions = caseClient.getCaseDefinitions(0, 1, CaseServicesClient.SORT_BY_CASE_DEFINITION_ID, true);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertHrCaseDefinition(definitions.get(0));

        definitions = caseClient.getCaseDefinitions(1, 1, CaseServicesClient.SORT_BY_CASE_DEFINITION_ID, true);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertCarInsuranceCaseDefinition(definitions.get(0));

        definitions = caseClient.getCaseDefinitions(0, 10, CaseServicesClient.SORT_BY_CASE_DEFINITION_ID, false);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(2);
        assertCarInsuranceCaseDefinition(definitions.get(0));
        assertHrCaseDefinition(definitions.get(1));
    }

    @Test
    public void testGetCaseDefinitionsSortingByCaseName() {
        List<CaseDefinition> definitions = caseClient.getCaseDefinitions(0, 10, CaseServicesClient.SORT_BY_CASE_DEFINITION_NAME, true);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(2);
        assertCarInsuranceCaseDefinition(definitions.get(0));
        assertHrCaseDefinition(definitions.get(1));
    }

    @Test
    public void testGetCaseDefinitionsSortingByDeploymentId() {
        createContainer(CONTAINER_ID2, releaseId);

        List<CaseDefinition> definitions = caseClient.getCaseDefinitions(0, 10, CaseServicesClient.SORT_BY_CASE_DEFINITION_DEPLOYMENT_ID, true);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(4);
        assertThat(CONTAINER_ID).isEqualTo(definitions.get(0).getContainerId());
        assertThat(CONTAINER_ID).isEqualTo(definitions.get(1).getContainerId());
        assertThat(CONTAINER_ID2).isEqualTo(definitions.get(2).getContainerId());
        assertThat(CONTAINER_ID2).isEqualTo(definitions.get(3).getContainerId());

        KieServerAssert.assertSuccess(client.disposeContainer(CONTAINER_ID2));
    }

    @Test
    public void testGetCaseDefinitionsWithFilter() {
        List<CaseDefinition> definitions = caseClient.getCaseDefinitions("User", 0, 10);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertHrCaseDefinition(definitions.get(0));

        definitions = caseClient.getCaseDefinitions("User", 1, 10);
        assertThat(definitions).isNotNull();
        assertThat(definitions).isEmpty();
    }

    @Test
    public void testGetCaseDefinitionsWithFilterSorting() {
        List<CaseDefinition> definitions = caseClient.getCaseDefinitions("Case", 0, 1, CaseServicesClient.SORT_BY_CASE_DEFINITION_ID, true);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertHrCaseDefinition(definitions.get(0));

        definitions = caseClient.getCaseDefinitions("Case", 1, 1, CaseServicesClient.SORT_BY_CASE_DEFINITION_ID, true);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertCarInsuranceCaseDefinition(definitions.get(0));

        definitions = caseClient.getCaseDefinitions("Case", 0, 10, CaseServicesClient.SORT_BY_CASE_DEFINITION_ID, false);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(2);
        assertCarInsuranceCaseDefinition(definitions.get(0));
        assertHrCaseDefinition(definitions.get(1));
    }

    @Test
    public void testGetCaseInstanceNotExistingContainer() {
        try {
            caseClient.getCaseInstance("not-existing-container", CASE_HR_DEF_ID);
            fail("Should have failed because of not existing container.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testGetCaseInstanceNotExistingCase() {
        try {
            caseClient.getCaseInstance(CONTAINER_ID, "not-existing-case");
            fail("Should have failed because of not existing case definition Id.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testGetCaseInstance() {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        CaseInstance caseInstance = caseClient.getCaseInstance(CONTAINER_ID, caseId);
        assertHrCaseInstance(caseInstance, caseId, USER_YODA);
        assertThat(caseInstance.getCaseFile()).isNull();
        assertThat(caseInstance.getRoleAssignments()).isNull();
        assertThat(caseInstance.getMilestones()).isNull();
        assertThat(caseInstance.getStages()).isNull();
    }

    @Test
    public void testGetCaseInstanceUserTaskCaseWithData() {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        caseClient.triggerAdHocFragment(CONTAINER_ID, caseId, "Milestone1", null);

        CaseInstance caseInstance = caseClient.getCaseInstance(CONTAINER_ID, caseId, true, true, true, true);
        assertHrCaseInstance(caseInstance, caseId, USER_YODA);

        KieServerAssert.assertNullOrEmpty("Stages should be empty.", caseInstance.getStages());

        // Assert case file
        assertThat(caseInstance.getCaseFile()).isNotNull();
        assertThat(caseInstance.getCaseFile().getData().get("s")).isEqualTo("first case started");

        // Assert role assignments
        assertThat(caseInstance.getRoleAssignments()).isNotNull();
        assertThat(caseInstance.getRoleAssignments()).hasSize(3);

        CaseRoleAssignment ownerRole = caseInstance.getRoleAssignments().get(0);
        assertThat(ownerRole.getName()).isEqualTo("owner");
        assertThat(ownerRole.getUsers()).hasSize(1);
        assertThat(ownerRole.getUsers().get(0)).isEqualTo(USER_YODA);
        KieServerAssert.assertNullOrEmpty("Groups should be empty.", ownerRole.getGroups());

        CaseRoleAssignment contactRole = caseInstance.getRoleAssignments().get(1);
        assertThat(contactRole.getName()).isEqualTo("contact");
        assertThat(contactRole.getUsers()).hasSize(1);
        assertThat(contactRole.getUsers().get(0)).isEqualTo(USER_JOHN);
        KieServerAssert.assertNullOrEmpty("Groups should be empty.", contactRole.getGroups());

        CaseRoleAssignment participantRole = caseInstance.getRoleAssignments().get(2);
        assertThat(participantRole.getName()).isEqualTo("participant");
        KieServerAssert.assertNullOrEmpty("Users should be empty.", participantRole.getUsers());
        KieServerAssert.assertNullOrEmpty("Groups should be empty.", participantRole.getGroups());

        // Assert milestones
        assertThat(caseInstance.getMilestones()).isNotNull();
        assertThat(caseInstance.getMilestones()).hasSize(2);

        CaseMilestone milestone = caseInstance.getMilestones().get(0);
        assertThat(milestone.getIdentifier()).isEqualTo("2");
        assertThat(milestone.getName()).isEqualTo("Milestone1");
        assertThat(milestone.getStatus()).isEqualTo("Completed");
        assertThat(milestone.getAchievedAt()).isNotNull();
        assertThat(milestone.isAchieved()).isTrue();

        milestone = caseInstance.getMilestones().get(1);
        assertThat(milestone.getIdentifier()).isNotNull();
        assertThat(milestone.getName()).isEqualTo("Milestone2");
        assertThat(milestone.getStatus()).isEqualTo("Available");
        assertThat(milestone.getAchievedAt()).isNull();
        assertThat(milestone.isAchieved()).isFalse();
    }

    @Test
    public void testGetCaseInstanceCarInsuranceClaimCaseWithData() {
        String caseId = startCarInsuranceClaimCase(USER_YODA, USER_JOHN, USER_YODA);

        CaseInstance caseInstance = caseClient.getCaseInstance(CONTAINER_ID, caseId, true, true, true, true);
        assertCarInsuranceCaseInstance(caseInstance, caseId, USER_YODA);

        KieServerAssert.assertNullOrEmpty("Milestones should be empty.", caseInstance.getMilestones());

        // Assert case file
        assertThat(caseInstance.getCaseFile()).isNotNull();
        assertThat(caseInstance.getCaseFile().getData().get("s")).isEqualTo("first case started");

        // Assert role assignments
        assertThat(caseInstance.getRoleAssignments()).isNotNull();
        assertThat(caseInstance.getRoleAssignments()).hasSize(4);

        CaseRoleAssignment ownerRole = caseInstance.getRoleAssignments().get(0);
        assertThat(ownerRole.getName()).isEqualTo("owner");
        assertThat(ownerRole.getUsers()).hasSize(1);
        assertThat(ownerRole.getUsers().get(0)).isEqualTo(USER_YODA);
        KieServerAssert.assertNullOrEmpty("Groups should be empty.", ownerRole.getGroups());

        CaseRoleAssignment insuredRole = caseInstance.getRoleAssignments().get(1);
        assertThat(insuredRole.getName()).isEqualTo("insured");
        assertThat(insuredRole.getUsers()).hasSize(1);
        assertThat(insuredRole.getUsers().get(0)).isEqualTo(USER_YODA);
        KieServerAssert.assertNullOrEmpty("Groups should be empty.", insuredRole.getGroups());

        CaseRoleAssignment assessorRole = caseInstance.getRoleAssignments().get(2);
        assertThat(assessorRole.getName()).isEqualTo("assessor");
        assertThat(assessorRole.getUsers().get(0)).isEqualTo(USER_YODA);
        KieServerAssert.assertNullOrEmpty("Groups should be empty.", assessorRole.getGroups());

        CaseRoleAssignment insuranceRepresentativeRole = caseInstance.getRoleAssignments().get(3);
        assertThat(insuranceRepresentativeRole.getName()).isEqualTo("insuranceRepresentative");
        assertThat(insuranceRepresentativeRole.getUsers()).hasSize(1);
        assertThat(insuranceRepresentativeRole.getUsers().get(0)).isEqualTo(USER_JOHN);
        KieServerAssert.assertNullOrEmpty("Groups should be empty.", insuranceRepresentativeRole.getGroups());

        // Assert stages
        assertThat(caseInstance.getStages()).isNotNull();
        assertThat(caseInstance.getStages()).hasSize(1);

        CaseStage stage = caseInstance.getStages().get(0);
        assertThat(stage.getIdentifier()).isNotNull();
        assertThat(stage.getName()).isEqualTo("Build claim report");
        assertThat(stage.getStatus()).isEqualTo("Active");

        List<NodeInstance> activeNodes = stage.getActiveNodes();
        assertThat(activeNodes).hasSize(1);
        assertThat(activeNodes.get(0).getName()).isEqualTo("Provide accident information");
        assertThat(activeNodes.get(0).getNodeType()).isEqualTo("HumanTaskNode");

        assertThat(stage.getAdHocFragments()).hasSize(2);
    }

    @Test
    public void testGetCaseInstances() {
        List<CaseInstance> caseInstances = caseClient.getCaseInstances(0, 10);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).isEmpty();

        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);
        String caseId2 = startCarInsuranceClaimCase(USER_YODA, USER_JOHN, USER_YODA);
        assertNotEquals(caseId, caseId2);

        caseInstances = caseClient.getCaseInstances(0, 10);
        assertThat(caseInstances).hasSize(2);

        List<String> mappedInstances = caseInstances.stream().map(CaseInstance::getCaseId).collect(Collectors.toList());
        assertThat(mappedInstances.contains(caseId)).isTrue();
        assertThat(mappedInstances.contains(caseId2)).isTrue();

        caseInstances = caseClient.getCaseInstances(0, 1);
        assertThat(caseInstances).hasSize(1);
        assertCarInsuranceCaseInstance(caseInstances.get(0), caseId2, USER_YODA);

        caseInstances = caseClient.getCaseInstances(1, 1);
        assertThat(caseInstances).hasSize(1);
        assertHrCaseInstance(caseInstances.get(0), caseId, USER_YODA);
    }

    @Test
    public void testGetCaseInstancesSorting() {
        String hrCaseId = startUserTaskCase(USER_YODA, USER_JOHN);
        String claimCaseId = startCarInsuranceClaimCase(USER_YODA, USER_JOHN, USER_YODA);
        assertNotEquals(hrCaseId, claimCaseId);

        List<CaseInstance> caseInstances = caseClient.getCaseInstances(0, 1, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, true);
        assertThat(caseInstances).hasSize(1);
        assertThat(caseInstances.get(0).getCaseId()).isEqualTo(claimCaseId);

        caseInstances = caseClient.getCaseInstances(1, 1, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, true);
        assertThat(caseInstances).hasSize(1);
        assertThat(caseInstances.get(0).getCaseId()).isEqualTo(hrCaseId);

        caseInstances = caseClient.getCaseInstances(0, 10, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, false);
        assertThat(caseInstances).hasSize(2);
        assertThat(caseInstances.get(0).getCaseId()).isEqualTo(hrCaseId);
        assertThat(caseInstances.get(1).getCaseId()).isEqualTo(claimCaseId);
    }

    @Test
    public void testGetCaseInstancesByStatus() {
        List<CaseInstance> caseInstances = caseClient.getCaseInstances(Arrays.asList(CaseStatus.CANCELLED.getName()), 0, 1000);
        assertThat(caseInstances).isNotNull();
        int abortedCaseInstanceCount = caseInstances.size();

        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        caseInstances = caseClient.getCaseInstances(Arrays.asList(CaseStatus.OPEN.getName()), 0, 10);
        assertThat(caseInstances).hasSize(1);
        assertHrCaseInstance(caseInstances.get(0), caseId, USER_YODA);

        caseClient.cancelCaseInstance(CONTAINER_ID, caseId);

        caseInstances = caseClient.getCaseInstances(Arrays.asList(CaseStatus.CANCELLED.getName()), 0, 1000);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances.size()).isEqualTo(abortedCaseInstanceCount + 1);
    }

    @Test
    public void testGetCaseInstancesByStatusSorting() {
        String hrCaseId = startUserTaskCase(USER_YODA, USER_JOHN);
        String claimCaseId = startCarInsuranceClaimCase(USER_YODA, USER_JOHN, USER_YODA);
        assertNotEquals(hrCaseId, claimCaseId);

        List<CaseInstance> caseInstances = caseClient.getCaseInstances(Arrays.asList(CaseStatus.OPEN.getName()), 0, 1, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, true);
        assertThat(caseInstances).hasSize(1);
        assertThat(caseInstances.get(0).getCaseId()).isEqualTo(claimCaseId);

        caseInstances = caseClient.getCaseInstances(Arrays.asList(CaseStatus.OPEN.getName()), 1, 1, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, true);
        assertThat(caseInstances).hasSize(1);
        assertThat(caseInstances.get(0).getCaseId()).isEqualTo(hrCaseId);

        caseInstances = caseClient.getCaseInstances(Arrays.asList(CaseStatus.OPEN.getName()), 0, 10, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, false);
        assertThat(caseInstances).hasSize(2);
        assertThat(caseInstances.get(0).getCaseId()).isEqualTo(hrCaseId);
        assertThat(caseInstances.get(1).getCaseId()).isEqualTo(claimCaseId);
    }

    @Test
    public void testGetCaseInstancesByNotExistingContainer() {
        List<CaseInstance> caseInstances = caseClient.getCaseInstancesByContainer("not-existing-container", Arrays.asList(CaseStatus.OPEN.getName()), 0, 10);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).isEmpty();
    }

    @Test
    public void testGetCaseInstancesByContainer() {
        List<CaseInstance> caseInstances = caseClient.getCaseInstancesByContainer(CONTAINER_ID, Arrays.asList(CaseStatus.OPEN.getName()), 0, 10);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).isEmpty();

        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);
        String caseId2 = startCarInsuranceClaimCase(USER_YODA, USER_JOHN, USER_YODA);

        caseInstances = caseClient.getCaseInstancesByContainer(CONTAINER_ID, Arrays.asList(CaseStatus.OPEN.getName()), 0, 1);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).hasSize(1);
        assertCarInsuranceCaseInstance(caseInstances.get(0), caseId2, USER_YODA);

        caseInstances = caseClient.getCaseInstancesByContainer(CONTAINER_ID, Arrays.asList(CaseStatus.OPEN.getName()), 1, 1);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).hasSize(1);
        assertHrCaseInstance(caseInstances.get(0), caseId, USER_YODA);
    }

    @Test
    public void testGetCaseInstancesByContainerSorting() {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);
        String caseId2 = startCarInsuranceClaimCase(USER_YODA, USER_JOHN, USER_YODA);

        List<CaseInstance> caseInstances = caseClient.getCaseInstancesByContainer(CONTAINER_ID, Arrays.asList(CaseStatus.OPEN.getName()), 0, 1, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, true);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).hasSize(1);
        assertCarInsuranceCaseInstance(caseInstances.get(0), caseId2, USER_YODA);

        caseInstances = caseClient.getCaseInstancesByContainer(CONTAINER_ID, Arrays.asList(CaseStatus.OPEN.getName()), 1, 1, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, true);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).hasSize(1);
        assertHrCaseInstance(caseInstances.get(0), caseId, USER_YODA);

        caseInstances = caseClient.getCaseInstancesByContainer(CONTAINER_ID, Arrays.asList(CaseStatus.OPEN.getName()), 0, 10, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, false);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).hasSize(2);
        assertHrCaseInstance(caseInstances.get(0), caseId, USER_YODA);
        assertCarInsuranceCaseInstance(caseInstances.get(1), caseId2, USER_YODA);
    }

    @Test
    public void testGetCaseInstancesByDefinitionNotExistingContainer() {
        List<CaseInstance> caseInstances = caseClient.getCaseInstancesByDefinition("not-existing-container", CASE_HR_DEF_ID, Arrays.asList(CaseStatus.OPEN.getName()), 0, 10);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).isEmpty();
    }

    @Test
    public void testGetCaseInstancesByNotExistingDefinition() {
        List<CaseInstance> caseInstances = caseClient.getCaseInstancesByDefinition(CONTAINER_ID, "not-existing-case", Arrays.asList(CaseStatus.OPEN.getName()), 0, 10);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).isEmpty();
    }

    @Test
    public void testGetCaseInstancesByDefinition() {
        List<CaseInstance> caseInstances = caseClient.getCaseInstancesByDefinition(CONTAINER_ID, CASE_HR_DEF_ID, Arrays.asList(CaseStatus.OPEN.getName()), 0, 10);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).isEmpty();

        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);
        String caseId2 = startCarInsuranceClaimCase(USER_JOHN, USER_YODA, USER_YODA);

        caseInstances = caseClient.getCaseInstancesByDefinition(CONTAINER_ID, CASE_HR_DEF_ID, Arrays.asList(CaseStatus.OPEN.getName()), 0, 1);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).hasSize(1);
        assertHrCaseInstance(caseInstances.get(0), caseId, USER_YODA);

        caseInstances = caseClient.getCaseInstancesByDefinition(CONTAINER_ID, CLAIM_CASE_DEF_ID, Arrays.asList(CaseStatus.OPEN.getName()), 0, 1);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).hasSize(1);
        assertCarInsuranceCaseInstance(caseInstances.get(0), caseId2, USER_YODA);

        caseInstances = caseClient.getCaseInstancesByDefinition(CONTAINER_ID, CLAIM_CASE_DEF_ID, Arrays.asList(CaseStatus.OPEN.getName()), 1, 1);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).isEmpty();
    }

    @Test
    public void testGetCaseInstancesByDefinitionSorting() {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);
        String caseId2 = startUserTaskCase(USER_YODA, USER_JOHN);

        List<CaseInstance> caseInstances = caseClient.getCaseInstancesByDefinition(CONTAINER_ID, CASE_HR_DEF_ID, Arrays.asList(CaseStatus.OPEN.getName()), 0, 1, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, true);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).hasSize(1);
        assertHrCaseInstance(caseInstances.get(0), caseId, USER_YODA);

        caseInstances = caseClient.getCaseInstancesByDefinition(CONTAINER_ID, CASE_HR_DEF_ID, Arrays.asList(CaseStatus.OPEN.getName()), 1, 1, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, true);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).hasSize(1);
        assertHrCaseInstance(caseInstances.get(0), caseId2, USER_YODA);

        caseInstances = caseClient.getCaseInstancesByDefinition(CONTAINER_ID, CASE_HR_DEF_ID, Arrays.asList(CaseStatus.OPEN.getName()), 0, 10, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, false);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).hasSize(2);
        assertHrCaseInstance(caseInstances.get(0), caseId2, USER_YODA);
        assertHrCaseInstance(caseInstances.get(1), caseId, USER_YODA);
    }

    @Test
    public void testGetCaseInstancesOwnedByNotExistingUser() throws Exception {
        List<CaseInstance> caseInstances = caseClient.getCaseInstancesOwnedBy("not-existing-user", Arrays.asList(CaseStatus.OPEN.getName()), 0, 10);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).isEmpty();
    }

    @Test
    public void testGetCaseInstancesOwnedBy() throws Exception {
        // Test is using user authentication, isn't available for local execution(which has mocked authentication info).
        Assume.assumeFalse(TestConfig.isLocalServer());

        List<CaseInstance> caseInstances = caseClient.getCaseInstancesOwnedBy(USER_YODA, Arrays.asList(CaseStatus.OPEN.getName()), 0, 10);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).isEmpty();

        try {
            changeUser(USER_YODA);
            String caseId = startUserTaskCase(USER_YODA, USER_JOHN);
            changeUser(USER_JOHN);
            String caseId2 = startCarInsuranceClaimCase(USER_JOHN, USER_YODA, USER_YODA);

            changeUser(USER_YODA);
            caseInstances = caseClient.getCaseInstancesOwnedBy(USER_YODA, Arrays.asList(CaseStatus.OPEN.getName()), 0, 1);
            assertThat(caseInstances).isNotNull();
            assertThat(caseInstances).hasSize(1);
            assertHrCaseInstance(caseInstances.get(0), caseId, USER_YODA);

            caseInstances = caseClient.getCaseInstancesOwnedBy(USER_YODA, Arrays.asList(CaseStatus.OPEN.getName()), 1, 1);
            assertThat(caseInstances).isNotNull();
            assertThat(caseInstances).isEmpty();

            changeUser(USER_JOHN);
            caseInstances = caseClient.getCaseInstancesOwnedBy(USER_JOHN, Arrays.asList(CaseStatus.OPEN.getName()), 0, 10);
            assertThat(caseInstances).isNotNull();
            assertThat(caseInstances).hasSize(1);
            assertCarInsuranceCaseInstance(caseInstances.get(0), caseId2, USER_JOHN);
        } finally {
            changeUser(TestConfig.getUsername());
        }
    }

    @Test
    public void testGetCaseInstancesOwnedBySorting() throws Exception {
        String hrCaseId = startUserTaskCase(USER_YODA, USER_JOHN);
        String claimCaseId = startCarInsuranceClaimCase(USER_JOHN, USER_YODA, USER_YODA);

        List<CaseInstance> caseInstances = caseClient.getCaseInstancesOwnedBy(USER_YODA, Arrays.asList(CaseStatus.OPEN.getName()), 0, 1, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, true);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).hasSize(1);
        assertCarInsuranceCaseInstance(caseInstances.get(0), claimCaseId, USER_YODA);

        caseInstances = caseClient.getCaseInstancesOwnedBy(USER_YODA, Arrays.asList(CaseStatus.OPEN.getName()), 1, 1, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, true);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).hasSize(1);
        assertHrCaseInstance(caseInstances.get(0), hrCaseId, USER_YODA);

        caseInstances = caseClient.getCaseInstancesOwnedBy(USER_YODA, Arrays.asList(CaseStatus.OPEN.getName()), 0, 10, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, false);
        assertThat(caseInstances).isNotNull();
        assertThat(caseInstances).hasSize(2);
        assertHrCaseInstance(caseInstances.get(0), hrCaseId, USER_YODA);
        assertCarInsuranceCaseInstance(caseInstances.get(1), claimCaseId, USER_YODA);
    }

    @Test
    public void testAdHocFragments() {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        List<CaseAdHocFragment> caseAdHocFragments = caseClient.getAdHocFragments(CONTAINER_ID, caseId);
        assertThat(caseAdHocFragments).hasSize(3);

        Map<String, CaseAdHocFragment> mappedAdHocFragments = caseAdHocFragments.stream().collect(Collectors.toMap(CaseAdHocFragment::getName, d -> d));
        assertThat(mappedAdHocFragments.containsKey("Milestone1")).isTrue();
        assertThat(mappedAdHocFragments.containsKey("Milestone2")).isTrue();
        assertThat(mappedAdHocFragments.containsKey("Hello2")).isTrue();
        assertThat(mappedAdHocFragments.get("Milestone1").getType()).isEqualTo("MilestoneNode");
        assertThat(mappedAdHocFragments.get("Milestone2").getType()).isEqualTo("MilestoneNode");
        assertThat(mappedAdHocFragments.get("Hello2").getType()).isEqualTo("HumanTaskNode");
    }

    @Test
    public void testAdHocFragmentsNotExistingCase() {
        try {
            caseClient.getAdHocFragments(CONTAINER_ID, "not-existing-case");
            fail("Should have failed because of non existing case Id.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testAdHocFragmentsNotExistingContainer() {
        try {
            caseClient.getAdHocFragments("not-existing-container", CASE_HR_DEF_ID);
            fail("Should have failed because of not existing container.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testGetProcessInstances() {
        String userTaskCase = startUserTaskCase(USER_YODA, USER_JOHN);
        String carInsuranceClaimCase = startCarInsuranceClaimCase(USER_JOHN, USER_YODA, USER_YODA);

        List<ProcessInstance> processInstances = caseClient.getProcessInstances(CONTAINER_ID, userTaskCase, Arrays.asList(STATE_ACTIVE), 0, 1);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).hasSize(1);
        assertHrProcessInstance(processInstances.get(0), userTaskCase);

        processInstances = caseClient.getProcessInstances(CONTAINER_ID, userTaskCase, Arrays.asList(STATE_ACTIVE), 1, 1);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).isEmpty();

        processInstances = caseClient.getProcessInstances(CONTAINER_ID, carInsuranceClaimCase, Arrays.asList(STATE_ACTIVE), 0, 10);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).hasSize(1);
        assertCarInsuranceProcessInstance(processInstances.get(0), carInsuranceClaimCase);

        caseClient.cancelCaseInstance(CONTAINER_ID, userTaskCase);

        processInstances = caseClient.getProcessInstances(CONTAINER_ID, userTaskCase, Arrays.asList(STATE_ACTIVE), 0, 10);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).isEmpty();

        processInstances = caseClient.getProcessInstances(CONTAINER_ID, userTaskCase, Arrays.asList(STATE_ABORTED), 0, 10);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).hasSize(1);
        assertHrProcessInstance(processInstances.get(0), userTaskCase, STATE_ABORTED);
    }

    @Test
    public void testGetProcessInstancesNotExistingCase() {
        List<ProcessInstance> processInstances = caseClient.getProcessInstances(CONTAINER_ID, "not-existing-case", Arrays.asList(STATE_ACTIVE), 0, 10);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).isEmpty();
    }

    @Test
    public void testGetProcessInstancesNotExistingContainer() {
        List<ProcessInstance> processInstances = caseClient.getProcessInstances("not-existing-container", CASE_HR_DEF_ID, Arrays.asList(STATE_ACTIVE), 0, 10);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).isEmpty();
    }

    @Test
    public void testGetProcessInstancesSorting() {
        String carInsuranceClaimCase = startCarInsuranceClaimCase(USER_JOHN, USER_YODA, USER_YODA);
        caseClient.addDynamicSubProcess(CONTAINER_ID, carInsuranceClaimCase, DATA_VERIFICATION_DEF_ID, new HashMap<>());

        List<ProcessInstance> processInstances = caseClient.getProcessInstances(CONTAINER_ID, carInsuranceClaimCase, Arrays.asList(STATE_ACTIVE, STATE_COMPLETED), 0, 1, CaseServicesClient.SORT_BY_PROCESS_INSTANCE_ID, true);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).hasSize(1);
        assertThat(CLAIM_CASE_DEF_ID).isEqualTo(processInstances.get(0).getProcessId());

        processInstances = caseClient.getProcessInstances(CONTAINER_ID, carInsuranceClaimCase, Arrays.asList(STATE_ACTIVE, STATE_COMPLETED), 1, 1, CaseServicesClient.SORT_BY_PROCESS_INSTANCE_ID, true);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).hasSize(1);
        assertThat(DATA_VERIFICATION_DEF_ID).isEqualTo(processInstances.get(0).getProcessId());

        processInstances = caseClient.getProcessInstances(CONTAINER_ID, carInsuranceClaimCase, Arrays.asList(STATE_ACTIVE, STATE_COMPLETED), 0, 10, CaseServicesClient.SORT_BY_PROCESS_INSTANCE_ID, false);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).hasSize(2);
        assertThat(DATA_VERIFICATION_DEF_ID).isEqualTo(processInstances.get(0).getProcessId());
        assertThat(CLAIM_CASE_DEF_ID).isEqualTo(processInstances.get(1).getProcessId());
    }

    @Test
    public void testGetActiveProcessInstances() {
        String userTaskCase = startUserTaskCase(USER_YODA, USER_JOHN);
        String carInsuranceClaimCase = startCarInsuranceClaimCase(USER_JOHN, USER_YODA, USER_YODA);

        List<ProcessInstance> processInstances = caseClient.getActiveProcessInstances(CONTAINER_ID, userTaskCase, 0, 1);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).hasSize(1);
        assertHrProcessInstance(processInstances.get(0), userTaskCase);

        processInstances = caseClient.getActiveProcessInstances(CONTAINER_ID, userTaskCase, 1, 1);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).isEmpty();

        processInstances = caseClient.getActiveProcessInstances(CONTAINER_ID, carInsuranceClaimCase, 0, 10);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).hasSize(1);
        assertCarInsuranceProcessInstance(processInstances.get(0), carInsuranceClaimCase);

        caseClient.cancelCaseInstance(CONTAINER_ID, userTaskCase);

        processInstances = caseClient.getActiveProcessInstances(CONTAINER_ID, userTaskCase, 0, 1);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).isEmpty();
    }

    @Test
    public void testGetActiveProcessInstancesNotExistingCase() {
        List<ProcessInstance> processInstances = caseClient.getActiveProcessInstances(CONTAINER_ID, "not-existing-case", 0, 10);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).isEmpty();
    }

    @Test
    public void testGetActiveProcessInstancesNotExistingContainer() {
        List<ProcessInstance> processInstances = caseClient.getActiveProcessInstances("not-existing-container", CASE_HR_DEF_ID, 0, 10);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).isEmpty();
    }

    @Test
    public void testGetActiveProcessInstancesSorting() {
        String caseId = startCarInsuranceClaimCase(USER_JOHN, USER_YODA, USER_YODA);
        caseClient.addDynamicSubProcess(CONTAINER_ID, caseId, USER_TASK_DEF_ID, Collections.emptyMap());

        List<ProcessInstance> processInstances = caseClient.getActiveProcessInstances(CONTAINER_ID, caseId, 0, 10,
                CaseServicesClient.SORT_BY_PROCESS_INSTANCE_ID, true);
        Assertions.assertThat(processInstances).isNotNull().hasSize(2);
        Assertions.assertThat(processInstances).extracting(ProcessInstance::getProcessId)
                .containsExactly(CLAIM_CASE_DEF_ID, USER_TASK_DEF_ID);

        processInstances = caseClient.getActiveProcessInstances(CONTAINER_ID, caseId, 0, 10,
                CaseServicesClient.SORT_BY_PROCESS_INSTANCE_ID, false);
        assertThat(processInstances).isNotNull();
        Assertions.assertThat(processInstances).isNotNull().hasSize(2);
        Assertions.assertThat(processInstances).extracting(ProcessInstance::getProcessId)
                .containsExactly(USER_TASK_DEF_ID, CLAIM_CASE_DEF_ID);

        processInstances = caseClient.getActiveProcessInstances(CONTAINER_ID, caseId, 0, 10,
                CaseServicesClient.SORT_BY_PROCESS_NAME, true);
        Assertions.assertThat(processInstances).isNotNull().hasSize(2);
        Assertions.assertThat(processInstances).extracting(ProcessInstance::getProcessId)
                .containsExactly(CLAIM_CASE_DEF_ID, USER_TASK_DEF_ID);
    }

    @Test
    public void testGetActiveProcessInstancesSortingNotExistingField() {
        String caseId = startCarInsuranceClaimCase(USER_JOHN, USER_YODA, USER_YODA);

        Assertions.assertThatThrownBy(() -> caseClient.getActiveProcessInstances(CONTAINER_ID, caseId, 0, 10,
                "xyz", true)).isInstanceOf(KieServicesException.class);
    }

    @Test
    public void testGetActiveProcessInstancesSortingNotExistingCase() {
        List<ProcessInstance> processInstances = caseClient.getActiveProcessInstances(CONTAINER_ID, "not-existing-case", 0, 10,
                CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, true);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).isEmpty();
    }

    @Test
    public void testGetActiveProcessInstancesSortingNotExistingContainer() {
        List<ProcessInstance> processInstances = caseClient.getActiveProcessInstances("not-existing-container",
                CASE_HR_DEF_ID, 0, 10, CaseServicesClient.SORT_BY_CASE_INSTANCE_ID, true);
        assertThat(processInstances).isNotNull();
        assertThat(processInstances).isEmpty();
    }

    @Test
    public void testCreateCaseWithCaseFileAndTriggerMilestones() {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        assertThat(caseId).isNotNull();
        assertThat(caseId.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        CaseInstance caseInstance = caseClient.getCaseInstance(CONTAINER_ID, caseId);
        assertHrCaseInstance(caseInstance, caseId, USER_YODA);

        List<CaseInstance> caseInstances = caseClient.getCaseInstancesOwnedBy(USER_YODA, null, 0, 10);
        assertThat(caseInstances).hasSize(1);

        List<CaseMilestone> milestones = caseClient.getMilestones(CONTAINER_ID, caseId, true, 0, 10);
        assertThat(milestones).isNotNull();
        assertThat(milestones).isEmpty();

        caseClient.triggerAdHocFragment(CONTAINER_ID, caseId, "Milestone1", null);

        milestones = caseClient.getMilestones(CONTAINER_ID, caseId, true, 0, 10);
        assertThat(milestones).isNotNull();
        assertThat(milestones).hasSize(1);

        CaseMilestone milestone = milestones.get(0);
        assertThat(milestone).isNotNull();
        assertThat(milestone.getName()).isEqualTo("Milestone1");
        assertThat(milestone.isAchieved()).isEqualTo(true);
        assertThat(milestone.getIdentifier()).isEqualTo("2");
        assertThat(milestone.getAchievedAt()).isNotNull();

        caseClient.triggerAdHocFragment(CONTAINER_ID, caseId, "Milestone2", null);
        milestones = caseClient.getMilestones(CONTAINER_ID, caseId, true, 0, 10);
        assertThat(milestones).isNotNull();
        assertThat(milestones).hasSize(1);

        milestones = caseClient.getMilestones(CONTAINER_ID, caseId, false, 0, 10);
        assertThat(milestones).isNotNull();
        assertThat(milestones).hasSize(2);

        Map<String, CaseMilestone> mappedMilestones = milestones.stream().collect(Collectors.toMap(CaseMilestone::getName, d -> d));
        assertThat(mappedMilestones.containsKey("Milestone1")).isTrue();
        assertThat(mappedMilestones.containsKey("Milestone2")).isTrue();

        assertThat(mappedMilestones.get("Milestone1").isAchieved()).isTrue();
        assertThat(mappedMilestones.get("Milestone2").isAchieved()).isFalse();

        caseInstances = caseClient.getCaseInstances(0, 10);
        assertThat(caseInstances).hasSize(1);

        assertHrCaseInstance(caseInstances.get(0), caseId, USER_YODA);

        // now auto complete milestone by inserting data
        caseClient.putCaseInstanceData(CONTAINER_ID, caseId, "dataComplete", true);
        milestones = caseClient.getMilestones(CONTAINER_ID, caseId, true, 0, 10);
        assertThat(milestones).isNotNull();
        assertThat(milestones).hasSize(2);
    }

    @Test
    public void testGetCaseMilestonesNotExistingContainer() {
        try {
            caseClient.getMilestones("not-existing-container", CASE_HR_DEF_ID, false, 0, 10);
            fail("Should have failed because of not existing case Id.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testGetCaseMilestonesNotExistingCase() {
        try {
            caseClient.getMilestones(CONTAINER_ID, "not-existing-case", false, 0, 10);
            fail("Should have failed because of not existing case definition Id.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testCreateCaseWithCaseFileAndDynamicActivities() {
        String caseId = startUserTaskCase(USER_JOHN, USER_YODA);

        assertThat(caseId).isNotNull();
        assertThat(caseId.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        List<NodeInstance> activeNodes = caseClient.getActiveNodes(CONTAINER_ID, caseId, 0, 10);
        assertThat(activeNodes).isNotNull();
        assertThat(activeNodes).hasSize(1);

        NodeInstance activeNode = activeNodes.get(0);
        assertThat(activeNode).isNotNull();
        assertThat(activeNode.getName()).isEqualTo("Hello1");

        List<NodeInstance> completedNodes = caseClient.getCompletedNodes(CONTAINER_ID, caseId, 0, 10);
        assertThat(completedNodes).isNotNull();
        assertThat(completedNodes).isEmpty();

        List<ProcessInstance> instances = caseClient.getActiveProcessInstances(CONTAINER_ID, caseId, 0, 10);
        assertThat(instances).isNotNull();
        assertThat(instances).hasSize(1);

        ProcessInstance pi = instances.get(0);
        assertThat(pi.getProcessId()).isEqualTo(CASE_HR_DEF_ID);
        assertThat(pi.getCorrelationKey()).isEqualTo(caseId);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("input", "text data");

        caseClient.addDynamicUserTask(CONTAINER_ID, caseId, "dynamic task", "simple description", USER_YODA, null, parameters);

        List<TaskSummary> tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
        assertThat(tasks).hasSize(1);
        TaskSummary task = tasks.get(0);
        assertThat(task.getName()).isEqualTo("dynamic task");
        assertThat(task.getDescription()).isEqualTo("simple description");
        assertThat(task.getStatus()).isEqualTo(Status.Reserved.toString());
        assertThat(task.getActualOwner()).isEqualTo(USER_YODA);

        activeNodes = caseClient.getActiveNodes(CONTAINER_ID, caseId, 0, 10);
        assertThat(activeNodes).isNotNull();
        assertThat(activeNodes).hasSize(2);

        List<String> nodeNames = activeNodes.stream().map(n -> n.getName()).collect(toList());
        assertThat(nodeNames.contains("[Dynamic] dynamic task")).isTrue();
        assertThat(nodeNames.contains("Hello1")).isTrue();

        taskClient.completeAutoProgress(CONTAINER_ID, task.getId(), USER_YODA, null);

        completedNodes = caseClient.getCompletedNodes(CONTAINER_ID, caseId, 0, 10);
        assertThat(completedNodes).isNotNull();
        assertThat(completedNodes).hasSize(1);

        NodeInstance completedNode = completedNodes.get(0);
        assertThat(completedNode).isNotNull();
        assertThat(completedNode.getName()).isEqualTo("[Dynamic] dynamic task");

        caseClient.addDynamicSubProcess(CONTAINER_ID, caseId, "DataVerification", parameters);

        instances = caseClient.getProcessInstances(CONTAINER_ID, caseId, Arrays.asList(1, 2, 3), 0, 10);
        assertThat(instances).isNotNull();
        assertThat(instances).hasSize(2);
    }

    @Test
    public void testCreateCaseWithCaseFileWithComments() {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        assertThat(caseId).isNotNull();
        assertThat(caseId.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        List<CaseComment> comments = caseClient.getComments(CONTAINER_ID, caseId, 0, 10);
        assertThat(comments).isNotNull();
        assertThat(comments).isEmpty();

        String commentId = caseClient.addComment(CONTAINER_ID, caseId, USER_YODA, "first comment");
        assertThat(commentId).isNotNull();
        
        comments = caseClient.getComments(CONTAINER_ID, caseId, 0, 10);
        assertThat(comments).isNotNull();
        assertThat(comments).hasSize(1);

        CaseComment comment = comments.get(0);
        assertThat(comment).isNotNull();
        assertThat(comment.getAuthor()).isEqualTo(USER_YODA);
        assertThat(comment.getText()).isEqualTo("first comment");
        assertThat(comment.getAddedAt()).isNotNull();
        assertThat(comment.getId()).isNotNull();
        
        assertThat(comment.getId()).isEqualTo(commentId);

        caseClient.updateComment(CONTAINER_ID, caseId, comment.getId(), USER_YODA, "updated comment");
        comments = caseClient.getComments(CONTAINER_ID, caseId, 0, 10);
        assertThat(comments).isNotNull();
        assertThat(comments).hasSize(1);

        comment = comments.get(0);
        assertThat(comment).isNotNull();
        assertThat(comment.getAuthor()).isEqualTo(USER_YODA);
        assertThat(comment.getText()).isEqualTo("updated comment");
        assertThat(comment.getAddedAt()).isNotNull();
        assertThat(comment.getId()).isNotNull();

        caseClient.removeComment(CONTAINER_ID, caseId, comment.getId());

        comments = caseClient.getComments(CONTAINER_ID, caseId, 0, 10);
        assertThat(comments).isNotNull();
        assertThat(comments).isEmpty();
    }

    @Test
    public void testGetCommentPagination() {
        int pageSize = 20;

        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);
        assertThat(caseId).isNotNull();
        assertThat(caseId.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        for (int i = 0; i < 55; i++) {
            caseClient.addComment(CONTAINER_ID, caseId, USER_YODA, "comment" + i);
        }

        List<CaseComment> firstPage = caseClient.getComments(CONTAINER_ID, caseId, 0, pageSize);
        assertThat(firstPage).isNotNull();
        assertThat(firstPage).hasSize(20);
        Iterator<CaseComment> firstPageIter = firstPage.iterator();
        for (int i = 0; firstPageIter.hasNext(); i++) {
            assertComment(firstPageIter.next(), USER_YODA, "comment" + i);
        }

        List<CaseComment> secondPage = caseClient.getComments(CONTAINER_ID, caseId, 1, pageSize);
        assertThat(secondPage).isNotNull();
        assertThat(secondPage).hasSize(20);
        Iterator<CaseComment> secondPageIter = secondPage.iterator();
        for (int i = 20; secondPageIter.hasNext(); i++) {
            assertComment(secondPageIter.next(), USER_YODA, "comment" + i);
        }

        List<CaseComment> thirdPage = caseClient.getComments(CONTAINER_ID, caseId, 2, pageSize);
        assertThat(thirdPage).isNotNull();
        assertThat(thirdPage).hasSize(15);
        Iterator<CaseComment> thirdPageIter = thirdPage.iterator();
        for (int i = 40; thirdPageIter.hasNext(); i++) {
            assertComment(thirdPageIter.next(), USER_YODA, "comment" + i);
        }
    }

    @Test
    public void testGetCaseCommentsNotExistingContainer() {
        try {
            caseClient.getComments("not-existing-container", CASE_HR_DEF_ID, 0, 10);
            fail("Should have failed because of not existing container.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testGetCaseCommentsNotExistingCase() {
        try {
            caseClient.getComments(CONTAINER_ID, "not-existing-case", 0, 10);
            fail("Should have failed because of not existing case definition Id.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testAddCaseCommentNotExistingContainer() {
        try {
            caseClient.addComment("not-existing-container", CASE_HR_DEF_ID, USER_YODA, "Random comment.");
            fail("Should have failed because of not existing container.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testAddCaseCommentNotExistingCase() {
        try {
            caseClient.addComment(CONTAINER_ID, "not-existing-case", USER_YODA, "Random comment.");
            fail("Should have failed because of not existing case definition Id.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testUpdateCaseCommentNotExistingContainer() {
        try {
            caseClient.updateComment("not-existing-container", CASE_HR_DEF_ID, "random-id", USER_YODA, "Random comment.");
            fail("Should have failed because of not existing container.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testUpdateCaseCommentNotExistingCase() {
        try {
            caseClient.updateComment(CONTAINER_ID, "not-existing-case", "random-id", USER_YODA, "Random comment.");
            fail("Should have failed because of not existing case definition Id.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testUpdateNotExistingCaseComment() {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        try {
            caseClient.updateComment(CONTAINER_ID, caseId, "not-existing-id", USER_YODA, "Random comment.");
            fail("Should have failed because of not existing comment Id.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testRemoveCaseCommentNotExistingContainer() {
        try {
            caseClient.removeComment("not-existing-container", CASE_HR_DEF_ID, "random-id");
            fail("Should have failed because of not existing container.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testRemoveCaseCommentNotExistingCase() {
        try {
            caseClient.removeComment(CONTAINER_ID, "not-existing-case", "random-id");
            fail("Should have failed because of not existing case definition Id.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testRemoveNotExistingCaseComment() {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        try {
            caseClient.removeComment(CONTAINER_ID, caseId, "not-existing-id");
            fail("Should have failed because of not existing comment Id.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testCreateDifferentTypesCases() {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        String caseClaimId = startCarInsuranceClaimCase(USER_JOHN, USER_YODA, USER_YODA);

        assertThat(caseId).isNotNull();
        assertThat(caseId.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        assertThat(caseClaimId).isNotNull();
        assertThat(caseClaimId.startsWith(CLAIM_CASE_ID_PREFIX)).isTrue();

        List<CaseInstance> caseInstances = caseClient.getCaseInstancesByContainer(CONTAINER_ID, Arrays.asList(CaseStatus.OPEN.getName()), 0, 10);
        assertThat(caseInstances).hasSize(2);

        List<String> caseDefs = caseInstances.stream().map(c -> c.getCaseDefinitionId()).collect(toList());
        assertThat(caseDefs.contains(CASE_HR_DEF_ID)).isTrue();
        assertThat(caseDefs.contains(CLAIM_CASE_DEF_ID)).isTrue();

        caseInstances = caseClient.getCaseInstancesByDefinition(CONTAINER_ID, CLAIM_CASE_DEF_ID, Arrays.asList(CaseStatus.OPEN.getName()), 0, 10);
        assertThat(caseInstances).hasSize(1);

        List<CaseStage> stages = caseClient.getStages(CONTAINER_ID, caseClaimId, true, 0, 10);
        assertThat(stages).hasSize(1);
        CaseStage caseStage = stages.get(0);
        assertThat(caseStage.getName()).isEqualTo("Build claim report");
        assertThat(caseStage.getAdHocFragments()).hasSize(2);

        List<CaseRoleAssignment> roles = caseClient.getRoleAssignments(CONTAINER_ID, caseClaimId);
        assertThat(roles).hasSize(4);

        Map<String, CaseRoleAssignment> mappedRoles = roles.stream().collect(toMap(CaseRoleAssignment::getName, r -> r));
        assertThat(mappedRoles.containsKey(CASE_OWNER_ROLE)).isTrue();
        assertThat(mappedRoles.containsKey(CASE_INSURED_ROLE)).isTrue();
        assertThat(mappedRoles.containsKey(CASE_INS_REP_ROLE)).isTrue();
        assertThat(mappedRoles.containsKey(CASE_ASSESSOR_ROLE)).isTrue();

        CaseRoleAssignment ownerRole = mappedRoles.get(CASE_OWNER_ROLE);
        assertThat(ownerRole.getUsers().contains(USER_YODA)).isTrue();
        KieServerAssert.assertNullOrEmpty("Groups should be empty", ownerRole.getGroups());

        CaseRoleAssignment insuredRole = mappedRoles.get(CASE_INSURED_ROLE);
        assertThat(insuredRole.getUsers().contains(USER_JOHN)).isTrue();
        KieServerAssert.assertNullOrEmpty("Groups should be empty", insuredRole.getGroups());

        CaseRoleAssignment insRepRole = mappedRoles.get(CASE_INS_REP_ROLE);
        assertThat(insRepRole.getUsers().contains(USER_YODA)).isTrue();
        KieServerAssert.assertNullOrEmpty("Groups should be empty", insRepRole.getGroups());

        CaseRoleAssignment assessorRole = mappedRoles.get(CASE_ASSESSOR_ROLE);
        assertThat(assessorRole.getUsers().contains(USER_YODA)).isTrue();
        KieServerAssert.assertNullOrEmpty("Groups should be empty", assessorRole.getGroups());

        caseClient.assignUserToRole(CONTAINER_ID, caseClaimId, CASE_ASSESSOR_ROLE, USER_MARY);
        caseClient.assignGroupToRole(CONTAINER_ID, caseClaimId, CASE_ASSESSOR_ROLE, "managers");

        roles = caseClient.getRoleAssignments(CONTAINER_ID, caseClaimId);
        assertThat(roles).hasSize(4);
        mappedRoles = roles.stream().collect(toMap(CaseRoleAssignment::getName, r -> r));

        assessorRole = mappedRoles.get(CASE_ASSESSOR_ROLE);
        assertThat(assessorRole.getUsers().contains(USER_MARY)).isTrue();
        assertThat(assessorRole.getGroups().contains("managers")).isTrue();

        caseClient.removeUserFromRole(CONTAINER_ID, caseClaimId, CASE_ASSESSOR_ROLE, USER_MARY);
        caseClient.removeUserFromRole(CONTAINER_ID, caseClaimId, CASE_ASSESSOR_ROLE, USER_YODA);
        caseClient.removeGroupFromRole(CONTAINER_ID, caseClaimId, CASE_ASSESSOR_ROLE, "managers");

        roles = caseClient.getRoleAssignments(CONTAINER_ID, caseClaimId);
        assertThat(roles).hasSize(4);
        mappedRoles = roles.stream().collect(toMap(CaseRoleAssignment::getName, r -> r));

        assessorRole = mappedRoles.get(CASE_ASSESSOR_ROLE);
        KieServerAssert.assertNullOrEmpty("Users should be empty", assessorRole.getUsers());
        KieServerAssert.assertNullOrEmpty("Groups should be empty", assessorRole.getGroups());
    }

    @Test
    public void testAssignUserToRoleNotExistingCase() {
        assertClientException(
                () -> caseClient.assignUserToRole(CONTAINER_ID, "not-existing-case", CASE_ASSESSOR_ROLE, USER_YODA),
                404,
                "Could not find case instance \"not-existing-case\"",
                "Case with id not-existing-case not found");
    }

    @Test
    public void testAssignGroupToRoleNotExistingCase() {
        assertClientException(
                () -> caseClient.assignGroupToRole(CONTAINER_ID, "not-existing-case", CASE_ASSESSOR_ROLE, "managers"),
                404,
                "Could not find case instance \"not-existing-case\"",
                "Case with id not-existing-case not found");
    }

    @Test
    public void testRemoveUserFromRoleNotExistingCase() {
        assertClientException(
                () -> caseClient.removeUserFromRole(CONTAINER_ID, "not-existing-case", CASE_ASSESSOR_ROLE, USER_YODA),
                404,
                "Could not find case instance \"not-existing-case\"",
                "Case with id not-existing-case not found");
    }

    @Test
    public void testRemoveGroupFromRoleNotExistingCase() {
        assertClientException(
                () -> caseClient.removeGroupFromRole(CONTAINER_ID, "not-existing-case", CASE_ASSESSOR_ROLE, "managers"),
                404,
                "Could not find case instance \"not-existing-case\"",
                "Case with id not-existing-case not found");
    }

    @Test
    public void testCaseRolesCardinality() {
        Map<String, Object> data = new HashMap<>();
        data.put("s", "first case started");
        CaseFile caseFile = CaseFile.builder()
                .data(data)
                .addUserAssignments(CASE_INSURED_ROLE, USER_YODA)
                .addUserAssignments(CASE_INS_REP_ROLE, USER_JOHN)
                .build();

        String caseId = caseClient.startCase(CONTAINER_ID, CLAIM_CASE_DEF_ID, caseFile);
        assertThat(caseId).isNotNull();
        
        // Try to add second user to insured role with cardinality 1
        assertClientException(
                () -> caseClient.assignUserToRole(CONTAINER_ID, caseId, CASE_INSURED_ROLE, USER_YODA),
                400,
                "Cannot add more users for role " + CASE_INSURED_ROLE);
    }

    @Test
    public void testGetCaseStages() {
        String caseClaimId = startCarInsuranceClaimCase(USER_YODA, USER_JOHN, USER_YODA);
        assertThat(caseClaimId).isNotNull();

        List<CaseStage> stages = caseClient.getStages(CONTAINER_ID, caseClaimId, true, 0, 10);
        assertThat(stages).hasSize(1);
        assertBuildClaimReportCaseStage(stages.iterator().next(), "Active");

        stages = caseClient.getStages(CONTAINER_ID, caseClaimId, true, 0, 10);
        assertThat(stages).hasSize(1);
        assertBuildClaimReportCaseStage(stages.iterator().next(), "Active");

        caseClient.putCaseInstanceData(CONTAINER_ID, caseClaimId, "claimReportDone", Boolean.TRUE);

        stages = caseClient.getStages(CONTAINER_ID, caseClaimId, false, 0, 10);
        assertThat(stages).hasSize(3);
        assertBuildClaimReportCaseStage(stages.get(0), "Completed");
        assertClaimAssesmentCaseStage(stages.get(1), "Active");
        assertEscalateRejectedClaimCaseStage(stages.get(2), "Available");

        stages = caseClient.getStages(CONTAINER_ID, caseClaimId, true, 0, 10);
        assertThat(stages).hasSize(1);
        assertClaimAssesmentCaseStage(stages.iterator().next(), "Active");
    }

    @Test
    public void testCompleteCaseStageAndAbort() {
        List<TaskSummary> tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
        assertThat(tasks).isEmpty();

        String caseClaimId = startCarInsuranceClaimCase(USER_YODA, USER_JOHN, USER_YODA);
        assertThat(caseClaimId).isNotNull();

        caseClient.putCaseInstanceData(CONTAINER_ID, caseClaimId, "claimReportDone", Boolean.TRUE);

        tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
        assertThat(tasks).hasSize(1);

        caseClient.cancelCaseInstance(CONTAINER_ID, caseClaimId);

        tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
        assertThat(tasks).isEmpty();
    }

    @Test
    public void testGetCaseStagesNotExistingContainer() {
        try {
            caseClient.getStages("not-existing-container", CASE_HR_DEF_ID, false, 0, 10);
            fail("Should have failed because of not existing container.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testGetCaseStagesNotExistingCase() {
        try {
            caseClient.getStages(CONTAINER_ID, "not-existing-case", false, 0, 10);
            fail("Should have failed because of not existing case definition Id.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testGetCaseTasksAsPotOwner() {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        assertThat(caseId).isNotNull();
        assertThat(caseId.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        List<TaskSummary> instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA, 0, 10);
        assertThat(instances).isNotNull();
        assertThat(instances).hasSize(1);

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA, 0, 10, "t.name", true);
        assertThat(instances).isNotNull();
        assertThat(instances).hasSize(1);

        String caseId2 = startUserTaskCase(USER_YODA, USER_JOHN);

        assertThat(caseId2).isNotNull();
        assertThat(caseId2.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA, Arrays.asList(Status.Ready.toString(), Status.Reserved.toString()), 0, 10);
        assertThat(instances).isNotNull();
        assertThat(instances).hasSize(1);

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId2, USER_YODA, Arrays.asList(Status.Ready.toString(), Status.Reserved.toString()), 0, 10, "t.name", true);
        assertThat(instances).isNotNull();
        assertThat(instances).hasSize(1);

        caseClient.destroyCaseInstance(CONTAINER_ID, caseId);
        caseClient.destroyCaseInstance(CONTAINER_ID, caseId2);
    }

    @Test
    public void testFindCaseTasksAssignedAsPotentialOwnerNotExistingCase() {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);
        Assertions.assertThat(caseId).isNotNull().startsWith(CASE_HR_ID_PREFIX);

        List<TaskSummary> instances = caseClient.findCaseTasksAssignedAsPotentialOwner("not-existing-case",
                USER_YODA, 0, 10);
        Assertions.assertThat(instances).isNotNull().isEmpty();

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner("not-existing-case", USER_YODA,
                Arrays.asList(Status.Ready.toString(), Status.Reserved.toString()), 0, 10);
        Assertions.assertThat(instances).isNotNull().isEmpty();

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner("not-existing-case", USER_YODA, 0, 10,
                "t.id", true);
        Assertions.assertThat(instances).isNotNull().isEmpty();

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner("not-existing-case", USER_YODA,
                Arrays.asList(Status.Ready.toString(), Status.Reserved.toString()), 0, 10,
                "t.name", false);
        Assertions.assertThat(instances).isNotNull().isEmpty();
    }

    @Test
    public void testFindCaseTasksAssignedAsPotentialOwnerNoTasks() {
        String caseId = startUserTaskCase(USER_JOHN, USER_MARY);
        Assertions.assertThat(caseId).isNotNull().startsWith(CASE_HR_ID_PREFIX);

        List<TaskSummary> instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA, 0, 10);
        Assertions.assertThat(instances).isNotNull().isEmpty();

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA,
                Arrays.asList(Status.Ready.toString(), Status.Reserved.toString()), 0, 10);
        Assertions.assertThat(instances).isNotNull().isEmpty();

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA, 0, 10, "t.id", true);
        Assertions.assertThat(instances).isNotNull().isEmpty();

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA,
                Arrays.asList(Status.Ready.toString(), Status.Reserved.toString()), 0, 10, "t.name", false);
        Assertions.assertThat(instances).isNotNull().isEmpty();
    }

    @Test
    public void testFindCaseTasksAssignedAsPotentialOwnerPaging() {
        String caseId = startUserTaskCase(USER_JOHN, USER_YODA);
        Assertions.assertThat(caseId).isNotNull().startsWith(CASE_HR_ID_PREFIX);

        caseClient.addDynamicUserTask(CONTAINER_ID, caseId, "TaskA", null, USER_YODA, null, Collections.emptyMap());
        caseClient.addDynamicUserTask(CONTAINER_ID, caseId, "TaskB", null, USER_YODA, null, Collections.emptyMap());
        caseClient.addDynamicUserTask(CONTAINER_ID, caseId, "TaskC", null, USER_YODA, null, Collections.emptyMap());

        List<TaskSummary> instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA, 0, 3);
        Assertions.assertThat(instances).isNotNull()
                .extracting(TaskSummary::getName).containsOnly("TaskA", "TaskB", "TaskC");

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA,
                Arrays.asList(Status.Ready.toString(), Status.Reserved.toString()), 1, 3);
        Assertions.assertThat(instances).isNotNull().isEmpty();

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA,
                Arrays.asList(Status.Ready.toString(), Status.Reserved.toString()), 0, 2, "t.id", true);
        Assertions.assertThat(instances).isNotNull()
                .extracting(TaskSummary::getName).containsOnly("TaskA", "TaskB");

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA,
                Arrays.asList(Status.Ready.toString(), Status.Reserved.toString()), 1, 2, "t.id", true);
        Assertions.assertThat(instances).isNotNull()
                .extracting(TaskSummary::getName).containsOnly("TaskC");

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA, 0, 1, "t.name", false);
        Assertions.assertThat(instances).isNotNull()
                .extracting(TaskSummary::getName).containsOnly("TaskC");

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA, 1, 1, "t.name", false);
        Assertions.assertThat(instances).isNotNull()
                .extracting(TaskSummary::getName).containsOnly("TaskB");

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA, 2, 1, "t.name", false);
        Assertions.assertThat(instances).isNotNull()
                .extracting(TaskSummary::getName).containsOnly("TaskA");
    }

    @Test
    public void testFindCaseTasksAssignedAsPotentialOwnerSorting() {
        String caseId = startUserTaskCase(USER_JOHN, USER_YODA);
        Assertions.assertThat(caseId).isNotNull().startsWith(CASE_HR_ID_PREFIX);

        caseClient.addDynamicUserTask(CONTAINER_ID, caseId, "TaskA", null, USER_YODA, null, Collections.emptyMap());
        caseClient.addDynamicUserTask(CONTAINER_ID, caseId, "TaskB", null, USER_YODA, null, Collections.emptyMap());
        caseClient.addDynamicUserTask(CONTAINER_ID, caseId, "TaskC", null, USER_YODA, null, Collections.emptyMap());

        List<TaskSummary> instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA, 0, 10, "t.id", true);
        Assertions.assertThat(instances).isNotNull()
                .extracting(TaskSummary::getName).containsExactly("TaskA", "TaskB", "TaskC");

        instances = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA,
                Arrays.asList(Status.Ready.toString(), Status.Reserved.toString()), 0, 10, "t.name", false);
        Assertions.assertThat(instances).isNotNull()
                .extracting(TaskSummary::getName).containsExactly("TaskC", "TaskB", "TaskA");
    }

    @Test
    public void testGetCaseTasksAsBusinessAdmin() throws Exception {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        assertThat(caseId).isNotNull();
        assertThat(caseId.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        changeUser(USER_ADMINISTRATOR);
        List<TaskSummary> instances = caseClient.findCaseTasksAssignedAsBusinessAdministrator(caseId, USER_ADMINISTRATOR, 0, 10);
        assertThat(instances).isNotNull();
        assertThat(instances).hasSize(1);

        changeUser(USER_YODA);
        String caseId2 = startUserTaskCase(USER_YODA, USER_JOHN);

        assertThat(caseId2).isNotNull();
        assertThat(caseId2.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        changeUser(USER_ADMINISTRATOR);
        instances = caseClient.findCaseTasksAssignedAsBusinessAdministrator(caseId, USER_ADMINISTRATOR, Arrays.asList(Status.Ready.toString(), Status.Reserved.toString()), 0, 10);
        assertThat(instances).isNotNull();
        assertThat(instances).hasSize(1);

        instances = caseClient.findCaseTasksAssignedAsBusinessAdministrator(caseId2, USER_ADMINISTRATOR, Arrays.asList(Status.Ready.toString(), Status.Reserved.toString()), 0, 10, "t.name", true);
        assertThat(instances).isNotNull();
        assertThat(instances).hasSize(1);

        changeUser(USER_YODA);
        caseClient.destroyCaseInstance(CONTAINER_ID, caseId);
        caseClient.destroyCaseInstance(CONTAINER_ID, caseId2);
    }

    @Test
    public void testGetCaseTasksAsStakeholder() throws Exception {
        // Test is using user authentication, isn't available for local execution(which has mocked authentication info).
        Assume.assumeFalse(TestConfig.isLocalServer());
        String caseId = startUserTaskCase(USER_JOHN, USER_YODA);

        assertThat(caseId).isNotNull();
        assertThat(caseId.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("input", "text data");
        parameters.put("TaskStakeholderId", USER_YODA);

        caseClient.addDynamicUserTask(CONTAINER_ID, caseId, "dynamic task", "simple description", USER_JOHN, null, parameters);

        List<TaskSummary> tasks = caseClient.findCaseTasksAssignedAsStakeholder(caseId, USER_YODA, 0, 10);
        assertThat(tasks).hasSize(1);
        TaskSummary task = tasks.get(0);
        assertThat(task.getName()).isEqualTo("dynamic task");
        assertThat(task.getDescription()).isEqualTo("simple description");
        assertThat(task.getStatus()).isEqualTo(Status.Reserved.toString());
        assertThat(task.getActualOwner()).isEqualTo(USER_JOHN);

        // start another case
        String caseId2 = startUserTaskCase(USER_JOHN, USER_YODA);
        assertThat(caseId2).isNotNull();
        assertThat(caseId2.startsWith(CASE_HR_ID_PREFIX)).isTrue();
        caseClient.addDynamicUserTask(CONTAINER_ID, caseId2, "dynamic task", "simple description", USER_JOHN, null, parameters);

        tasks = caseClient.findCaseTasksAssignedAsStakeholder(caseId, USER_YODA, Arrays.asList(Status.Ready.toString(), Status.Reserved.toString()), 0, 10);
        assertThat(tasks).isNotNull();
        assertThat(tasks).hasSize(1);

        tasks = caseClient.findCaseTasksAssignedAsStakeholder(caseId2, USER_YODA, Arrays.asList(Status.Ready.toString(), Status.Reserved.toString()), 0, 10, "t.name", true);
        assertThat(tasks).isNotNull();
        assertThat(tasks).hasSize(1);

        changeUser(USER_JOHN);
        caseClient.destroyCaseInstance(CONTAINER_ID, caseId);
    }

    @Test
    public void testGetTriggerTask() throws Exception {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        assertThat(caseId).isNotNull();
        assertThat(caseId.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        changeUser(USER_JOHN);
        List<TaskSummary> caseTasks = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_JOHN, 0, 10);
        assertThat(caseTasks).isNotNull();
        assertThat(caseTasks).isEmpty();

        changeUser(USER_YODA);
        caseClient.triggerAdHocFragment(CONTAINER_ID, caseId, HELLO_2_TASK, Collections.EMPTY_MAP);

        changeUser(USER_JOHN);
        caseTasks = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_JOHN, 0, 10);
        assertThat(caseTasks).isNotNull();
        assertThat(caseTasks).hasSize(1);

        TaskSummary task = caseTasks.get(0);
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo(HELLO_2_TASK);
        assertThat(task.getActualOwner()).isEqualTo(USER_JOHN);

        changeUser(USER_YODA);
        caseTasks = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA, 0, 10);
        assertThat(caseTasks).isNotNull();
        assertThat(caseTasks).hasSize(1);

        task = caseTasks.get(0);
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo(HELLO_1_TASK);
        assertThat(task.getActualOwner()).isEqualTo(USER_YODA);

        caseClient.destroyCaseInstance(CONTAINER_ID, caseId);
    }

    @Ignore // test is ignore due JBPM-6001 and JBPM-6008
    @Test
    public void testTriggerTaskIntoStage() throws Exception {
        String caseClaimId = startCarInsuranceClaimCase(USER_YODA, USER_JOHN, USER_YODA);
        assertThat(caseClaimId).isNotNull();

        List<CaseStage> stages = caseClient.getStages(CONTAINER_ID, caseClaimId, true, 0, 10);
        assertThat(stages).hasSize(1);
        CaseStage stage = stages.iterator().next();
        assertBuildClaimReportCaseStage(stage, "Active");

        List<TaskSummary> tasks = caseClient.findCaseTasksAssignedAsPotentialOwner(caseClaimId, USER_YODA, 0, 10);
        assertThat(tasks).isNotNull();
        int countOfTaskBefore = tasks.size();

        assertThat(stage.getIdentifier()).isNotNull();
        caseClient.triggerAdHocFragmentInStage(CONTAINER_ID, caseClaimId, stage.getIdentifier(), SUBMIT_POLICE_REPORT_TASK, Collections.EMPTY_MAP);

        tasks = caseClient.findCaseTasksAssignedAsPotentialOwner(caseClaimId, USER_YODA, 0, 10);
        assertThat(tasks).isNotNull();
        assertThat(tasks.size()).isEqualTo(countOfTaskBefore + 1);

        TaskSummary task = tasks.get(0);
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo(SUBMIT_POLICE_REPORT_TASK);

        caseClient.putCaseInstanceData(CONTAINER_ID, caseClaimId, "claimReportDone", Boolean.TRUE);
        stages = caseClient.getStages(CONTAINER_ID, caseClaimId, false, 0, 10);
        assertThat(stages).hasSize(3);
        assertBuildClaimReportCaseStage(stages.get(0), "Completed");
        assertClaimAssesmentCaseStage(stages.get(1), "Active");
        assertEscalateRejectedClaimCaseStage(stages.get(2), "Available");

        tasks = caseClient.findCaseTasksAssignedAsPotentialOwner(caseClaimId, USER_YODA, 0, 10);
        assertThat(tasks).isNotNull();
        assertThat(tasks).hasSize(1);
        task = tasks.get(0);
        assertNotEquals(SUBMIT_POLICE_REPORT_TASK, task.getName());

        assertClientException(
                () -> caseClient.triggerAdHocFragmentInStage(CONTAINER_ID, caseClaimId, stage.getIdentifier(), SUBMIT_POLICE_REPORT_TASK, Collections.EMPTY_MAP),
                400,
                "Could not trigger Fragment for Completed stage " + stage.getName());

        caseClient.destroyCaseInstance(CONTAINER_ID, caseClaimId);
    }

    @Test
    public void testTriggerTaskInCanceledCase() throws Exception {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        assertThat(caseId).isNotNull();
        assertThat(caseId.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        List<TaskSummary> caseTasks = caseClient.findCaseTasksAssignedAsPotentialOwner(caseId, USER_YODA, 0, 10);
        assertThat(caseTasks).isNotNull();
        assertThat(caseTasks).hasSize(1);

        caseClient.cancelCaseInstance(CONTAINER_ID, caseId);

        assertClientException(
                () -> caseClient.triggerAdHocFragment(CONTAINER_ID, caseId, HELLO_2_TASK, Collections.EMPTY_MAP),
                404,
                "Could not find case instance \"" + caseId + "\"",
                "Case with id " + caseId + " was not found");

        caseClient.destroyCaseInstance(CONTAINER_ID, caseId);
    }

    @Test
    public void testCaseInstanceAuthorization() throws Exception {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        assertThat(caseId).isNotNull();
        assertThat(caseId.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        changeUser(USER_JOHN);

        try {
            caseClient.cancelCaseInstance(CONTAINER_ID, caseId);
            fail("User john is not an owner so is not allowed to cancel case instance");
        } catch (KieServicesException e) {
            String errorMessage = e.getMessage();
            assertThat(errorMessage.contains("User " + USER_JOHN + " is not authorized")).isTrue();
        }
        try {
            caseClient.destroyCaseInstance(CONTAINER_ID, caseId);
            fail("User john is not an owner so is not allowed to destroy case instance");
        } catch (KieServicesException e) {
            String errorMessage = e.getMessage();
            assertThat(errorMessage.contains("User " + USER_JOHN + " is not authorized")).isTrue();
        }

        changeUser(USER_YODA);

        caseClient.cancelCaseInstance(CONTAINER_ID, caseId);

        caseClient.destroyCaseInstance(CONTAINER_ID, caseId);
    }

    @Test
    public void testGetProcessDefinitionsByContainer() {
        List<ProcessDefinition> definitions = caseClient.findProcessesByContainerId(CONTAINER_ID, 0, 10);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(3);

        List<String> mappedDefinitions = definitions.stream().map(ProcessDefinition::getId).collect(Collectors.toList());
        assertThat(mappedDefinitions.contains("DataVerification")).isTrue();
        assertThat(mappedDefinitions.contains("hiring")).isTrue();

        definitions = caseClient.findProcessesByContainerId(CONTAINER_ID, 0, 1);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertThat(definitions.get(0).getId()).isEqualTo("DataVerification");

        definitions = caseClient.findProcessesByContainerId(CONTAINER_ID, 1, 1);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertThat(definitions.get(0).getId()).isEqualTo(USER_TASK_DEF_ID);

        definitions = caseClient.findProcessesByContainerId(CONTAINER_ID, 0, 1, CaseServicesClient.SORT_BY_PROCESS_NAME, false);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertThat(definitions.get(0).getId()).isEqualTo("hiring");
    }

    @Test
    public void testGetProcessDefinitions() {
        List<ProcessDefinition> definitions = caseClient.findProcesses("hir", 0, 10);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertThat(definitions.get(0).getId()).isEqualTo("hiring");

        definitions = caseClient.findProcesses(0, 1, CaseServicesClient.SORT_BY_PROCESS_NAME, false);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(1);
        assertThat(definitions.get(0).getId()).isEqualTo("hiring");

        definitions = caseClient.findProcesses(0, 10);
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(3);

        List<String> mappedDefinitions = definitions.stream().map(ProcessDefinition::getId).collect(Collectors.toList());
        assertThat(mappedDefinitions.contains("DataVerification")).isTrue();
        assertThat(mappedDefinitions.contains("hiring")).isTrue();
    }

    @Test
    public void testTriggerNotExistingAdHocFragments() {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        assertThat(caseId).isNotNull();
        assertThat(caseId.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        CaseInstance caseInstance = caseClient.getCaseInstance(CONTAINER_ID, caseId);
        assertHrCaseInstance(caseInstance, caseId, USER_YODA);

        List<CaseInstance> caseInstances = caseClient.getCaseInstancesOwnedBy(USER_YODA, null, 0, 10);
        assertThat(caseInstances).hasSize(1);

        List<CaseMilestone> milestones = caseClient.getMilestones(CONTAINER_ID, caseId, true, 0, 10);
        assertThat(milestones).isNotNull();
        assertThat(milestones).isEmpty();
        try {
            caseClient.triggerAdHocFragment(CONTAINER_ID, caseId, "not existing", null);
            fail("Should have failed because of not existing comment Id.");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test
    public void testGetCaseInstanceDataItems() {
        String caseClaimId = startCarInsuranceClaimCase(USER_YODA, USER_JOHN, USER_YODA);
        assertThat(caseClaimId).isNotNull();

        List<CaseFileDataItem> dataItems = caseClient.getCaseInstanceDataItems(caseClaimId, 0, 10);
        assertThat(dataItems).isNotNull();
        assertThat(dataItems).isEmpty();

        List<CaseStage> stages = caseClient.getStages(CONTAINER_ID, caseClaimId, true, 0, 10);
        assertThat(stages).hasSize(1);
        assertBuildClaimReportCaseStage(stages.iterator().next(), "Active");

        stages = caseClient.getStages(CONTAINER_ID, caseClaimId, true, 0, 10);
        assertThat(stages).hasSize(1);
        assertBuildClaimReportCaseStage(stages.iterator().next(), "Active");

        caseClient.putCaseInstanceData(CONTAINER_ID, caseClaimId, "claimReportDone", Boolean.TRUE);

        dataItems = caseClient.getCaseInstanceDataItems(caseClaimId, 0, 10);
        assertThat(dataItems).isNotNull();
        assertThat(dataItems).hasSize(1);

        CaseFileDataItem dataItem = dataItems.get(0);
        assertThat(dataItem.getCaseId()).isEqualTo(caseClaimId);
        assertThat(dataItem.getName()).isEqualTo("claimReportDone");
        assertThat(dataItem.getValue()).isEqualTo("true");
        assertThat(dataItem.getType()).isEqualTo(Boolean.class.getName());
        assertThat(dataItem.getLastModifiedBy()).isEqualTo(USER_YODA);

        dataItems = caseClient.getCaseInstanceDataItemsByType(caseClaimId, Arrays.asList(Boolean.class.getName()), 0, 10);
        assertThat(dataItems).isNotNull();
        assertThat(dataItems).hasSize(1);

        dataItems = caseClient.getCaseInstanceDataItemsByType(caseClaimId, Arrays.asList(String.class.getName()), 0, 10);
        assertThat(dataItems).isNotNull();
        assertThat(dataItems).isEmpty();

        dataItems = caseClient.getCaseInstanceDataItemsByName(caseClaimId, Arrays.asList("claimReportDone"), 0, 10);
        assertThat(dataItems).isNotNull();
        assertThat(dataItems).hasSize(1);

        dataItems = caseClient.getCaseInstanceDataItemsByName(caseClaimId, Arrays.asList("notExisting"), 0, 10);
        assertThat(dataItems).isNotNull();
        assertThat(dataItems).isEmpty();

        stages = caseClient.getStages(CONTAINER_ID, caseClaimId, false, 0, 10);
        assertThat(stages).hasSize(3);
        assertBuildClaimReportCaseStage(stages.get(0), "Completed");
        assertClaimAssesmentCaseStage(stages.get(1), "Active");
        assertEscalateRejectedClaimCaseStage(stages.get(2), "Available");

        stages = caseClient.getStages(CONTAINER_ID, caseClaimId, true, 0, 10);
        assertThat(stages).hasSize(1);
        assertClaimAssesmentCaseStage(stages.iterator().next(), "Active");
    }

    @Test
    public void testGetCaseInstanceByData() {
        String caseClaimId = startCarInsuranceClaimCase(USER_YODA, USER_JOHN, USER_YODA);
        assertThat(caseClaimId).isNotNull();

        List<CaseStage> stages = caseClient.getStages(CONTAINER_ID, caseClaimId, true, 0, 10);
        assertThat(stages).hasSize(1);
        assertBuildClaimReportCaseStage(stages.iterator().next(), "Active");

        stages = caseClient.getStages(CONTAINER_ID, caseClaimId, true, 0, 10);
        assertThat(stages).hasSize(1);
        assertBuildClaimReportCaseStage(stages.iterator().next(), "Active");

        List<CaseInstance> caseInstances = caseClient.getCaseInstancesByData("claimReportDone", Arrays.asList(CaseStatus.OPEN.getName()), 0, 10);
        assertThat(caseInstances).isEmpty();

        caseClient.putCaseInstanceData(CONTAINER_ID, caseClaimId, "claimReportDone", Boolean.TRUE);

        caseInstances = caseClient.getCaseInstancesByData("claimReportDone", Arrays.asList(CaseStatus.OPEN.getName()), 0, 10);
        assertThat(caseInstances).hasSize(1);

        caseInstances = caseClient.getCaseInstancesByData("claimReportDone", "false", Arrays.asList(CaseStatus.OPEN.getName()), 0, 10);
        assertThat(caseInstances).isEmpty();

        caseInstances = caseClient.getCaseInstancesByData("claimReportDone", "true", Arrays.asList(CaseStatus.OPEN.getName()), 0, 10);
        assertThat(caseInstances).hasSize(1);

        stages = caseClient.getStages(CONTAINER_ID, caseClaimId, false, 0, 10);
        assertThat(stages).hasSize(3);
        assertBuildClaimReportCaseStage(stages.get(0), "Completed");
        assertClaimAssesmentCaseStage(stages.get(1), "Active");
        assertEscalateRejectedClaimCaseStage(stages.get(2), "Available");

        stages = caseClient.getStages(CONTAINER_ID, caseClaimId, true, 0, 10);
        assertThat(stages).hasSize(1);
        assertClaimAssesmentCaseStage(stages.iterator().next(), "Active");
    }

    @Test
    public void testGetCaseInstancesByDataPaging() {
        String case1 = startCarInsuranceClaimCase(USER_YODA, USER_JOHN, USER_YODA);
        Assertions.assertThat(case1).isNotNull().startsWith(CLAIM_CASE_ID_PREFIX);
        String case2 = startCarInsuranceClaimCase(USER_YODA, USER_JOHN, USER_YODA);
        Assertions.assertThat(case2).isNotNull().startsWith(CLAIM_CASE_ID_PREFIX);
        String case3 = startCarInsuranceClaimCase(USER_YODA, USER_JOHN, USER_YODA);
        Assertions.assertThat(case3).isNotNull().startsWith(CLAIM_CASE_ID_PREFIX);

        caseClient.putCaseInstanceData(CONTAINER_ID, case1, "claimReportDone", Boolean.TRUE);
        caseClient.putCaseInstanceData(CONTAINER_ID, case2, "claimReportDone", Boolean.TRUE);
        caseClient.putCaseInstanceData(CONTAINER_ID, case3, "claimReportDone", Boolean.TRUE);

        List<CaseInstance> caseInstances = caseClient.getCaseInstancesByData("claimReportDone",
                Collections.singletonList(CaseStatus.OPEN.getName()), 0, 3);
        Assertions.assertThat(caseInstances).extracting(CaseInstance::getCaseId).containsOnly(case1, case2, case3);

        caseInstances = caseClient.getCaseInstancesByData("claimReportDone",
                Collections.singletonList(CaseStatus.OPEN.getName()), 1, 3);
        Assertions.assertThat(caseInstances).isEmpty();

        caseInstances = caseClient.getCaseInstancesByData("claimReportDone",
                Collections.singletonList(CaseStatus.OPEN.getName()), 0, 2);
        Assertions.assertThat(caseInstances).extracting(CaseInstance::getCaseId).containsOnly(case1, case2);

        caseInstances = caseClient.getCaseInstancesByData("claimReportDone",
                Collections.singletonList(CaseStatus.OPEN.getName()), 1, 2);
        Assertions.assertThat(caseInstances).extracting(CaseInstance::getCaseId).containsOnly(case3);

        caseInstances = caseClient.getCaseInstancesByData("claimReportDone",
                Collections.singletonList(CaseStatus.OPEN.getName()), 2, 2);
        Assertions.assertThat(caseInstances).extracting(CaseInstance::getCaseId).isEmpty();

        caseInstances = caseClient.getCaseInstancesByData("claimReportDone",
                Collections.singletonList(CaseStatus.OPEN.getName()), 0, 1);
        Assertions.assertThat(caseInstances).extracting(CaseInstance::getCaseId).containsOnly(case1);

        caseInstances = caseClient.getCaseInstancesByData("claimReportDone",
                Collections.singletonList(CaseStatus.OPEN.getName()), 1, 1);
        Assertions.assertThat(caseInstances).extracting(CaseInstance::getCaseId).containsOnly(case2);

        caseInstances = caseClient.getCaseInstancesByData("claimReportDone",
                Collections.singletonList(CaseStatus.OPEN.getName()), 2, 1);
        Assertions.assertThat(caseInstances).extracting(CaseInstance::getCaseId).containsOnly(case3);
    }

    @Test
    public void testCreateCaseWithCaseFileWithCommentsSorted() throws Exception {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);

        assertThat(caseId).isNotNull();
        assertThat(caseId.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        caseClient.assignUserToRole(CONTAINER_ID, caseId, CASE_CONTACT_ROLE, USER_MARY);

        List<CaseComment> comments = caseClient.getComments(CONTAINER_ID, caseId, 0, 10);
        assertThat(comments).isNotNull();
        assertThat(comments).isEmpty();
        try {
            caseClient.addComment(CONTAINER_ID, caseId, USER_YODA, "yoda's comment");
            changeUser(USER_JOHN);
            caseClient.addComment(CONTAINER_ID, caseId, USER_JOHN, "john's comment");
            changeUser(USER_MARY);
            caseClient.addComment(CONTAINER_ID, caseId, USER_MARY, "mary's comment");

            comments = caseClient.getComments(CONTAINER_ID, caseId, 0, 10);
            assertThat(comments).isNotNull();
            assertThat(comments).hasSize(3);

            CaseComment comment = comments.get(0);
            assertThat(comment).isNotNull();
            assertThat(comment.getAuthor()).isEqualTo(USER_YODA);
            assertThat(comment.getText()).isEqualTo("yoda's comment");
            assertThat(comment.getAddedAt()).isNotNull();
            assertThat(comment.getId()).isNotNull();

            comment = comments.get(1);
            assertThat(comment).isNotNull();
            assertThat(comment.getAuthor()).isEqualTo(USER_JOHN);
            assertThat(comment.getText()).isEqualTo("john's comment");
            assertThat(comment.getAddedAt()).isNotNull();
            assertThat(comment.getId()).isNotNull();

            comment = comments.get(2);
            assertThat(comment).isNotNull();
            assertThat(comment.getAuthor()).isEqualTo(USER_MARY);
            assertThat(comment.getText()).isEqualTo("mary's comment");
            assertThat(comment.getAddedAt()).isNotNull();
            assertThat(comment.getId()).isNotNull();

            comments = caseClient.getComments(CONTAINER_ID, caseId, CaseServicesClient.COMMENT_SORT_BY_AUTHOR, 0, 10);
            assertThat(comments).isNotNull();
            assertThat(comments).hasSize(3);

            comment = comments.get(0);
            assertThat(comment).isNotNull();
            assertThat(comment.getAuthor()).isEqualTo(USER_JOHN);
            assertThat(comment.getText()).isEqualTo("john's comment");
            assertThat(comment.getAddedAt()).isNotNull();
            assertThat(comment.getId()).isNotNull();

            comment = comments.get(1);
            assertThat(comment).isNotNull();
            assertThat(comment.getAuthor()).isEqualTo(USER_MARY);
            assertThat(comment.getText()).isEqualTo("mary's comment");
            assertThat(comment.getAddedAt()).isNotNull();
            assertThat(comment.getId()).isNotNull();

            comment = comments.get(2);
            assertThat(comment).isNotNull();
            assertThat(comment.getAuthor()).isEqualTo(USER_YODA);
            assertThat(comment.getText()).isEqualTo("yoda's comment");
            assertThat(comment.getAddedAt()).isNotNull();
            assertThat(comment.getId()).isNotNull();

            comments = caseClient.getComments(CONTAINER_ID, caseId, CaseServicesClient.COMMENT_SORT_BY_DATE, 0, 10);
            assertThat(comments).isNotNull();
            assertThat(comments).hasSize(3);

            comment = comments.get(0);
            assertThat(comment).isNotNull();
            assertThat(comment.getAuthor()).isEqualTo(USER_YODA);
            assertThat(comment.getText()).isEqualTo("yoda's comment");
            assertThat(comment.getAddedAt()).isNotNull();
            assertThat(comment.getId()).isNotNull();

            comment = comments.get(1);
            assertThat(comment).isNotNull();
            assertThat(comment.getAuthor()).isEqualTo(USER_JOHN);
            assertThat(comment.getText()).isEqualTo("john's comment");
            assertThat(comment.getAddedAt()).isNotNull();
            assertThat(comment.getId()).isNotNull();

            comment = comments.get(2);
            assertThat(comment).isNotNull();
            assertThat(comment.getAuthor()).isEqualTo(USER_MARY);
            assertThat(comment.getText()).isEqualTo("mary's comment");
            assertThat(comment.getAddedAt()).isNotNull();
            assertThat(comment.getId()).isNotNull();
        } finally {
            changeUser(USER_YODA);
        }
    }

    @Test
    public void testAddDynamicProcessToCaseNotExistingCase() {
        String invalidCaseId = "not-existing-case-id";
        assertClientException(() -> caseClient.addDynamicSubProcess(CONTAINER_ID, invalidCaseId, CLAIM_CASE_DEF_ID, null),
                404,
                "Could not find case instance \"" + invalidCaseId + "\"",
                "Case with id " + invalidCaseId + " not found");
    }

    @Test
    public void testAddDynamicProcessToCaseNotExistingProcessDefinition() {
        String invalidProcessId = "not-existing-process-id";
        Map<String, Object> data = new HashMap<>();
        data.put("s", "first case started");
        CaseFile caseFile = CaseFile.builder()
                .data(data)
                .addUserAssignments(CASE_INSURED_ROLE, USER_YODA)
                .addUserAssignments(CASE_INS_REP_ROLE, USER_JOHN)
                .build();

        String caseId = caseClient.startCase(CONTAINER_ID, CLAIM_CASE_DEF_ID, caseFile);
        assertThat(caseId).isNotNull();

        assertClientException(() -> caseClient.addDynamicSubProcess(CONTAINER_ID, caseId, invalidProcessId, null),
                404,
                "Could not find process definition \"" + invalidProcessId + "\" in container \"" + CONTAINER_ID + "\"",
                "No process definition found with id: " + invalidProcessId);
    }
    
    @Test
    public void testCreateCaseWithCaseFileWithCommentsWithRestrictions() {
        String caseId = startUserTaskCase(USER_YODA, USER_JOHN);
        // add a contact role to yoda so it can access a case once owner role is removed
        caseClient.assignUserToRole(CONTAINER_ID, caseId, CASE_CONTACT_ROLE, USER_YODA);

        assertThat(caseId).isNotNull();
        assertThat(caseId.startsWith(CASE_HR_ID_PREFIX)).isTrue();

        List<CaseComment> comments = caseClient.getComments(CONTAINER_ID, caseId, 0, 10);
        assertThat(comments).isNotNull();
        assertThat(comments).isEmpty();

        List<String> restrictions = new ArrayList<>();
        restrictions.add(CASE_OWNER_ROLE);
        
        caseClient.addComment(CONTAINER_ID, caseId, USER_YODA, "first comment", restrictions);

        comments = caseClient.getComments(CONTAINER_ID, caseId, 0, 10);
        assertThat(comments).isNotNull();
        assertThat(comments).hasSize(1);

        CaseComment comment = comments.get(0);
        assertThat(comment).isNotNull();
        assertThat(comment.getAuthor()).isEqualTo(USER_YODA);
        assertThat(comment.getText()).isEqualTo("first comment");
        assertThat(comment.getAddedAt()).isNotNull();
        assertThat(comment.getId()).isNotNull();
        
        // remove yoda from owner role to simulate lack of access
        caseClient.removeUserFromRole(CONTAINER_ID, caseId, CASE_OWNER_ROLE, USER_YODA);
        
        comments = caseClient.getComments(CONTAINER_ID, caseId, 0, 10);
        assertThat(comments).isNotNull();
        assertThat(comments).isEmpty();

        final String commentId = comment.getId();
        assertClientException(() -> caseClient.updateComment(CONTAINER_ID, caseId, commentId, USER_YODA, "updated comment"), 403, "");
        
        // add back yoda to owner role
        caseClient.assignUserToRole(CONTAINER_ID, caseId, CASE_OWNER_ROLE, USER_YODA);
        
        caseClient.updateComment(CONTAINER_ID, caseId, comment.getId(), USER_YODA, "updated comment");
        comments = caseClient.getComments(CONTAINER_ID, caseId, 0, 10);
        assertThat(comments).isNotNull();
        assertThat(comments).hasSize(1);

        comment = comments.get(0);
        assertThat(comment).isNotNull();
        assertThat(comment.getAuthor()).isEqualTo(USER_YODA);
        assertThat(comment.getText()).isEqualTo("updated comment");
        assertThat(comment.getAddedAt()).isNotNull();
        assertThat(comment.getId()).isNotNull();

        final String updatedCommentId = comment.getId();
        // remove yoda from owner role to simulate lack of access
        caseClient.removeUserFromRole(CONTAINER_ID, caseId, CASE_OWNER_ROLE, USER_YODA);
        
        assertClientException(() -> caseClient.removeComment(CONTAINER_ID, caseId, updatedCommentId), 403, "");

        // add back yoda to owner role
        caseClient.assignUserToRole(CONTAINER_ID, caseId, CASE_OWNER_ROLE, USER_YODA);
        
        caseClient.removeComment(CONTAINER_ID, caseId, updatedCommentId);
        comments = caseClient.getComments(CONTAINER_ID, caseId, 0, 10);
        assertThat(comments).isNotNull();
        assertThat(comments).isEmpty();
    }
    

    private String startUserTaskCase(String owner, String contact) {
        Map<String, Object> data = new HashMap<>();
        data.put("s", "first case started");
        CaseFile caseFile = CaseFile.builder()
                .addUserAssignments(CASE_OWNER_ROLE, owner)
                .addUserAssignments(CASE_CONTACT_ROLE, contact)
                .data(data)
                .build();

        String caseId = caseClient.startCase(CONTAINER_ID, CASE_HR_DEF_ID, caseFile);
        assertThat(caseId).isNotNull();
        return caseId;
    }

    private String startCarInsuranceClaimCase(String insured, String insuranceRep, String assessor) {
        Map<String, Object> data = new HashMap<>();
        data.put("s", "first case started");
        CaseFile caseFile = CaseFile.builder()
                .addUserAssignments(CASE_INSURED_ROLE, insured)
                .addUserAssignments(CASE_INS_REP_ROLE, insuranceRep)
                .addUserAssignments(CASE_ASSESSOR_ROLE, assessor)
                .data(data)
                .build();

        String caseId = caseClient.startCase(CONTAINER_ID, CLAIM_CASE_DEF_ID, caseFile);
        assertThat(caseId).isNotNull();
        return caseId;
    }

    private void assertHrCaseInstance(CaseInstance caseInstance, String caseId, String owner) {
        assertThat(caseInstance).isNotNull();
        assertThat(caseInstance.getCaseId()).isEqualTo(caseId);
        assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(CASE_HR_DEF_ID);
        assertThat(caseInstance.getCaseDescription()).isEqualTo(CASE_HR_DESRIPTION);
        assertThat(caseInstance.getCaseOwner()).isEqualTo(owner);
        assertThat(caseInstance.getCaseStatus().intValue()).isEqualTo(CaseStatus.OPEN.getId());
        assertThat(caseInstance.getStartedAt()).isNotNull();
        assertThat(caseInstance.getCompletedAt()).isNull();
        assertThat(caseInstance.getCompletionMessage()).isEqualTo("");
        assertThat(caseInstance.getContainerId()).isEqualTo(CONTAINER_ID);
    }

    private void assertCarInsuranceCaseInstance(CaseInstance caseInstance, String caseId, String owner) {
        assertThat(caseInstance).isNotNull();
        assertThat(caseInstance.getCaseId()).isEqualTo(caseId);
        assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(CLAIM_CASE_DEF_ID);
        assertThat(caseInstance.getCaseDescription()).isEqualTo(CLAIM_CASE_DESRIPTION);
        assertThat(caseInstance.getCaseOwner()).isEqualTo(owner);
        assertThat(caseInstance.getCaseStatus().intValue()).isEqualTo(CaseStatus.OPEN.getId());
        assertThat(caseInstance.getStartedAt()).isNotNull();
        assertThat(caseInstance.getCompletedAt()).isNull();
        assertThat(caseInstance.getCompletionMessage()).isEqualTo("");
        assertThat(caseInstance.getContainerId()).isEqualTo(CONTAINER_ID);
    }

    private void assertHrCaseDefinition(CaseDefinition caseDefinition) {
        assertThat(caseDefinition).isNotNull();
        assertThat(caseDefinition.getIdentifier()).isEqualTo(CASE_HR_DEF_ID);
        assertThat(caseDefinition.getName()).isEqualTo(CASE_HR_NAME);
        assertThat(caseDefinition.getCaseIdPrefix()).isEqualTo(CASE_HR_ID_PREFIX);
        assertThat(caseDefinition.getVersion()).isEqualTo(CASE_HR_VERSION);
        assertThat(caseDefinition.getAdHocFragments()).hasSize(3);
        KieServerAssert.assertNullOrEmpty("Stages should be empty", caseDefinition.getCaseStages());
        assertThat(caseDefinition.getContainerId()).isEqualTo(CONTAINER_ID);

        // Milestones checks
        assertThat(caseDefinition.getMilestones()).hasSize(2);
        assertThat(caseDefinition.getMilestones().get(0).getName()).isEqualTo("Milestone1");
        assertThat(caseDefinition.getMilestones().get(0).getIdentifier()).isEqualTo("_SomeID4");
        assertThat(caseDefinition.getMilestones().get(0).isMandatory()).as("Case shouldn't be mandatory.").isFalse();
        assertThat(caseDefinition.getMilestones().get(1).getName()).isEqualTo("Milestone2");
        assertThat(caseDefinition.getMilestones().get(1).getIdentifier()).isEqualTo("_5");
        assertThat(caseDefinition.getMilestones().get(1).isMandatory()).as("Case shouldn't be mandatory.").isFalse();

        // Roles check
        assertThat(caseDefinition.getRoles()).hasSize(3);
        assertThat(caseDefinition.getRoles().containsKey("owner")).as("Role 'owner' is missing.").isTrue();
        assertThat(caseDefinition.getRoles().containsKey("contact")).as("Role 'contact' is missing.").isTrue();
        assertThat(caseDefinition.getRoles().containsKey("participant")).as("Role 'participant' is missing.").isTrue();
    }

    private void assertCarInsuranceCaseDefinition(CaseDefinition caseDefinition) {
        assertThat(caseDefinition).isNotNull();
        assertThat(caseDefinition.getIdentifier()).isEqualTo(CLAIM_CASE_DEF_ID);
        assertThat(caseDefinition.getName()).isEqualTo(CLAIM_CASE_NAME);
        assertThat(caseDefinition.getCaseIdPrefix()).isEqualTo(CLAIM_CASE_ID_PREFIX);
        assertThat(caseDefinition.getVersion()).isEqualTo(CLAIM_CASE_VERSION);
        assertThat(caseDefinition.getAdHocFragments()).hasSize(1);
        KieServerAssert.assertNullOrEmpty("Milestones should be empty.", caseDefinition.getMilestones());
        assertThat(caseDefinition.getContainerId()).isEqualTo(CONTAINER_ID);

        // Stages check
        assertThat(caseDefinition.getCaseStages()).hasSize(3);
        assertThat(caseDefinition.getCaseStages().get(0).getName()).isEqualTo("Build claim report");
        assertThat(caseDefinition.getCaseStages().get(0).getIdentifier()).isNotNull();
        assertThat(caseDefinition.getCaseStages().get(1).getName()).isEqualTo("Claim assesment");
        assertThat(caseDefinition.getCaseStages().get(1).getIdentifier()).isNotNull();
        assertThat(caseDefinition.getCaseStages().get(2).getName()).isEqualTo("Escalate rejected claim");
        assertThat(caseDefinition.getCaseStages().get(2).getIdentifier()).isNotNull();

        List<CaseAdHocFragment> buildClaimFragments = caseDefinition.getCaseStages().get(0).getAdHocFragments();
        assertThat(buildClaimFragments).hasSize(2);
        assertThat(buildClaimFragments.get(0).getName()).isEqualTo("Provide accident information");
        assertThat(buildClaimFragments.get(0).getType()).isEqualTo("HumanTaskNode");
        assertThat(buildClaimFragments.get(1).getName()).isEqualTo("Submit police report");
        assertThat(buildClaimFragments.get(1).getType()).isEqualTo("HumanTaskNode");

        List<CaseAdHocFragment> claimAssesmentFragments = caseDefinition.getCaseStages().get(1).getAdHocFragments();
        assertThat(claimAssesmentFragments).hasSize(2);
        assertThat(claimAssesmentFragments.get(0).getName()).isEqualTo("Classify claim");
        assertThat(claimAssesmentFragments.get(0).getType()).isEqualTo("RuleSetNode");
        assertThat(claimAssesmentFragments.get(1).getName()).isEqualTo("Calculate claim");
        assertThat(claimAssesmentFragments.get(1).getType()).isEqualTo("WorkItemNode");

        List<CaseAdHocFragment> escalateRejectedClaimFragments = caseDefinition.getCaseStages().get(2).getAdHocFragments();
        assertThat(escalateRejectedClaimFragments).hasSize(1);
        assertThat(escalateRejectedClaimFragments.get(0).getName()).isEqualTo("Negotiation meeting");
        assertThat(escalateRejectedClaimFragments.get(0).getType()).isEqualTo("HumanTaskNode");

        // Roles check
        assertThat(caseDefinition.getRoles()).hasSize(3);
        assertThat(caseDefinition.getRoles().containsKey("insured")).as("Role 'insured' is missing.").isTrue();
        assertThat(caseDefinition.getRoles().containsKey("insuranceRepresentative")).as("Role 'insuranceRepresentative' is missing.").isTrue();
        assertThat(caseDefinition.getRoles().containsKey("assessor")).as("Role 'assessor' is missing.").isTrue();
    }

    private void assertHrProcessInstance(ProcessInstance processInstance, String caseId) {
        assertHrProcessInstance(processInstance, caseId, STATE_ACTIVE);
    }

    private void assertHrProcessInstance(ProcessInstance processInstance, String caseId, long processInstanceState) {
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getId()).isNotNull();
        assertThat(processInstance.getCorrelationKey()).isEqualTo(caseId);
        assertThat(processInstance.getState().intValue()).isEqualTo(processInstanceState);
        assertThat(processInstance.getProcessId()).isEqualTo(CASE_HR_DEF_ID);
        assertThat(processInstance.getProcessName()).isEqualTo(CASE_HR_NAME);
        assertThat(processInstance.getProcessVersion()).isEqualTo(CASE_HR_VERSION);
        assertThat(processInstance.getContainerId()).isEqualTo(CONTAINER_ID);
        assertThat(processInstance.getProcessInstanceDescription()).isEqualTo(CASE_HR_DESRIPTION);
        assertThat(processInstance.getInitiator()).isEqualTo(USER_YODA);
        assertThat(processInstance.getParentId().longValue()).isEqualTo(-1L);
        assertThat(processInstance.getCorrelationKey()).isNotNull();
        assertThat(processInstance.getDate()).isNotNull();
    }

    private void assertCarInsuranceProcessInstance(ProcessInstance processInstance, String caseId) {
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getId()).isNotNull();
        assertThat(processInstance.getCorrelationKey()).isEqualTo(caseId);
        assertThat(processInstance.getState().intValue()).isEqualTo(STATE_ACTIVE);
        assertThat(processInstance.getProcessId()).isEqualTo(CLAIM_CASE_DEF_ID);
        assertThat(processInstance.getProcessName()).isEqualTo(CLAIM_CASE_NAME);
        assertThat(processInstance.getProcessVersion()).isEqualTo(CLAIM_CASE_VERSION);
        assertThat(processInstance.getContainerId()).isEqualTo(CONTAINER_ID);
        assertThat(processInstance.getProcessInstanceDescription()).isEqualTo(CLAIM_CASE_DESRIPTION);
        assertThat(processInstance.getInitiator()).isEqualTo(USER_YODA);
        assertThat(processInstance.getParentId().longValue()).isEqualTo(-1L);
        assertThat(processInstance.getCorrelationKey()).isNotNull();
        assertThat(processInstance.getDate()).isNotNull();
    }

    private void assertBuildClaimReportCaseStage(CaseStage stage, String status) {
        assertThat(stage.getName()).isEqualTo("Build claim report");
        assertThat(stage.getIdentifier()).isNotNull();
        assertThat(stage.getStatus()).isEqualTo(status);

        if (status.endsWith("Active")) {
            List<NodeInstance> activeNodes = stage.getActiveNodes();
            assertThat(activeNodes).hasSize(1);
            assertThat(activeNodes.get(0).getName()).isEqualTo("Provide accident information");
            assertThat(activeNodes.get(0).getNodeType()).isEqualTo("HumanTaskNode");
        } else {
            KieServerAssert.assertNullOrEmpty("Active nodes should be null or empty.", stage.getActiveNodes());
        }

        List<CaseAdHocFragment> adHocFragments = stage.getAdHocFragments();
        assertThat(adHocFragments).hasSize(2);
        assertThat(adHocFragments.get(0).getName()).isEqualTo("Provide accident information");
        assertThat(adHocFragments.get(0).getType()).isEqualTo("HumanTaskNode");
        assertThat(adHocFragments.get(1).getName()).isEqualTo("Submit police report");
        assertThat(adHocFragments.get(1).getType()).isEqualTo("HumanTaskNode");
    }

    private void assertClaimAssesmentCaseStage(CaseStage stage, String status) {
        assertThat(stage.getName()).isEqualTo("Claim assesment");
        assertThat(stage.getIdentifier()).isNotNull();
        assertThat(stage.getStatus()).isEqualTo(status);

        if (status.endsWith("Active")) {
            List<NodeInstance> activeNodes = stage.getActiveNodes();
            assertThat(activeNodes).hasSize(1);
            assertThat(activeNodes.get(0).getName()).isEqualTo("Assessor evaluation");
            assertThat(activeNodes.get(0).getNodeType()).isEqualTo("HumanTaskNode");
        } else {
            KieServerAssert.assertNullOrEmpty("Active nodes should be null or empty.", stage.getActiveNodes());
        }

        List<CaseAdHocFragment> adHocFragments = stage.getAdHocFragments();
        assertThat(adHocFragments).hasSize(2);
        assertThat(adHocFragments.get(0).getName()).isEqualTo("Classify claim");
        assertThat(adHocFragments.get(0).getType()).isEqualTo("RuleSetNode");
        assertThat(adHocFragments.get(1).getName()).isEqualTo("Calculate claim");
        assertThat(adHocFragments.get(1).getType()).isEqualTo("WorkItemNode");
    }

    private void assertEscalateRejectedClaimCaseStage(CaseStage stage, String status) {
        assertThat(stage.getName()).isEqualTo("Escalate rejected claim");
        assertThat(stage.getIdentifier()).isNotNull();
        assertThat(stage.getStatus()).isEqualTo(status);

        KieServerAssert.assertNullOrEmpty("Active nodes should be null or empty.", stage.getActiveNodes());

        List<CaseAdHocFragment> adHocFragments = stage.getAdHocFragments();
        assertThat(adHocFragments).hasSize(1);
        assertThat(adHocFragments.get(0).getName()).isEqualTo("Negotiation meeting");
        assertThat(adHocFragments.get(0).getType()).isEqualTo("HumanTaskNode");
    }

    private void assertComment(CaseComment comment, String author, String text) {
        assertThat(comment).isNotNull();
        assertThat(author).isEqualTo(comment.getAuthor());
        assertThat(text).isEqualTo(comment.getText());
    }
}
