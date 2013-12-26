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

package net.shibboleth.idp.profile.impl;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.RequestContextBuilder;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml.saml2.core.impl.LogoutRequestBuilder;
import org.springframework.util.Assert;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.test.MockRequestContext;
import org.testng.annotations.Test;

/** {@link CreateAuthenticationContext} unit test. */
public class CreateAuthenticationContextTest {

    /** Test that the action errors out properly if there is no inbound message context. */
    @Test public void testNoInboundMessageContext() throws Exception {
        ProfileRequestContext profileCtx = new ProfileRequestContext();

        CreateAuthenticationContext action = new CreateAuthenticationContext();
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertEvent(result, EventIds.INVALID_MSG_CTX);
    }

    /** Test that the action errors out properly if there is no inbound message. */
    @Test public void testNoInboundMessage() throws Exception {
        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setInboundMessage(null).buildProfileRequestContext();

        CreateAuthenticationContext action = new CreateAuthenticationContext();
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertEvent(result, EventIds.INVALID_MSG_CTX);
    }

    /** Test that the action errors out properly if the inbound message is not a SAML 2 AuthnRequest. */
    @Test public void testNoSAML2AuthnRequest() throws Exception {
        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setInboundMessage(new LogoutRequestBuilder().buildObject())
                        .buildProfileRequestContext();

        CreateAuthenticationContext action = new CreateAuthenticationContext();
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertEvent(result, EventIds.INVALID_MSG_CTX);
    }

    /** Test that the action proceeds properly if the inbound message is a SAML2 AuthnRequest. */
    @Test public void testCreateAuthenticationContext() throws Exception {
        AuthnRequest authnRequest = new AuthnRequestBuilder().buildObject();
        authnRequest.setIsPassive(true);
        authnRequest.setForceAuthn(true);

        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setInboundMessage(authnRequest).buildProfileRequestContext();

        CreateAuthenticationContext action = new CreateAuthenticationContext();
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertProceedEvent(result);
        AuthenticationContext authnCtx = profileCtx.getSubcontext(AuthenticationContext.class);
        Assert.notNull(authnCtx);
        Assert.isTrue(authnCtx.isForceAuthn());
        Assert.isTrue(authnCtx.isPassive());

        // TODO test other properties
    }
}
