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

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.servlet.impl.HttpServletRequestResponseContext;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link DetectIdentitySwitch} unit test. */
@SuppressWarnings("javadoc")
public class DetectIdentitySwitchTest extends SessionManagerBaseTestCase {
    
    private RequestContext src;
    
    private ProfileRequestContext prc;
    
    private AuthenticationContext ac;
    
    private SessionContext sc;
    
    private SubjectCanonicalizationContext c14n;
    
    private DetectIdentitySwitch action;
    
    @BeforeMethod public void setUpAction() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        ac = prc.ensureSubcontext(AuthenticationContext.class);
        sc = prc.ensureSubcontext(SessionContext.class);
        c14n = prc.ensureSubcontext(SubjectCanonicalizationContext.class);

        action = new DetectIdentitySwitch();
        action.setSessionManager(sessionManager);
        action.initialize();
    }

    @Test public void testNoSession() {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        c14n.setPrincipalName("joe");
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }
    
    @Test public void testSesssion() throws SessionException {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        
        sc.setIdPSession(sessionManager.createSession("joe"));
        ac.setActiveResults(CollectionSupport.singletonList(new AuthenticationResult("test1", new UsernamePrincipal("joe"))));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(sc.getIdPSession());
        Assert.assertEquals(ac.getActiveResults().size(), 1);
    }

    @Test public void testMatch() throws SessionException {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        
        sc.setIdPSession(sessionManager.createSession("joe"));
        ac.setActiveResults(CollectionSupport.singletonList(new AuthenticationResult("test1", new UsernamePrincipal("joe"))));
        c14n.setPrincipalName("joe");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(sc.getIdPSession());
        Assert.assertEquals(ac.getActiveResults().size(), 1);
    }

    @Test public void testMismatch() throws SessionException {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        
        sc.setIdPSession(sessionManager.createSession("joe"));
        ac.setActiveResults(CollectionSupport.singletonList(new AuthenticationResult("test1", new UsernamePrincipal("joe"))));
        c14n.setPrincipalName("joe2");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.IDENTITY_SWITCH);
        Assert.assertNull(sc.getIdPSession());
        Assert.assertEquals(ac.getActiveResults().size(), 0);
    }
    
}