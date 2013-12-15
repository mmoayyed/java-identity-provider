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

import net.shibboleth.idp.profile.ActionTestingSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.saml.profile.SAMLEventIds;
import net.shibboleth.idp.saml.profile.saml1.Saml1ActionTestingSupport;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.saml.saml1.core.Status;
import org.opensaml.saml.saml1.core.StatusCode;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link AddResponseShell} unit test. */
public class AddResponseShellTest extends OpenSAMLInitBaseTestCase {

    @Test public void testAddResponse() throws Exception {
        RequestContext springRequestContext =
                new RequestContextBuilder().setRelyingPartyProfileConfigurations(
                        Saml1ActionTestingSupport.buildProfileConfigurations()).buildRequestContext();

        AddResponseShell action = new AddResponseShell();
        action.setId("test");
        action.initialize();
        Event result = action.execute(springRequestContext);
        ActionTestingSupport.assertProceedEvent(result);

        ProfileRequestContext<Object, Response> profileRequestContext =
                (ProfileRequestContext<Object, Response>) springRequestContext.getConversationScope().get(
                        ProfileRequestContext.BINDING_KEY);
        MessageContext<Response> outMsgCtx = profileRequestContext.getOutboundMessageContext();
        Response response = outMsgCtx.getMessage();

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getID());
        Assert.assertNotNull(response.getIssueInstant());
        Assert.assertEquals(response.getVersion(), SAMLVersion.VERSION_11);

        Status status = response.getStatus();
        Assert.assertNotNull(status);
        Assert.assertNotNull(status.getStatusCode());
        Assert.assertEquals(status.getStatusCode().getValue(), StatusCode.SUCCESS);

        BasicMessageMetadataContext messageMetadata = outMsgCtx.getSubcontext(BasicMessageMetadataContext.class, false);
        Assert.assertNotNull(messageMetadata);
        Assert.assertEquals(messageMetadata.getMessageId(), response.getID());
        Assert.assertEquals(messageMetadata.getMessageIssueInstant(), response.getIssueInstant().getMillis());
    }

    @Test public void testAddResponseWhenResponseAlreadyExist() throws Exception {
        RequestContext springRequestContext =
                new RequestContextBuilder().setOutboundMessage(Saml1ActionTestingSupport.buildResponse())
                        .setRelyingPartyProfileConfigurations(Saml1ActionTestingSupport.buildProfileConfigurations())
                        .buildRequestContext();

        AddResponseShell action = new AddResponseShell();
        action.setId("test");
        action.initialize();

        Event result = action.execute(springRequestContext);

        ActionTestingSupport.assertEvent(result, SAMLEventIds.RESPONSE_EXISTS);
    }
}