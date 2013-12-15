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

import java.util.Collections;
import java.util.Set;

import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

/** Unit test for {@link AbstractSAMLProfileConfiguration}. */
public class AbstractSamlProfileConfigurationTest {

    @Test public void testSignAssertionsCriteria() {
        MockSamlProfileConfiguration config = new MockSamlProfileConfiguration();
        Assert.assertNotNull(config.getSignAssertionsPredicate());

        config.setSignAssertionsPredicate(Predicates.<ProfileRequestContext> alwaysFalse());
        Assert.assertSame(config.getSignAssertionsPredicate(), Predicates.<ProfileRequestContext> alwaysFalse());

        try {
            config.setSignAssertionsPredicate(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // excepted this
        }
    }

    @Test public void testSignResponsesCriteria() {
        MockSamlProfileConfiguration config = new MockSamlProfileConfiguration();
        Assert.assertNotNull(config.getSignResponsesPredicate());

        config.setSignResponsesPredicate(Predicates.<ProfileRequestContext> alwaysFalse());
        Assert.assertSame(config.getSignResponsesPredicate(), Predicates.<ProfileRequestContext> alwaysFalse());

        try {
            config.setSignResponsesPredicate(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // excepted this
        }
    }

    @Test public void testSignRequestsCriteria() {
        MockSamlProfileConfiguration config = new MockSamlProfileConfiguration();
        Assert.assertNotNull(config.getSignedRequestsPredicate());

        config.setSignedRequestsPredicate(Predicates.<ProfileRequestContext> alwaysFalse());
        Assert.assertSame(config.getSignedRequestsPredicate(), Predicates.<ProfileRequestContext> alwaysFalse());

        try {
            config.setSignedRequestsPredicate(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // excepted this
        }
    }

    @Test public void testAssertionLifetime() {
        MockSamlProfileConfiguration config = new MockSamlProfileConfiguration();
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

    @Test public void testAdditionalAudiencesForAssertion() {
        MockSamlProfileConfiguration config = new MockSamlProfileConfiguration();
        Assert.assertNotNull(config.getAdditionalAudiencesForAssertion());
        Assert.assertTrue(config.getAdditionalAudiencesForAssertion().isEmpty());

        config.setAdditionalAudienceForAssertion(null);
        Assert.assertNotNull(config.getAdditionalAudiencesForAssertion());
        Assert.assertTrue(config.getAdditionalAudiencesForAssertion().isEmpty());

        config.setAdditionalAudienceForAssertion(Lists.newArrayList("", null, " foo"));

        Set<String> audiences = config.getAdditionalAudiencesForAssertion();
        Assert.assertNotNull(audiences);
        Assert.assertEquals(audiences.size(), 1);
        Assert.assertTrue(audiences.contains("foo"));

        try {
            audiences.add("bar");
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            // expected this
        }

        config.setAdditionalAudienceForAssertion(Collections.EMPTY_LIST);
        Assert.assertNotNull(config.getAdditionalAudiencesForAssertion());
        Assert.assertTrue(config.getAdditionalAudiencesForAssertion().isEmpty());
    }

    /** Mock class for test {@link AbstractSAMLProfileConfiguration}. */
    private static class MockSamlProfileConfiguration extends AbstractSAMLProfileConfiguration {

        /** Constructor. */
        public MockSamlProfileConfiguration() {
            super("mock");
        }
    }
}