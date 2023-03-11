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

package net.shibboleth.idp.authn.impl.tests;

import java.security.Principal;
import java.util.List;

import javax.security.auth.Subject;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.admin.BasicAdministrativeFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.impl.FinalizeAuthentication;
import net.shibboleth.idp.authn.impl.PopulateAuthenticationContext;
import net.shibboleth.idp.authn.principal.ProxyAuthenticationPrincipal;
import net.shibboleth.idp.authn.principal.impl.ExactPrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.testing.TestPrincipal;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.saml2.profile.config.impl.BrowserSSOProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.FunctionSupport;

/** {@link FinalizeAuthentication} unit test. */
@SuppressWarnings("javadoc")
public class FinalizeAuthenticationTest extends OpenSAMLInitBaseTestCase {

    protected RequestContext src;
    protected ProfileRequestContext prc;
    protected List<AuthenticationFlowDescriptor> authenticationFlows;

    private FinalizeAuthentication action; 

    protected void initializeMembers() throws ComponentInitializationException {        
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        prc.addSubcontext(new AuthenticationContext(), true);

        authenticationFlows = List.of(new AuthenticationFlowDescriptor(),
                new AuthenticationFlowDescriptor(), new AuthenticationFlowDescriptor());
        authenticationFlows.get(0).setId("test1");
        authenticationFlows.get(1).setId("test2");
        authenticationFlows.get(1).setPassiveAuthenticationSupported(true);
        authenticationFlows.get(2).setId("test3");
    }

    @BeforeMethod protected void setUp() throws ComponentInitializationException {        
        initializeMembers();
        
        final PopulateAuthenticationContext bootstrap = new PopulateAuthenticationContext();
        assert authenticationFlows!=null;
        bootstrap.setAvailableFlows(authenticationFlows);
        bootstrap.setPotentialFlowsLookupStrategy(FunctionSupport.constant(authenticationFlows));
        bootstrap.initialize();

        bootstrap.execute(src);

        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx!= null;
        rpCtx.setProfileConfig(new BrowserSSOProfileConfiguration());
        
        action = new FinalizeAuthentication();
        action.initialize();
    }
    
    @Test public void testNotSet() {
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
    }

    @Test public void testMismatch() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx!= null;
        authCtx.setRequiredName("foo");
        
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        active.getSubject().getPrincipals().add(new TestPrincipal("bar2"));
        
        authCtx.setAuthenticationResult(active);

        prc.getOrCreateSubcontext(SubjectCanonicalizationContext.class).setPrincipalName("bar");

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_SUBJECT);
    }

    @Test public void testRequestUnsupported() {
        prc.getOrCreateSubcontext(SubjectCanonicalizationContext.class).setPrincipalName("foo");
        
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        active.getSubject().getPrincipals().add(new TestPrincipal("bar2"));
        
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx!=null;
        authCtx.setAuthenticationResult(active);
        
        final RequestedPrincipalContext rpCtx = new RequestedPrincipalContext();
        rpCtx.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "florp", new ExactPrincipalEvalPredicateFactory());
        rpCtx.setMatchingPrincipal(new TestPrincipal("bar1"));
        rpCtx.setOperator("florp");
        rpCtx.setRequestedPrincipals(CollectionSupport.singletonList(new TestPrincipal("bar1")));
        authCtx.addSubcontext(rpCtx);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
    }

    @Test public void testSwitchesPrincipal() {
        prc.getOrCreateSubcontext(SubjectCanonicalizationContext.class).setPrincipalName("foo");
        
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        active.getSubject().getPrincipals().add(new TestPrincipal("bar2"));
        
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx!=null;
        authCtx.setAuthenticationResult(active);
        
        final RequestedPrincipalContext rpCtx = new RequestedPrincipalContext();
        rpCtx.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "florp", new ExactPrincipalEvalPredicateFactory());
        rpCtx.setMatchingPrincipal(new TestPrincipal("bar1"));
        rpCtx.setOperator("florp");
        rpCtx.setRequestedPrincipals(CollectionSupport.singletonList(new TestPrincipal("bar2")));
        authCtx.addSubcontext(rpCtx);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        SubjectContext sc = prc.getSubcontext(SubjectContext.class);
        assert sc!= null;
        Assert.assertEquals(sc.getPrincipalName(), "foo");
        Assert.assertEquals(sc.getAuthenticationResults().size(), 1);
        final Principal matchingPrincipal = rpCtx.getMatchingPrincipal();
        assert matchingPrincipal!= null;
        Assert.assertEquals(matchingPrincipal.getName(), "bar2");
    }

    @Test public void testNothingActive() {
        prc.getOrCreateSubcontext(SubjectCanonicalizationContext.class).setPrincipalName("foo");
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
    }

    @Test public void testOneActive() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        active.getSubject().getPrincipals().add(
                new ProxyAuthenticationPrincipal(CollectionSupport.singletonList(ActionTestingSupport.OUTBOUND_MSG_ISSUER)));
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx!=null;
        authCtx.setActiveResults(CollectionSupport.arrayAsList(active));
        authCtx.setAuthenticationResult(active);
        prc.getOrCreateSubcontext(SubjectCanonicalizationContext.class).setPrincipalName("foo");
        
        Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        SubjectContext sc = prc.getSubcontext(SubjectContext.class);
        assert sc!= null;
        Assert.assertEquals(sc.getPrincipalName(), "foo");
        Assert.assertEquals(sc.getAuthenticationResults().size(), 1);
        
        prc.removeSubcontext(SubjectContext.class);
        authCtx.getOrCreateSubcontext(RequestedPrincipalContext.class);
        
        event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        sc = prc.getSubcontext(SubjectContext.class);
        assert sc!= null;
        Assert.assertEquals(sc.getPrincipalName(), "foo");
        Assert.assertEquals(sc.getAuthenticationResults().size(), 1);
    }

    @Test public void testMultipleActive() {
        final AuthenticationResult active1 = new AuthenticationResult("test1", new Subject());
        final AuthenticationResult active2 = new AuthenticationResult("test2", new Subject());
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx!=null;
        authCtx.setActiveResults(CollectionSupport.arrayAsList(active1));
        authCtx.setAuthenticationResult(active2);
        prc.getOrCreateSubcontext(SubjectCanonicalizationContext.class).setPrincipalName("foo");
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectContext sc = prc.getSubcontext(SubjectContext.class);
        assert sc!= null;
        Assert.assertEquals(sc.getPrincipalName(), "foo");
        Assert.assertEquals(sc.getAuthenticationResults().size(), 2);
    }

    @Test public void testZeroProxyCount() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        final ProxyAuthenticationPrincipal proxy =
                new ProxyAuthenticationPrincipal(CollectionSupport.singletonList(ActionTestingSupport.OUTBOUND_MSG_ISSUER));
        proxy.setProxyCount(0);
        active.getSubject().getPrincipals().add(proxy);
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx!=null;
        authCtx.setActiveResults(CollectionSupport.arrayAsList(active));
        authCtx.setAuthenticationResult(active);
        prc.getOrCreateSubcontext(SubjectCanonicalizationContext.class).setPrincipalName("foo");

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
    }

    @Test public void testZeroProxyCountAdminFlow() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        final ProxyAuthenticationPrincipal proxy =
                new ProxyAuthenticationPrincipal(CollectionSupport.singletonList(ActionTestingSupport.OUTBOUND_MSG_ISSUER));
        proxy.setProxyCount(0);
        active.getSubject().getPrincipals().add(proxy);
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx!=null;
        authCtx.setActiveResults(CollectionSupport.arrayAsList(active));
        authCtx.setAuthenticationResult(active);
        prc.getOrCreateSubcontext(SubjectCanonicalizationContext.class).setPrincipalName("foo");

        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx!= null;
        rpCtx.setProfileConfig(new BasicAdministrativeFlowDescriptor("admin/test"));

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);
        SubjectContext sc = prc.getSubcontext(SubjectContext.class);
        assert sc!= null;
        Assert.assertEquals(sc.getPrincipalName(), "foo");
        Assert.assertEquals(sc.getAuthenticationResults().size(), 1);
    }

    @Test public void testZeroProxyCountNoRP() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        final ProxyAuthenticationPrincipal proxy =
                new ProxyAuthenticationPrincipal(CollectionSupport.singletonList(ActionTestingSupport.OUTBOUND_MSG_ISSUER));
        proxy.setProxyCount(0);
        active.getSubject().getPrincipals().add(proxy);
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx!=null;
        authCtx.setActiveResults(CollectionSupport.arrayAsList(active));
        authCtx.setAuthenticationResult(active);
        prc.getOrCreateSubcontext(SubjectCanonicalizationContext.class).setPrincipalName("foo");

        prc.removeSubcontext(RelyingPartyContext.class);

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);
        SubjectContext sc = prc.getSubcontext(SubjectContext.class);
        assert sc!= null;
        Assert.assertEquals(sc.getPrincipalName(), "foo");
        Assert.assertEquals(sc.getAuthenticationResults().size(), 1);
    }

    @Test public void testValidProxyAudience() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        final ProxyAuthenticationPrincipal proxy =
                new ProxyAuthenticationPrincipal(CollectionSupport.singletonList(ActionTestingSupport.OUTBOUND_MSG_ISSUER));
        proxy.setProxyCount(10);
        proxy.getAudiences().add(ActionTestingSupport.INBOUND_MSG_ISSUER);
        active.getSubject().getPrincipals().add(proxy);
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx!=null;
        authCtx.setActiveResults(CollectionSupport.arrayAsList(active));
        authCtx.setAuthenticationResult(active);
        prc.getOrCreateSubcontext(SubjectCanonicalizationContext.class).setPrincipalName("foo");

        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        SubjectContext sc = prc.getSubcontext(SubjectContext.class);
        assert sc!= null;
        Assert.assertEquals(sc.getPrincipalName(), "foo");
        Assert.assertEquals(sc.getAuthenticationResults().size(), 1);
    }

    @Test public void testInvalidProxyAudience() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        final ProxyAuthenticationPrincipal proxy =
                new ProxyAuthenticationPrincipal(CollectionSupport.singletonList(ActionTestingSupport.OUTBOUND_MSG_ISSUER));
        proxy.setProxyCount(10);
        proxy.getAudiences().add(ActionTestingSupport.OUTBOUND_MSG_ISSUER);
        active.getSubject().getPrincipals().add(proxy);
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx!=null;
        authCtx.setActiveResults(CollectionSupport.arrayAsList(active));
        authCtx.setAuthenticationResult(active);
        prc.getOrCreateSubcontext(SubjectCanonicalizationContext.class).setPrincipalName("foo");

        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
    }

}