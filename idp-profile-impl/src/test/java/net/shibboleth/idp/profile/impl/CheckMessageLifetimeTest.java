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

import java.util.concurrent.TimeUnit;

import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link CheckMessageLifetime}. */
public class CheckMessageLifetimeTest {

    @Test public void testMessageLifetime() throws Exception {
        CheckMessageLifetime action = new CheckMessageLifetime();
        action.setId("mock");

        Assert.assertEquals(action.getMessageLifetime(), TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES));

        action.setMessageLifetime(500);
        Assert.assertEquals(action.getMessageLifetime(), 500);

        try {
            action.initialize();
            action.setMessageLifetime(100);
            Assert.fail();
        } catch (UnmodifiableComponentException e) {
            Assert.assertEquals(action.getMessageLifetime(), 500);
        }
    }

    @Test public void testValidMessage() throws Exception {
        CheckMessageLifetime action = new CheckMessageLifetime();
        action.setId("mock");
        action.initialize();

        Event result =
                action.doExecute(null, null,
                        new RequestContextBuilder().setInboundMessageId(ActionTestingSupport.INBOUND_MSG_ID)
                                .setInboundMessageIssuer(ActionTestingSupport.INBOUND_MSG_ISSUER)
                                .setInboundMessageIssueInstant(System.currentTimeMillis()).buildProfileRequestContext());
        ActionTestingSupport.assertProceedEvent(result);
    }

    @Test public void testFutureMessage() throws Exception {
        CheckMessageLifetime action = new CheckMessageLifetime();
        action.setId("mock");
        action.initialize();

        Event result =
                action.doExecute(null, null,
                        new RequestContextBuilder().setInboundMessageId(ActionTestingSupport.INBOUND_MSG_ID)
                                .setInboundMessageIssuer(ActionTestingSupport.INBOUND_MSG_ISSUER)
                                .setInboundMessageIssueInstant(System.currentTimeMillis() + 1000000)
                                .buildProfileRequestContext());

        ActionTestingSupport.assertEvent(result, CheckMessageLifetime.FUTURE_ISSUE_INSTANT);
    }

    @Test public void testPastMessage() throws Exception {
        CheckMessageLifetime action = new CheckMessageLifetime();
        action.setId("mock");
        action.initialize();

        Event result =
                action.doExecute(null, null,
                        new RequestContextBuilder().setInboundMessageId(ActionTestingSupport.INBOUND_MSG_ID)
                                .setInboundMessageIssuer(ActionTestingSupport.INBOUND_MSG_ISSUER)
                                .setInboundMessageIssueInstant(System.currentTimeMillis() - 1000000)
                                .buildProfileRequestContext());

        ActionTestingSupport.assertEvent(result, CheckMessageLifetime.PAST_ISSUE_INSTANT);
    }
}