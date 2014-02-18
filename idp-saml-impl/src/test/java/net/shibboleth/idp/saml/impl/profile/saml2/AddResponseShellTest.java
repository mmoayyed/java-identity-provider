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

package net.shibboleth.idp.saml.impl.profile.saml2;

import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.saml.profile.saml2.SAML2ActionTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link AddResponseShell} unit test. */
public class AddResponseShellTest extends OpenSAMLInitBaseTestCase {

    private AddResponseShell action;

    @BeforeMethod public void setUp() throws ComponentInitializationException {
        action = new AddResponseShell();
        action.setId("test");
        action.initialize();
    }

    @Test public void testAddResponse() throws ProfileException, ComponentInitializationException {
        final RequestContext rc =
                new RequestContextBuilder().setRelyingPartyProfileConfigurations(
                        SAML2ActionTestingSupport.buildProfileConfigurations()).buildRequestContext();
        final ProfileRequestContext prc = new WebflowRequestContextProfileRequestContextLookup().apply(rc);

        final Event event = action.execute(rc);
        ActionTestingSupport.assertProceedEvent(event);

        final MessageContext<Response> outMsgCtx = prc.getOutboundMessageContext();
        final Response response = outMsgCtx.getMessage();

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getID());
        Assert.assertNotNull(response.getIssueInstant());
        Assert.assertEquals(response.getVersion(), SAMLVersion.VERSION_20);

        Assert.assertNotNull(response.getIssuer());
        Assert.assertEquals(response.getIssuer().getValue(), ActionTestingSupport.OUTBOUND_MSG_ISSUER);

        final Status status = response.getStatus();
        Assert.assertNotNull(status);
        Assert.assertNotNull(status.getStatusCode());
        Assert.assertEquals(status.getStatusCode().getValue(), StatusCode.SUCCESS_URI);

        final BasicMessageMetadataContext messageMetadata =
                outMsgCtx.getSubcontext(BasicMessageMetadataContext.class, false);
        Assert.assertNotNull(messageMetadata);
        Assert.assertEquals(messageMetadata.getMessageId(), response.getID());
        Assert.assertEquals(messageMetadata.getMessageIssueInstant(), response.getIssueInstant().getMillis());
    }

    @Test public void testAddResponseWhenResponseAlreadyExist() throws ProfileException,
            ComponentInitializationException {
        final RequestContext rc =
                new RequestContextBuilder().setOutboundMessage(SAML2ActionTestingSupport.buildResponse())
                        .setRelyingPartyProfileConfigurations(SAML2ActionTestingSupport.buildProfileConfigurations())
                        .buildRequestContext();

        final Event event = action.execute(rc);
        ActionTestingSupport.assertEvent(event, EventIds.INVALID_MSG_CTX);
    }

}