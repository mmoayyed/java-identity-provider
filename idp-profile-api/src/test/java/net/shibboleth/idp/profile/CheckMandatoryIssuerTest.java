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

package net.shibboleth.idp.profile;

import org.opensaml.messaging.context.BasicMessageContext;
import org.opensaml.messaging.context.BasicMessageMetadataSubcontext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.test.MockRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link CheckMandatoryIssuer}. */
public class CheckMandatoryIssuerTest {

    @Test
    public void testExecute() {
        BasicMessageContext messageContext = new BasicMessageContext();
        BasicMessageMetadataSubcontext messageSubcontext = new BasicMessageMetadataSubcontext(messageContext);
        messageSubcontext.setMessageIssuer("foo");

        ProfileRequestContext requestContext = new ProfileRequestContext();
        requestContext.setInboundMessageContext(messageContext);

        MockRequestContext springContext = new MockRequestContext();
        springContext.getConversationScope().put(ProfileRequestContext.BINDING_KEY, requestContext);

        CheckMandatoryIssuer action = new CheckMandatoryIssuer();

        Event result = action.execute(springContext);
        Assert.assertEquals(result.getSource(), action.getId());
        Assert.assertEquals(ActionSupport.PROCEED_EVENT_ID, result.getId());

        messageSubcontext.setMessageIssuer(null);
        result = action.execute(springContext);
        Assert.assertEquals(result.getSource(), action.getId());
        Assert.assertEquals(ActionSupport.ERROR_EVENT_ID, result.getId());
        Assert.assertNotNull(result.getAttributes().get(ActionSupport.ERROR_MESSAGE_ID));
    }
}