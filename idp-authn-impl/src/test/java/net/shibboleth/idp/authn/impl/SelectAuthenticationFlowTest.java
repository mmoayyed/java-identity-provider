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

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.PreferredPrincipalContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.principal.TestPrincipal;
import net.shibboleth.idp.authn.principal.impl.ExactPrincipalEvalPredicateFactory;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link SelectAuthenticationFlow} unit test. */
public class SelectAuthenticationFlowTest extends BaseAuthenticationContextTest {
    
    private SelectAuthenticationFlow action; 
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        action = new SelectAuthenticationFlow();
        action.initialize();
    }
    
    @Test public void testNoRequestNoneActive() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "test1");
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        Assert.assertEquals(authCtx.getAttemptedFlow(), authCtx.getPotentialFlows().get(event.getId()));
        Assert.assertEquals(authCtx.getAttemptedFlow().getId(), "test1");
    }

    @Test public void testNoRequestNoneActivePassive() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setIsPassive(true);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "test2");
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        Assert.assertEquals(authCtx.getAttemptedFlow(), authCtx.getPotentialFlows().get(event.getId()));
        Assert.assertEquals(authCtx.getAttemptedFlow().getId(), "test2");
    }

    @Test public void testNoRequestNoneActiveIntermediate() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.getIntermediateFlows().put("test1", authCtx.getPotentialFlows().get("test1"));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "test2");
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        Assert.assertEquals(authCtx.getAttemptedFlow(), authCtx.getPotentialFlows().get(event.getId()));
        Assert.assertEquals(authCtx.getAttemptedFlow().getId(), "test2");
    }
    
    @Test public void testNoRequestActive() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setActiveResults(Arrays.asList(active));
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(active, authCtx.getAuthenticationResult());
    }

    @Test public void testNoRequestForced() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.setForceAuthn(true);
        
        final Event event = action.execute(src);
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        Assert.assertEquals(authCtx.getAttemptedFlow(), authCtx.getPotentialFlows().get(event.getId()));
    }

    @Test public void testRequestNoMatch() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(Arrays.<Principal>asList(new TestPrincipal("foo")));
        authCtx.addSubcontext(rpc, true);
        
        final Event event = action.execute(src);

        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
    }

    @Test public void testPreferredNoMatch() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final List<Principal> principals = Arrays.<Principal>asList(new TestPrincipal("test3"));
        final PreferredPrincipalContext ppc = new PreferredPrincipalContext();
        ppc.setPreferredPrincipals(principals);
        authCtx.addSubcontext(ppc, true);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "test3");
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        Assert.assertEquals(authCtx.getAttemptedFlow().getId(), "test3");
    }

    @Test public void testPreferredNoneActive() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final List<Principal> principals = Arrays.<Principal>asList(new TestPrincipal("test3"));
        final PreferredPrincipalContext ppc = new PreferredPrincipalContext();
        ppc.setPreferredPrincipals(principals);
        authCtx.addSubcontext(ppc, true);
        authCtx.getPotentialFlows().get("test3").setSupportedPrincipals(principals);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "test3");
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        Assert.assertEquals(authCtx.getAttemptedFlow().getId(), "test3");
    }
    
    @Test public void testPreferredPickActiveNonMatch() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final List<Principal> principals = Arrays.<Principal>asList(new TestPrincipal("test3"),
                new TestPrincipal("test2"));
        final PreferredPrincipalContext ppc = new PreferredPrincipalContext();
        ppc.setPreferredPrincipals(principals);
        authCtx.addSubcontext(ppc, true);
        final AuthenticationResult active = new AuthenticationResult("test1", new Subject());
        active.getSubject().getPrincipals().add(new TestPrincipal("test1"));
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.getPotentialFlows().get("test3").setSupportedPrincipals(Collections.singletonList(principals.get(0)));
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(active, authCtx.getAuthenticationResult());
    }

    @Test public void testPreferredPickActiveMatch() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final List<Principal> principals = Arrays.<Principal>asList(new TestPrincipal("test3"),
                new TestPrincipal("test2"));
        final PreferredPrincipalContext ppc = new PreferredPrincipalContext();
        ppc.setPreferredPrincipals(principals);
        authCtx.addSubcontext(ppc, true);
        final AuthenticationResult active1 = new AuthenticationResult("test1", new Subject());
        final AuthenticationResult active3 = new AuthenticationResult("test3", new Subject());
        active1.getSubject().getPrincipals().add(new TestPrincipal("test1"));
        active3.getSubject().getPrincipals().add(new TestPrincipal("test3"));
        authCtx.setActiveResults(Arrays.asList(active1, active3));
        authCtx.getPotentialFlows().get("test3").setSupportedPrincipals(Collections.singletonList(principals.get(0)));
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(active3, authCtx.getAuthenticationResult());
    }

    @Test public void testRequestNoneActive() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final List<Principal> principals = Arrays.<Principal>asList(new TestPrincipal("test3"));
        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(principals);
        authCtx.addSubcontext(rpc, true);
        authCtx.getPotentialFlows().get("test3").setSupportedPrincipals(principals);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "test3");
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        Assert.assertEquals(authCtx.getAttemptedFlow().getId(), "test3");
    }

    @Test public void testRequestNoneActiveIntermediate() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.getIntermediateFlows().put("test2", authCtx.getPotentialFlows().get("test2"));
        final List<Principal> principals = Arrays.<Principal>asList(new TestPrincipal("test3"),
                new TestPrincipal("test2"));
        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(principals);
        authCtx.addSubcontext(rpc, true);
        authCtx.getPotentialFlows().get("test2").setSupportedPrincipals(principals);
        authCtx.getPotentialFlows().get("test3").setSupportedPrincipals(principals);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "test3");
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        Assert.assertEquals(authCtx.getAttemptedFlow().getId(), "test3");
    }
    
    @Test public void testRequestPickInactive() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final List<Principal> principals = Arrays.<Principal>asList(new TestPrincipal("test3"),
                new TestPrincipal("test2"));
        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(principals);
        authCtx.addSubcontext(rpc, true);
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        active.getSubject().getPrincipals().add(new TestPrincipal("test2"));
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.getPotentialFlows().get("test3").setSupportedPrincipals(Collections.singletonList(principals.get(0)));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "test3");
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        Assert.assertEquals(authCtx.getAttemptedFlow(), authCtx.getPotentialFlows().get("test3"));
    }

    @Test public void testRequestPickActive() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final List<Principal> principals = Arrays.<Principal>asList(new TestPrincipal("test3"),
                new TestPrincipal("test2"));
        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(principals);
        authCtx.addSubcontext(rpc, true);
        final AuthenticationResult active = new AuthenticationResult("test3", new Subject());
        active.getSubject().getPrincipals().add(new TestPrincipal("test3"));
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.getPotentialFlows().get("test3").setSupportedPrincipals(Collections.singletonList(principals.get(0)));
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(active, authCtx.getAuthenticationResult());
    }

    @Test public void testRequestFavorSSO() throws ComponentInitializationException {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final List<Principal> principals = Arrays.<Principal>asList(new TestPrincipal("test3"),
                new TestPrincipal("test2"));
        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(principals);
        authCtx.addSubcontext(rpc, true);
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        active.getSubject().getPrincipals().add(new TestPrincipal("test2"));
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.getPotentialFlows().get("test3").setSupportedPrincipals(Collections.singletonList(principals.get(0)));
        
        action = new SelectAuthenticationFlow();
        action.setFavorSSO(true);
        action.initialize();
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(active, authCtx.getAuthenticationResult());
    }
    
}