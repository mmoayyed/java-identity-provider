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

import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.saml.impl.profile.SamlActionTestingSupport;
import net.shibboleth.idp.saml.impl.profile.saml1.CheckRequestVersion;
import net.shibboleth.idp.saml.impl.profile.saml1.Saml1ActionTestingSupport;
import net.shibboleth.idp.saml.impl.profile.saml1.CheckRequestVersion.InvalidMessageVersionException;

import org.opensaml.messaging.context.BasicMessageContext;
import org.opensaml.saml1.core.AttributeQuery;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

//TODO convert to SAML 2 authn requests

/** {@link CheckRequestVersion} unit test. */
public class CheckRequestVersionTest {

    /** Test the action errors out properly when there is a null message */
    @Test
    public void testNullMessage() {
        ProfileRequestContext<AttributeQuery, Object> profileRequestContext =
                SamlActionTestingSupport.buildProfileRequestContext();
        RequestContext springRequestContext =
                SamlActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        CheckRequestVersion action = new CheckRequestVersion();
        Event result = action.execute(springRequestContext);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getId(), ActionSupport.ERROR_EVENT_ID);

        Object error = result.getAttributes().get(ActionSupport.ERROR_THROWABLE_ID);
        Assert.assertNotNull(error);
        Assert.assertTrue(CheckRequestVersion.InvalidMessageVersionException.class.isInstance(error));
    }

    /** Test that the action accepts SAML 1.0 and 1.1 messages. */
    @Test
    public void testSaml1Message() {
        ProfileRequestContext<AttributeQuery, Object> profileRequestContext =
                SamlActionTestingSupport.buildProfileRequestContext();
        RequestContext springRequestContext =
                SamlActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        BasicMessageContext<AttributeQuery> inMsgCtx =
                (BasicMessageContext<AttributeQuery>) profileRequestContext.getInboundMessageContext();
        inMsgCtx.setMessage(Saml1ActionTestingSupport.buildAttributeQuery(null));

        CheckRequestVersion action = new CheckRequestVersion();
        Event result = action.execute(springRequestContext);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getId(), ActionSupport.PROCEED_EVENT_ID);
    }

    /** Test that the action errors out on SAML 2 messages. */
    @Test
    public void testSaml2Message() {
        ProfileRequestContext<AttributeQuery, Object> profileRequestContext =
                SamlActionTestingSupport.buildProfileRequestContext();
        RequestContext springRequestContext =
                SamlActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        BasicMessageContext<AttributeQuery> inMsgCtx =
                (BasicMessageContext<AttributeQuery>) profileRequestContext.getInboundMessageContext();
        inMsgCtx.setMessage(Saml1ActionTestingSupport.buildAttributeQuery(null));

        CheckRequestVersion action = new CheckRequestVersion();
        Event result = action.execute(springRequestContext);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getId(), ActionSupport.ERROR_EVENT_ID);

        Object error = result.getAttributes().get(ActionSupport.ERROR_THROWABLE_ID);
        Assert.assertNotNull(error);
        Assert.assertTrue(CheckRequestVersion.InvalidMessageVersionException.class.isInstance(error));
    }
}