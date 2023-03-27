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

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.ext.saml2aslo.Asynchronous;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.shared.component.ComponentInitializationException;

/** {@link PreProcessLogoutMessage} unit test. */
@SuppressWarnings("javadoc")
public class PreProcessLogoutMessageTest extends OpenSAMLInitBaseTestCase {
    
    private RequestContext src;
    
    private ProfileRequestContext prc;
    
    private PreProcessLogoutMessage action;
    
    @BeforeMethod public void setUpAction() throws ComponentInitializationException {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
        
        action = new PreProcessLogoutMessage();
        action.initialize();
    }
    
    @Test public void testNoMessage() {
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_MESSAGE);
    }

    @Test public void testLogoutResponse() {
        //final NameID nameId = SAML2ActionTestingSupport.buildNameID("jdoe");
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildLogoutResponse());
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, PreProcessLogoutMessage.IS_LOGOUT_RESPONSE);
    }
    
    @Test public void testLogoutRequest() {
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildLogoutRequest(null));
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
    }

    @Test public void testAssumeAsyncLogoutRequest() throws ComponentInitializationException {
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildLogoutRequest(null));
        
        action = new PreProcessLogoutMessage();
        action.setAssumeAsynchronousLogout(true);
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, PreProcessLogoutMessage.IS_LOGOUT_REQUEST_ASYNC);
    }

    @Test public void testAsyncLogoutRequest() {
        final MessageContext imc = prc.getInboundMessageContext();
        assert imc!=null;
        imc.setMessage(SAML2ActionTestingSupport.buildLogoutRequest(null));
        
        final SAMLObjectBuilder<Extensions> extsBuilder = (SAMLObjectBuilder<Extensions>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<Extensions>ensureBuilder(
                        Extensions.DEFAULT_ELEMENT_NAME);
        final SAMLObjectBuilder<Asynchronous> asyncBuilder = (SAMLObjectBuilder<Asynchronous>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<Asynchronous>ensureBuilder(
                        Asynchronous.DEFAULT_ELEMENT_NAME);
        final LogoutRequest lr = (LogoutRequest)imc.getMessage();
        assert lr!=null;
        final Extensions exts = extsBuilder.buildObject();
        exts.getUnknownXMLObjects().add(asyncBuilder.buildObject());
        lr.setExtensions(exts);

        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, PreProcessLogoutMessage.IS_LOGOUT_REQUEST_ASYNC);
    }
    
}