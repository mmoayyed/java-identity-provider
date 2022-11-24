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

package net.shibboleth.idp.authn.impl;


import java.util.Arrays;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.impl.testing.BaseAuthenticationContextTest;
import net.shibboleth.idp.profile.testing.ActionTestingSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.NonnullSupplier;

/** {@link PreserveAuthenticationFlowState} unit test. */
public class PreserveAuthenticationFlowStateTest extends BaseAuthenticationContextTest {
    
    private PreserveAuthenticationFlowState action; 
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        super.setUp();
        
        action = new PreserveAuthenticationFlowState();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new NonnullSupplier<> () {public HttpServletRequest get() { return request;}});
        action.setParameterNames(Arrays.asList("foo", "foo2"));
        action.initialize();
    }
    
    @Test public void testNoServlet() throws ComponentInitializationException {
        action = new PreserveAuthenticationFlowState();
        action.initialize();
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertTrue(prc.getSubcontext(AuthenticationContext.class).getAuthenticationStateMap().isEmpty());
    }

    @Test public void testNoParameters() throws ComponentInitializationException {
        action = new PreserveAuthenticationFlowState();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        action.setHttpServletRequestSupplier(new NonnullSupplier<> () {public HttpServletRequest get() { return request;}});
        action.initialize();
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertTrue(prc.getSubcontext(AuthenticationContext.class).getAuthenticationStateMap().isEmpty());
    }

    @Test public void testNoneFound(){
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        Assert.assertTrue(prc.getSubcontext(AuthenticationContext.class).getAuthenticationStateMap().isEmpty());
    }
    
    @Test public void testNoValues() {
        
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("foo", (String) null);
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        Assert.assertEquals(authCtx.getAuthenticationStateMap().size(), 1);
        Assert.assertTrue(authCtx.getAuthenticationStateMap().containsKey("foo"));
        Assert.assertNull(authCtx.getAuthenticationStateMap().get("foo"));
    }
    
    @Test public void testSingleValued() {
        
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("foo", "bar");
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("foo2", "bar2");
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        Assert.assertEquals(authCtx.getAuthenticationStateMap().size(), 2);
        Assert.assertEquals(authCtx.getAuthenticationStateMap().get("foo"), "bar");
        Assert.assertEquals(authCtx.getAuthenticationStateMap().get("foo2"), "bar2");
    }
    
    @Test public void testMultiValued() {
        
        ((MockHttpServletRequest) action.getHttpServletRequest()).addParameter("foo", new String[]{"bar", "bar2"});
        
        final Event event = action.execute(src);
        
        ActionTestingSupport.assertProceedEvent(event);
        
        final AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class);
        Assert.assertEquals(authCtx.getAuthenticationStateMap().size(), 1);
        Assert.assertEquals(authCtx.getAuthenticationStateMap().get("foo"), Arrays.asList("bar", "bar2"));
    }
    
}