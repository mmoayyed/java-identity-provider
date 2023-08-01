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
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.FunctionSupport;

import java.util.List;

import org.opensaml.profile.testing.ActionTestingSupport;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Base class for further action tests. */
final public class PopulateAuthenticationContextTest extends BaseAuthenticationContextTest {

    @BeforeMethod
    public void setUp() throws ComponentInitializationException {
        initializeMembers();
    }

    /**
     * Test available flows == potential flows.
     * 
     * @throws ComponentInitializationException if something goes wrong
     */
    @Test public void testIdentical() throws ComponentInitializationException {
        
        final PopulateAuthenticationContext action = new PopulateAuthenticationContext();
        action.setAvailableFlows(authenticationFlows);
        action.setPotentialFlowsLookupStrategy(FunctionSupport.constant(authenticationFlows));
        action.initialize();

        action.execute(src);
        ActionTestingSupport.assertProceedEvent(prc);
        
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;

        Assert.assertEquals(authCtx.getAvailableFlows().size(), 3);
        Assert.assertNotNull(authCtx.getAvailableFlows().get("test1"));
        Assert.assertNotNull(authCtx.getAvailableFlows().get("test2"));
        Assert.assertNotNull(authCtx.getAvailableFlows().get("test3"));
        
        Assert.assertEquals(authCtx.getPotentialFlows(), authCtx.getAvailableFlows());
    }
    
    /**
     * Test available flows != potential flows.
     * 
     * @throws ComponentInitializationException if something goes wrong
     */
    @Test public void testNonIdentical() throws ComponentInitializationException {
        
        final PopulateAuthenticationContext action = new PopulateAuthenticationContext();
        action.setAvailableFlows(authenticationFlows);
        
        final AuthenticationFlowDescriptor unavailableFlow = new AuthenticationFlowDescriptor();
        unavailableFlow.setId("test4");
        action.setPotentialFlowsLookupStrategy(FunctionSupport.constant(List.of(authenticationFlows.get(0), unavailableFlow)));
        action.initialize();

        action.execute(src);
        ActionTestingSupport.assertProceedEvent(prc);
        
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;

        Assert.assertEquals(authCtx.getAvailableFlows().size(), 3);
        Assert.assertNotNull(authCtx.getAvailableFlows().get("test1"));
        Assert.assertNotNull(authCtx.getAvailableFlows().get("test2"));
        Assert.assertNotNull(authCtx.getAvailableFlows().get("test3"));

        Assert.assertEquals(authCtx.getPotentialFlows().size(), 1);
        Assert.assertNotNull(authCtx.getPotentialFlows().get("test1"));
        Assert.assertNull(authCtx.getPotentialFlows().get("test2"));
        Assert.assertNull(authCtx.getPotentialFlows().get("test3"));
        Assert.assertNull(authCtx.getPotentialFlows().get("test4"));
    }
    
    /**
     * Test active flow filtering.
     * 
     * @throws ComponentInitializationException if something goes wrong
     */
    @Test public void testFiltered() throws ComponentInitializationException {
        
        final PopulateAuthenticationContext action = new PopulateAuthenticationContext();
        action.setAvailableFlows(authenticationFlows);
        action.setPotentialFlowsLookupStrategy(FunctionSupport.constant(authenticationFlows));
        action.setActiveFlowsLookupStrategy(FunctionSupport.constant(CollectionSupport.singletonList("test2")));
        action.initialize();

        action.execute(src);
        ActionTestingSupport.assertProceedEvent(prc);
        
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        assert authCtx != null;

        Assert.assertEquals(authCtx.getAvailableFlows().size(), 3);
        Assert.assertNotNull(authCtx.getAvailableFlows().get("test1"));
        Assert.assertNotNull(authCtx.getAvailableFlows().get("test2"));
        Assert.assertNotNull(authCtx.getAvailableFlows().get("test3"));
        
        Assert.assertEquals(authCtx.getPotentialFlows().size(), 1);
        Assert.assertNull(authCtx.getPotentialFlows().get("test1"));
        Assert.assertNotNull(authCtx.getPotentialFlows().get("test2"));
        Assert.assertNull(authCtx.getPotentialFlows().get("test3"));
    }

}
