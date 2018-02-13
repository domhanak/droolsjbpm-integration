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

package org.kie.server.api.marshalling;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class MarshallingFormatTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testEmptyMarshallingFormat() {
        exception.expectMessage("Invalid marshalling format []");
        MarshallingFormat.fromType("");
    }

    @Test
    public void testNullMarshallingFormat() {
        exception.expectMessage("Invalid marshalling format [null]");
        MarshallingFormat.fromType(null);
    }

    @Test
    public void testNonNullEmptyInvalidMarshallingFormat() {
        exception.expectMessage("Invalid marshalling format [JAX]");
        MarshallingFormat.fromType("JAX");
    }

    @Test
    public void testExpectedMarshallingFormats() {
        assertThat(MarshallingFormat.fromType("json")).isEqualTo(MarshallingFormat.JSON);
        assertThat(MarshallingFormat.fromType("xml")).isEqualTo(MarshallingFormat.JAXB);
        assertThat(MarshallingFormat.fromType("xstream")).isEqualTo(MarshallingFormat.XSTREAM);

        assertThat(MarshallingFormat.fromType("application/json")).isEqualTo(MarshallingFormat.JSON);
        assertThat(MarshallingFormat.fromType("application/xml")).isEqualTo(MarshallingFormat.JAXB);
        assertThat(MarshallingFormat.fromType("application/xstream")).isEqualTo(MarshallingFormat.XSTREAM);
    }

    @Test
    public void testMarshallingFormatsWithExtraneousParameters() {
        assertThat(MarshallingFormat.fromType("application/json;")).isEqualTo(MarshallingFormat.JSON);
        assertThat(MarshallingFormat.fromType("application/xml;")).isEqualTo(MarshallingFormat.JAXB);
        assertThat(MarshallingFormat.fromType("application/xstream;")).isEqualTo(MarshallingFormat.XSTREAM);
        assertThat(MarshallingFormat.fromType("application/json;encode=")).isEqualTo(MarshallingFormat.JSON);
        assertThat(MarshallingFormat.fromType("application/xml;encode=utf-8")).isEqualTo(MarshallingFormat.JAXB);
        assertThat(MarshallingFormat.fromType("application/xstream;utf-8")).isEqualTo(MarshallingFormat.XSTREAM);
    }

    @Test
    public void testMarshallingFormatCase() {
        assertThat(MarshallingFormat.fromType("JSON")).isEqualTo(MarshallingFormat.JSON);
    }

    @Test
    public void testEdgeCaseWithJaxb() {
        assertThat(MarshallingFormat.fromType("jaxb")).isEqualTo(MarshallingFormat.JAXB);
        assertThat(MarshallingFormat.fromType("JAXB")).isEqualTo(MarshallingFormat.JAXB);
    }
}
