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

package net.shibboleth.idp.saml.profile.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import net.shibboleth.utilities.java.support.logic.FunctionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;

/** Unit test for {@link AbstractSAMLProfileConfiguration}. */
public class AbstractSAMLProfileConfigurationTest {

    @Test public void testSignAssertionsCriteria() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
        Assert.assertNotNull(config.getSignAssertions());

        config.setSignAssertions(Predicates.<ProfileRequestContext> alwaysFalse());
        Assert.assertSame(config.getSignAssertions(), Predicates.<ProfileRequestContext> alwaysFalse());

        try {
            config.setSignAssertions(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // excepted this
        }
    }

    @Test public void testSignResponsesCriteria() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
        Assert.assertNotNull(config.getSignResponses());

        config.setSignResponses(Predicates.<ProfileRequestContext> alwaysFalse());
        Assert.assertSame(config.getSignResponses(), Predicates.<ProfileRequestContext> alwaysFalse());

        try {
            config.setSignResponses(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // excepted this
        }
    }

    @Test public void testSignRequestsCriteria() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
        Assert.assertNotNull(config.getSignRequests());

        config.setSignRequests(Predicates.<ProfileRequestContext> alwaysFalse());
        Assert.assertSame(config.getSignRequests(), Predicates.<ProfileRequestContext> alwaysFalse());

        try {
            config.setSignRequests(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // excepted this
        }
    }

    @Test public void testAssertionLifetime() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
        Assert.assertTrue(config.getAssertionLifetime() > 0);

        config.setAssertionLifetime(100);
        Assert.assertEquals(config.getAssertionLifetime(), 100);

        try {
            config.setAssertionLifetime(0);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }

        try {
            config.setAssertionLifetime(-100);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }
    }

    @Test public void testIndirectAssertionLifetime() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
        config.setAssertionLifetimeLookupStrategy(FunctionSupport.<ProfileRequestContext,Long>constant(500L));
        Assert.assertEquals(config.getAssertionLifetime(), 500L);

        config.setAssertionLifetimeLookupStrategy(FunctionSupport.<ProfileRequestContext,Long>constant(null));
        Assert.assertEquals(config.getAssertionLifetime(), 5 * 60 * 1000);
    }

    @SuppressWarnings("deprecation")
    @Test public void testIncludeNotBefore() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
        Assert.assertTrue(config.includeConditionsNotBefore());

        config.setIncludeConditionsNotBefore(false);
        Assert.assertFalse(config.includeConditionsNotBefore());
    }

    @Test public void testIndirectIncludeNotBefore() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();

        config.setIncludeConditionsNotBeforePredicate(Predicates.<ProfileRequestContext>alwaysFalse());
        Assert.assertFalse(config.includeConditionsNotBefore());
    }

    @SuppressWarnings("deprecation")
    @Test public void testAdditionalAudiencesForAssertion() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
        Assert.assertNotNull(config.getAdditionalAudiencesForAssertion());
        Assert.assertTrue(config.getAdditionalAudiencesForAssertion().isEmpty());

        config.setAdditionalAudienceForAssertion(Arrays.asList("", null, " foo"));

        final Set<String> audiences = config.getAdditionalAudiencesForAssertion();
        Assert.assertNotNull(audiences);
        Assert.assertEquals(audiences.size(), 1);
        Assert.assertTrue(audiences.contains("foo"));

        try {
            audiences.add("bar");
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            // expected this
        }

        config.setAdditionalAudienceForAssertion(Collections.<String>emptyList());
        Assert.assertNotNull(config.getAdditionalAudiencesForAssertion());
        Assert.assertTrue(config.getAdditionalAudiencesForAssertion().isEmpty());
    }

    @Test public void testIndirectAudiencesForAssertion() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
        final Set<String> audiences = new HashSet<>();
        audiences.add("foo");
        audiences.add("bar");
        config.setAdditionalAudiencesForAssertion(audiences);
        Assert.assertEquals(config.getAdditionalAudiencesForAssertion(), audiences);
    }

    /** Mock class for test {@link AbstractSAMLProfileConfiguration}. */
    private static class MockSAMLProfileConfiguration extends AbstractSAMLProfileConfiguration {

        /** Constructor. */
        public MockSAMLProfileConfiguration() {
            super("mock");
        }
    }

}