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

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.component.ComponentInitializationException;

import java.time.Duration;

import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link FilterFlowsByForcedAuthn} unit test. */
@SuppressWarnings("javadoc")
public class FilterFlowsByForceAuthnTest extends BaseAuthenticationContextTest {
    
    private FilterFlowsByForcedAuthn action; 
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        action = new FilterFlowsByForcedAuthn();
        action.initialize();
    }
    
    @Test public void testNonForced() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        authCtx.setForceAuthn(false);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(authCtx.getPotentialFlows().size(), 3);
    }

    @Test public void testNoFiltering() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        authCtx.setForceAuthn(true);
        for (final AuthenticationFlowDescriptor fd : authCtx.getPotentialFlows().values()) {
            fd.setForcedAuthenticationSupported(true);
        }
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(authCtx.getPotentialFlows().size(), 3);
    }

    @Test public void testPartialFiltering() {
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;
        authCtx.setMaxAge(Duration.ofHours(1));
        authCtx.getPotentialFlows().get("test2").setForcedAuthenticationSupported(true);
        
        final Event event = action.execute(src);
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertEquals(authCtx.getPotentialFlows().size(), 1);
        Assert.assertNull(authCtx.getPotentialFlows().get("test1"));
        Assert.assertNotNull(authCtx.getPotentialFlows().get("test2"));
    }
    
}