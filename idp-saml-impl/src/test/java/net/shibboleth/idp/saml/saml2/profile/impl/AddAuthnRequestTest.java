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

package net.shibboleth.idp.saml.saml2.profile.impl;

import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.profile.IdPEventIds;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.saml.authn.principal.AuthenticationMethodPrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.saml2.profile.SAML2ActionTestingSupport;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.ParentContextLookup;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.IDPEntry;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.Scoping;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link AddAuthnRequest} unit test. */
public class AddAuthnRequestTest extends OpenSAMLInitBaseTestCase {

    private RequestContext rc;
    private AuthenticationContext ac;
    private ProfileRequestContext prc1,prc2;
    private RelyingPartyContext rpc;
    private AddAuthnRequest action;
    
    /**
     * Set up test state.
     * 
     * @throws ComponentInitializationException on error
     */
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        rc = new RequestContextBuilder().buildRequestContext();
        prc1 = new WebflowRequestContextProfileRequestContextLookup().apply(rc);
        ac = prc1.getSubcontext(AuthenticationContext.class, true);
        prc2 = ac.getSubcontext(ProfileRequestContext.class, true);
        prc2.setOutboundMessageContext(new MessageContext());
        
        rpc = prc2.getSubcontext(RelyingPartyContext.class, true);
        rpc.setRelyingPartyId(ActionTestingSupport.INBOUND_MSG_ISSUER);
        final RelyingPartyConfiguration rp = new RelyingPartyConfiguration();
        rp.setId("mock");
        rp.setResponderId(ActionTestingSupport.OUTBOUND_MSG_ISSUER);
        rp.setDetailedErrors(true);
        rp.initialize();
        rpc.setConfiguration(rp);
        rpc.setProfileConfig(new BrowserSSOProfileConfiguration());
        
        action = new AddAuthnRequest();
        action.setProfileContextLookupStrategy(
                new ChildContextLookup<>(ProfileRequestContext.class).compose(
                        new ChildContextLookup<>(AuthenticationContext.class).compose(
                                new WebflowRequestContextProfileRequestContextLookup())));
        action.setAuthenticationContextLookupStrategy(new ParentContextLookup<>(AuthenticationContext.class));
        action.setIssuerLookupStrategy(new ResponderIdLookupFunction());
        action.initialize();
    }
    
    /** Test that the action errors out properly if there is no relying party context. */
    @Test public void testNoRelyingPartyContext() {
        prc2.removeSubcontext(RelyingPartyContext.class);

        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, IdPEventIds.INVALID_PROFILE_CONFIG);
    }

    /** Test that the action errors out properly if there is no context. */
    @Test public void testNoMessageContext() {
        prc2.setOutboundMessageContext(null);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_MSG_CTX);
    }

    /** Test that the action errors out properly if there is a message there. */
    @Test public void testExistingMessage() {
        prc2.getOutboundMessageContext().setMessage(SAML2ActionTestingSupport.buildAuthnRequest());
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_MSG_CTX);
    }
    
    /** Test that the action works in vanilla form. */
    @Test public void testSimple() {
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        final AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        assertEquals(request.getIssuer().getValue(), ActionTestingSupport.OUTBOUND_MSG_ISSUER);
        assertFalse(request.isForceAuthn());
        assertFalse(request.isPassive());
        
        final NameIDPolicy nid = request.getNameIDPolicy();
        assertNotNull(nid);
        assertNull(nid.getFormat());
        assertTrue(nid.getAllowCreate());
        
        assertNull(request.getRequestedAuthnContext());
    }

    /** Test that the action works for ForceAuthn/IsPassive. */
    @Test public void testFlags() {
        ac.setIsPassive(true);
        ac.setForceAuthn(true);
        
        Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        final AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        assertEquals(request.getIssuer().getValue(), ActionTestingSupport.OUTBOUND_MSG_ISSUER);
        assertTrue(request.isForceAuthn());
        assertTrue(request.isPassive());
        
        prc2.getOutboundMessageContext().setMessage(null);
        ((BrowserSSOProfileConfiguration) rpc.getProfileConfig()).setForceAuthn(false);

        event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        assertFalse(((AuthnRequest) prc2.getOutboundMessageContext().getMessage()).isForceAuthn());
    }

    /** Test that the action works with a NameID format set. */
    @Test public void testNameIDFormat() {
        ((BrowserSSOProfileConfiguration) rpc.getProfileConfig()).setNameIDFormatPrecedence(
                Arrays.asList(NameIDType.EMAIL, NameIDType.KERBEROS));
        
        Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        final AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        final NameIDPolicy nid = request.getNameIDPolicy();
        assertNotNull(nid);
        assertEquals(nid.getFormat(), NameIDType.EMAIL);
        assertTrue(nid.getAllowCreate());
    }

    /** Test with Scoping element but no count. */
    @Test public void testScopingNoCount() {
        ac.getProxiableAuthorities().add("foo");
        ac.getProxiableAuthorities().add("bar");
        
        Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        final AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        final Scoping scoping = request.getScoping();
        assertNotNull(scoping);
        assertNull(scoping.getProxyCount());
        assertNotNull(scoping.getIDPList());
        
        final Set<String> requestedAuthorities = scoping.getIDPList().getIDPEntrys()
                .stream()
                .map(IDPEntry::getProviderID)
                .filter(id -> id != null)
                .collect(Collectors.toUnmodifiableSet());
        
        assertEquals(requestedAuthorities, ac.getProxiableAuthorities());
    }

    /** Test with Scoping element and count of 1. */
    @Test public void testScopingCount1() {

        ac.setProxyCount(1);
        
        Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        final AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        final Scoping scoping = request.getScoping();
        assertNotNull(scoping);
        assertNull(scoping.getIDPList());
        assertEquals(scoping.getProxyCount(), Integer.valueOf(0));
    }

    /** Test with Scoping element and count of 5. */
    @Test public void testScopingCount5() {

        ac.setProxyCount(5);
        
        Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        final AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        final Scoping scoping = request.getScoping();
        assertNotNull(scoping);
        assertNull(scoping.getIDPList());
        assertEquals(scoping.getProxyCount(), Integer.valueOf(4));
    }

    /** Test with Scoping element and count of 0 (this shouldn't really happen). */
    @Test public void testScopingCount0() {

        ac.setProxyCount(0);
        
        Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        final AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        final Scoping scoping = request.getScoping();
        assertNotNull(scoping);
        assertNull(scoping.getIDPList());
        assertEquals(scoping.getProxyCount(), Integer.valueOf(0));
    }

    /** Test that the action works for RequestedAuthnContext. */
    @Test public void testAuthnContext() {
        
        final RequestedPrincipalContext reqctx = ac.getSubcontext(RequestedPrincipalContext.class, true);
        reqctx.setOperator("exact");
        reqctx.setRequestedPrincipals(
                Arrays.asList(new AuthnContextClassRefPrincipal(AuthnContext.KERBEROS_AUTHN_CTX),
                        new AuthenticationMethodPrincipal(AuthenticationStatement.KERBEROS_AUTHN_METHOD),
                        new AuthnContextClassRefPrincipal(AuthnContext.X509_AUTHN_CTX)));
        
        Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        RequestedAuthnContext rac = request.getRequestedAuthnContext();
        assertNotNull(rac);
        assertEquals(rac.getComparison(), AuthnContextComparisonTypeEnumeration.EXACT);
        assertEquals(rac.getAuthnContextClassRefs().size(), 2);
        assertEquals(rac.getAuthnContextClassRefs().get(0).getURI(), AuthnContext.KERBEROS_AUTHN_CTX);
        assertEquals(rac.getAuthnContextClassRefs().get(1).getURI(), AuthnContext.X509_AUTHN_CTX);

        ((BrowserSSOProfileConfiguration) rpc.getProfileConfig()).setAuthnContextComparison(
                AuthnContextComparisonTypeEnumeration.EXACT);
        ((BrowserSSOProfileConfiguration) rpc.getProfileConfig()).setDefaultAuthenticationMethods(
                Arrays.asList(new AuthnContextClassRefPrincipal(AuthnContext.KERBEROS_AUTHN_CTX),
                        new AuthnContextClassRefPrincipal(AuthnContext.X509_AUTHN_CTX)));

        prc2.getOutboundMessageContext().setMessage(null);
        event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);

        request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        rac = request.getRequestedAuthnContext();
        assertNotNull(rac);
        assertEquals(rac.getComparison(), AuthnContextComparisonTypeEnumeration.EXACT);
        assertEquals(rac.getAuthnContextClassRefs().size(), 2);
        assertEquals(rac.getAuthnContextClassRefs().get(0).getURI(), AuthnContext.KERBEROS_AUTHN_CTX);
        assertEquals(rac.getAuthnContextClassRefs().get(1).getURI(), AuthnContext.X509_AUTHN_CTX);
    }

}