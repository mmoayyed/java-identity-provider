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
import java.util.regex.Pattern;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.ConstantSupplier;

/** {@link ValidateRemoteUser} unit test. */
public class ValidateRemoteUserTest extends BaseAuthenticationContextTest {
    
    private ValidateRemoteUser action; 
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        action = new ValidateRemoteUser();
        action.setAllowedUsernames(Arrays.asList("bar", "baz"));
        action.setDeniedUsernames(Arrays.asList("foo"));
        action.setMatchExpression(Pattern.compile("^ba(r|z|n)$"));
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
        action.initialize();
    }

    @Test public void testMissingFlow() {
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
    }
    
    @Test public void testMissingUser() {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testMissingUser2() throws ComponentInitializationException {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        doExtract();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testUnauthorized() throws ComponentInitializationException {
        getMockHttpServletRequest(action).setRemoteUser("bam");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        doExtract();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_CREDENTIALS);
    }

    @Test public void testAuthorized() throws ComponentInitializationException {
        getMockHttpServletRequest(action).setRemoteUser("baz");
        
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        doExtract();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final AuthenticationResult ar = ac.getAuthenticationResult();
        assert ar != null;
        Assert.assertEquals(ar.getSubject().getPrincipals(
                UsernamePrincipal.class).iterator().next().getName(), "baz");
    }

    @Test public void testDenyist() throws ComponentInitializationException {
        getMockHttpServletRequest(action).setRemoteUser("foo");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        doExtract();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_CREDENTIALS);
    }
    
    @Test public void testPattern() throws ComponentInitializationException {
        getMockHttpServletRequest(action).setRemoteUser("ban");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        doExtract();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final AuthenticationResult ar = ac.getAuthenticationResult();
        assert ar != null;
        Assert.assertEquals(ar.getSubject().getPrincipals(
                UsernamePrincipal.class).iterator().next().getName(), "ban");
    }
    
    private void doExtract() throws ComponentInitializationException {
        final ExtractRemoteUser extract = new ExtractRemoteUser();
        extract.setHttpServletRequestSupplier(action.getHttpServletRequestSupplier());
        extract.initialize();
        extract.execute(src);
    }
}
