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

package net.shibboleth.idp.saml.impl.profile.saml1;

import java.security.KeyPair;

import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.saml.profile.SAMLEventIds;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml1.core.Assertion;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.saml.saml1.profile.SAML1ActionTestingSupport;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.crypto.KeySupport;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.test.MockRequestContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link SignAssertions} unit test. */
public class SignAssertionsTest extends OpenSAMLInitBaseTestCase {

    private SignAssertions action;

    private RequestContext src;

    private ProfileRequestContext prc;

    @BeforeMethod public void setUp() throws ComponentInitializationException {

        action = new SignAssertions();
        action.setId("test");
        action.initialize();

        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);
    }

    @Test public void testNoProfileRequestContext() throws Exception {
        src = new MockRequestContext();
        Event result = action.execute(src);
        ActionTestingSupport.assertEvent(result, EventIds.INVALID_PROFILE_CTX);
    }

    @Test public void testNoOutboundMessageContext() throws Exception {
        src.getConversationScope().put(ProfileRequestContext.BINDING_KEY, new ProfileRequestContext());
        Event result = action.execute(src);
        ActionTestingSupport.assertEvent(result, EventIds.INVALID_MSG_CTX);
    }

    @Test public void testNoSecurityParametersContext() throws Exception {
        Event result = action.execute(src);
        ActionTestingSupport.assertProceedEvent(result);
    }

    @Test public void testNoSignatureSigningParameters() throws Exception {
        prc.getOutboundMessageContext().addSubcontext(new SecurityParametersContext());
        Event result = action.execute(src);
        ActionTestingSupport.assertProceedEvent(result);
    }

    @Test public void testNoResponse() throws Exception {
        SecurityParametersContext secParamCtx = new SecurityParametersContext();
        secParamCtx.setSignatureSigningParameters(new SignatureSigningParameters());
        prc.getOutboundMessageContext().addSubcontext(secParamCtx);

        Event result = action.execute(src);
        ActionTestingSupport.assertEvent(result, SAMLEventIds.NO_RESPONSE);
    }

    @Test public void testNoAssertions() throws Exception {
        Response response = SAML1ActionTestingSupport.buildResponse();
        src = new RequestContextBuilder().setOutboundMessage(response).buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);

        SecurityParametersContext secParamCtx = new SecurityParametersContext();
        secParamCtx.setSignatureSigningParameters(new SignatureSigningParameters());
        prc.getOutboundMessageContext().addSubcontext(secParamCtx);

        Event result = action.execute(src);
        ActionTestingSupport.assertEvent(result, SAMLEventIds.NO_ASSERTION);
    }

    @Test public void testSignAssertions() throws Exception {
        Assertion assertion = SAML1ActionTestingSupport.buildAssertion();
        Response response = SAML1ActionTestingSupport.buildResponse();
        response.getAssertions().add(assertion);
        src = new RequestContextBuilder().setOutboundMessage(response).buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);

        SignatureSigningParameters signingParameters = new SignatureSigningParameters();
        KeyPair kp = KeySupport.generateKeyPair("RSA", 1024, null);
        signingParameters.setSigningCredential(CredentialSupport.getSimpleCredential(kp.getPublic(), kp.getPrivate()));

        SecurityParametersContext secParamCtx = new SecurityParametersContext();
        secParamCtx.setSignatureSigningParameters(signingParameters);
        prc.getOutboundMessageContext().addSubcontext(secParamCtx);

        Event result = action.execute(src);

        ActionTestingSupport.assertProceedEvent(result);
    }

    // TODO Test that assertion was signed correctly ?

    // TODO Test event id when signing throws an exception.
}
