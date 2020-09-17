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

package net.shibboleth.idp.authn.proxy.impl;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.testing.ActionTestingSupport;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit test for {@link InitializeProxyProfileRequestContext}. */
public class InitializeProxyProfileRequestContextTest {

    private ProfileRequestContext prc;
    
    private InitializeProxyProfileRequestContext action;
    
    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        prc = new RequestContextBuilder().buildProfileRequestContext();
        
        action = new InitializeProxyProfileRequestContext();
        action.setProfileId("nested");
        action.initialize();
    }
    
    @Test public void testFailure() throws Exception {

        action.execute(prc);
        ActionTestingSupport.assertEvent(prc, EventIds.INVALID_PROFILE_CTX);
    }
    
    @Test public void testSuccess() throws Exception {

        prc.addSubcontext(new AuthenticationContext());
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);

        final ProfileRequestContext prc2 =
                prc.getSubcontext(AuthenticationContext.class).getSubcontext(ProfileRequestContext.class);
        Assert.assertNotNull(prc2);
        Assert.assertEquals(prc2.getProfileId(), "nested");
    }

}