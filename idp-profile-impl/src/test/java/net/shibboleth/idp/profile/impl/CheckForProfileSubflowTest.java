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

package net.shibboleth.idp.profile.impl;

import net.shibboleth.idp.profile.ActionTestingSupport;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.relyingparty.MockProfileConfiguration;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link CheckForProfileSubflow} unit test. */
public class CheckForProfileSubflowTest {

    private RequestContext src;
    
    private ProfileRequestContext prc;

    private CheckForProfileSubflow action;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);

        action = new CheckForProfileSubflow();
        action.setDirection(CheckForProfileSubflow.Direction.INBOUND);
    }
    
    @Test public void testNoRelyingPartyContext() throws Exception {
        prc.removeSubcontext(RelyingPartyContext.class);
        
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }

    @Test public void testNoProfileConfiguration() throws Exception {
        prc.getSubcontext(RelyingPartyContext.class).setProfileConfig(null);
        
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }

    @Test public void testNoFlow() throws Exception {
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }
    
    @Test public void testInboundFlow() throws Exception {
        final MockProfileConfiguration mock = new MockProfileConfiguration("test");
        mock.setInboundSubflowId("foo");
        prc.getSubcontext(RelyingPartyContext.class).setProfileConfig(mock);
        
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "foo");
    }
    
    @Test public void testOutboundFlow() throws Exception {
        final MockProfileConfiguration mock = new MockProfileConfiguration("test");
        mock.setOutboundSubflowId("foo");
        prc.getSubcontext(RelyingPartyContext.class).setProfileConfig(mock);
        
        action.setDirection(CheckForProfileSubflow.Direction.OUTBOUND);
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "foo");
    }

}