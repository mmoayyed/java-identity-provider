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

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.shibboleth.shared.logic.ConstraintViolationException;
import net.shibboleth.shared.logic.FunctionSupport;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;

/** Unit test for {@link AbstractSAMLProfileConfiguration}. */
public class AbstractSAMLProfileConfigurationTest {

    @Test public void testSignAssertionsCriteria() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();

        config.setSignAssertions(false);
        Assert.assertFalse(config.isSignAssertions(null));

        try {
            config.setSignAssertionsPredicate(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // excepted this
        }
    }

    @Test public void testSignResponsesCriteria() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();

        config.setSignResponses(false);
        Assert.assertFalse(config.isSignResponses(null));

        try {
            config.setSignResponsesPredicate(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // excepted this
        }
    }

    @Test public void testSignRequestsCriteria() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();

        config.setSignRequests(false);
        Assert.assertFalse(config.isSignRequests(null));

        try {
            config.setSignRequestsPredicate(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // excepted this
        }
    }

    @Test public void testAssertionLifetime() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
        Assert.assertTrue(config.getAssertionLifetime(null).toMillis() > 0);

        config.setAssertionLifetime(Duration.ofMillis(100));
        Assert.assertEquals(config.getAssertionLifetime(null), Duration.ofMillis(100));

        try {
            config.setAssertionLifetime(Duration.ZERO);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }

        try {
            config.setAssertionLifetime(Duration.ofMillis(-100));
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }
    }

    @Test public void testIndirectAssertionLifetime() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
        config.setAssertionLifetimeLookupStrategy(FunctionSupport.constant(Duration.ofMillis(500)));
        Assert.assertEquals(config.getAssertionLifetime(null), Duration.ofMillis(500));

        config.setAssertionLifetimeLookupStrategy(FunctionSupport.constant(null));
        try {
            config.getAssertionLifetime(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }
    }

    @Test public void testIncludeNotBefore() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
        Assert.assertTrue(config.isIncludeConditionsNotBefore(null));

        config.setIncludeConditionsNotBefore(false);
        Assert.assertFalse(config.isIncludeConditionsNotBefore(null));
    }

    @Test public void testIndirectIncludeNotBefore() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();

        config.setIncludeConditionsNotBeforePredicate(Predicates.alwaysFalse());
        Assert.assertFalse(config.isIncludeConditionsNotBefore(null));
    }

    @Test public void testAdditionalAudiencesForAssertion() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
        Assert.assertNotNull(config.getAdditionalAudiencesForAssertion(null));
        Assert.assertTrue(config.getAdditionalAudiencesForAssertion(null).isEmpty());

        config.setAdditionalAudiencesForAssertion(Arrays.asList("", null, " foo"));

        final Set<String> audiences = config.getAdditionalAudiencesForAssertion(null);
        Assert.assertNotNull(audiences);
        Assert.assertEquals(audiences.size(), 1);
        Assert.assertTrue(audiences.contains("foo"));

        try {
            audiences.add("bar");
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            // expected this
        }

        config.setAdditionalAudiencesForAssertion(null);
        Assert.assertNotNull(config.getAdditionalAudiencesForAssertion(null));
        Assert.assertTrue(config.getAdditionalAudiencesForAssertion(null).isEmpty());
    }

    @Test public void testIndirectAudiencesForAssertion() {
        final MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
        final Set<String> audiences = new HashSet<>();
        audiences.add("foo");
        audiences.add("bar");
        config.setAdditionalAudiencesForAssertionLookupStrategy(FunctionSupport.constant(audiences));
        Assert.assertEquals(config.getAdditionalAudiencesForAssertion(null), audiences);
    }

    /** Mock class for test {@link AbstractSAMLProfileConfiguration}. */
    private static class MockSAMLProfileConfiguration extends AbstractSAMLProfileConfiguration {

        /** Constructor. */
        public MockSAMLProfileConfiguration() {
            super("mock");
        }
    }

}