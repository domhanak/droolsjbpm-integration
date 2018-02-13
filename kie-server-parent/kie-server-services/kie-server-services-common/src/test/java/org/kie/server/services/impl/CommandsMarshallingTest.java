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

package org.kie.server.services.impl;

import java.util.concurrent.TimeUnit;

import org.drools.core.command.runtime.AdvanceSessionTimeCommand;
import org.drools.core.command.runtime.DisposeCommand;
import org.drools.core.command.runtime.GetGlobalCommand;
import org.drools.core.command.runtime.GetSessionTimeCommand;
import org.drools.core.command.runtime.SetGlobalCommand;
import org.drools.core.command.runtime.process.StartProcessCommand;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.drools.core.command.runtime.rule.DeleteCommand;
import org.drools.core.command.runtime.rule.FireAllRulesCommand;
import org.drools.core.command.runtime.rule.GetFactHandlesCommand;
import org.drools.core.command.runtime.rule.GetObjectCommand;
import org.drools.core.command.runtime.rule.GetObjectsCommand;
import org.drools.core.command.runtime.rule.InsertElementsCommand;
import org.drools.core.command.runtime.rule.InsertObjectCommand;
import org.drools.core.command.runtime.rule.ModifyCommand;
import org.drools.core.command.runtime.rule.QueryCommand;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.server.api.marshalling.Marshaller;
import org.kie.server.api.marshalling.MarshallerFactory;
import org.kie.server.api.marshalling.MarshallingFormat;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandsMarshallingTest {
    private Marshaller marshaller = MarshallerFactory.getMarshaller( MarshallingFormat.XSTREAM, Thread.currentThread().getContextClassLoader() );

    @Test
    public void testMarshallInsertObjectCommand() {
        String xmlCommand = "<insert>\n" +
                            "  <string>String value</string>\n" +
                            "</insert>";
        InsertObjectCommand command = marshaller.unmarshall( xmlCommand, InsertObjectCommand.class );
        assertThat(command.getObject().toString() ).isEqualTo("String value");

        assertThat(marshaller.marshall( command ) ).isEqualTo(xmlCommand);
    }

    @Test
    public void testMarshallModifyCommand() {
        String xmlCommand = "<modify fact-handle=\"0:234:345:456:567:789\">\n" +
                            "  <set accessor=\"age\" value=\"30\"/>\n" +
                            "</modify>";
        ModifyCommand command = marshaller.unmarshall(xmlCommand, ModifyCommand.class);
        assertThat(command.getSetters()).hasSize(1);

        assertEquals("<modify fact-handle=\"0:234:345:456:567:789:NON_TRAIT:null\">\n" +
                "  <set accessor=\"age\" value=\"30\"/>\n" +
                "</modify>", marshaller.marshall(command));
    }

    @Test
    public void testMarshallGetObjectCommand() {
        String xmlCommand = "<get-object fact-handle=\"0:234:345:456:567:789\" out-identifier=\"test\"/>";
        GetObjectCommand command = marshaller.unmarshall(xmlCommand, GetObjectCommand.class);
        assertThat(command.getOutIdentifier()).isEqualTo("test");

        assertThat(marshaller.marshall(command)).isEqualTo("<get-object fact-handle=\"0:234:345:456:567:789:NON_TRAIT:null\" out-identifier=\"test\"/>");
    }

    @Test
    public void testMarshallInsertElementsCommand() {
        String xmlCommand = "<insert-elements>\n" +
                "  <string>test1</string>\n" +
                "  <string>test2</string>\n" +
                "</insert-elements>";
        InsertElementsCommand command = marshaller.unmarshall(xmlCommand, InsertElementsCommand.class);
        assertThat(command.getObjects()).hasSize(2);

        assertThat(marshaller.marshall(command)).isEqualTo(xmlCommand);
    }

    @Test
    public void testMarshallFireAllRulesCommand() {
        String xmlCommand = "<fire-all-rules max=\"10\" out-identifier=\"result\"/>";
        FireAllRulesCommand command = marshaller.unmarshall(xmlCommand, FireAllRulesCommand.class);
        assertThat(command.getMax()).isEqualTo(10);

        assertThat(marshaller.marshall(command)).isEqualTo(xmlCommand);
    }
    
    @Test
    public void testMarshallDisposeCommand() {
        String xmlCommand = "<dispose/>";
        DisposeCommand command = marshaller.unmarshall(xmlCommand, DisposeCommand.class);

        assertThat(marshaller.marshall(command)).isEqualTo(xmlCommand);
    }

    @Test
    public void testMarshallStartProcessCommand() {
        String xmlCommand = "<start-process processId=\"org.drools.task.processOne\" out-identifier=\"id\"/>";
        StartProcessCommand command = marshaller.unmarshall(xmlCommand, StartProcessCommand.class);
        assertThat(command.getProcessId()).isEqualTo("org.drools.task.processOne");

        assertThat(marshaller.marshall(command)).isEqualTo(xmlCommand);
    }

    @Test
    public void testMarshallQueryCommand() {
        String xmlCommand = "<query out-identifier=\"persons-out\" name=\"persons\"/>";
        QueryCommand command = marshaller.unmarshall(xmlCommand, QueryCommand.class);
        assertThat(command.getName()).isEqualTo("persons");

        assertThat(marshaller.marshall(command)).isEqualTo(xmlCommand);
    }

    @Test
    public void testMarshallSetGlobalCommand() {
        String xmlCommand = "<set-global identifier=\"helper\" out-identifier=\"output\">\n" +
                "  <list/>\n" +
                "</set-global>";
        SetGlobalCommand command = marshaller.unmarshall(xmlCommand, SetGlobalCommand.class);
        assertThat(command.getIdentifier()).isEqualTo("helper");

        assertThat(marshaller.marshall(command)).isEqualTo(xmlCommand);
    }

    @Test
    public void testMarshallGetGlobalCommand() {
        String xmlCommand = "<get-global identifier=\"helper\" out-identifier=\"helperOutput\"/>";
        GetGlobalCommand command = marshaller.unmarshall(xmlCommand, GetGlobalCommand.class);
        assertThat(command.getIdentifier()).isEqualTo("helper");

        assertThat(marshaller.marshall(command)).isEqualTo(xmlCommand);
    }

    @Test
    public void testMarshallGetObjectsCommand() {
        String xmlCommand = "<get-objects out-identifier=\"objects\"/>";
        GetObjectsCommand command = marshaller.unmarshall(xmlCommand, GetObjectsCommand.class);
        assertThat(command.getOutIdentifier()).isEqualTo("objects");

        assertThat(marshaller.marshall(command)).isEqualTo(xmlCommand);
    }

    @Test
    @Ignore("Set focus command not yet supported")
    public void testMarshallAgendaGroupSetFocusCommand() {
        String xmlCommand = "<agenda-group-set-focus name=\"my-agenda-group\"/>";
        AgendaGroupSetFocusCommand command = marshaller.unmarshall(xmlCommand, AgendaGroupSetFocusCommand.class);
        assertThat(command.getName()).isEqualTo("my-agenda-group");

        assertThat(marshaller.marshall(command)).isEqualTo(xmlCommand);
    }

    @Test
    public void testMarshallDeleteCommand() {
        String xmlCommand = "<delete fact-handle=\"0:234:345:456:567:789\"/>";
        DeleteCommand command = marshaller.unmarshall(xmlCommand, DeleteCommand.class);

        assertThat(marshaller.marshall(command)).isEqualTo("<delete fact-handle=\"0:234:345:456:567:789:NON_TRAIT:null\"/>");
    }

    @Test
    public void testMarshallGetFactHandlesCommand() {
        String xmlCommand = "<get-fact-handles/>";
        GetFactHandlesCommand command = marshaller.unmarshall( xmlCommand, GetFactHandlesCommand.class );

        assertThat(marshaller.marshall(command)).isEqualTo("<get-fact-handles disconnected=\"false\"/>");
    }

    @Test
    public void testMarshallGetSessionTimeCommand() {
        String xmlCommand = "<get-session-time out-identifier=\"session-currenttime\"/>";
        GetSessionTimeCommand command = marshaller.unmarshall( xmlCommand, GetSessionTimeCommand.class );
        assertThat(marshaller.marshall(command)).isEqualTo(xmlCommand);
    }

    @Test
    public void testMarshallAdvanceSessionTimeCommand() {
        String xmlCommand = "<advance-session-time out-identifier=\"session-advancecurrenttime\" amount=\"2\" unit=\"DAYS\"/>";
        AdvanceSessionTimeCommand command = marshaller.unmarshall( xmlCommand, AdvanceSessionTimeCommand.class );
        assertThat(command.getAmount() ).isEqualTo(2L);
        assertThat(command.getUnit() ).isEqualTo(TimeUnit.DAYS);
        assertThat(marshaller.marshall(command)).isEqualTo(xmlCommand);
    }

    // TODO determine what other commands are supported and add tests for them

}
