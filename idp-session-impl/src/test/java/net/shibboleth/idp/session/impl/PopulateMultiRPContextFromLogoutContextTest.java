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

import java.util.Collection;

import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.context.MultiRelyingPartyContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.session.BasicSPSession;
import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link PopulateMultiRPContextFromLogoutContext} unit test. */
public class PopulateMultiRPContextFromLogoutContextTest {
    
    private RequestContext src;
    
    private ProfileRequestContext prc;
    
    private PopulateMultiRPContextFromLogoutContext action;
    
    @BeforeMethod public void setUpAction() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);

        action = new PopulateMultiRPContextFromLogoutContext();
        action.initialize();
    }
    
    @Test public void testNoContext() {
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNull(prc.getSubcontext(MultiRelyingPartyContext.class));
    }
    
    @Test public void testEmptyContext() {
        prc.getSubcontext(LogoutContext.class, true);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final MultiRelyingPartyContext multiCtx = prc.getSubcontext(MultiRelyingPartyContext.class);
        Assert.assertNotNull(multiCtx);
        Assert.assertTrue(multiCtx.getRelyingPartyContexts().isEmpty());
    }

    @Test public void testOne() {
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class, true);
        logoutCtx.getSessions("foo").add(new BasicSPSession("foo", "bar", 1, 1));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final MultiRelyingPartyContext multiCtx = prc.getSubcontext(MultiRelyingPartyContext.class);
        Assert.assertNotNull(multiCtx);
        
        final Collection<RelyingPartyContext> contexts = multiCtx.getRelyingPartyContexts("logout");
        Assert.assertEquals(contexts.size(), 1);
        Assert.assertEquals(contexts.iterator().next().getRelyingPartyId(), "foo");
    }

    @Test public void testTwoSame() {
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class, true);
        logoutCtx.getSessions("foo").add(new BasicSPSession("foo", "bar", 1, 1));
        logoutCtx.getSessions("foo").add(new BasicSPSession("foo", "bar", 1, 1));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final MultiRelyingPartyContext multiCtx = prc.getSubcontext(MultiRelyingPartyContext.class);
        Assert.assertNotNull(multiCtx);
        
        final Collection<RelyingPartyContext> contexts = multiCtx.getRelyingPartyContexts("logout");
        Assert.assertEquals(contexts.size(), 1);
        Assert.assertEquals(contexts.iterator().next().getRelyingPartyId(), "foo");
    }

    @Test public void testTwoDifferent() {
        final LogoutContext logoutCtx = prc.getSubcontext(LogoutContext.class, true);
        logoutCtx.getSessions("foo").add(new BasicSPSession("foo", "baz", 1, 1));
        logoutCtx.getSessions("bar").add(new BasicSPSession("bar", "baz", 1, 1));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final MultiRelyingPartyContext multiCtx = prc.getSubcontext(MultiRelyingPartyContext.class);
        Assert.assertNotNull(multiCtx);
        
        final Collection<RelyingPartyContext> contexts = multiCtx.getRelyingPartyContexts("logout");
        Assert.assertEquals(contexts.size(), 2);
        Assert.assertEquals(multiCtx.getRelyingPartyContextById("foo").getRelyingPartyId(), "foo");
        Assert.assertEquals(multiCtx.getRelyingPartyContextById("bar").getRelyingPartyId(), "bar");
    }

}