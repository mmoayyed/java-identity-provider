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

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.authn.principal.impl.ExactPrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.testing.TestPrincipal;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.net.IPRange;
import net.shibboleth.shared.testing.ConstantSupplier;

/** {@link ValidateUserAgentAddress} unit test. */
@SuppressWarnings("javadoc")
public class ValidateUserAgentAddressTest extends BaseAuthenticationContextTest {
    
    private ValidateUserAgentAddress action; 
    
    private Object nullObj;
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        action = new ValidateUserAgentAddress();
        action.setMappings(CollectionSupport.singletonMap("foo",
                CollectionSupport.singletonList(IPRange.parseCIDRBlock("192.168.1.0/24"))));
        action.setSupportedPrincipals(CollectionSupport.singletonList(new TestPrincipal("UserAgentAuthentication")));
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new ConstantSupplier<>(request));
        action.initialize();
    }

    @Test public void testMissingFlow() {
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_AUTHN_CTX);
    }
    
    @Test public void testMissingAddress() {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @SuppressWarnings("null")
    @Test public void testMissingAddress2() throws ComponentInitializationException {
        getMockHttpServletRequest(action).setRemoteAddr((String) nullObj);

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        doExtract();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }

    @Test public void testUnauthorized() throws ComponentInitializationException {
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        doExtract();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.INVALID_CREDENTIALS);
    }

    @Test public void testIncompatible() throws ComponentInitializationException {
        getMockHttpServletRequest(action).setRemoteAddr("192.168.1.1");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(CollectionSupport.singletonList(new TestPrincipal("PasswordAuthentication")));
        ac.addSubcontext(rpc, true);
        
        doExtract();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.REQUEST_UNSUPPORTED);
    }

    @Test public void testCompatible() throws ComponentInitializationException {
        getMockHttpServletRequest(action).setRemoteAddr("192.168.1.1");

        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class);
        assert ac != null;
        ac.setAttemptedFlow(authenticationFlows.get(0));
        
        final RequestedPrincipalContext rpc = new RequestedPrincipalContext();
        rpc.getPrincipalEvalPredicateFactoryRegistry().register(
                TestPrincipal.class, "exact", new ExactPrincipalEvalPredicateFactory());
        rpc.setOperator("exact");
        rpc.setRequestedPrincipals(CollectionSupport.singletonList(new TestPrincipal("UserAgentAuthentication")));
        ac.addSubcontext(rpc, true);
        
        doExtract();
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        final AuthenticationResult ar = ac.getAuthenticationResult();
        assert ar != null;
        Assert.assertEquals(ar.getSubject().getPrincipals(
                UsernamePrincipal.class).iterator().next().getName(), "foo");
    }
    
    private void doExtract() throws ComponentInitializationException {
        final ExtractUserAgentAddress extract = new ExtractUserAgentAddress();
        extract.setHttpServletRequestSupplier(action.getHttpServletRequestSupplier());
        extract.initialize();
        extract.execute(src);
    }
    
}