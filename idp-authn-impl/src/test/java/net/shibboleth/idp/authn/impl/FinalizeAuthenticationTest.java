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

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectContext;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionTestingSupport;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link FinalizeAuthentication} unit test. */
public class FinalizeAuthenticationTest extends InitializeAuthenticationContextTest {
    
    private FinalizeAuthentication action; 
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        action = new FinalizeAuthentication();
        action.initialize();
    }
    
    @Test public void testNotSet() throws ProfileException {
        action.execute(prc);
        
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertNull(prc.getSubcontext(SubjectContext.class, false));
    }

    @Test public void testNothingActive() throws ProfileException {
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        authCtx.setCanonicalPrincipalName("foo");
        
        action.execute(prc);
        
        ActionTestingSupport.assertProceedEvent(prc);
        SubjectContext sc = prc.getSubcontext(SubjectContext.class, false);
        Assert.assertNotNull(sc);
        Assert.assertEquals(sc.getPrincipalName(), "foo");
    }

    @Test public void testOneActive() throws ProfileException {
        AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        authCtx.setCanonicalPrincipalName("foo");
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.setAuthenticationResult(active);
        
        action.execute(prc);
        
        ActionTestingSupport.assertProceedEvent(prc);
        SubjectContext sc = prc.getSubcontext(SubjectContext.class, false);
        Assert.assertNotNull(sc);
        Assert.assertEquals(sc.getPrincipalName(), "foo");
        Assert.assertEquals(sc.getAuthenticationResults().size(), 1);
    }

    @Test public void testMultipleActive() throws ProfileException {
        AuthenticationResult active1 = new AuthenticationResult("test1", new Subject());
        AuthenticationResult active2 = new AuthenticationResult("test2", new Subject());
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        authCtx.setCanonicalPrincipalName("foo");
        authCtx.setActiveResults(Arrays.asList(active1));
        authCtx.setAuthenticationResult(active2);
        
        action.execute(prc);
        
        ActionTestingSupport.assertProceedEvent(prc);
        SubjectContext sc = prc.getSubcontext(SubjectContext.class, false);
        Assert.assertNotNull(sc);
        Assert.assertEquals(sc.getPrincipalName(), "foo");
        Assert.assertEquals(sc.getAuthenticationResults().size(), 2);
    }
}