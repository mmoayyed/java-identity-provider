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

package net.shibboleth.idp.saml.profile.impl;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.FunctionSupport;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Scoping;
import org.opensaml.saml.saml2.testing.SAML2ActionTestingSupport;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;

/** {@link InitializeAuthenticationContext} unit test. */
@SuppressWarnings("javadoc")
public class InitializeAuthenticationContextTest extends OpenSAMLInitBaseTestCase {

    private InitializeAuthenticationContext action;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        action = new InitializeAuthenticationContext();
        action.setProxyCountLookupStrategy(FunctionSupport.constant(1));
        action.initialize();
    }
    
    /**
     * Test that the action functions properly if there is no inbound message context.
     *  
     * @throws ComponentInitializationException ...
     */
    @Test public void testNoInboundMessageContext() throws ComponentInitializationException {
        final RequestContext requestCtx = new RequestContextBuilder().buildRequestContext();
        final ProfileRequestContext prc = new WebflowRequestContextProfileRequestContextLookup().apply(requestCtx);
        prc.setInboundMessageContext(null);

        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);

        final AuthenticationContext authnCtx = prc.getSubcontext(AuthenticationContext.class);
        Assert.assertNotNull(authnCtx);
        Assert.assertFalse(authnCtx.isForceAuthn());
        Assert.assertFalse(authnCtx.isPassive());
        Assert.assertEquals(authnCtx.getProxyCount(), Integer.valueOf(1));
    }

    /**
     * Test that the action functions properly if there is no inbound message.
     * 
     * @throws ComponentInitializationException ...
     */
    @Test public void testNoInboundMessage() throws ComponentInitializationException {
        final RequestContext requestCtx = new RequestContextBuilder().setInboundMessage(null).buildRequestContext();
        final ProfileRequestContext prc = new WebflowRequestContextProfileRequestContextLookup().apply(requestCtx);

        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);

        final AuthenticationContext authnCtx = prc.getSubcontext(AuthenticationContext.class);
        Assert.assertNotNull(authnCtx);
        Assert.assertFalse(authnCtx.isForceAuthn());
        Assert.assertFalse(authnCtx.isPassive());
        Assert.assertEquals(authnCtx.getProxyCount(), Integer.valueOf(1));
    }

    /**
     * Test that the action functions properly if the inbound message is not a SAML 2 AuthnRequest.
     * 
     * @throws ComponentInitializationException ...
     */
    @Test public void testSAML1AuthnRequest() throws ComponentInitializationException {
        final RequestContext requestCtx =
                new RequestContextBuilder().setInboundMessage(
                        new IdPInitiatedSSORequest("https://sp.example.org/sp", null, null, null)
                        ).buildRequestContext();
        final ProfileRequestContext prc = new WebflowRequestContextProfileRequestContextLookup().apply(requestCtx);
        
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);

        AuthenticationContext authnCtx = prc.getSubcontext(AuthenticationContext.class);
        Assert.assertNotNull(authnCtx);
        Assert.assertFalse(authnCtx.isForceAuthn());
        Assert.assertFalse(authnCtx.isPassive());
        Assert.assertEquals(authnCtx.getProxyCount(), Integer.valueOf(1));
    }

    /**
     * Test that the action proceeds properly if the inbound message is a SAML2 AuthnRequest.
     * 
     * @throws ComponentInitializationException ...
     */
    @Test public void testCreateAuthenticationContext() throws ComponentInitializationException {
        final AuthnRequest authnRequest = SAML2ActionTestingSupport.buildAuthnRequest();
        authnRequest.setIsPassive(true);
        authnRequest.setForceAuthn(true);

        final RequestContext requestCtx =
                new RequestContextBuilder().setInboundMessage(authnRequest).buildRequestContext();
        final ProfileRequestContext prc = new WebflowRequestContextProfileRequestContextLookup().apply(requestCtx);

        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationContext authnCtx = prc.getSubcontext(AuthenticationContext.class);
        Assert.assertNotNull(authnCtx);
        Assert.assertTrue(authnCtx.isForceAuthn());
        Assert.assertTrue(authnCtx.isPassive());
        Assert.assertEquals(authnCtx.getProxyCount(), Integer.valueOf(1));
    }

    @Test public void testScopingIgnored() throws ComponentInitializationException {
        final AuthnRequest authnRequest = SAML2ActionTestingSupport.buildAuthnRequest();
        authnRequest.setIsPassive(true);
        authnRequest.setForceAuthn(true);

        final RequestContext requestCtx =
                new RequestContextBuilder().setInboundMessage(authnRequest).buildRequestContext();
        final ProfileRequestContext prc = new WebflowRequestContextProfileRequestContextLookup().apply(requestCtx);

        final Scoping scoping = SAML2ActionTestingSupport.buildScoping(0, null);
        ((AuthnRequest) prc.getInboundMessageContext().getMessage()).setScoping(scoping);

        action = new InitializeAuthenticationContext();
        action.setProxyCountLookupStrategy(FunctionSupport.constant(1));
        action.setIgnoreScopingPredicate(Predicates.alwaysTrue());
        action.initialize();
        
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationContext authnCtx = prc.getSubcontext(AuthenticationContext.class);
        Assert.assertNotNull(authnCtx);
        Assert.assertTrue(authnCtx.isForceAuthn());
        Assert.assertTrue(authnCtx.isPassive());
        Assert.assertEquals(authnCtx.getProxyCount(), Integer.valueOf(1));
    }

    @Test public void testScopingDisallowed() throws ComponentInitializationException {
        final AuthnRequest authnRequest = SAML2ActionTestingSupport.buildAuthnRequest();
        authnRequest.setIsPassive(true);
        authnRequest.setForceAuthn(true);

        final RequestContext requestCtx =
                new RequestContextBuilder().setInboundMessage(authnRequest).buildRequestContext();
        final ProfileRequestContext prc = new WebflowRequestContextProfileRequestContextLookup().apply(requestCtx);

        final Scoping scoping = SAML2ActionTestingSupport.buildScoping(0, null);
        ((AuthnRequest) prc.getInboundMessageContext().getMessage()).setScoping(scoping);
        
        final BrowserSSOProfileConfiguration config = new BrowserSSOProfileConfiguration();
        config.setDisallowedFeatures(BrowserSSOProfileConfiguration.FEATURE_SCOPING);
        
        prc.getOrCreateSubcontext(RelyingPartyContext.class).setProfileConfig(config);
        
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertEvent(event, EventIds.ACCESS_DENIED);
        final AuthenticationContext authnCtx = prc.getSubcontext(AuthenticationContext.class);
        Assert.assertNull(authnCtx);
    }
    
    @Test public void testProxyCount() throws ComponentInitializationException {
        final AuthnRequest authnRequest = SAML2ActionTestingSupport.buildAuthnRequest();
        authnRequest.setIsPassive(true);
        authnRequest.setForceAuthn(true);

        final RequestContext requestCtx =
                new RequestContextBuilder().setInboundMessage(authnRequest).buildRequestContext();
        final ProfileRequestContext prc = new WebflowRequestContextProfileRequestContextLookup().apply(requestCtx);

        final Scoping scoping = SAML2ActionTestingSupport.buildScoping(0, null);
        ((AuthnRequest) prc.getInboundMessageContext().getMessage()).setScoping(scoping);
        
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);
        final AuthenticationContext authnCtx = prc.getSubcontext(AuthenticationContext.class);
        Assert.assertEquals(authnCtx.getProxyCount(), Integer.valueOf(0));
    }

    @Test public void testProxyList() throws ComponentInitializationException {
        final AuthnRequest authnRequest = SAML2ActionTestingSupport.buildAuthnRequest();
        authnRequest.setIsPassive(true);
        authnRequest.setForceAuthn(true);

        final RequestContext requestCtx =
                new RequestContextBuilder().setInboundMessage(authnRequest).buildRequestContext();
        final ProfileRequestContext prc = new WebflowRequestContextProfileRequestContextLookup().apply(requestCtx);

        final Scoping scoping = SAML2ActionTestingSupport.buildScoping(0, CollectionSupport.setOf("foo", "bar"));
        ((AuthnRequest) prc.getInboundMessageContext().getMessage()).setScoping(scoping);
        
        final Event event = action.execute(requestCtx);
        ActionTestingSupport.assertProceedEvent(event);
        final AuthenticationContext authnCtx = prc.getSubcontext(AuthenticationContext.class);
        Assert.assertEquals(authnCtx.getProxyCount(), Integer.valueOf(0));
        Assert.assertEquals(authnCtx.getProxiableAuthorities(), CollectionSupport.setOf("foo", "bar"));
    }

}
