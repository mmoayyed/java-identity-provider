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

import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.authn.principal.AuthenticationMethodPrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.saml2.profile.config.impl.BrowserSSOProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.context.navigate.IssuerLookupFunction;
import net.shibboleth.profile.relyingparty.BasicRelyingPartyConfiguration;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.ParentContextLookup;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.ext.reqattr.RequestedAttributes;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.IDPEntry;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.Scoping;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.opensaml.xmlsec.config.BasicXMLSecurityConfiguration;
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
        ac = prc1.getOrCreateSubcontext(AuthenticationContext.class);
        prc2 = ac.getOrCreateSubcontext(ProfileRequestContext.class);
        prc2.setOutboundMessageContext(new MessageContext());
        
        rpc = prc2.getOrCreateSubcontext(RelyingPartyContext.class);
        rpc.setRelyingPartyId(ActionTestingSupport.INBOUND_MSG_ISSUER);
        final BasicRelyingPartyConfiguration rp = new BasicRelyingPartyConfiguration();
        rp.setId("mock");
        rp.setIssuer(ActionTestingSupport.OUTBOUND_MSG_ISSUER);
        rp.setDetailedErrors(true);
        rp.initialize();
        rpc.setConfiguration(rp);
        rpc.setProfileConfig(new BrowserSSOProfileConfiguration());
        ((BrowserSSOProfileConfiguration) rpc.getProfileConfig()).setSecurityConfiguration(new BasicXMLSecurityConfiguration());
        
        action = new AddAuthnRequest();
        action.setProfileContextLookupStrategy(
                new ChildContextLookup<>(ProfileRequestContext.class).compose(
                        new ChildContextLookup<>(AuthenticationContext.class).compose(
                                new WebflowRequestContextProfileRequestContextLookup())));
        action.setAuthenticationContextLookupStrategy(new ParentContextLookup<>(AuthenticationContext.class));
        action.setIssuerLookupStrategy(new IssuerLookupFunction());
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
        assertNull(request.getAttributeConsumingServiceIndex());
        assertNull(request.getExtensions());
        
        final NameIDPolicy nid = request.getNameIDPolicy();
        assertNotNull(nid);
        assertNull(nid.getFormat());
        assertNull(nid.getSPNameQualifier());
        assertTrue(nid.getAllowCreate());
        
        assertNull(request.getRequestedAuthnContext());
        
        final Scoping scoping = request.getScoping();
        assertEquals(scoping.getRequesterIDs().get(0).getURI(), ActionTestingSupport.INBOUND_MSG_ISSUER);
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
                CollectionSupport.listOf(NameIDType.EMAIL, NameIDType.KERBEROS));
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        final AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        final NameIDPolicy nid = request.getNameIDPolicy();
        assertNotNull(nid);
        assertEquals(nid.getFormat(), NameIDType.EMAIL);
        assertTrue(nid.getAllowCreate());
    }

    /** Test that the action works with SPNameQualifier set. */
    @Test public void testSPNameQualifier() {
        ((BrowserSSOProfileConfiguration) rpc.getProfileConfig()).setSPNameQualifier(ActionTestingSupport.INBOUND_MSG_ISSUER);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        final AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        final NameIDPolicy nid = request.getNameIDPolicy();
        assertNotNull(nid);
        assertEquals(nid.getSPNameQualifier(), ActionTestingSupport.INBOUND_MSG_ISSUER);
    }

    /** Test that the action works with AttributeConsumingrServiceIndex set. */
    @Test public void testAttributeIndex() {
        ((BrowserSSOProfileConfiguration) rpc.getProfileConfig()).setAttributeIndex(42);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        final AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        assertEquals(request.getAttributeConsumingServiceIndex(), 42);
    }

    /** Test that the action works with RequestedAttributes set. */
    @Test public void testRequestedAttributes() {
        final XMLObjectBuilderFactory bf = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final SAMLObjectBuilder<RequestedAttribute> reqAttrBuilder =
                (SAMLObjectBuilder<RequestedAttribute>) bf.<RequestedAttribute>getBuilderOrThrow(
                        RequestedAttribute.DEFAULT_ELEMENT_NAME);
        final RequestedAttribute attr1 = reqAttrBuilder.buildObject();
        attr1.setNameFormat(Attribute.URI_REFERENCE);
        attr1.setName("https://attr1.example.org");
        final RequestedAttribute attr2 = reqAttrBuilder.buildObject();
        attr2.setNameFormat(Attribute.URI_REFERENCE);
        attr2.setName("https://attr2.example.org");

        ((BrowserSSOProfileConfiguration) rpc.getProfileConfig()).setRequestedAttributes(CollectionSupport.listOf(attr1, attr2));
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        final AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        assertNotNull(request.getExtensions());
        assertEquals(request.getExtensions().getUnknownXMLObjects(RequestedAttributes.DEFAULT_ELEMENT_NAME).size(), 1);
        final RequestedAttributes extension =
                (RequestedAttributes) request.getExtensions().getUnknownXMLObjects(RequestedAttributes.DEFAULT_ELEMENT_NAME).get(0);
        assertEquals(extension.getRequestedAttributes().size(), 2);
    }

    /** Test with Scoping element but no count. */
    @Test public void testScopingNoCount() {
        ac.getProxiableAuthorities().add("foo");
        ac.getProxiableAuthorities().add("bar");
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        final AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        final Scoping scoping = request.getScoping();
        assertNotNull(scoping);
        assertNull(scoping.getProxyCount());
        assertNotNull(scoping.getIDPList());
        assertEquals(scoping.getRequesterIDs().get(0).getURI(), ActionTestingSupport.INBOUND_MSG_ISSUER);
        
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
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        final AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        final Scoping scoping = request.getScoping();
        assertNotNull(scoping);
        assertNull(scoping.getIDPList());
        assertEquals(scoping.getProxyCount(), Integer.valueOf(0));
        assertEquals(scoping.getRequesterIDs().get(0).getURI(), ActionTestingSupport.INBOUND_MSG_ISSUER);
    }

    /** Test with Scoping element and count of 5. */
    @Test public void testScopingCount5() {
        ac.setProxyCount(5);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        final AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        final Scoping scoping = request.getScoping();
        assertNotNull(scoping);
        assertNull(scoping.getIDPList());
        assertEquals(scoping.getProxyCount(), Integer.valueOf(4));
        assertEquals(scoping.getRequesterIDs().get(0).getURI(), ActionTestingSupport.INBOUND_MSG_ISSUER);
    }

    /** Test with Scoping element and count of 0 (this shouldn't really happen). */
    @Test public void testScopingCount0() {
        ac.setProxyCount(0);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        assertNotNull(prc2.getOutboundMessageContext().getMessage());
        assertTrue(prc2.getOutboundMessageContext().getMessage() instanceof AuthnRequest);

        final AuthnRequest request = (AuthnRequest) prc2.getOutboundMessageContext().getMessage();
        final Scoping scoping = request.getScoping();
        assertNotNull(scoping);
        assertNull(scoping.getIDPList());
        assertEquals(scoping.getProxyCount(), Integer.valueOf(0));
        assertEquals(scoping.getRequesterIDs().get(0).getURI(), ActionTestingSupport.INBOUND_MSG_ISSUER);
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