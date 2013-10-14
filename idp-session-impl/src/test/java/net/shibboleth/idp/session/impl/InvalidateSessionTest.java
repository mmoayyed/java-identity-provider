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

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.UsernamePrincipal;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.session.SessionException;
import net.shibboleth.idp.session.context.SessionContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.net.HttpServletRequestResponseContext;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link InvalidateSession} unit test. */
public class InvalidateSessionTest extends SessionManagerBaseTestCase {
    
    private ProfileRequestContext prc;
    
    private AuthenticationContext ac;
    
    private SessionContext sc;
    
    private InvalidateSession action;
    
    @BeforeMethod public void setUpAction() throws ComponentInitializationException {
        prc = new ProfileRequestContext();
        ac = prc.getSubcontext(AuthenticationContext.class, true);
        sc = prc.getSubcontext(SessionContext.class, true);

        action = new InvalidateSession();
        action.setSessionManager(sessionManager);
        action.initialize();
    }

    @Test public void testNoSession() throws ProfileException {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, EventIds.INVALID_PROFILE_CTX);
    }
    
    @Test public void testSuccess() throws ProfileException, SessionException {
        HttpServletRequestResponseContext.loadCurrent(new MockHttpServletRequest(), new MockHttpServletResponse());
        
        sc.setIdPSession(sessionManager.createSession("joe"));
        ac.setCanonicalPrincipalName("joe");
        ac.setActiveResults(Collections.singletonList(new AuthenticationResult("test1", new UsernamePrincipal("joe"))));
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertNull(sc.getIdPSession());
        Assert.assertTrue(ac.getActiveResults().isEmpty());
        Assert.assertNull(ac.getCanonicalPrincipalName());
    }

}