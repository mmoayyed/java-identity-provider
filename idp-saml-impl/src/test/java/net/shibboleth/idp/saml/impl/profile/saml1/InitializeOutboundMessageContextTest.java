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

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link InitializeOutboundMessageContext} unit test. */
public class InitializeOutboundMessageContextTest {

    private InitializeOutboundMessageContext action;

    @BeforeMethod public void setUp() {
        action = new InitializeOutboundMessageContext();
    }

    @Test public void testMinimal() throws ProfileException {
        ProfileRequestContext prc = new ProfileRequestContext();

        action.execute(prc);

        ActionTestingSupport.assertEvent(prc, EventIds.INVALID_MSG_CTX);
    }

    // TODO more tests

}
