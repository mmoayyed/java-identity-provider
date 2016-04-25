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

package net.shibboleth.idp.authn;

import java.security.Principal;
import java.util.Collections;

import javax.security.auth.Subject;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.MultiFactorAuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.principal.TestPrincipal;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.authn.principal.impl.ExactPrincipalEvalPredicateFactory;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link MultiFactorAuthenticationTransition} unit test. */
public class MultiFactorAuthenticationTransitionTest {

    private ProfileRequestContext prc;
    private AuthenticationContext ac;
    private MultiFactorAuthenticationContext mfa;
    private MultiFactorAuthenticationTransition transition;
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(new RequestContextBuilder().buildRequestContext());
        ac = prc.getSubcontext(AuthenticationContext.class, true);
        mfa = ac.getSubcontext(MultiFactorAuthenticationContext.class, true);
        mfa.setTransitionMap(Collections.singletonMap("test", transition));
        final Subject subject = new Subject();
        subject.getPrincipals().add(new UsernamePrincipal("foo"));
        subject.getPrincipals().add(new TestPrincipal("bar"));
        mfa.getAuthenticationResults().add(new AuthenticationResult("test", subject));
        transition = new MultiFactorAuthenticationTransition();
    }

    
    /** Tests behavior under "empty" conditions. */
    @Test public void testEmptyState() {
        mfa.getAuthenticationResults().clear();
        Assert.assertFalse(transition.getCompletionCondition().apply(prc));
        Assert.assertNull(transition.getResultMergingStrategy().apply(prc));
        Assert.assertNull(transition.getNextFlowStrategy().apply(prc));
    }

    /** Tests whether any result will satisfy a default request. */
    @Test public void testCompletionNoRequestedPrincipals() {
        mfa.setMergedAuthenticationResult(mfa.getAuthenticationResults().get(0));
        Assert.assertTrue(transition.getCompletionCondition().apply(prc));
    }
    
    /** Tests whether result satisfies request when it should. */
    @Test public void testCompletionRequestedPrincipalsSuccess() {
        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();        
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(Collections.<Principal>singletonList(new TestPrincipal("bar")));
        ac.addSubcontext(rpc);
        mfa.setMergedAuthenticationResult(mfa.getAuthenticationResults().get(0));
        Assert.assertTrue(transition.getCompletionCondition().apply(prc));
    }

    /** Tests whether result satisfies request when it shouldn't. */
    @Test public void testCompletionRequestedPrincipalsFailure() {
        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();        
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(Collections.<Principal>singletonList(new TestPrincipal("baz")));
        ac.addSubcontext(rpc);
        mfa.setMergedAuthenticationResult(mfa.getAuthenticationResults().get(0));
        Assert.assertFalse(transition.getCompletionCondition().apply(prc));
    }

    /** Tests "merge" of a single result. */
    @Test public void testSingleResult() {
        final AuthenticationResult result = transition.getResultMergingStrategy().apply(prc);
        Assert.assertEquals(result, mfa.getAuthenticationResults().get(0));
        Assert.assertEquals(result.getSubject(), mfa.getAuthenticationResults().get(0).getSubject());
    }

    /** Tests default merging of results. */
    @Test public void testMergedResults() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new UsernamePrincipal("foo2"));
        subject.getPrincipals().add(new TestPrincipal("bar"));
        mfa.getAuthenticationResults().add(new AuthenticationResult("test2", subject));
        
        final AuthenticationResult result = transition.getResultMergingStrategy().apply(prc);
        Assert.assertEquals(result, mfa.getAuthenticationResults().get(1));
        
        final Subject merged = result.getSubject();
        Assert.assertEquals(merged.getPrincipals().size(), 3);
        Assert.assertTrue(merged.getPrincipals(UsernamePrincipal.class).contains(new UsernamePrincipal("foo")));
        Assert.assertTrue(merged.getPrincipals(UsernamePrincipal.class).contains(new UsernamePrincipal("foo2")));
        Assert.assertFalse(merged.getPrincipals(UsernamePrincipal.class).contains(new UsernamePrincipal("foo3")));
        Assert.assertTrue(merged.getPrincipals(TestPrincipal.class).contains(new TestPrincipal("bar")));
        Assert.assertFalse(merged.getPrincipals(TestPrincipal.class).contains(new TestPrincipal("baz")));
    }
}