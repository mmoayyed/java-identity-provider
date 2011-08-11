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

package net.shibboleth.idp.saml.relyingparty;

import net.shibboleth.idp.profile.ProfileRequestContext;

import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.StaticResponseEvaluableCriterion;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link AbstractSAMLProfileConfiguration}. */
public class AbstractSAMLProfileConfigurationTest {

    @Test
    public void testSignAssertionsCriteria() {
        MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
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
        MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
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
        MockSAMLProfileConfiguration config = new MockSAMLProfileConfiguration();
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

    /** Mock class for test {@link AbstractSAMLProfileConfiguration}. */
    private static class MockSAMLProfileConfiguration extends AbstractSAMLProfileConfiguration {

        /** Constructor. */
        public MockSAMLProfileConfiguration() {
            super("mock");
        }
    }
}