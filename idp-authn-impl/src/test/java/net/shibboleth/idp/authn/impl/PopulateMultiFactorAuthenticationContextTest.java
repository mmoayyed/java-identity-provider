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

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.MultiFactorAuthenticationTransition;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.MultiFactorAuthenticationContext;
import net.shibboleth.idp.authn.impl.DefaultAuthenticationResultSerializer;
import net.shibboleth.idp.authn.principal.AuthenticationResultPrincipal;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link PopulateMultiFactorAuthenticationContext} unit test. */
public class PopulateMultiFactorAuthenticationContextTest {

    private RequestContext rc;
    private ProfileRequestContext prc;
    private AuthenticationContext ac;
    private PopulateMultiFactorAuthenticationContext action;
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        rc = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(rc);
        ac = prc.getSubcontext(AuthenticationContext.class, true);
        ac.setAttemptedFlow(new AuthenticationFlowDescriptor());
        ac.getAttemptedFlow().setId("authn/MFA");
        ac.getAttemptedFlow().setResultSerializer(new DefaultAuthenticationResultSerializer());
        ac.getAttemptedFlow().initialize();
        action = new PopulateMultiFactorAuthenticationContext();
    }

    @Test public void testEmpty() throws ComponentInitializationException {
        action.initialize();
        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.RESELECT_FLOW);
        
        final MultiFactorAuthenticationContext mfa = ac.getSubcontext(MultiFactorAuthenticationContext.class);
        Assert.assertNull(mfa);
    }
    
    @Test public void testTransitions() throws ComponentInitializationException {
        action.setTransitionMapLookupStrategy(
                FunctionSupport.<ProfileRequestContext,Map<String,MultiFactorAuthenticationTransition>>constant(
                        Collections.singletonMap("", new MultiFactorAuthenticationTransition())));
        action.initialize();
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        final MultiFactorAuthenticationContext mfa = ac.getSubcontext(MultiFactorAuthenticationContext.class);
        Assert.assertNotNull(mfa);
        Assert.assertEquals(ac.getAttemptedFlow(), mfa.getAuthenticationFlowDescriptor());
        Assert.assertEquals(mfa.getTransitionMap().size(), 1);
        Assert.assertNotNull(mfa.getTransitionMap().get(null));
        Assert.assertTrue(mfa.getActiveResults().isEmpty());
    }

    @Test public void testResults() throws ComponentInitializationException {
        final Subject subject = new Subject();
        AuthenticationResult result = new AuthenticationResult("foo", new Subject());
        result.setAuthenticationInstant(Instant.now().minusSeconds(7200));
        subject.getPrincipals().add(new AuthenticationResultPrincipal(result));
        result = new AuthenticationResult("bar", new Subject());
        subject.getPrincipals().add(new AuthenticationResultPrincipal(result));
        result = new AuthenticationResult("baz", new Subject());
        subject.getPrincipals().add(new AuthenticationResultPrincipal(result));
        result = new AuthenticationResult("bav", new Subject());
        result.setAuthenticationInstant(Instant.now().minusSeconds(1000));
        subject.getPrincipals().add(new AuthenticationResultPrincipal(result));
        result = new AuthenticationResult("bag", new Subject());
        result.setAuthenticationInstant(Instant.now().minusSeconds(2000));
        subject.getPrincipals().add(new AuthenticationResultPrincipal(result));
        
        ac.getActiveResults().put("authn/MFA", new AuthenticationResult("authn/MFA", subject));
        
        AuthenticationFlowDescriptor desc = new AuthenticationFlowDescriptor();
        desc.setId("foo");
        desc.setResultSerializer(new DefaultAuthenticationResultSerializer());
        desc.setLifetime(3600 * 1000);
        desc.initialize();
        ac.getAvailableFlows().put(desc.getId(), desc);

        desc = new AuthenticationFlowDescriptor();
        desc.setId("bar");
        desc.setResultSerializer(new DefaultAuthenticationResultSerializer());
        desc.setLifetime(3600 * 1000);
        desc.initialize();
        ac.getAvailableFlows().put(desc.getId(), desc);

        desc = new AuthenticationFlowDescriptor();
        desc.setId("bav");
        desc.setResultSerializer(new DefaultAuthenticationResultSerializer());
        desc.setLifetime(3600 * 1000);
        desc.initialize();
        ac.getAvailableFlows().put(desc.getId(), desc);

        desc = new AuthenticationFlowDescriptor();
        desc.setId("bag");
        desc.setResultSerializer(new DefaultAuthenticationResultSerializer());
        desc.setLifetime(3600 * 1000);
        desc.initialize();
        ac.getAvailableFlows().put(desc.getId(), desc);

        ac.setMaxAge(1800 * 1000);
        
        action.setTransitionMapLookupStrategy(
                FunctionSupport.<ProfileRequestContext,Map<String,MultiFactorAuthenticationTransition>>constant(
                        Collections.singletonMap("", new MultiFactorAuthenticationTransition())));
        action.initialize();
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        final MultiFactorAuthenticationContext mfa = ac.getSubcontext(MultiFactorAuthenticationContext.class);
        Assert.assertNotNull(mfa);
        Assert.assertEquals(ac.getAttemptedFlow(), mfa.getAuthenticationFlowDescriptor());
        Assert.assertEquals(mfa.getActiveResults().size(), 2);
        Assert.assertNull(mfa.getActiveResults().get("foo"));
        Assert.assertNotNull(mfa.getActiveResults().get("bar"));
        Assert.assertNull(mfa.getActiveResults().get("baz"));
        Assert.assertNotNull(mfa.getActiveResults().get("bav"));
        Assert.assertNull(mfa.getActiveResults().get("bag"));
    }
    
}