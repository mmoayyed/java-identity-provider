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

import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.UsernamePrincipal;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionTestingSupport;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link FinalizeAuthenticationFlow} unit test. */
public class FinalizeAuthenticationFlowTest extends InitializeAuthenticationContextTest {
    
    private FinalizeAuthenticationFlow action; 
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        action = new FinalizeAuthenticationFlow();
        action.initialize();
    }

    @Test public void testNoResult() throws ProfileException {
        action.execute(prc);
        
        ActionTestingSupport.assertProceedEvent(prc);
    }
    
    @Test public void testNoContext() throws ProfileException {
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        AuthenticationResult result = new AuthenticationResult("test2", new Subject());
        authCtx.setAuthenticationResult(result);
        action.execute(prc);
        
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT_C14N_CTX);
    }

    @Test public void testNoPrincipalName() throws ProfileException {
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        AuthenticationResult result = new AuthenticationResult("test2", new Subject());
        authCtx.setAuthenticationResult(result);
        prc.addSubcontext(new SubjectCanonicalizationContext(new Subject()), true);
        
        action.execute(prc);
        
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT_C14N_CTX);
    }

    @Test public void testValid() throws Exception {
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        AuthenticationResult result = new AuthenticationResult("test2", new Subject());
        result.getSubject().getPrincipals().add(new UsernamePrincipal("foo"));
        authCtx.setAuthenticationResult(result);
        prc.addSubcontext(new SubjectCanonicalizationContext(result.getSubject()), true);
        
        SimpleSubjectCanonicalization c14n = new SimpleSubjectCanonicalization();
        c14n.initialize();
        c14n.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        
        action.execute(prc);
        
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertEquals(result.getCanonicalPrincipalName(), "foo");
        Assert.assertEquals(authCtx.getCanonicalPrincipalName(), "foo");
    }
    
    @Test public void testSwitch() throws Exception {
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        AuthenticationResult result = new AuthenticationResult("test2", new Subject());
        result.getSubject().getPrincipals().add(new UsernamePrincipal("foo"));
        authCtx.setAuthenticationResult(result);
        authCtx.setCanonicalPrincipalName("bar");
        prc.addSubcontext(new SubjectCanonicalizationContext(result.getSubject()), true);
        
        SimpleSubjectCanonicalization c14n = new SimpleSubjectCanonicalization();
        c14n.initialize();
        c14n.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        
        action.execute(prc);
        
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.IDENTITY_SWITCH);
        Assert.assertEquals(result.getCanonicalPrincipalName(), "foo");
        Assert.assertEquals(authCtx.getCanonicalPrincipalName(), "bar");
    }
}