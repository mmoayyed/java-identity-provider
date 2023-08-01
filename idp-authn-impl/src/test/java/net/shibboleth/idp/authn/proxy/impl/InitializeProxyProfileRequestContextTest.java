/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.shared.component.ComponentInitializationException;

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
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        final ProfileRequestContext prc2 =
                ac.getSubcontext(ProfileRequestContext.class);
        assert prc2 != null;;
        Assert.assertEquals(prc2.getProfileId(), "nested");
    }

}