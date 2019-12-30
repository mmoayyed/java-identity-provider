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
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.impl.FinalizeMultiFactorAuthentication;
import net.shibboleth.idp.authn.principal.AuthenticationResultPrincipal;
import net.shibboleth.idp.authn.principal.TestPrincipal;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link FinalizeMultiFactorAuthentication} unit test. */
public class FinalizeMultiFactorAuthenticationTest extends BaseMultiFactorAuthenticationContextTest {
    
    private FinalizeMultiFactorAuthentication action;
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        final Subject subject = new Subject();
        subject.getPrincipals().add(new UsernamePrincipal("foo"));
        subject.getPrincipals().add(new TestPrincipal("bar"));
        mfa.getActiveResults().put("test", new AuthenticationResult("test", subject));
        
        action = new FinalizeMultiFactorAuthentication();
    }

    /**
     * Tests no MFA context.
     * 
     * @throws ComponentInitializationException ...
     */
    @Test public void testNoContext() throws ComponentInitializationException {
        ac.removeSubcontext(mfa);
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_PROFILE_CTX);
    }

    /**
     * Tests a null result.
     * 
     * @throws ComponentInitializationException ...
     */
    @Test public void testNullResult() throws ComponentInitializationException {
        action.setResultMergingStrategy(FunctionSupport.<ProfileRequestContext,AuthenticationResult>constant(null));
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
    }

    /**
     * Tests "merge" of a single result.
     * 
     * @throws ComponentInitializationException ...
     */
    @Test public void testSingleResult() throws ComponentInitializationException {
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationResult result = ac.getAuthenticationResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getAuthenticationFlowId(), "authn/MFA");
        final Subject merged = result.getSubject();
        Assert.assertEquals(merged.getPrincipals().size(), 3);
        Assert.assertEquals(merged.getPrincipals(AuthenticationResultPrincipal.class).size(), 1);
        Assert.assertTrue(merged.getPrincipals(UsernamePrincipal.class).contains(new UsernamePrincipal("foo")));
        Assert.assertFalse(merged.getPrincipals(UsernamePrincipal.class).contains(new UsernamePrincipal("foo2")));
        Assert.assertTrue(merged.getPrincipals(TestPrincipal.class).contains(new TestPrincipal("bar")));
        Assert.assertFalse(merged.getPrincipals(TestPrincipal.class).contains(new TestPrincipal("baz")));
        
        Assert.assertNotNull(prc.getSubcontext(SubjectCanonicalizationContext.class));
        Assert.assertSame(merged, prc.getSubcontext(SubjectCanonicalizationContext.class).getSubject());
    }

    /**
     * Tests default merging of results.
     * 
     * @throws ComponentInitializationException ...
     */
    @Test public void testMergedResults() throws ComponentInitializationException {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new UsernamePrincipal("foo2"));
        subject.getPrincipals().add(new TestPrincipal("bar"));
        mfa.getActiveResults().put("test2", new AuthenticationResult("test2", subject));
        
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);

        final AuthenticationResult result = ac.getAuthenticationResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getAuthenticationFlowId(), "authn/MFA");
        final Subject merged = result.getSubject();
        Assert.assertEquals(merged.getPrincipals().size(), 5);
        Assert.assertEquals(merged.getPrincipals(AuthenticationResultPrincipal.class).size(), 2);
        Assert.assertTrue(merged.getPrincipals(UsernamePrincipal.class).contains(new UsernamePrincipal("foo")));
        Assert.assertTrue(merged.getPrincipals(UsernamePrincipal.class).contains(new UsernamePrincipal("foo2")));
        Assert.assertFalse(merged.getPrincipals(UsernamePrincipal.class).contains(new UsernamePrincipal("foo3")));
        Assert.assertTrue(merged.getPrincipals(TestPrincipal.class).contains(new TestPrincipal("bar")));
        Assert.assertFalse(merged.getPrincipals(TestPrincipal.class).contains(new TestPrincipal("baz")));
        
        Assert.assertNotNull(prc.getSubcontext(SubjectCanonicalizationContext.class));
        Assert.assertSame(merged, prc.getSubcontext(SubjectCanonicalizationContext.class).getSubject());
    }
}
