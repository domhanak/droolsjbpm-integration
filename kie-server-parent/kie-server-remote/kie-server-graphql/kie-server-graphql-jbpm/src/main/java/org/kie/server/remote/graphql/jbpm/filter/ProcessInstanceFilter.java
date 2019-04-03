package org.kie.server.remote.graphql.jbpm.filter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.kie.server.remote.graphql.jbpm.exceptions.NoFilterMatchException;

public class ProcessInstanceFilter {
    private List<Integer> statesIn;
    private String initiator;
    private String containerId;
    private String processId;
    private String correlationKey;
    private String processName;
    private String variableName;
    private String variableValue;

    public enum AllowedCombinations {
        PROCESS_ID,
        CORRELATION_KEY,
        STATES_AND_INITIATOR,
        STATES_AND_CONTAINER_ID,
        STATES_AND_CORRELATION_KEY,
        STATES_AND_VARIABLE_NAME,
        STATES_AND_VARIABLE_NAME_AND_VARIABLE_VALUE,
        STATES_AND_INITIATOR_AND_PROCESS_ID,
        STATES_AND_INITIATOR_AND_PROCESS_NAME,
    }

    public AllowedCombinations getSelectedFilterCombination() {
        if (processId != null && (initiator == null && containerId == null && correlationKey == null && processName == null
                && variableName == null && variableValue == null && statesIn == null)) {
            return AllowedCombinations.PROCESS_ID;
        }
        if (correlationKey != null && (initiator == null && containerId == null && processId == null && processName == null
                && variableName == null && variableValue == null && statesIn == null)) {
            return AllowedCombinations.CORRELATION_KEY;
        }
        if (statesIn != null && initiator != null && (containerId == null && processId == null && processName == null
                && variableName == null && variableValue == null && correlationKey == null)) {
            return AllowedCombinations.STATES_AND_INITIATOR;
        }
        if (statesIn != null && containerId != null && (initiator == null && processId == null && processName == null
                && variableName == null && variableValue == null && correlationKey == null)) {
            return AllowedCombinations.STATES_AND_CONTAINER_ID;
        }
        if (statesIn != null && correlationKey != null && (containerId == null && processId == null && processName == null
                && variableName == null && variableValue == null && initiator == null)) {
            return AllowedCombinations.STATES_AND_CORRELATION_KEY;
        }
        if (statesIn != null && variableName != null && (containerId == null && processId == null && processName == null
                && initiator == null && variableValue == null && correlationKey == null)) {
            return AllowedCombinations.STATES_AND_VARIABLE_NAME;
        }
        if (statesIn != null && variableName != null && variableValue != null && (containerId == null && processId == null
                && processName == null && initiator == null  && correlationKey == null)) {
            return AllowedCombinations.STATES_AND_VARIABLE_NAME_AND_VARIABLE_VALUE;
        }
        if (statesIn != null && initiator != null && processId != null && (containerId == null && variableValue == null
                && processName == null && variableName == null  && correlationKey == null)) {
            return AllowedCombinations.STATES_AND_INITIATOR_AND_PROCESS_ID;
        }
        if (statesIn != null && initiator != null && processName != null && (containerId == null && variableValue == null
                && processId == null && variableName == null  && correlationKey == null)) {
            return AllowedCombinations.STATES_AND_INITIATOR_AND_PROCESS_NAME;
        }

        throw new NoFilterMatchException("Selected filter properties do not match any know combinations.");
    }

    @JsonProperty("statesIn")
    public List<Integer> getStatesIn() {
        return statesIn;
    }

    public void setStatesIn(List<Integer> statesIn) {
        this.statesIn = statesIn;
    }

    @JsonProperty("initiator")
    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    @JsonProperty("containerId")
    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    @JsonProperty("processId")
    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    @JsonProperty("correlationKey")
    public String getCorrelationKey() {
        return correlationKey;
    }

    public void setCorrelationKey(String correlationKey) {
        this.correlationKey = correlationKey;
    }

    @JsonProperty("processName")
    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
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
}
