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

import net.shibboleth.idp.profile.ProfileRequestContext;

import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.StaticResponseEvaluableCriterion;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/** Unit test for {@link AbstractSamlProfileConfiguration}. */
public class AbstractSamlProfileConfigurationTest {

    @Test
    public void testSignAssertionsCriteria() {
        MockSamlProfileConfiguration config = new MockSamlProfileConfiguration();
        Assert.assertNotNull(config.getSignAssertionsCriteria());

        config.setSignAssertionsCriteria((EvaluableCriterion<ProfileRequestContext>) StaticResponseEvaluableCriterion.FALSE_RESPONSE);
        Assert.assertSame(config.getSignAssertionsCriteria(),
                (EvaluableCriterion<ProfileRequestContext>) StaticResponseEvaluableCriterion.FALSE_RESPONSE);

        try {
            config.setSignAssertionsCriteria(null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // excepted this
        }
    }

    @Test
    public void testSignResponsesCriteria() {
        MockSamlProfileConfiguration config = new MockSamlProfileConfiguration();
        Assert.assertNotNull(config.getSignResponsesCriteria());

        config.setSignResponsesCriteria((EvaluableCriterion<ProfileRequestContext>) StaticResponseEvaluableCriterion.FALSE_RESPONSE);
        Assert.assertSame(config.getSignResponsesCriteria(),
                (EvaluableCriterion<ProfileRequestContext>) StaticResponseEvaluableCriterion.FALSE_RESPONSE);

        try {
            config.setSignResponsesCriteria(null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // excepted this
        }
    }

    @Test
    public void testSignRequestsCriteria() {
        MockSamlProfileConfiguration config = new MockSamlProfileConfiguration();
        Assert.assertNotNull(config.getSignedRequestsCriteria());

        config.setSignedRequestsCriteria((EvaluableCriterion<ProfileRequestContext>) StaticResponseEvaluableCriterion.FALSE_RESPONSE);
        Assert.assertSame(config.getSignedRequestsCriteria(),
                (EvaluableCriterion<ProfileRequestContext>) StaticResponseEvaluableCriterion.FALSE_RESPONSE);

        try {
            config.setSignedRequestsCriteria(null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // excepted this
        }
    }

    @Test
    public void testAssertionLifetime() {
        MockSamlProfileConfiguration config = new MockSamlProfileConfiguration();
        Assert.assertTrue(config.getAssertionLifetime() > 0);

        config.setAssertionLifetime(100);
        Assert.assertEquals(config.getAssertionLifetime(), 100);

        try {
            config.setAssertionLifetime(0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected this
        }

        try {
            config.setAssertionLifetime(-100);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected this
        }
    }

    @Test
    public void testAdditionalAudiencesForAssertion() {
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
    private static class MockSamlProfileConfiguration extends AbstractSamlProfileConfiguration {

        /** Constructor. */
        public MockSamlProfileConfiguration() {
            super("mock");
        }
    }
}