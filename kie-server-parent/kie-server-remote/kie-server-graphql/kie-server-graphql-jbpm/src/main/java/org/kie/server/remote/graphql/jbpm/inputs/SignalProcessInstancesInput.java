package org.kie.server.remote.graphql.jbpm.inputs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SignalProcessInstancesInput {
    private List<Long> ids;
    private String signalName;
    private Object event;
    private String containerId;

    @JsonProperty("ids")
    public List<Long> getIds() {
        return ids;
    }

    @JsonProperty("signalName")
    public String getSignalName() {
        return signalName;
    }

    @JsonProperty("event")
    public Object getEvent() {
        return event;
    }

    @JsonProperty("containerId")
    public String getContainerId() {
        return containerId;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public void setSignalName(String signalName) {
        this.signalName = signalName;
    }

    public void setEvent(Object event) {
        this.event = event;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }
}
