package org.kie.server.remote.graphql.client.helper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class JbpmGraphQLResponseData {

    @JsonProperty("data")
    private JsonNode data;

    @JsonProperty("extensions")
    private JsonNode extensions;

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }

    public JsonNode getExtensions() {
        return extensions;
    }

    public void setExtensions(JsonNode extensions) {
        this.extensions = extensions;
    }

    @Override
    public String toString() {
        return "JbpmGraphQLResponseData{" +
                "data=" + data +
                ", extensions=" + extensions +
                '}';
    }
}
