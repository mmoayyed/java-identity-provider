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

import java.util.Collections;

import net.shibboleth.idp.profile.IdPEventIds;

import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.impl.SelectProfileConfiguration;
import net.shibboleth.idp.relyingparty.MockProfileConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;

import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link SelectProfileConfiguration} unit test. */
public class SelectProfileConfigurationTest {

    /** Test that the action errors out properly if there is no relying party context. */
    @Test public void testNoRelyingPartyContext() throws Exception {
        ProfileRequestContext profileCtx = new ProfileRequestContext();

        SelectProfileConfiguration action = new SelectProfileConfiguration();
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);

        ActionTestingSupport.assertEvent(profileCtx, IdPEventIds.INVALID_RELYING_PARTY_CTX);
    }

    /** Test that the action errors out properly if there is no relying party configuration. */
    @Test public void testNoRelyingPartyConfiguration() throws Exception {
        ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        profileCtx.getSubcontext(RelyingPartyContext.class).setRelyingPartyConfiguration(null);

        SelectProfileConfiguration action = new SelectProfileConfiguration();
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);

        ActionTestingSupport.assertEvent(profileCtx, IdPEventIds.INVALID_RELYING_PARTY_CONFIG);
    }

    /** Test that the action errors out properly if the desired profile configuration is not configured. */
    @Test public void testInvalidProfileConfiguration() throws Exception {
        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setRelyingPartyProfileConfigurations(
                        Collections.<ProfileConfiguration>singleton(new MockProfileConfiguration("mock"))
                            ).buildProfileRequestContext();

        SelectProfileConfiguration action = new SelectProfileConfiguration();
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);

        ActionTestingSupport.assertEvent(profileCtx, IdPEventIds.INVALID_PROFILE_CONFIG);
    }

    /** Test that the action selects the appropriate profile configuration and proceeds properly. */
    @Test public void testSelectProfileConfiguration() throws Exception {
        ProfileRequestContext profileCtx =
                new RequestContextBuilder().setRelyingPartyProfileConfigurations(
                        Collections.<ProfileConfiguration>singleton(new MockProfileConfiguration("mock"))
                            ).buildProfileRequestContext();

        profileCtx.setProfileId("mock");

        SelectProfileConfiguration action = new SelectProfileConfiguration();
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);

        ActionTestingSupport.assertProceedEvent(profileCtx);

        Assert.assertNotNull(profileCtx.getSubcontext(RelyingPartyContext.class).getProfileConfig());
        Assert.assertEquals(profileCtx.getSubcontext(RelyingPartyContext.class).getProfileConfig().getId(), "mock");
    }
    
}