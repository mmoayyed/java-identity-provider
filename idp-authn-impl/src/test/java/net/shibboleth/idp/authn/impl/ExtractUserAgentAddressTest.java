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

package net.shibboleth.idp.authn.impl;


import java.net.InetAddress;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UserAgentContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.ConstantSupplier;

/** {@link ExtractUserAgentAddress} unit test. */
public class ExtractUserAgentAddressTest extends BaseAuthenticationContextTest {
    
    private ExtractUserAgentAddress action;
    
    public Object nullObj;
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        action = new ExtractUserAgentAddress();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
        action.initialize();
    }
    
    @SuppressWarnings("null")
    @Test public void testMissingAddress() {
        getMockHttpServletRequest(action).setRemoteAddr((String) nullObj);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testValidAddress() {
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx!=null;
        UserAgentContext uaCtx = authCtx.getSubcontext(UserAgentContext.class);
        assert uaCtx!=null;
        InetAddress addr = uaCtx.getAddress();
        assert addr !=null;
        Assert.assertEquals(addr.getHostAddress(), MockHttpServletRequest.DEFAULT_REMOTE_ADDR);
    }

    @Test public void testInvalidAddress() {
        getMockHttpServletRequest(action).setRemoteAddr("zorkmids");
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }
}