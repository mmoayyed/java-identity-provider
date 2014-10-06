/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.profile.spring.relyingparty.metadata;

import java.io.IOException;

import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.impl.ReloadMetadata;
import net.shibboleth.idp.profile.impl.ReloadServiceConfiguration;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

import org.joda.time.DateTime;
import org.opensaml.saml.metadata.resolver.RefreshableMetadataResolver;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ReloadServiceConfigurationTest extends AbstractMetadataParserTest {

    /** the service. */
    private ReloadableService<RefreshableMetadataResolver> service;

    private RequestContext src;
    
    @BeforeClass public void setup() throws IOException {
        service = getBean(ReloadableService.class, "../reload/beans.xml");
    }
    
    @BeforeMethod public void setUpAction() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        new WebflowRequestContextProfileRequestContextLookup().apply(src);
    }

    public void service() {
        final DateTime time = service.getLastReloadAttemptInstant();

        service.reload();
        Assert.assertNotEquals(time, service.getLastReloadAttemptInstant());
    }

    public void metadata() throws ResolverException {
        final ServiceableComponent<RefreshableMetadataResolver> comp = service.getServiceableComponent();
        // No obvious way to check metadata refresh
        // TODO
        try {
            comp.getComponent().refresh();
        } finally {
            comp.unpinComponent();
        }
    }
    
    public void serviceAction() throws ComponentInitializationException {
        final DateTime time = service.getLastReloadAttemptInstant();

        final ReloadServiceConfiguration action = new ReloadServiceConfiguration();
        //action.setMetadataResolver(service);
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        Assert.assertNotEquals(time, service.getLastReloadAttemptInstant());        
    }
    
    public void metadataAction() throws ComponentInitializationException {
        final DateTime time = service.getLastReloadAttemptInstant();

        final ReloadMetadata action = new ReloadMetadata();
        action.setMetadataResolver(service);
        action.initialize();

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        Assert.assertEquals(time, service.getLastReloadAttemptInstant());        
    }

    @AfterClass public void teardown() {
        ComponentSupport.destroy(service);
    }

}
