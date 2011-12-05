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

import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.saml.impl.profile.SamlActionTestingSupport;

import org.opensaml.common.SAMLVersion;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml1.core.Response;
import org.opensaml.saml1.core.Status;
import org.opensaml.saml1.core.StatusCode;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/** {@link AddResponseShell} unit test. */
public class AddResponseShellTest {

    @BeforeSuite()
    public void initOpenSAML() throws InitializationException {
        InitializationService.initialize();
    }
    
    @Test
    public void testAddResponse() throws Exception {
        ProfileRequestContext<Object, Response> profileRequestContext =
                SamlActionTestingSupport.buildProfileRequestContext();
        
        Saml1ActionTestingSupport.buildRelyingPartySubcontext(profileRequestContext, null);

        RequestContext springRequestContext =
                SamlActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        AddResponseShell action = new AddResponseShell();
        action.setId("test");
        action.initialize();
        Event result = action.execute(springRequestContext);
        Assert.assertEquals(result.getId(), ActionSupport.PROCEED_EVENT_ID);

        MessageContext<Response> outMsgCtx = ActionSupport.getRequiredOutboundMessageContext(action, profileRequestContext);
        Response response = outMsgCtx.getMessage();

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getID());
        Assert.assertNotNull(response.getIssueInstant());
        Assert.assertEquals(response.getVersion(), SAMLVersion.VERSION_11);

        Status status = response.getStatus();
        Assert.assertNotNull(status);
        Assert.assertNotNull(status.getStatusCode());
        Assert.assertEquals(status.getStatusCode().getValue(), StatusCode.SUCCESS);
    }

    @Test
    public void testAddResponseWhenResponseAlreadyExist() {
        ProfileRequestContext<Object, Response> profileRequestContext =
                SamlActionTestingSupport.buildProfileRequestContext();

        Response response = Saml1ActionTestingSupport.buildResponse();
        profileRequestContext.getOutboundMessageContext().setMessage(response);

        RequestContext springRequestContext =
                SamlActionTestingSupport.buildMockSpringRequestContext(profileRequestContext);

        AddResponseShell action = new AddResponseShell();
        try{
            action.execute(springRequestContext);
            Assert.fail();
        } catch (Exception e) {
            // expected this
        }
    }
}