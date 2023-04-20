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

package net.shibboleth.idp.authn.context.impl;

import java.time.Instant;
import java.util.Arrays;

import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactoryRegistry;
import net.shibboleth.idp.authn.testing.TestPrincipal;
import net.shibboleth.shared.collection.CollectionSupport;

import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link AuthenticationContext} unit test. */
@Test
public class AuthenticationContextTest {

    /**
     * Tests initiation instant instantiation.
     * 
     * @throws Exception if something goes wrong
     */
    public void testInitiationInstant() throws Exception {
        Instant start = Instant.now();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);

        AuthenticationContext ctx = new AuthenticationContext();
        Assert.assertTrue(ctx.getInitiationInstant().isAfter(start));
    }

    /**
     * Tests mutating forcing authentication.
     * 
     * @throws Exception if something goes wrong
     */
    public void testForcingAuthentication() throws Exception {
        AuthenticationContext ctx = new AuthenticationContext();
        Assert.assertFalse(ctx.isForceAuthn());

        ctx.setForceAuthn(true);
        Assert.assertTrue(ctx.isForceAuthn());
    }

    /**
     * Tests active results.
     * 
     * @throws Exception if something goes wrong
     */
    public void testActiveResults() throws Exception {
        final AuthenticationResult result = new AuthenticationResult("test", new Subject());

        final AuthenticationContext ctx = new AuthenticationContext();
        Assert.assertTrue(ctx.getActiveResults().isEmpty());
        
        ctx.setActiveResults(Arrays.asList(result));

        Assert.assertEquals(ctx.getActiveResults().size(), 1);
        Assert.assertEquals(ctx.getActiveResults().get("test"), result);
    }
    
    /**
     * Tests potential flow instantiation.
     * 
     * @throws Exception if something goes wrong
     */
    public void testPotentialFlows() throws Exception {
        AuthenticationContext ctx = new AuthenticationContext();
        Assert.assertTrue(ctx.getPotentialFlows().isEmpty());

        final AuthenticationFlowDescriptor descriptor = new AuthenticationFlowDescriptor();
        descriptor.setId("test");
        ctx = new AuthenticationContext();
        ctx.getPotentialFlows().put(descriptor.getId(), descriptor);
        Assert.assertEquals(ctx.getPotentialFlows().size(), 1);
        Assert.assertEquals(ctx.getPotentialFlows().get("test"), descriptor);
    }

    /**
     * Tests mutating attempted flow.
     * 
     * @throws Exception if something goes wrong
     */
    public void testAttemptedFlow() throws Exception {
        final AuthenticationContext ctx = new AuthenticationContext();
        Assert.assertNull(ctx.getAttemptedFlow());

        final AuthenticationFlowDescriptor descriptor = new AuthenticationFlowDescriptor();
        descriptor.setId("test");
        ctx.setAttemptedFlow(descriptor);
        Assert.assertEquals(ctx.getAttemptedFlow(), descriptor);
    }

    /**
     * Tests setting completion instant.
     * 
     * @throws Exception if something goes wrong
     */
    public void testCompletionInstant() throws Exception {
        final AuthenticationContext ctx = new AuthenticationContext();
        Assert.assertNull(ctx.getCompletionInstant());

        Instant now = Instant.now();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);

        ctx.setCompletionInstant();
        final Instant completion = ctx.getCompletionInstant();
        assert completion != null && completion.isAfter(now);
    }
    
    /**
     * Tests RequestedPrincipalContext helpers.
     * 
     * @throws Exception if something goes wrong
     */
    public void testRequestedPrincipalContextHelpers() throws Exception {
        final AuthenticationContext ctx = new AuthenticationContext();
        ctx.setPrincipalEvalPredicateFactoryRegistry(new PrincipalEvalPredicateFactoryRegistry());
        
        ctx.addRequestedPrincipalContext("foo", new TestPrincipal("bar"), false);
        RequestedPrincipalContext rpCtx = ctx.getSubcontext(RequestedPrincipalContext.class);
        assert rpCtx!=null;
        Assert.assertEquals(rpCtx.getOperator(), "foo");
        Assert.assertEquals(rpCtx.getRequestedPrincipals(), CollectionSupport.singletonList(new TestPrincipal("bar")));
        
        Assert.assertFalse(ctx.addRequestedPrincipalContext("foo", new TestPrincipal("bar"), false));
        
        ctx.addRequestedPrincipalContext("fob", TestPrincipal.class.getName(), "baz", true);
        rpCtx = ctx.getSubcontext(RequestedPrincipalContext.class);
        assert rpCtx!=null;
        Assert.assertEquals(rpCtx.getOperator(), "fob");
        Assert.assertEquals(rpCtx.getRequestedPrincipals(), CollectionSupport.singletonList(new TestPrincipal("baz")));

        ctx.addRequestedPrincipalContext("fog", TestPrincipal.class.getName(), CollectionSupport.listOf("baf", "bag"), true);
        rpCtx = ctx.getSubcontext(RequestedPrincipalContext.class);
        assert rpCtx!=null;
        Assert.assertEquals(rpCtx.getOperator(), "fog");
        Assert.assertEquals(rpCtx.getRequestedPrincipals().size(), 2);
    }
    
    /**
     * Tests helper method when bad type used.
     * 
     * @throws Exception
     */
    @Test(expectedExceptions = ClassCastException.class)
    public void testRequestedPrincipalContextHelperBadType() throws Exception {
        final AuthenticationContext ctx = new AuthenticationContext();
        ctx.setPrincipalEvalPredicateFactoryRegistry(new PrincipalEvalPredicateFactoryRegistry());
        
        ctx.addRequestedPrincipalContext("fob", AuthenticationContext.class.getName(), "baz", false);
    }
    
}
