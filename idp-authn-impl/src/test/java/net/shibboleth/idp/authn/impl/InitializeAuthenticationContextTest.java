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

import net.shibboleth.idp.authn.context.AuthenticationContext;

import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.context.ProfileRequestContext;

import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link InitializeAuthenticationContext} unit test. */
public class InitializeAuthenticationContextTest {

    /** Test that the authentication context is properly added if an idp session exists. */
    @Test public void testAction() throws Exception {

        // TODO
        // AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");
        // Collection<AuthenticationWorkflowDescriptor> availableFlows = Arrays.asList(descriptor);

        ProfileRequestContext profileCtx = new ProfileRequestContext();

        InitializeAuthenticationContext action = new InitializeAuthenticationContext();
        action.setId("test");
        action.initialize();

        action.doExecute(profileCtx);
        ActionTestingSupport.assertProceedEvent(profileCtx);

        AuthenticationContext authCtx = profileCtx.getSubcontext(AuthenticationContext.class, false);
        Assert.assertNotNull(authCtx);

        // TODO
        // Assert.assertEquals(ctx.getAvailableWorkflows().size(), 1);
        // Assert.assertEquals(ctx.getAvailableWorkflows().get("test"), descriptor);
    }

}
