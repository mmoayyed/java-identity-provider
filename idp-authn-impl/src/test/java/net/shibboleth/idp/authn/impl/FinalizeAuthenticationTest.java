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
import java.util.Collections;

import javax.security.auth.Subject;

import net.shibboleth.idp.admin.BasicAdministrativeFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.authn.principal.ProxyAuthenticationPrincipal;
import net.shibboleth.idp.authn.principal.impl.ExactPrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.testing.TestPrincipal;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;

import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link FinalizeAuthentication} unit test. */
public class FinalizeAuthenticationTest extends BaseAuthenticationContextTest {
    
    private FinalizeAuthentication action; 
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        prc.getSubcontext(RelyingPartyContext.class).setProfileConfig(new BrowserSSOProfileConfiguration());
        
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
        authCtx.setRequiredName("foo");
        
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        active.getSubject().getPrincipals().add(new TestPrincipal("bar2"));
        
        authCtx.setAuthenticationResult(active);

        prc.getSubcontext(SubjectCanonicalizationContext.class, true).setPrincipalName("bar");

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_SUBJECT);
    }

    @Test public void testRequestUnsupported() {
        prc.getSubcontext(SubjectCanonicalizationContext.class, true).setPrincipalName("foo");
        
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        active.getSubject().getPrincipals().add(new TestPrincipal("bar2"));
        
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setAuthenticationResult(active);
        
        final RequestedPrincipalContext rpCtx = new RequestedPrincipalContext();
        rpCtx.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "florp", new ExactPrincipalEvalPredicateFactory());
        rpCtx.setMatchingPrincipal(new TestPrincipal("bar1"));
        rpCtx.setOperator("florp");
        rpCtx.setRequestedPrincipals(Collections.singletonList(new TestPrincipal("bar1")));
        authCtx.addSubcontext(rpCtx);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
    }

    @Test public void testSwitchesPrincipal() {
        prc.getSubcontext(SubjectCanonicalizationContext.class, true).setPrincipalName("foo");
        
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        active.getSubject().getPrincipals().add(new TestPrincipal("bar2"));
        
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setAuthenticationResult(active);
        
        final RequestedPrincipalContext rpCtx = new RequestedPrincipalContext();
        rpCtx.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "florp", new ExactPrincipalEvalPredicateFactory());
        rpCtx.setMatchingPrincipal(new TestPrincipal("bar1"));
        rpCtx.setOperator("florp");
        rpCtx.setRequestedPrincipals(Collections.singletonList(new TestPrincipal("bar2")));
        authCtx.addSubcontext(rpCtx);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        SubjectContext sc = prc.getSubcontext(SubjectContext.class);
        Assert.assertNotNull(sc);
        Assert.assertEquals(sc.getPrincipalName(), "foo");
        Assert.assertEquals(sc.getAuthenticationResults().size(), 1);
        Assert.assertEquals(rpCtx.getMatchingPrincipal().getName(), "bar2");
    }

    @Test public void testNothingActive() {
        prc.getSubcontext(SubjectCanonicalizationContext.class, true).setPrincipalName("foo");
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
    }

    @Test public void testOneActive() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        active.getSubject().getPrincipals().add(
                new ProxyAuthenticationPrincipal(Collections.singletonList(ActionTestingSupport.OUTBOUND_MSG_ISSUER)));
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.setAuthenticationResult(active);
        prc.getSubcontext(SubjectCanonicalizationContext.class, true).setPrincipalName("foo");
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        SubjectContext sc = prc.getSubcontext(SubjectContext.class);
        Assert.assertNotNull(sc);
        Assert.assertEquals(sc.getPrincipalName(), "foo");
        Assert.assertEquals(sc.getAuthenticationResults().size(), 1);
    }

    @Test public void testMultipleActive() {
        final AuthenticationResult active1 = new AuthenticationResult("test1", new Subject());
        final AuthenticationResult active2 = new AuthenticationResult("test2", new Subject());
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setActiveResults(Arrays.asList(active1));
        authCtx.setAuthenticationResult(active2);
        prc.getSubcontext(SubjectCanonicalizationContext.class, true).setPrincipalName("foo");
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        final SubjectContext sc = prc.getSubcontext(SubjectContext.class);
        Assert.assertNotNull(sc);
        Assert.assertEquals(sc.getPrincipalName(), "foo");
        Assert.assertEquals(sc.getAuthenticationResults().size(), 2);
    }

    @Test public void testZeroProxyCount() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        final ProxyAuthenticationPrincipal proxy =
                new ProxyAuthenticationPrincipal(Collections.singletonList(ActionTestingSupport.OUTBOUND_MSG_ISSUER));
        proxy.setProxyCount(0);
        active.getSubject().getPrincipals().add(proxy);
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.setAuthenticationResult(active);
        prc.getSubcontext(SubjectCanonicalizationContext.class, true).setPrincipalName("foo");

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
    }

    @Test public void testZeroProxyCountAdminFlow() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        final ProxyAuthenticationPrincipal proxy =
                new ProxyAuthenticationPrincipal(Collections.singletonList(ActionTestingSupport.OUTBOUND_MSG_ISSUER));
        proxy.setProxyCount(0);
        active.getSubject().getPrincipals().add(proxy);
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.setAuthenticationResult(active);
        prc.getSubcontext(SubjectCanonicalizationContext.class, true).setPrincipalName("foo");

        prc.getSubcontext(RelyingPartyContext.class).setProfileConfig(new BasicAdministrativeFlowDescriptor("admin/test"));

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);
        SubjectContext sc = prc.getSubcontext(SubjectContext.class);
        Assert.assertNotNull(sc);
        Assert.assertEquals(sc.getPrincipalName(), "foo");
        Assert.assertEquals(sc.getAuthenticationResults().size(), 1);
    }

    @Test public void testZeroProxyCountNoRP() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        final ProxyAuthenticationPrincipal proxy =
                new ProxyAuthenticationPrincipal(Collections.singletonList(ActionTestingSupport.OUTBOUND_MSG_ISSUER));
        proxy.setProxyCount(0);
        active.getSubject().getPrincipals().add(proxy);
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.setAuthenticationResult(active);
        prc.getSubcontext(SubjectCanonicalizationContext.class, true).setPrincipalName("foo");

        prc.removeSubcontext(RelyingPartyContext.class);

        final Event event = action.execute(src);

        ActionTestingSupport.assertProceedEvent(event);
        SubjectContext sc = prc.getSubcontext(SubjectContext.class);
        Assert.assertNotNull(sc);
        Assert.assertEquals(sc.getPrincipalName(), "foo");
        Assert.assertEquals(sc.getAuthenticationResults().size(), 1);
    }

    @Test public void testValidProxyAudience() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        final ProxyAuthenticationPrincipal proxy =
                new ProxyAuthenticationPrincipal(Collections.singletonList(ActionTestingSupport.OUTBOUND_MSG_ISSUER));
        proxy.setProxyCount(10);
        proxy.getAudiences().add(ActionTestingSupport.INBOUND_MSG_ISSUER);
        active.getSubject().getPrincipals().add(proxy);
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.setAuthenticationResult(active);
        prc.getSubcontext(SubjectCanonicalizationContext.class, true).setPrincipalName("foo");

        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        SubjectContext sc = prc.getSubcontext(SubjectContext.class);
        Assert.assertNotNull(sc);
        Assert.assertEquals(sc.getPrincipalName(), "foo");
        Assert.assertEquals(sc.getAuthenticationResults().size(), 1);
    }

    @Test public void testInvalidProxyAudience() {
        final AuthenticationResult active = new AuthenticationResult("test2", new Subject());
        final ProxyAuthenticationPrincipal proxy =
                new ProxyAuthenticationPrincipal(Collections.singletonList(ActionTestingSupport.OUTBOUND_MSG_ISSUER));
        proxy.setProxyCount(10);
        proxy.getAudiences().add(ActionTestingSupport.OUTBOUND_MSG_ISSUER);
        active.getSubject().getPrincipals().add(proxy);
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        authCtx.setActiveResults(Arrays.asList(active));
        authCtx.setAuthenticationResult(active);
        prc.getSubcontext(SubjectCanonicalizationContext.class, true).setPrincipalName("foo");

        final Event event = action.execute(src);
        
        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
        Assert.assertNull(prc.getSubcontext(SubjectContext.class));
    }

}