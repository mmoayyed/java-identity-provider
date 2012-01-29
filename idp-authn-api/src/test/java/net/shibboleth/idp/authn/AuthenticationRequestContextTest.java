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

package net.shibboleth.idp.authn;

import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

public class AuthenticationRequestContextTest {

    @Test public void testInstantation() throws Exception {
        long start = System.currentTimeMillis();
        // this is here to allow the context's creation time to deviate from the 'start' time
        Thread.sleep(50);

        AuthenticationRequestContext ctx = new AuthenticationRequestContext();
        Assert.assertNull(ctx.getAttemptedWorkflow());
        Assert.assertNull(ctx.getAuthenticatedPrincipal());
        Assert.assertEquals(ctx.getCompletionInstant(), 0);
        Assert.assertNull(ctx.getDefaultAuthenticationWorfklow());
        Assert.assertTrue(ctx.getInitiationInstant() > start);
        Assert.assertFalse(ctx.isForcingAuthentication());
        Assert.assertNotNull(ctx.getRequestedWorkflows());
        Assert.assertTrue(ctx.getRequestedWorkflows().isEmpty());
    }

    @Test public void testAttemptedWorkflow() {
        AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");

        AuthenticationRequestContext ctx = new AuthenticationRequestContext();
        ctx.setAttemptedWorkflow(descriptor);
        Assert.assertEquals(ctx.getAttemptedWorkflow(), descriptor);

        ctx.setAttemptedWorkflow(null);
        Assert.assertNull(ctx.getAttemptedWorkflow());
    }

    @Test public void testAuthenticatedPrincipal() {
        UsernamePrincipal principal = new UsernamePrincipal("bob");

        AuthenticationRequestContext ctx = new AuthenticationRequestContext();
        ctx.setAuthenticatedPrincipal(principal);
        Assert.assertEquals(ctx.getAuthenticatedPrincipal(), principal);

        ctx.setAuthenticatedPrincipal(null);
        Assert.assertNull(ctx.getAuthenticatedPrincipal());
    }

    @Test public void testCompletionInstant() throws Exception {
        AuthenticationRequestContext ctx = new AuthenticationRequestContext();

        long now = System.currentTimeMillis();
        // this is here to allow the context's completion time to deviate from the 'now' time
        Thread.sleep(50);

        ctx.setCompletionInstant();
        Assert.assertTrue(ctx.getCompletionInstant() > now);
    }

    @Test public void testDefaultAuthenticationMethod() {
        AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");

        AuthenticationRequestContext ctx = new AuthenticationRequestContext();
        ctx.setDefaultWorfklow(descriptor);
        Assert.assertEquals(ctx.getDefaultAuthenticationWorfklow(), descriptor);

        ctx.setDefaultWorfklow(null);
        Assert.assertNull(ctx.getDefaultAuthenticationWorfklow());
    }

    @Test public void testForcingAuthentication() {
        AuthenticationRequestContext ctx = new AuthenticationRequestContext();
        ctx.setForcingAuthentication(true);
        Assert.assertTrue(ctx.isForcingAuthentication());

        ctx.setForcingAuthentication(false);
        Assert.assertFalse(ctx.isForcingAuthentication());
    }

    @Test public void testRequestedWorkflows() {
        ArrayList<AuthenticationWorkflowDescriptor> descriptors = new ArrayList<AuthenticationWorkflowDescriptor>();
        descriptors.add(new AuthenticationWorkflowDescriptor("foo"));
        descriptors.add(new AuthenticationWorkflowDescriptor("bar"));
        descriptors.add(new AuthenticationWorkflowDescriptor("foo"));
        descriptors.add(null);

        AuthenticationRequestContext ctx = new AuthenticationRequestContext();
        ctx.setRequestedWorkflows(descriptors);
        Assert.assertTrue(ctx.getRequestedWorkflows() != descriptors);
        Assert.assertNotNull(ctx.getRequestedWorkflows());
        Assert.assertEquals(ctx.getRequestedWorkflows().size(), 2);
    }
}