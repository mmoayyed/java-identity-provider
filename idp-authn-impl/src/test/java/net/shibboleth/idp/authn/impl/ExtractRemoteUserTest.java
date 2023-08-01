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


import java.util.Arrays;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernameContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.ConstantSupplier;

/** {@link ExtractRemoteUser} unit test. */
public class ExtractRemoteUserTest extends BaseAuthenticationContextTest {
    
    private ExtractRemoteUser action; 
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        action = new ExtractRemoteUser();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
    }

    @Test public void testNoConfig() {
        try {
            action.setCheckRemoteUser(false);
            action.initialize();
            Assert.fail();
        } catch (ComponentInitializationException e) {
            
        }
    }
    
    @Test public void testNoServlet() throws ComponentInitializationException {
        action.setHttpServletRequestSupplier(null);
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testMissingIdentity() throws ComponentInitializationException {
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testRemoteUser() throws ComponentInitializationException {
        getMockHttpServletRequest(action).setRemoteUser("foo");
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        final UsernameContext unCtx = authCtx.getSubcontext(UsernameContext.class);
        assert unCtx != null;
        Assert.assertEquals(unCtx.getUsername(), "foo");
    }

    @Test public void testAttribute() throws ComponentInitializationException {
        getMockHttpServletRequest(action).setAttribute("Username", "foo");
        action.setCheckAttributes(Arrays.asList("Username"));
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        final UsernameContext unCtx = authCtx.getSubcontext(UsernameContext.class);
        assert unCtx != null;
        Assert.assertEquals(unCtx.getUsername(), "foo");
    }

    @Test public void testHeader() throws ComponentInitializationException {
        getMockHttpServletRequest(action).addHeader("X-Username", "foo");
        action.setCheckAttributes(Arrays.asList("Username"));
        action.setCheckHeaders(Arrays.asList("X-Username"));
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        UsernameContext unCtx = authCtx.getSubcontext(UsernameContext.class);
        assert unCtx != null;
        Assert.assertEquals(unCtx.getUsername(), "foo");
    }

    @Test public void testTransforms() throws ComponentInitializationException {
        getMockHttpServletRequest(action).setRemoteUser(" Foo@osu.edu ");
        action.setTrim(true);
        action.setTransforms(Arrays.asList(new Pair<>("^(.+)@osu\\.edu$", "$1")));
        action.setLowercase(true);
        action.initialize();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        final UsernameContext unCtx = authCtx.getSubcontext(UsernameContext.class);
        assert unCtx != null;
        Assert.assertEquals(unCtx.getUsername(), "foo");
    }
}