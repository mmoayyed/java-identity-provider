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
import java.util.List;

import javax.security.auth.Subject;

import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.PreferredPrincipalContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.authn.principal.impl.ExactPrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.testing.TestPrincipal;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;

/** {@link SelectAuthenticationFlow} unit test. */
@SuppressWarnings("javadoc")
public class SelectAuthenticationFlowTest extends BaseAuthenticationContextTest {
    
    private SelectAuthenticationFlow action; 
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        action = new SelectAuthenticationFlow();
        action.initialize();
    }
    
    @Test public void testNoRequestNoneActive() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "test1");
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        final AuthenticationFlowDescriptor attemptedFlow = authCtx.getAttemptedFlow();
        assert attemptedFlow != null && event != null;

        Assert.assertEquals(attemptedFlow, authCtx.getPotentialFlows().get(event.getId()));
        Assert.assertEquals(attemptedFlow.getId(), "test1");
    }

    @Test public void testNoRequestNoneActivePassive() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        authCtx.setIsPassive(true);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "test2");
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        final AuthenticationFlowDescriptor attemptedFlow = authCtx.getAttemptedFlow();
        assert attemptedFlow != null && event != null;
        Assert.assertEquals(attemptedFlow, authCtx.getPotentialFlows().get(event.getId()));
        Assert.assertEquals(attemptedFlow.getId(), "test2");
    }

    @Test public void testNoRequestNoneActiveIntermediate() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        authCtx.getIntermediateFlows().put("test1", authCtx.getPotentialFlows().get("test1"));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "test2");
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        final AuthenticationFlowDescriptor attemptedFlow = authCtx.getAttemptedFlow();
        assert attemptedFlow != null && event != null;
        Assert.assertEquals(attemptedFlow, authCtx.getPotentialFlows().get(event.getId()));
        Assert.assertEquals(attemptedFlow.getId(), "test2");
    }
    
    @Test public void testNoRequestActive() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        authCtx.setActiveResults(CollectionSupport.singletonList(active));
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(active, authCtx.getAuthenticationResult());
    }

    @Test public void testNoRequestForced() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        authCtx.setActiveResults(CollectionSupport.singletonList(active));
        authCtx.setForceAuthn(true);
        
        final Event event = action.execute(src);
        assert event != null;
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        Assert.assertEquals(authCtx.getAttemptedFlow(), authCtx.getPotentialFlows().get(event.getId()));
    }

    @Test public void testRequestNoMatch() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(CollectionSupport.singletonList(new TestPrincipal("foo")));
        assert authCtx != null;
        authCtx.addSubcontext(rpc, true);
        
        final Event event = action.execute(src);

        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
    }

    @Test public void testPreferredNoMatch() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final List<Principal> principals = CollectionSupport.singletonList(new TestPrincipal("test3"));
        final PreferredPrincipalContext ppc = new PreferredPrincipalContext();
        ppc.setPreferredPrincipals(principals);
        assert authCtx != null;
        authCtx.addSubcontext(ppc, true);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "test3");
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        final AuthenticationFlowDescriptor attemptedFlow = authCtx.getAttemptedFlow();
        assert attemptedFlow != null && event != null;

        Assert.assertEquals(attemptedFlow.getId(), "test3");
    }

    @Test public void testPreferredNoneActive() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final List<Principal> principals = CollectionSupport.singletonList(new TestPrincipal("test3"));
        final PreferredPrincipalContext ppc = new PreferredPrincipalContext();
        ppc.setPreferredPrincipals(principals);
        assert authCtx != null;
        authCtx.addSubcontext(ppc, true);
        authCtx.getPotentialFlows().get("test3").setSupportedPrincipals(principals);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "test3");
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        final AuthenticationFlowDescriptor attemptedFlow = authCtx.getAttemptedFlow();
        assert attemptedFlow != null;

        Assert.assertEquals(attemptedFlow.getId(), "test3");
    }
    
    @Test public void testPreferredPickActiveNonMatch() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final List<Principal> principals = CollectionSupport.listOf(new TestPrincipal("test3"),
                new TestPrincipal("test2"));
        final PreferredPrincipalContext ppc = new PreferredPrincipalContext();
        ppc.setPreferredPrincipals(principals);
        assert authCtx != null;
        authCtx.addSubcontext(ppc, true);
        final AuthenticationResult active = new AuthenticationResult("test1", new Subject());
        active.getSubject().getPrincipals().add(new TestPrincipal("test1"));
        authCtx.setActiveResults(CollectionSupport.singletonList(active));
        authCtx.getPotentialFlows().get("test3").setSupportedPrincipals(CollectionSupport.singletonList(principals.get(0)));
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(active, authCtx.getAuthenticationResult());
    }

    @Test public void testPreferredPickActiveMatch() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final List<Principal> principals = CollectionSupport.listOf(new TestPrincipal("test3"),
                new TestPrincipal("test2"));
        final PreferredPrincipalContext ppc = new PreferredPrincipalContext();
        ppc.setPreferredPrincipals(principals);
        assert authCtx != null;
        authCtx.addSubcontext(ppc, true);
        final AuthenticationResult active1 = new AuthenticationResult("test1", new Subject());
        final AuthenticationResult active3 = new AuthenticationResult("test3", new Subject());
        active1.getSubject().getPrincipals().add(new TestPrincipal("test1"));
        active3.getSubject().getPrincipals().add(new TestPrincipal("test3"));
        authCtx.setActiveResults(CollectionSupport.listOf(active1, active3));
        authCtx.getPotentialFlows().get("test3").setSupportedPrincipals(CollectionSupport.singletonList(principals.get(0)));
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(active3, authCtx.getAuthenticationResult());
    }

    @Test public void testRequestNoneActive() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final List<Principal> principals = CollectionSupport.singletonList(new TestPrincipal("test3"));
        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(principals);
        assert authCtx != null;
        authCtx.addSubcontext(rpc, true);
        authCtx.getPotentialFlows().get("test3").setSupportedPrincipals(principals);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "test3");
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        final AuthenticationFlowDescriptor attemptedFlow = authCtx.getAttemptedFlow();
        assert attemptedFlow != null;
        Assert.assertEquals(attemptedFlow.getId(), "test3");
    }

    @Test public void testRequestNoneActiveIntermediate() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        authCtx.getIntermediateFlows().put("test2", authCtx.getPotentialFlows().get("test2"));
        final List<Principal> principals = CollectionSupport.listOf(new TestPrincipal("test3"),
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
        final AuthenticationFlowDescriptor flow = authCtx.getAttemptedFlow();
        assert flow != null;

        Assert.assertEquals(flow.getId(), "test3");
    }
    
    @Test public void testRequestPickInactive() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final List<Principal> principals = CollectionSupport.listOf(new TestPrincipal("test3"),
                new TestPrincipal("test2"));
        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(principals);
        assert authCtx != null;
        authCtx.addSubcontext(rpc, true);
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        active.getSubject().getPrincipals().add(new TestPrincipal("test2"));
        authCtx.setActiveResults(CollectionSupport.singletonList(active));
        authCtx.getPotentialFlows().get("test3").setSupportedPrincipals(CollectionSupport.singletonList(principals.get(0)));
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, "test3");
        
        Assert.assertNull(authCtx.getAuthenticationResult());
        Assert.assertEquals(authCtx.getAttemptedFlow(), authCtx.getPotentialFlows().get("test3"));
    }

    @Test public void testRequestPickActive() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        final List<Principal> principals = CollectionSupport.listOf(new TestPrincipal("test3"),
                new TestPrincipal("test2"));
        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(principals);
        assert authCtx != null;
        authCtx.addSubcontext(rpc, true);
        final AuthenticationResult active = new AuthenticationResult("test3", new Subject());
        active.getSubject().getPrincipals().add(new TestPrincipal("test3"));
        authCtx.setActiveResults(CollectionSupport.singletonList(active));
        authCtx.getPotentialFlows().get("test3").setSupportedPrincipals(CollectionSupport.singletonList(principals.get(0)));
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(active, authCtx.getAuthenticationResult());
    }

    @Test public void testRequestFavorSSO() throws ComponentInitializationException {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        final List<Principal> principals = CollectionSupport.listOf(new TestPrincipal("test3"),
                new TestPrincipal("test2"));
        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(principals);
        authCtx.addSubcontext(rpc, true);
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        active.getSubject().getPrincipals().add(new TestPrincipal("test2"));
        authCtx.setActiveResults(CollectionSupport.singletonList(active));
        authCtx.getPotentialFlows().get("test3").setSupportedPrincipals(CollectionSupport.singletonList(principals.get(0)));
        
        action = new SelectAuthenticationFlow();
        action.setFavorSSO(true);
        action.initialize();
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(active, authCtx.getAuthenticationResult());
    }
    
}