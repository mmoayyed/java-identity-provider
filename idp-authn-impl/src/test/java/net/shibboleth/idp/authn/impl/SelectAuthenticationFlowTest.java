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

package net.shibboleth.idp.authn.impl;

import java.util.Arrays;

import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.context.EventContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link SelectAuthenticationFlow} unit test. */
public class SelectAuthenticationFlowTest extends InitializeAuthenticationContextTest {
    
    private SelectAuthenticationFlow action; 
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        action = new SelectAuthenticationFlow();
        action.initialize();
    }
    
    @Test public void testNoRequestNoneActive() throws ProfileException {
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        
        action.execute(prc);
        
        EventContext<String> event = prc.getSubcontext(EventContext.class, false);
        Assert.assertNull(authCtx.getAuthenticationResult());
        Assert.assertEquals(authCtx.getAttemptedFlow(), authCtx.getPotentialFlows().get(event.getEvent()));
    }

    @Test public void testNoRequestActive() throws ProfileException {
        AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        authCtx.setActiveResults(Arrays.asList(active));
        
        action.execute(prc);
        
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertEquals(active, authCtx.getAuthenticationResult());
    }

    @Test public void testNoRequestForced() throws ProfileException {
        AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.setForceAuthn(true);
        
        action.execute(prc);
        
        EventContext<String> event = prc.getSubcontext(EventContext.class, false);
        Assert.assertNull(authCtx.getAuthenticationResult());
        Assert.assertEquals(authCtx.getAttemptedFlow(), authCtx.getPotentialFlows().get(event.getEvent()));
    }

    @Test public void testRequestNoMatch() throws ProfileException {
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        authCtx.setRequestedFlows(Arrays.asList(new AuthenticationFlowDescriptor("foo")));
        
        action.execute(prc);
        
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.NO_REQUESTED_FLOW);
    }

    @Test public void testRequestNoneActive() throws ProfileException {
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        authCtx.setRequestedFlows(Arrays.asList(authCtx.getPotentialFlows().get("test3")));
        
        action.execute(prc);
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        Assert.assertEquals(authCtx.getAttemptedFlow(), authCtx.getPotentialFlows().get("test3"));
    }

    @Test public void testRequestPickInactive() throws ProfileException {
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.setRequestedFlows(
                Arrays.asList(authCtx.getPotentialFlows().get("test3"), authCtx.getPotentialFlows().get("test2")));
        
        action.execute(prc);
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        Assert.assertEquals(authCtx.getAttemptedFlow(), authCtx.getPotentialFlows().get("test3"));
    }

    @Test public void testRequestPickActive() throws ProfileException {
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        AuthenticationResult active = new AuthenticationResult("test3", new Subject());
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.setRequestedFlows(
                Arrays.asList(authCtx.getPotentialFlows().get("test3"), authCtx.getPotentialFlows().get("test2")));
        
        action.execute(prc);
        
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertEquals(active, authCtx.getAuthenticationResult());
    }

    @Test public void testRequestFavorSSO() throws ProfileException {
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.setRequestedFlows(
                Arrays.asList(authCtx.getPotentialFlows().get("test3"), authCtx.getPotentialFlows().get("test2")));
        
        action.setFavorSSO(true);
        action.execute(prc);
        
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertEquals(active, authCtx.getAuthenticationResult());
    }
}