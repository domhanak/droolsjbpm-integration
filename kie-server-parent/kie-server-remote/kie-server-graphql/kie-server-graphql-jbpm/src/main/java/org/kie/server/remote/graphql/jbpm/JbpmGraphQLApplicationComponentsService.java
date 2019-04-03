/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.remote.graphql.jbpm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import graphql.GraphQL;

import org.jbpm.services.api.DefinitionService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;

import org.kie.api.executor.ExecutorService;
import org.kie.server.services.api.KieServerApplicationComponentsService;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.api.SupportedTransports;
import org.kie.server.services.jbpm.DefinitionServiceBase;
import org.kie.server.services.jbpm.JbpmKieServerExtension;

public class JbpmGraphQLApplicationComponentsService implements KieServerApplicationComponentsService {

    private static final String OWNER_EXTENSION = JbpmKieServerExtension.EXTENSION_NAME;

    @Override
    public Collection<Object> getAppComponents(String extension, SupportedTransports type, Object... services) {
        // skip calls from other than owning extension
        if ( !OWNER_EXTENSION.equals(extension) ) {
            return Collections.emptyList();
        }

        ProcessService processService = null;
        RuntimeDataService runtimeDataService = null;
        DefinitionService definitionService = null;
        KieServerRegistry context = null;

        for( Object object : services ) {
            // in case given service is null (meaning was not configured) continue with next one
            if (object == null) {
                continue;
            }
            if( ProcessService.class.isAssignableFrom(object.getClass()) ) {
                processService = (ProcessService) object;
            } else if( RuntimeDataService.class.isAssignableFrom(object.getClass()) ) {
                runtimeDataService = (RuntimeDataService) object;
            } else if( DefinitionService.class.isAssignableFrom(object.getClass()) ) {
                definitionService = (DefinitionService) object;
            } else if (KieServerRegistry.class.isAssignableFrom(object.getClass())) {
                context = (KieServerRegistry) object;
            }
        }

        DefinitionServiceBase definitionServiceBase = new DefinitionServiceBase(definitionService, context);
        JbpmGraphQLServiceProvider serviceProvider = new JbpmGraphQLServiceProvider(definitionServiceBase,
                                                                                    processService,
                                                                                    runtimeDataService,
                                                                                    context);

        List<Object> components = new ArrayList<>();
        components.add(new JbpmGraphQLResource(serviceProvider, context));
        return components;
    }
}