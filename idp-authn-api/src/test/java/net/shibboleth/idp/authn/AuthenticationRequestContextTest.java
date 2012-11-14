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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import net.shibboleth.idp.session.AuthenticationEvent;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.ServiceSession;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Optional;

/** {@link AuthenticationRequestContext} unit test. */
public class AuthenticationRequestContextTest {

    /** Tests active session instantiation. */
    @Test public void testActiveSession() throws Exception {
        AuthenticationRequestContext ctx = new AuthenticationRequestContext(null, null);
        Assert.assertEquals(ctx.getActiveSession(), Optional.absent());

        IdPSession idpSession = new IdPSession("test", new byte[] {0, 0, 0});
        ctx = new AuthenticationRequestContext(idpSession, null);
        Assert.assertEquals(ctx.getActiveSession().get(), idpSession);
    }

    /** Tests initiation instant instantiation. */
    @Test public void testInitiationInstant() throws Exception {
        long start = System.currentTimeMillis();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);

        AuthenticationRequestContext ctx = new AuthenticationRequestContext(null, null);
        Assert.assertTrue(ctx.getInitiationInstant() > start);
    }

    /** Tests mutating forcing authentication. */
    @Test public void testForcingAuthentication() throws Exception {
        AuthenticationRequestContext ctx = new AuthenticationRequestContext(null, null);
        Assert.assertFalse(ctx.isForcingAuthentication());

        ctx.setForcingAuthentication(true);
        Assert.assertTrue(ctx.isForcingAuthentication());
    }

    /** Tests active workflow instantiation. */
    @Test public void testActiveWorkFlows() throws Exception {
        final IdPSession idpSession = new IdPSession("test", new byte[] {0, 0, 0});
        final AuthenticationEvent event = new AuthenticationEvent("test", new UsernamePrincipal("foo"));
        final ServiceSession serviceSession = new ServiceSession("serviceSession", event);
        final AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");
        final Collection<AuthenticationWorkflowDescriptor> nullDescriptors =
                Arrays.asList(new AuthenticationWorkflowDescriptor[] {null});

        AuthenticationRequestContext ctx = new AuthenticationRequestContext(null, null);
        Assert.assertTrue(ctx.getActiveWorkflows().isEmpty());

        ctx = new AuthenticationRequestContext(idpSession, null);
        Assert.assertTrue(ctx.getActiveWorkflows().isEmpty());

        ctx = new AuthenticationRequestContext(null, nullDescriptors);
        Assert.assertTrue(ctx.getActiveWorkflows().isEmpty());

        ctx = new AuthenticationRequestContext(idpSession, nullDescriptors);
        Assert.assertTrue(ctx.getActiveWorkflows().isEmpty());

        ctx = new AuthenticationRequestContext(null, Arrays.asList(descriptor));
        Assert.assertTrue(ctx.getActiveWorkflows().isEmpty());

        ctx = new AuthenticationRequestContext(idpSession, Arrays.asList(descriptor));
        Assert.assertTrue(ctx.getActiveWorkflows().isEmpty());

        idpSession.addServiceSession(serviceSession);
        Assert.assertTrue(ctx.getActiveWorkflows().isEmpty());

        ctx = new AuthenticationRequestContext(idpSession, null);
        Assert.assertTrue(ctx.getActiveWorkflows().isEmpty());

        ctx = new AuthenticationRequestContext(null, nullDescriptors);
        Assert.assertTrue(ctx.getActiveWorkflows().isEmpty());

        ctx = new AuthenticationRequestContext(idpSession, nullDescriptors);
        Assert.assertTrue(ctx.getActiveWorkflows().isEmpty());

        ctx = new AuthenticationRequestContext(null, Arrays.asList(descriptor));
        Assert.assertTrue(ctx.getActiveWorkflows().isEmpty());

        ctx = new AuthenticationRequestContext(idpSession, Arrays.asList(descriptor));
        Assert.assertEquals(ctx.getActiveWorkflows().size(), 1);
        Assert.assertEquals(ctx.getActiveWorkflows().get("test"), descriptor);
    }

    /** Tests available workflow instantiation. */
    @Test public void testAvailableWorkflows() throws Exception {
        AuthenticationRequestContext ctx = new AuthenticationRequestContext(null, null);
        Assert.assertTrue(ctx.getAvailableWorkflows().isEmpty());

        ctx = new AuthenticationRequestContext(null, Arrays.asList(new AuthenticationWorkflowDescriptor[] {null}));
        Assert.assertTrue(ctx.getAvailableWorkflows().isEmpty());

        ctx = new AuthenticationRequestContext(null, Collections.EMPTY_LIST);
        Assert.assertTrue(ctx.getAvailableWorkflows().isEmpty());

        AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");
        ctx = new AuthenticationRequestContext(null, Arrays.asList(descriptor));
        Assert.assertEquals(ctx.getAvailableWorkflows().size(), 1);
        Assert.assertEquals(ctx.getAvailableWorkflows().get("test"), descriptor);
    }

    /** Tests potential workflow instantiation. */
    @Test public void testPotentialWorkflows() throws Exception {
        AuthenticationRequestContext ctx = new AuthenticationRequestContext(null, null);
        Assert.assertTrue(ctx.getPotentialWorkflows().isEmpty());

        ctx = new AuthenticationRequestContext(null, Arrays.asList(new AuthenticationWorkflowDescriptor[] {null}));
        Assert.assertTrue(ctx.getPotentialWorkflows().isEmpty());

        ctx = new AuthenticationRequestContext(null, Collections.EMPTY_LIST);
        Assert.assertTrue(ctx.getPotentialWorkflows().isEmpty());

        AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");
        ctx = new AuthenticationRequestContext(null, Arrays.asList(descriptor));
        Assert.assertEquals(ctx.getPotentialWorkflows().size(), 1);
        Assert.assertEquals(ctx.getPotentialWorkflows().get("test"), descriptor);
    }

    /** Tests mutating requested workflows. */
    @Test public void testRequestedWorkflows() throws Exception {
        AuthenticationRequestContext ctx = new AuthenticationRequestContext(null, null);
        Assert.assertTrue(ctx.getRequestedWorkflows().isEmpty());

        ctx.setRequestedWorkflows(null);
        Assert.assertTrue(ctx.getRequestedWorkflows().isEmpty());

        ctx.setRequestedWorkflows(Arrays.asList(new AuthenticationWorkflowDescriptor[] {null}));
        Assert.assertTrue(ctx.getPotentialWorkflows().isEmpty());

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
        AuthenticationRequestContext ctx = new AuthenticationRequestContext(null, null);
        Assert.assertEquals(ctx.getAttemptedWorkflow(), Optional.absent());

        try {
            ctx.setAttemptedWorkflow(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            Assert.assertEquals(ctx.getAttemptedWorkflow(), Optional.absent());
        }

        AuthenticationWorkflowDescriptor descriptor = new AuthenticationWorkflowDescriptor("test");
        ctx.setAttemptedWorkflow(descriptor);
        Assert.assertEquals(ctx.getAttemptedWorkflow().get(), descriptor);
    }

    /** Tests mutating authenticated principal. */
    @Test public void testAuthenticatedPrincipal() throws Exception {
        AuthenticationRequestContext ctx = new AuthenticationRequestContext(null, null);
        Assert.assertEquals(ctx.getAuthenticatedPrincipal(), Optional.absent());

        try {
            ctx.setAuthenticatedPrincipal(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            Assert.assertEquals(ctx.getAuthenticatedPrincipal(), Optional.absent());
        }

        UsernamePrincipal foo = new UsernamePrincipal("foo");
        ctx.setAuthenticatedPrincipal(foo);
        Assert.assertEquals(ctx.getAuthenticatedPrincipal().get(), foo);
    }

    /** Tests setting completion instant. */
    @Test public void testCompletionInstant() throws Exception {
        AuthenticationRequestContext ctx = new AuthenticationRequestContext(null, null);
        Assert.assertEquals(ctx.getCompletionInstant(), 0);

        long now = System.currentTimeMillis();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);

        ctx.setCompletionInstant();
        Assert.assertTrue(ctx.getCompletionInstant() > now);
    }

    /** Tests building authentication event. */
    @Test public void testAuthenticationEvent() throws Exception {
        AuthenticationRequestContext ctx = new AuthenticationRequestContext(null, null);

        try {
            ctx.buildAuthenticationEvent();
            Assert.fail();
        } catch (IllegalStateException e) {
            // ok
        }

        ctx = new AuthenticationRequestContext(null, null);
        ctx.setAttemptedWorkflow(new AuthenticationWorkflowDescriptor("test"));

        try {
            ctx.buildAuthenticationEvent();
            Assert.fail();
        } catch (IllegalStateException e) {
            // ok
        }

        UsernamePrincipal foo = new UsernamePrincipal("foo");
        ctx = new AuthenticationRequestContext(null, null);
        ctx.setAuthenticatedPrincipal(foo);

        try {
            ctx.buildAuthenticationEvent();
            Assert.fail();
        } catch (IllegalStateException e) {
            // ok
        }

        ctx = new AuthenticationRequestContext(null, null);
        ctx.setAttemptedWorkflow(new AuthenticationWorkflowDescriptor("test"));
        ctx.setAuthenticatedPrincipal(foo);

        Assert.assertEquals(ctx.buildAuthenticationEvent().getAuthenticationWorkflow(), "test");
        Assert.assertEquals(ctx.buildAuthenticationEvent().getAuthenticatedPrincipal(), foo);
    }
}
