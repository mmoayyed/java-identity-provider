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
import net.shibboleth.idp.profile.InvalidInboundMessageContextException;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.saml.impl.profile.saml2.CheckRequestVersion.InvalidMessageVersionException;

import org.opensaml.common.SAMLVersion;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.saml2.core.AttributeQuery;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/** {@link CheckRequestVersion} unit test. */
public class CheckRequestVersionTest {

    @BeforeSuite()
    public void initOpenSAML() throws InitializationException {
        InitializationService.initialize();
    }

    /** Test the action errors out properly when there is a null message */
    @Test
    public void testNullMessage() throws Exception {
        CheckRequestVersion action = new CheckRequestVersion();
        action.setId("test");
        action.initialize();

        try {
            action.execute(new RequestContextBuilder().buildRequestContext());
            Assert.fail();
        } catch (InvalidInboundMessageContextException e) {
            // expected this
        }
    }

    /** Test that the action accepts SAML 1.0 and 1.1 messages. */
    @Test
    public void testSaml1Message() throws Exception {
        AttributeQuery request = Saml2ActionTestingSupport.buildAttributeQueryRequest(null);
        request.setVersion(SAMLVersion.VERSION_11);

        RequestContext springRequestContext =
                new RequestContextBuilder().setInboundMessage(request)
                        .setRelyingPartyProfileConfigurations(Saml2ActionTestingSupport.buildProfileConfigurations())
                        .buildRequestContext();

        CheckRequestVersion action = new CheckRequestVersion();
        action.setId("test");
        action.initialize();

        try {
            action.execute(springRequestContext);
            Assert.fail();
        } catch (InvalidMessageVersionException e) {
            // expected this
        }
    }

    /** Test that the action errors out on SAML 2 messages. */
    @Test
    public void testSaml2Message() throws Exception {
        RequestContext springRequestContext =
                new RequestContextBuilder()
                        .setInboundMessage(Saml2ActionTestingSupport.buildAttributeQueryRequest(null))
                        .setRelyingPartyProfileConfigurations(Saml2ActionTestingSupport.buildProfileConfigurations())
                        .buildRequestContext();

        CheckRequestVersion action = new CheckRequestVersion();
        action.setId("test");
        action.initialize();

        Event result = action.execute(springRequestContext);

        ActionTestingSupport.assertProceedEvent(result);
    }
}