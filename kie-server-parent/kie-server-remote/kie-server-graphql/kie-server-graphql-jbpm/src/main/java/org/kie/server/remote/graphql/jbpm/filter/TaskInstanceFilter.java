package org.kie.server.remote.graphql.jbpm.filter;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.kie.server.remote.graphql.jbpm.exceptions.NoFilterMatchException;

public class TaskInstanceFilter {

    private List<String> statesIn;
    private String businessAdminId;
    private String potentialOwnerId;
    private List<String> groupIds;
    private Date fromExpirationalDate;
    private Long processInstanceId;
    private String variableName;
    private String variableValue;
    private String ownerId;

    public enum AllowedCombinations {
        OWNER_ID,
        POTENTIAL_OWNER_ID_AND_GROUP_IDS,
        STATES_AND_POTENTIAL_OWNER_ID,
        STATES_AND_BUSINESS_ADMIN_ID,
        STATES_AND_PROCESS_INSTANCE_ID,
        STATES_AND_POTENTIAL_OWNER_ID_AND_FROM_DATE,
        STATES_AND_POTENTIAL_OWNER_ID_AND_GROUP_IDS,
        STATES_AND_OWNER_ID_AND_VARIABLE_NAME,
        STATES_AND_OWNER_ID_AND_VARIABLE_NAME_AND_VARIABLE_VALUE,
    }

    public AllowedCombinations getSelectedFilterCombination() {
        if (ownerId != null && (businessAdminId == null && potentialOwnerId == null && groupIds == null && fromExpirationalDate == null
                && processInstanceId == null && variableName == null && variableValue == null && statesIn == null)) {
            return AllowedCombinations.OWNER_ID;
        }
        if (potentialOwnerId != null && groupIds != null && (businessAdminId == null && ownerId == null && fromExpirationalDate == null
                && processInstanceId == null && variableName == null && variableValue == null && statesIn == null)) {
            return AllowedCombinations.POTENTIAL_OWNER_ID_AND_GROUP_IDS;
        }
        if (statesIn != null && potentialOwnerId != null && (businessAdminId == null && ownerId == null && groupIds == null && fromExpirationalDate == null
                && processInstanceId == null && variableName == null && variableValue == null)) {
            return AllowedCombinations.STATES_AND_POTENTIAL_OWNER_ID;
        }
        if (statesIn != null && businessAdminId != null && (potentialOwnerId == null && ownerId == null && groupIds == null && fromExpirationalDate == null
                && processInstanceId == null && variableName == null && variableValue == null)) {
            return AllowedCombinations.STATES_AND_BUSINESS_ADMIN_ID;
        }
        if (statesIn != null && processInstanceId != null && (potentialOwnerId == null && ownerId == null && groupIds == null && fromExpirationalDate == null
                && businessAdminId == null && variableName == null && variableValue == null)) {
            return AllowedCombinations.STATES_AND_PROCESS_INSTANCE_ID;
        }
        if (statesIn != null && potentialOwnerId != null && fromExpirationalDate != null && (processInstanceId == null && ownerId == null && groupIds == null
                && businessAdminId == null && variableName == null && variableValue == null)) {
            return AllowedCombinations.STATES_AND_POTENTIAL_OWNER_ID_AND_FROM_DATE;
        }
        if (statesIn != null && potentialOwnerId != null && groupIds != null && (processInstanceId == null && ownerId == null && fromExpirationalDate == null
                && businessAdminId == null && variableName == null && variableValue == null)) {
            return AllowedCombinations.STATES_AND_POTENTIAL_OWNER_ID_AND_GROUP_IDS;
        }
        if (statesIn != null && ownerId != null && variableName != null && (processInstanceId == null && potentialOwnerId == null && fromExpirationalDate == null
                && businessAdminId == null && groupIds == null && variableValue == null)) {
            return AllowedCombinations.STATES_AND_OWNER_ID_AND_VARIABLE_NAME;
        }
        if (statesIn != null && ownerId != null && variableName != null && variableValue != null && (processInstanceId == null && potentialOwnerId == null
                && fromExpirationalDate == null && businessAdminId == null && groupIds == null)) {
            return AllowedCombinations.STATES_AND_OWNER_ID_AND_VARIABLE_NAME_AND_VARIABLE_VALUE;
        }
        throw new NoFilterMatchException("Selected filter properties do not match any know combinations.");
    }

    @JsonProperty("statesIn")
    public List<String> getStatesIn() {
        return statesIn;
    }

    public void setStatesIn(List<String> statesIn) {
        this.statesIn = statesIn;
    }

    @JsonProperty("businessAdminId")
    public String getBusinessAdminId() {
        return businessAdminId;
    }

    public void setBusinessAdminId(String businessAdminId) {
        this.businessAdminId = businessAdminId;
    }

    @JsonProperty("potentialOwnerId")
    public String getPotentialOwnerId() {
        return potentialOwnerId;
    }

    public void setPotentialOwnerId(String potentionalOwnerId) {
        this.potentialOwnerId = potentionalOwnerId;
    }

    @JsonProperty("groupIds")
    public List<String> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<String> groupId) {
        this.groupIds = groupId;
    }

    @JsonProperty("fromExpirationalDate")
    public Date getFromExpirationalDate() {
        return fromExpirationalDate;
    }

    public void setFromExpirationalDate(Date fromExpirationalDate) {
        this.fromExpirationalDate = fromExpirationalDate;
    }

    @JsonProperty("processInstanceId")
    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @JsonProperty("variableName")
    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    @JsonProperty("variableValue")
    public String getVariableValue() {
        return variableValue;
    }

    public void setVariableValue(String variableValue) {
        this.variableValue = variableValue;
    }

    @JsonProperty("ownerId")
    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
}
