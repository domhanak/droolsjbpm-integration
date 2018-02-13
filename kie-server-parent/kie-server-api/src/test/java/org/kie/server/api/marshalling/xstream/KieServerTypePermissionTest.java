/*
 * Copyright 2017 - 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.api.marshalling.xstream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.kie.server.api.KieServerConstants.SYSTEM_XSTREAM_ENABLED_PACKAGES;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.kie.server.api.marshalling.objects.AnotherMessage;
import org.kie.server.api.marshalling.objects.Message;
import org.kie.server.api.marshalling.objects.Top;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.admin.EmailNotification;
import org.kie.server.api.model.cases.CaseDefinition;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.dmn.DMNModelInfo;

public class KieServerTypePermissionTest {

    @After
    public void cleanup() {
        System.clearProperty(SYSTEM_XSTREAM_ENABLED_PACKAGES);
    }
    
    @Test
    public void testDefaultAcceptableClasses() {
        
        KieServerTypePermission permission = new KieServerTypePermission(new HashSet<>());
        
        assertThat(permission.allows(KieContainerResource.class)).isTrue();
        assertThat(permission.allows(ProcessDefinition.class)).isTrue();
        assertThat(permission.allows(EmailNotification.class)).isTrue();
        assertThat(permission.allows(CaseDefinition.class)).isTrue();
        assertThat(permission.allows(DMNModelInfo.class)).isTrue();
    }
    
    @Test
    public void testDefaultForbiddenClasses() {
        
        KieServerTypePermission permission = new KieServerTypePermission(new HashSet<>());
        
        assertThat(permission.allows(Top.class)).isFalse();
        
    }
    
    @Test
    public void testExplicitlyGivenClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(Top.class);
        KieServerTypePermission permission = new KieServerTypePermission(classes);
        
        assertThat(permission.allows(Top.class)).isTrue();
        
    }
    
    @Test
    public void testSystemPropertyGivenClasses() {
        System.setProperty(SYSTEM_XSTREAM_ENABLED_PACKAGES, "org.kie.server.api.marshalling.objects.Top,org.kie.server.api.marshalling.objects.Message");
        
        Set<Class<?>> classes = new HashSet<>();    
        KieServerTypePermission permission = new KieServerTypePermission(classes);
        
        assertThat(permission.allows(Top.class)).isTrue();
        assertThat(permission.allows(Message.class)).isTrue();
        assertThat(permission.allows(AnotherMessage.class)).isFalse();
    }
}
