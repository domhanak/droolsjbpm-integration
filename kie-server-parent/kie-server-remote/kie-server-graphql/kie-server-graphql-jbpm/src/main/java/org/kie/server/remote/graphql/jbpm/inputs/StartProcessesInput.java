package org.kie.server.remote.graphql.jbpm.inputs;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StartProcessesInput {
    private int batchSize;
    private String id;
    private String containerId;
    private String correlationKey;
    private Map<String, Object> variables;

    @JsonProperty("batchSize")
    public int getBatchSize() {
        return batchSize;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("containerId")
    public String getContainerId() {
        return containerId;
    }

    @JsonProperty("correlationKey")
    public String getCorrelationKey() {
        return correlationKey;
    }

    @JsonProperty("variables")
    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public void setCorrelationKey(String correlationKey) {
        this.correlationKey = correlationKey;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
}
