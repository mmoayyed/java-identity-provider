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

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.context.AuthenticationContext;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionTestingSupport;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link FilterFlowsByForcedAuthn} unit test. */
public class FilterFlowsByForceAuthnTest extends InitializeAuthenticationContextTest {
    
    private FilterFlowsByForcedAuthn action; 
    
    @BeforeMethod public void setUp() throws Exception {
        super.setUp();
        
        action = new FilterFlowsByForcedAuthn();
        action.initialize();
    }
    
    @Test public void testNonForced() throws Exception {
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        authCtx.setForceAuthn(false);
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertEquals(authCtx.getPotentialFlows().size(), 3);
    }

    @Test public void testNoFiltering() throws ProfileException {
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        authCtx.setForceAuthn(true);
        for (AuthenticationFlowDescriptor fd : authCtx.getPotentialFlows().values()) {
            fd.setForcedAuthenticationSupported(true);
        }
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertEquals(authCtx.getPotentialFlows().size(), 3);
    }

    @Test public void testPartialFiltering() throws ProfileException {
        AuthenticationContext authCtx = prc.getSubcontext(AuthenticationContext.class, false);
        authCtx.setForceAuthn(true);
        authCtx.getPotentialFlows().get("test2").setForcedAuthenticationSupported(true);
        
        action.execute(prc);
        ActionTestingSupport.assertProceedEvent(prc);
        Assert.assertEquals(authCtx.getPotentialFlows().size(), 1);
        Assert.assertNull(authCtx.getPotentialFlows().get("test1"));
        Assert.assertNotNull(authCtx.getPotentialFlows().get("test2"));
    }
}