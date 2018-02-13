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

package org.kie.server.client;

import java.net.URLDecoder;
import java.net.URLEncoder;

import org.junit.Test;
import org.kie.server.api.ConversationId;
import org.kie.server.api.model.ReleaseId;

import static org.assertj.core.api.Assertions.*;

public class ConversationIdTest {

    @Test
    public void testParseConversationIdFromString() throws Exception {
         String conversationId = "'kie-server-id':'my-container':'org.kie:kjar:1.0':'12345abcdef'";

        ConversationId instance = ConversationId.fromString(conversationId);
        assertThat(instance).isNotNull();

        assertThat(instance.getKieServerId()).isEqualTo("kie-server-id");
        assertThat(instance.getContainerId()).isEqualTo("my-container");
        assertThat(instance.getReleaseId().toExternalForm()).isEqualTo("org.kie:kjar:1.0");
        assertThat(instance.getUniqueString()).isEqualTo("12345abcdef");

        String conversationIdUrlEncoded = URLEncoder.encode(conversationId, "UTF-8");
        assertThat(instance.toString()).isEqualTo(conversationIdUrlEncoded);
    }

    @Test
    public void testParseConversationId() {
        ConversationId instance = ConversationId.from("kie-server-id", "my-container", new ReleaseId("org.kie", "kjar", "1.0"));
        assertThat(instance).isNotNull();

        assertThat(instance.getKieServerId()).isEqualTo("kie-server-id");
        assertThat(instance.getContainerId()).isEqualTo("my-container");
        assertThat(instance.getReleaseId().toExternalForm()).isEqualTo("org.kie:kjar:1.0");

    }

    @Test
    public void testParseConversationIdWithColonInName() {
        ConversationId instance = ConversationId.from("kie:server:id", "my:container", new ReleaseId("org.kie", "kjar", "1.0"));
        assertThat(instance).isNotNull();

        assertThat(instance.getKieServerId()).isEqualTo("kie:server:id");
        assertThat(instance.getContainerId()).isEqualTo("my:container");
        assertThat(instance.getReleaseId().toExternalForm()).isEqualTo("org.kie:kjar:1.0");

    }

    @Test
    public void testParseConversationRoundTripping() throws Exception {
        String conversationId = "'kie-server-id':'my-container':'org.kie:kjar:1.0':'12345abcdef'";

        ConversationId instance = ConversationId.fromString(conversationId);
        assertThat(instance).isNotNull();

        assertThat(instance.getKieServerId()).isEqualTo("kie-server-id");
        assertThat(instance.getContainerId()).isEqualTo("my-container");
        assertThat(instance.getReleaseId().toExternalForm()).isEqualTo("org.kie:kjar:1.0");
        assertThat(instance.getUniqueString()).isEqualTo("12345abcdef");

        // check the incoming toString
        String conversationIdUrlEncoded = URLEncoder.encode(conversationId, "UTF-8");
        assertThat(instance.toString()).isEqualTo(conversationIdUrlEncoded);

        // url decode it and check with raw conversationId
        assertThat("UTF-8")).isEqualTo(conversationId, URLDecoder.decode(instance.toString());

        // now build ConversationId from url encoded string
        instance = ConversationId.fromString(instance.toString());
        assertThat(instance).isNotNull();

        assertThat(instance.getKieServerId()).isEqualTo("kie-server-id");
        assertThat(instance.getContainerId()).isEqualTo("my-container");
        assertThat(instance.getReleaseId().toExternalForm()).isEqualTo("org.kie:kjar:1.0");
        assertThat(instance.getUniqueString()).isEqualTo("12345abcdef");
    }
}
