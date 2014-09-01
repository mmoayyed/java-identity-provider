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

package net.shibboleth.idp.session.impl;

import java.util.Collections;

import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.context.MultiRelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.session.BasicSPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link ProcessLogoutRelyingParties} unit test. */
public class ProcessLogoutRelyingPartiesText {
    
    private RequestContext src;
    
    private ProfileRequestContext prc;
    
    private PopulateMultiRPContextFromLogoutContext populate;
    
    private ProcessLogoutRelyingParties action;
    
    @BeforeMethod public void setUpAction() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);

        populate = new PopulateMultiRPContextFromLogoutContext();
        populate.initialize();
        
        action = new ProcessLogoutRelyingParties();
    }
    
    @Test public void testNoContext() throws ComponentInitializationException {
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNull(prc.getSubcontext(MultiRelyingPartyContext.class));
    }
    
    @Test public void testEmptyContext() throws ComponentInitializationException {
        prc.getSubcontext(LogoutContext.class, true);
        
        action.initialize();
        Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
        
        prc.getSubcontext(MultiRelyingPartyContext.class, true);
        event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }

    @Test public void testUnregistered() throws ComponentInitializationException {
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class, true);
        logoutCtx.getSessions("foo").add(new BasicSPSession("foo", "bar", 1, 1));
        
        Event event = populate.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        action.initialize();
        event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }

    @Test public void testRegistered() throws SessionException, ComponentInitializationException {
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class, true);
        logoutCtx.getSessions("foo").add(new BasicSPSession("foo", "bar", 1, 1));
        logoutCtx.getSessions("foo").add(new BasicSPSession("foo", "bar", 1, 1));

        Event event = populate.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        action.setElaborationFlowMap(Collections.<Class<? extends SPSession>,String>singletonMap(BasicSPSession.class, "basic"));
        action.initialize();
        event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "basic");
        Assert.assertEquals(prc.getSubcontext(MultiRelyingPartyContext.class).getCurrentRelyingPartyContext().getRelyingPartyId(), "foo");
        event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }

    @Test public void testMultiRegistered() throws SessionException, ComponentInitializationException {
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class, true);
        logoutCtx.getSessions("foo").add(new BasicSPSession("foo", "bar", 1, 1));
        logoutCtx.getSessions("foo2").add(new BasicSPSession("foo2", "bar", 1, 1));

        Event event = populate.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        action.setElaborationFlowMap(Collections.<Class<? extends SPSession>,String>singletonMap(BasicSPSession.class, "basic"));
        action.initialize();
        event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "basic");
        Assert.assertNotNull(prc.getSubcontext(MultiRelyingPartyContext.class).getCurrentRelyingPartyContext());
        event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "basic");
        Assert.assertNotNull(prc.getSubcontext(MultiRelyingPartyContext.class).getCurrentRelyingPartyContext());
        event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }
    
}