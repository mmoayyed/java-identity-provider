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

import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.saml.binding.BindingDescriptor;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml1.core.Request;
import org.opensaml.saml.saml1.testing.SAML1ActionTestingSupport;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link InitializeOutboundMessageContextForError} unit test. */
@SuppressWarnings("javadoc")
public class InitializeOutboundMessageContextForErrorTest extends OpenSAMLInitBaseTestCase {

    private RequestContext src;
    
    private ProfileRequestContext prc;
    
    private Request attributeQuery;
    
    private InitializeOutboundMessageContextForError action;

    @BeforeMethod public void setUp() throws ComponentInitializationException {
        attributeQuery = SAML1ActionTestingSupport.buildAttributeQueryRequest(
                SAML1ActionTestingSupport.buildSubject("jdoe"));
        src = new RequestContextBuilder().setInboundMessage(attributeQuery).buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        prc.setOutboundMessageContext(null);
        action = new InitializeOutboundMessageContextForError();
        final BindingDescriptor bd = new BindingDescriptor();
        bd.setId(SAMLConstants.SAML1_SOAP11_BINDING_URI);
        bd.initialize();
        action.setOutboundBinding(bd);
        action.initialize();
    }
    
    @Test(expectedExceptions=ComponentInitializationException.class)
    public void testBadConfig() throws ComponentInitializationException {
        final InitializeOutboundMessageContextForError a = new InitializeOutboundMessageContextForError();
        a.initialize();
    }

    @Test public void testWithOutboundContext() {
        prc.setOutboundMessageContext(new MessageContext());
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }
    
    @Test public void testNoRelyingPartyContext() {
        prc.removeSubcontext(RelyingPartyContext.class);

        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        final SAMLBindingContext bindingCtx = omc.getSubcontext(SAMLBindingContext.class); 
        assert bindingCtx!=null;
        Assert.assertEquals(bindingCtx.getBindingUri(), SAMLConstants.SAML1_SOAP11_BINDING_URI);
        Assert.assertNull(omc.getSubcontext(SAMLPeerEntityContext.class));
    }

    @Test public void testNoPeerEntityContext() {
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        
        final SAMLBindingContext bindingCtx = omc.getSubcontext(SAMLBindingContext.class); 
        assert bindingCtx!=null;
        Assert.assertEquals(bindingCtx.getBindingUri(), SAMLConstants.SAML1_SOAP11_BINDING_URI);
        Assert.assertNull(omc.getSubcontext(SAMLPeerEntityContext.class));
    }

    @Test public void testPeerEntityContextNoIssuer() {
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        SAMLPeerEntityContext ctx = imc.ensureSubcontext(SAMLPeerEntityContext.class);
        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx!=null;
        rpCtx.setRelyingPartyIdContextTree(ctx);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        final SAMLBindingContext bindingCtx = omc.getSubcontext(SAMLBindingContext.class); 
        assert bindingCtx!=null;
        Assert.assertEquals(bindingCtx.getBindingUri(), SAMLConstants.SAML1_SOAP11_BINDING_URI);
        ctx = omc.getSubcontext(SAMLPeerEntityContext.class);
        assert ctx!=null;
        Assert.assertNull(ctx.getEntityId());
    }

    @Test public void testPeerEntityContextIssuer() {
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        SAMLPeerEntityContext ctx = imc.ensureSubcontext(SAMLPeerEntityContext.class);
        final RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        assert rpCtx!=null;
        rpCtx.setRelyingPartyIdContextTree(ctx);
        Constraint.isNotNull(attributeQuery.getAttributeQuery(), "Query was null").setResource("issuer");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertNotNull(prc.getOutboundMessageContext());
        final MessageContext omc = prc.getOutboundMessageContext();
        assert omc!=null;
        final SAMLBindingContext bindingCtx = omc.getSubcontext(SAMLBindingContext.class); 
        assert bindingCtx!=null;
        Assert.assertEquals(bindingCtx.getBindingUri(), SAMLConstants.SAML1_SOAP11_BINDING_URI);
        ctx = omc.getSubcontext(SAMLPeerEntityContext.class);
        assert ctx!=null;
        Assert.assertEquals(ctx.getEntityId(), "issuer");
    }

}