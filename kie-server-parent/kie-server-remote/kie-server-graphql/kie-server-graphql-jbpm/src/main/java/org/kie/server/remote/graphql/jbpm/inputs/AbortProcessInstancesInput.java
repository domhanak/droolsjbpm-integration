package org.kie.server.remote.graphql.jbpm.inputs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AbortProcessInstancesInput {
    private List<Long> instanceIds;
    private String containerId;

    @JsonProperty("ids")
    public List<Long> getIds() {
        return instanceIds;
    }

    public void setIds(List<Long> instanceIds) {
        this.instanceIds = instanceIds;
    }

    @JsonProperty("containerId")
    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }
}
