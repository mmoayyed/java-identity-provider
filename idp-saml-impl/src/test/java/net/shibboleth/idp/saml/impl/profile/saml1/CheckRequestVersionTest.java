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
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.saml.profile.SAMLEventIds;
import net.shibboleth.idp.saml.profile.saml1.SAML1ActionTestingSupport;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml1.core.Request;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.testng.annotations.Test;

/** {@link CheckRequestVersion} unit test. */
public class CheckRequestVersionTest extends OpenSAMLInitBaseTestCase {

    /** Test the action errors out properly when there is a null message */
    @Test public void testNullMessage() throws Exception {
        // TODO
    }

    /** Test that the action accepts SAML 1.0 and 1.1 messages. */
    @Test public void testSaml1Message() throws Exception {
        RequestContext springRequestContext =
                new RequestContextBuilder()
                        .setInboundMessage(SAML1ActionTestingSupport.buildAttributeQueryRequest(null))
                        .setRelyingPartyProfileConfigurations(SAML1ActionTestingSupport.buildProfileConfigurations())
                        .buildRequestContext();

        CheckRequestVersion action = new CheckRequestVersion();
        action.setId("test");
        action.initialize();

        Event result = action.execute(springRequestContext);

        ActionTestingSupport.assertProceedEvent(result);
    }

    /** Test that the action errors out on SAML 2 messages. */
    @Test public void testSaml2Message() throws Exception {
        Request request = SAML1ActionTestingSupport.buildAttributeQueryRequest(null);
        request.setVersion(SAMLVersion.VERSION_20);

        RequestContext springRequestContext =
                new RequestContextBuilder().setInboundMessage(request)
                        .setRelyingPartyProfileConfigurations(SAML1ActionTestingSupport.buildProfileConfigurations())
                        .buildRequestContext();

        CheckRequestVersion action = new CheckRequestVersion();
        action.setId("test");
        action.initialize();

        Event result = action.execute(springRequestContext);
        
        ActionTestingSupport.assertEvent(result, SAMLEventIds.INVALID_MESSAGE_VERSION);
    }
}