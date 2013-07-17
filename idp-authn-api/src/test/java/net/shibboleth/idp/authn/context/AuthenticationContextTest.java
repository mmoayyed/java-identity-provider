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

package net.shibboleth.idp.authn.context;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import net.shibboleth.idp.authn.AuthenticationWorkflowDescriptor;

import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link AuthenticationContext} unit test. */
public class AuthenticationContextTest {

    /** Tests initiation instant instantiation. */
    @Test public void testInitiationInstant() throws Exception {
        long start = System.currentTimeMillis();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);

        AuthenticationContext ctx = new AuthenticationContext(null);
        Assert.assertTrue(ctx.getInitiationInstant() > start);
    }

    /** Tests mutating forcing authentication. */
    @Test public void testForcingAuthentication() throws Exception {
        AuthenticationContext ctx = new AuthenticationContext(null);
        Assert.assertFalse(ctx.isForceAuthn());

        ctx.setForceAuthn(true);
        Assert.assertTrue(ctx.isForceAuthn());
    }

    /** Tests active workflows. */
    @Test public void testActiveWorkFlows() throws Exception {
        final AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");

        AuthenticationContext ctx = new AuthenticationContext(null);
        Assert.assertTrue(ctx.getActiveWorkflows().isEmpty());
        
        ctx.setActiveWorkflows(Arrays.asList(descriptor));

        Assert.assertEquals(ctx.getActiveWorkflows().size(), 1);
        Assert.assertEquals(ctx.getActiveWorkflows().get("test"), descriptor);
    }
    
    /** Tests potential workflow instantiation. */
    @Test public void testPotentialWorkflows() throws Exception {
        AuthenticationContext ctx = new AuthenticationContext(null);
        Assert.assertTrue(ctx.getPotentialWorkflows().isEmpty());

        ctx = new AuthenticationContext(Collections.EMPTY_LIST);
        Assert.assertTrue(ctx.getPotentialWorkflows().isEmpty());

        AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");
        ctx = new AuthenticationContext(Arrays.asList(descriptor));
        Assert.assertEquals(ctx.getPotentialWorkflows().size(), 1);
        Assert.assertEquals(ctx.getPotentialWorkflows().get("test"), descriptor);
    }

    /** Tests mutating requested workflows. */
    @Test public void testRequestedWorkflows() throws Exception {
        AuthenticationContext ctx = new AuthenticationContext(null);
        Assert.assertTrue(ctx.getRequestedWorkflows().isEmpty());

        ctx.setRequestedWorkflows(Collections.EMPTY_LIST);
        Assert.assertTrue(ctx.getRequestedWorkflows().isEmpty());

        AuthenticationWorkflowDescriptor descriptor1 = new AuthenticationWorkflowDescriptor("test1");
        AuthenticationWorkflowDescriptor descriptor2 = new AuthenticationWorkflowDescriptor("test2");

        ctx.setRequestedWorkflows(Arrays.asList(descriptor1, descriptor2));

        Iterator<AuthenticationWorkflowDescriptor> iterator = ctx.getRequestedWorkflows().values().iterator();
        Assert.assertEquals(ctx.getRequestedWorkflows().size(), 2);
        Assert.assertEquals(iterator.next(), descriptor1);
        Assert.assertEquals(iterator.next(), descriptor2);
    }

    /** Tests mutating attempted workflows. */
    @Test public void testAttemptedWorkflow() throws Exception {
        AuthenticationContext ctx = new AuthenticationContext(null);
        Assert.assertNull(ctx.getAttemptedWorkflow());

        AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");
        ctx.setAttemptedWorkflow(descriptor);
        Assert.assertEquals(ctx.getAttemptedWorkflow(), descriptor);
    }

    /** Tests setting completion instant. */
    @Test public void testCompletionInstant() throws Exception {
        AuthenticationContext ctx = new AuthenticationContext(null);
        Assert.assertEquals(ctx.getCompletionInstant(), 0);

        long now = System.currentTimeMillis();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);

        ctx.setCompletionInstant();
        Assert.assertTrue(ctx.getCompletionInstant() > now);
    }
}
