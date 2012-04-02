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

import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Optional;

/** {@link AuthenticationRequestContext} unit test/ */
public class AuthenticationRequestContextTest {

    /** Tests that everything is properly initialized during object construction. */
    @Test public void testInstantation() throws Exception {
        long start = System.currentTimeMillis();
        // this is here to allow the context's creation time to deviate from the 'start' time
        Thread.sleep(50);

        AuthenticationRequestContext ctx = new AuthenticationRequestContext();
        Assert.assertEquals(ctx.getAttemptedWorkflow(), Optional.absent());
        Assert.assertEquals(ctx.getAuthenticatedPrincipal(), Optional.absent());
        Assert.assertEquals(ctx.getCompletionInstant(), 0);
        Assert.assertEquals(ctx.getDefaultAuthenticationWorfklow(), Optional.absent());
        Assert.assertTrue(ctx.getInitiationInstant() > start);
        Assert.assertFalse(ctx.isForcingAuthentication());
        Assert.assertNotNull(ctx.getRequestedWorkflows());
        Assert.assertTrue(ctx.getRequestedWorkflows().isEmpty());
    }

    /** Tests mutating attempted workflow descriptor. */
    @Test public void testAttemptedWorkflow() {
        AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");

        AuthenticationRequestContext ctx = new AuthenticationRequestContext();
        ctx.setAttemptedWorkflow(descriptor);
        Assert.assertEquals(ctx.getAttemptedWorkflow().get(), descriptor);

        try {
            ctx.setAttemptedWorkflow(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            Assert.assertEquals(ctx.getAttemptedWorkflow().get(), descriptor);
        }
    }

    /** Tests mutating authenticated principal. */
    @Test public void testAuthenticatedPrincipal() {
        UsernamePrincipal principal = new UsernamePrincipal("bob");

        AuthenticationRequestContext ctx = new AuthenticationRequestContext();
        ctx.setAuthenticatedPrincipal(principal);
        Assert.assertEquals(ctx.getAuthenticatedPrincipal().get(), principal);

        try {
            ctx.setAuthenticatedPrincipal(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            Assert.assertEquals(ctx.getAuthenticatedPrincipal().get(), principal);
        }
    }

    /** Tests mutating authentication completion instant. */
    @Test public void testCompletionInstant() throws Exception {
        AuthenticationRequestContext ctx = new AuthenticationRequestContext();

        long now = System.currentTimeMillis();
        // this is here to allow the context's completion time to deviate from the 'now' time
        Thread.sleep(50);

        ctx.setCompletionInstant();
        Assert.assertTrue(ctx.getCompletionInstant() > now);
    }

    /** Tests mutating default authentication workflow. */
    @Test public void testDefaultAuthenticationWorkflow() {
        AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");

        AuthenticationRequestContext ctx = new AuthenticationRequestContext();
        ctx.setDefaultWorfklow(descriptor);
        Assert.assertEquals(ctx.getDefaultAuthenticationWorfklow().get(), descriptor);

        ctx.setDefaultWorfklow(null);
        Assert.assertEquals(ctx.getDefaultAuthenticationWorfklow(), Optional.absent());
    }

    /** Tests mutating forced authentication. */
    @Test public void testForcingAuthentication() {
        AuthenticationRequestContext ctx = new AuthenticationRequestContext();
        ctx.setForcingAuthentication(true);
        Assert.assertTrue(ctx.isForcingAuthentication());

        ctx.setForcingAuthentication(false);
        Assert.assertFalse(ctx.isForcingAuthentication());
    }

    /** Tests mutating requested authentication workflows. */
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