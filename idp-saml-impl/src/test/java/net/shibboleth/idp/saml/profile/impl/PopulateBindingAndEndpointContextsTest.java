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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.saml2.profile.config.impl.BrowserSSOProfileConfiguration;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.xml.XMLParserException;

import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.binding.BindingDescriptor;
import org.opensaml.saml.common.binding.impl.DefaultEndpointResolver;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.profile.SAMLEventIds;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.testing.SAML2ActionTestingSupport;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.google.common.base.Predicates;

/** Unit test for {@link PopulateBindingAndEndpointContexts}. */
@SuppressWarnings("javadoc")
public class PopulateBindingAndEndpointContextsTest extends XMLObjectBaseTestCase {

    private static final String RELAY_STATE = "foo";
    private static final String LOCATION = "https://sp.example.org/ACS";
    private static final String LOCATION_POST = "https://sp.example.org/POST2";
    private static final String LOCATION_ART = "https://sp.example.org/Art2";

    private RequestContext rc;

    private BrowserSSOProfileConfiguration profileConfig;
    
    private ProfileRequestContext prc;
    
    private PopulateBindingAndEndpointContexts action;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        final AuthnRequest request = SAML2ActionTestingSupport.buildAuthnRequest();
        request.setAssertionConsumerServiceURL(LOCATION_POST);
        request.setProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        
        profileConfig = new BrowserSSOProfileConfiguration();
        
        rc = new RequestContextBuilder().setInboundMessage(request).setRelyingPartyProfileConfigurations(
                Collections.<ProfileConfiguration>singletonList(profileConfig)).buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(rc);
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc !=null;
        imc.getOrCreateSubcontext(SAMLBindingContext.class).setRelayState(RELAY_STATE);
        
        // Set these up so the context will be seen as anonymous or not based on metadata in the outbound context.
        final RelyingPartyContext rpc = prc.getSubcontext(RelyingPartyContext.class);
        assert rpc!=null;
        rpc.setVerificationLookupStrategy(new SAMLVerificationLookupStrategy());
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        rpc.setRelyingPartyIdContextTree(omc.getOrCreateSubcontext(SAMLPeerEntityContext.class));
        
        action = new PopulateBindingAndEndpointContexts();
        action.setEndpointResolver(new DefaultEndpointResolver<>());
        action.setEndpointType(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        final List<BindingDescriptor> bindings = new ArrayList<>();
        bindings.add(new BindingDescriptor());
        bindings.get(0).setId(SAMLConstants.SAML2_POST_BINDING_URI);
        bindings.get(0).initialize();
        action.setBindingDescriptorsLookupStrategy(FunctionSupport.<ProfileRequestContext,List<BindingDescriptor>>constant(bindings));
        action.initialize();
    }

    @Test(expectedExceptions = ComponentInitializationException.class)
    public void testNoResolver() throws ComponentInitializationException {
        final PopulateBindingAndEndpointContexts badaction = new PopulateBindingAndEndpointContexts();
        badaction.initialize();
    }
    
    @Test(expectedExceptions = ComponentInitializationException.class)
    public void testBadEndpointType() throws ComponentInitializationException {
        final PopulateBindingAndEndpointContexts badaction = new PopulateBindingAndEndpointContexts();
        badaction.setEndpointType(AuthnRequest.DEFAULT_ELEMENT_NAME);
        badaction.initialize();
    }
    
    @Test
    public void testNoOutboundContext() {
        prc.setOutboundMessageContext(null);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_MSG_CTX);
    }

    @Test
    public void testNoBindings() throws ComponentInitializationException {
        final BindingDescriptor binding = new BindingDescriptor();
        binding.setId(SAMLConstants.SAML2_POST_BINDING_URI);
        binding.setActivationCondition(Predicates.<ProfileRequestContext>alwaysFalse());
        binding.initialize();
        final PopulateBindingAndEndpointContexts badaction = new PopulateBindingAndEndpointContexts();
        badaction.setEndpointResolver(new DefaultEndpointResolver<>());
        badaction.setBindingDescriptorsLookupStrategy(
                FunctionSupport.<ProfileRequestContext,List<BindingDescriptor>>constant(Collections.singletonList(binding)));
        badaction.initialize();
        
        final Event event = badaction.execute(rc);
        ActionTestingSupport.assertEvent(event, SAMLEventIds.ENDPOINT_RESOLUTION_FAILED);
    }
    
    @Test
    public void testNoMetadata() {
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        
        final SAMLBindingContext bindingCtx = omc.getSubcontext(SAMLBindingContext.class);
        assert bindingCtx!=null;
        Assert.assertNotNull(bindingCtx.getBindingDescriptor());
        Assert.assertEquals(bindingCtx.getRelayState(), RELAY_STATE);
        Assert.assertEquals(bindingCtx.getBindingUri(), SAMLConstants.SAML2_POST_BINDING_URI);
        
        final SAMLPeerEntityContext pec = omc.getSubcontext(SAMLPeerEntityContext.class);
        assert pec!=null;
        final SAMLEndpointContext epCtx = pec.getSubcontext(SAMLEndpointContext.class, false);
        assert epCtx!=null;
        final Endpoint ep =epCtx.getEndpoint();
        assert ep!=null;
        Assert.assertEquals(ep.getBinding(), SAMLConstants.SAML2_POST_BINDING_URI);
        Assert.assertEquals(ep.getLocation(), LOCATION_POST);
    }

    /**
     * An SP with no endpoints in metadata.
     * 
     * @throws UnmarshallingException ...
     */
    @Test
    public void testNoEndpoints() throws UnmarshallingException {
        final EntityDescriptor entity = loadMetadata("/net/shibboleth/idp/saml/impl/profile/SPNoEndpoints.xml");
        final SAMLMetadataContext mdCtx = new SAMLMetadataContext();
        mdCtx.setEntityDescriptor(entity);
        mdCtx.setRoleDescriptor(entity.getSPSSODescriptor("required"));
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        omc.getOrCreateSubcontext(SAMLPeerEntityContext.class).addSubcontext(mdCtx);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, SAMLEventIds.ENDPOINT_RESOLUTION_FAILED);
    }

    /**
     * An SP with no endpoints in metadata interacting with signed requests.
     * 
     * @throws UnmarshallingException ...
     */
    @Test
    public void testSignedNoEndpoints() throws UnmarshallingException {
        final EntityDescriptor entity = loadMetadata("/net/shibboleth/idp/saml/impl/profile/SPNoEndpoints.xml");
        final SAMLMetadataContext mdCtx = new SAMLMetadataContext();
        mdCtx.setEntityDescriptor(entity);
        mdCtx.setRoleDescriptor(entity.getSPSSODescriptor("required"));
        MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        omc.getOrCreateSubcontext(SAMLPeerEntityContext.class).addSubcontext(mdCtx);
        
        Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, SAMLEventIds.ENDPOINT_RESOLUTION_FAILED);
        
        // Allow signed, but request isn't.
        profileConfig.setSkipEndpointValidationWhenSigned(true);
        event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, SAMLEventIds.ENDPOINT_RESOLUTION_FAILED);
        
        // Request is signed but we don't care.
        profileConfig.setSkipEndpointValidationWhenSigned(false);
        MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        SAMLBindingContext sbc = imc.getSubcontext(SAMLBindingContext.class);
        assert sbc != null;
        sbc.setHasBindingSignature(true);
        event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, SAMLEventIds.ENDPOINT_RESOLUTION_FAILED);

        // Request is signed and we care.
        profileConfig.setSkipEndpointValidationWhenSigned(true);
        imc = prc.getInboundMessageContext();
        assert imc!=null;
        sbc = imc.getSubcontext(SAMLBindingContext.class);
        assert sbc != null;
        sbc.setHasBindingSignature(true);
        event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        omc = prc.getOutboundMessageContext();
        assert omc!=null;
        final SAMLBindingContext bindingCtx = omc.getSubcontext(SAMLBindingContext.class);
        assert bindingCtx!=null;
        Assert.assertNotNull(bindingCtx.getBindingDescriptor());
        Assert.assertEquals(bindingCtx.getRelayState(), RELAY_STATE);
        Assert.assertEquals(bindingCtx.getBindingUri(), SAMLConstants.SAML2_POST_BINDING_URI);
    }
    
    /**
     * No endpoint with the location requested.
     * 
     * @throws UnmarshallingException ...
     */
    @Test
    public void testBadLocation() throws UnmarshallingException {
        final EntityDescriptor entity = loadMetadata("/net/shibboleth/idp/saml/impl/profile/SPWithEndpoints.xml");
        final SAMLMetadataContext mdCtx = new SAMLMetadataContext();
        mdCtx.setEntityDescriptor(entity);
        mdCtx.setRoleDescriptor(entity.getSPSSODescriptor("required"));
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        omc.getOrCreateSubcontext(SAMLPeerEntityContext.class).addSubcontext(mdCtx);
        
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc != null;
        final AuthnRequest authnRequest = (AuthnRequest) imc.getMessage();
        assert authnRequest!=null;
        authnRequest.setAssertionConsumerServiceURL(LOCATION);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, SAMLEventIds.ENDPOINT_RESOLUTION_FAILED);
    }

    /**
     * No endpoint at a location with the right binding requested.
     * 
     * @throws UnmarshallingException ...
     */
    @Test
    public void testBadBinding() throws UnmarshallingException {
        final EntityDescriptor entity = loadMetadata("/net/shibboleth/idp/saml/impl/profile/SPWithEndpoints.xml");
        final SAMLMetadataContext mdCtx = new SAMLMetadataContext();
        mdCtx.setEntityDescriptor(entity);
        mdCtx.setRoleDescriptor(entity.getSPSSODescriptor("required"));
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;

        omc.getOrCreateSubcontext(SAMLPeerEntityContext.class).addSubcontext(mdCtx);
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc != null;
        final AuthnRequest authnRequest = (AuthnRequest) imc.getMessage();
        assert authnRequest!=null;
        authnRequest.setProtocolBinding(SAMLConstants.SAML2_SOAP11_BINDING_URI);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, SAMLEventIds.ENDPOINT_RESOLUTION_FAILED);
    }

    /**
     * Endpoint matches but we don't support the binding.
     * 
     * @throws UnmarshallingException ...
     */
    @Test
    public void testUnsupportedBinding() throws UnmarshallingException {
        final EntityDescriptor entity = loadMetadata("/net/shibboleth/idp/saml/impl/profile/SPWithEndpoints.xml");
        final SAMLMetadataContext mdCtx = new SAMLMetadataContext();
        mdCtx.setEntityDescriptor(entity);
        mdCtx.setRoleDescriptor(entity.getSPSSODescriptor("required"));
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        omc.getOrCreateSubcontext(SAMLPeerEntityContext.class).addSubcontext(mdCtx);
        
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc != null;
        final AuthnRequest authnRequest = (AuthnRequest) imc.getMessage();
        assert authnRequest!=null;
        authnRequest.setAssertionConsumerServiceURL(LOCATION_ART);
        authnRequest.setProtocolBinding(SAMLConstants.SAML2_ARTIFACT_BINDING_URI);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, SAMLEventIds.ENDPOINT_RESOLUTION_FAILED);
    }
    
    /**
     * No endpoint with a requested index.
     * 
     * @throws UnmarshallingException ...
     */
    @Test
    public void testBadIndex() throws UnmarshallingException {
        final EntityDescriptor entity = loadMetadata("/net/shibboleth/idp/saml/impl/profile/SPWithEndpoints.xml");
        final SAMLMetadataContext mdCtx = new SAMLMetadataContext();
        mdCtx.setEntityDescriptor(entity);
        mdCtx.setRoleDescriptor(entity.getSPSSODescriptor("required"));
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        omc.getOrCreateSubcontext(SAMLPeerEntityContext.class).addSubcontext(mdCtx);

        final MessageContext imc = prc.getInboundMessageContext();
        assert imc != null;
        final AuthnRequest authnRequest = (AuthnRequest) imc.getMessage();
        assert authnRequest!=null;
        authnRequest.setAssertionConsumerServiceIndex(10);
        authnRequest.setAssertionConsumerServiceURL(null);
        authnRequest.setProtocolBinding(null);

        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, SAMLEventIds.ENDPOINT_RESOLUTION_FAILED);
    }
    
    /**
     * Test SOAP case.
     * 
     * @throws ComponentInitializationException ...
     */
    @Test
    public void testSynchronous() throws ComponentInitializationException {
        
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        final SAMLBindingContext sbc = imc.getSubcontext(SAMLBindingContext.class);
        assert sbc != null;

        sbc.setBindingUri(
                SAMLConstants.SAML2_SOAP11_BINDING_URI);
        
        final BindingDescriptor binding = new BindingDescriptor();
        binding.setId(SAMLConstants.SAML2_SOAP11_BINDING_URI);
        binding.setSynchronous(true);
        binding.initialize();
        final PopulateBindingAndEndpointContexts badaction = new PopulateBindingAndEndpointContexts();
        badaction.setEndpointResolver(new DefaultEndpointResolver<>());
        badaction.setBindingDescriptorsLookupStrategy(
                FunctionSupport.<ProfileRequestContext,List<BindingDescriptor>>constant(Collections.singletonList(binding)));
        badaction.initialize();
        
        final Event event = badaction.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        final SAMLBindingContext bindingCtx = omc.getSubcontext(SAMLBindingContext.class);
        assert bindingCtx!=null;
        Assert.assertEquals(bindingCtx.getRelayState(), RELAY_STATE);
        Assert.assertEquals(bindingCtx.getBindingUri(), SAMLConstants.SAML2_SOAP11_BINDING_URI);
        Assert.assertSame(binding, bindingCtx.getBindingDescriptor());
    }
    
    /**
     * Requested location/binding are in metadata.
     * 
     * @throws UnmarshallingException ...
     */
    @Test
    public void testInMetadata() throws UnmarshallingException {
        final EntityDescriptor entity = loadMetadata("/net/shibboleth/idp/saml/impl/profile/SPWithEndpoints.xml");
        final SAMLMetadataContext mdCtx = new SAMLMetadataContext();
        mdCtx.setEntityDescriptor(entity);
        mdCtx.setRoleDescriptor(entity.getSPSSODescriptor("required"));
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        omc.getOrCreateSubcontext(SAMLPeerEntityContext.class).addSubcontext(mdCtx);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        final SAMLBindingContext bindingCtx = omc.getSubcontext(SAMLBindingContext.class);
        assert bindingCtx!=null;
        Assert.assertNotNull(bindingCtx.getBindingDescriptor());
        Assert.assertEquals(bindingCtx.getRelayState(), RELAY_STATE);
        Assert.assertEquals(bindingCtx.getBindingUri(), SAMLConstants.SAML2_POST_BINDING_URI);
        
        final SAMLPeerEntityContext pec = omc.getSubcontext(SAMLPeerEntityContext.class);
        assert pec!=null;
        final SAMLEndpointContext epCtx = pec.getSubcontext(SAMLEndpointContext.class, false);
        assert epCtx!=null;
        final Endpoint ep =epCtx.getEndpoint();
        assert ep!=null;

        Assert.assertEquals(ep.getBinding(), SAMLConstants.SAML2_POST_BINDING_URI);
        Assert.assertEquals(ep.getLocation(), LOCATION_POST);
    }

    /**
     * Requested index is in metadata.
     * 
     * @throws UnmarshallingException ...
     */
    @Test
    public void testIndexInMetadata() throws UnmarshallingException {
        final EntityDescriptor entity = loadMetadata("/net/shibboleth/idp/saml/impl/profile/SPWithEndpoints.xml");
        final SAMLMetadataContext mdCtx = new SAMLMetadataContext();
        mdCtx.setEntityDescriptor(entity);
        mdCtx.setRoleDescriptor(entity.getSPSSODescriptor("required"));
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        omc.getOrCreateSubcontext(SAMLPeerEntityContext.class).addSubcontext(mdCtx);

        
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc != null;
        final AuthnRequest authnRequest = (AuthnRequest) imc.getMessage();
        assert authnRequest!=null;
        authnRequest.setAssertionConsumerServiceIndex(2);
        authnRequest.setAssertionConsumerServiceURL(null);
        authnRequest.setProtocolBinding(null);

        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);

        final SAMLBindingContext bindingCtx = omc.getSubcontext(SAMLBindingContext.class);
        assert bindingCtx!=null;
        Assert.assertNotNull(bindingCtx.getBindingDescriptor());
        Assert.assertEquals(bindingCtx.getRelayState(), RELAY_STATE);
        Assert.assertEquals(bindingCtx.getBindingUri(), SAMLConstants.SAML2_POST_BINDING_URI);
        
        final SAMLPeerEntityContext pec = omc.getSubcontext(SAMLPeerEntityContext.class);
        assert pec!=null;
        final SAMLEndpointContext epCtx = pec.getSubcontext(SAMLEndpointContext.class, false);
        assert epCtx!=null;
        final Endpoint ep =epCtx.getEndpoint();
        assert ep!=null;
        Assert.assertEquals(ep.getBinding(), SAMLConstants.SAML2_POST_BINDING_URI);
        Assert.assertEquals(ep.getLocation(), LOCATION_POST);
    }

    /**
     * No endpoint with a requested index.
     * 
     * @throws UnmarshallingException ...
     */
    @Test
    public void testIndexUnsupportedBinding() throws UnmarshallingException {
        final EntityDescriptor entity = loadMetadata("/net/shibboleth/idp/saml/impl/profile/SPWithEndpoints.xml");
        final SAMLMetadataContext mdCtx = new SAMLMetadataContext();
        mdCtx.setEntityDescriptor(entity);
        mdCtx.setRoleDescriptor(entity.getSPSSODescriptor("required"));
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        omc.getOrCreateSubcontext(SAMLPeerEntityContext.class).addSubcontext(mdCtx);

        final MessageContext imc = prc.getInboundMessageContext();
        assert imc != null;
        final AuthnRequest authnRequest = (AuthnRequest) imc.getMessage();
        assert authnRequest!=null;
        authnRequest.setAssertionConsumerServiceIndex(3);
        authnRequest.setAssertionConsumerServiceURL(null);
        authnRequest.setProtocolBinding(null);

        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, SAMLEventIds.ENDPOINT_RESOLUTION_FAILED);
    }
    
    /**
     * Get the default endpoint.
     * 
     * @throws UnmarshallingException ...
     */
    @Test
    public void testDefault() throws UnmarshallingException {
        final EntityDescriptor entity = loadMetadata("/net/shibboleth/idp/saml/impl/profile/SPWithEndpoints.xml");
        final SAMLMetadataContext mdCtx = new SAMLMetadataContext();
        mdCtx.setEntityDescriptor(entity);
        mdCtx.setRoleDescriptor(entity.getSPSSODescriptor("required"));
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        omc.getOrCreateSubcontext(SAMLPeerEntityContext.class).addSubcontext(mdCtx);

        final MessageContext imc = prc.getInboundMessageContext();
        assert imc != null;
        final AuthnRequest authnRequest = (AuthnRequest) imc.getMessage();
        assert authnRequest!=null;
        authnRequest.setAssertionConsumerServiceURL(null);
        authnRequest.setProtocolBinding(null);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);

        final SAMLBindingContext bindingCtx = omc.getSubcontext(SAMLBindingContext.class);
        assert bindingCtx!=null;
        Assert.assertNotNull(bindingCtx.getBindingDescriptor());
        Assert.assertEquals(bindingCtx.getRelayState(), RELAY_STATE);
        Assert.assertEquals(bindingCtx.getBindingUri(), SAMLConstants.SAML2_POST_BINDING_URI);
        
        final SAMLPeerEntityContext pec = omc.getSubcontext(SAMLPeerEntityContext.class);
        assert pec!=null;
        final SAMLEndpointContext epCtx = pec.getSubcontext(SAMLEndpointContext.class, false);
        assert epCtx!=null;
        final Endpoint ep =epCtx.getEndpoint();
        assert ep!=null;
        Assert.assertEquals(ep.getBinding(), SAMLConstants.SAML2_POST_BINDING_URI);
        Assert.assertEquals(ep.getLocation(), LOCATION_POST.replace("POST2", "POST"));
    }
    
    /**
     * Test a SAML 1 request (use of SAML2 bindings here is just for simplicity in testing).
     * 
     * @throws UnmarshallingException ...
     */
    @Test
    public void testSAML1InMetadata() throws UnmarshallingException {
        final EntityDescriptor entity = loadMetadata("/net/shibboleth/idp/saml/impl/profile/SPWithEndpoints.xml");
        final SAMLMetadataContext mdCtx = new SAMLMetadataContext();
        mdCtx.setEntityDescriptor(entity);
        mdCtx.setRoleDescriptor(entity.getSPSSODescriptor("required"));
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        omc.getOrCreateSubcontext(SAMLPeerEntityContext.class).addSubcontext(mdCtx);
        
        final IdPInitiatedSSORequest saml1Request = new IdPInitiatedSSORequest("foo", LOCATION_POST, null, null);
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc != null;
        imc.setMessage(saml1Request);
        
        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);
        
        final SAMLBindingContext bindingCtx = omc.getSubcontext(SAMLBindingContext.class);
        assert bindingCtx!=null;
        Assert.assertNotNull(bindingCtx.getBindingDescriptor());
        Assert.assertEquals(bindingCtx.getRelayState(), RELAY_STATE);
        Assert.assertEquals(bindingCtx.getBindingUri(), SAMLConstants.SAML2_POST_BINDING_URI);
        
        final SAMLPeerEntityContext pec = omc.getSubcontext(SAMLPeerEntityContext.class);
        assert pec!=null;
        final SAMLEndpointContext epCtx = pec.getSubcontext(SAMLEndpointContext.class, false);
        assert epCtx!=null;
        final Endpoint ep =epCtx.getEndpoint();
        assert ep!=null;
        Assert.assertEquals(ep.getBinding(), SAMLConstants.SAML2_POST_BINDING_URI);
        Assert.assertEquals(ep.getLocation(), LOCATION_POST);
    }
    
    @Nonnull private EntityDescriptor loadMetadata(@Nonnull @NotEmpty final String path) throws UnmarshallingException {
        
        try {
            final URL url = getClass().getResource(path);
            Document doc = parserPool.parse(new FileInputStream(new File(url.toURI())));
            final Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(doc.getDocumentElement());
            assert unmarshaller!=null;
            return (EntityDescriptor) unmarshaller.unmarshall(doc.getDocumentElement());
        } catch (FileNotFoundException | XMLParserException | URISyntaxException e) {
            throw new UnmarshallingException(e);
        }
    }
    
}
