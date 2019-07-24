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

package net.shibboleth.idp.attribute.filter.policyrule.logic.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nullable;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.filter.PolicyRequirementRule;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.idp.attribute.filter.matcher.impl.AbstractMatcherPolicyRuleTest;
import net.shibboleth.idp.attribute.filter.matcher.impl.DataSources;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;


/** {@link AndPolicyRule} unit test. */
public class AndPolicyRuleTest extends AbstractMatcherPolicyRuleTest {

    @BeforeTest public void setup() throws Exception {
        super.setUp();
    }

    @Test public void testNullArguments() throws Exception {
        final AndPolicyRule rule = newAndPolicyRule(Arrays.asList(PolicyRequirementRule.MATCHES_ALL));
        rule.setId("test");
        rule.initialize();

        try {
            rule.matches(null);
            fail();
        } catch (final ConstraintViolationException e) {
            // expected this
        }
    }

    @Test(expectedExceptions = {ComponentInitializationException.class}) public void emptyInput()
            throws ComponentInitializationException {
        final AndPolicyRule rule = newAndPolicyRule(Collections.emptyList());
        rule.setId("test");
        rule.initialize();
    }

    @Test public void testMatches() throws ComponentInitializationException {
        AndPolicyRule rule = newAndPolicyRule(Arrays.asList(PolicyRequirementRule.MATCHES_NONE, PolicyRequirementRule.MATCHES_NONE));
        rule.setId("Test");
        rule.initialize();
        assertEquals(rule.matches(DataSources.unPopulatedFilterContext()), Tristate.FALSE);

        rule = newAndPolicyRule(Arrays.asList(PolicyRequirementRule.MATCHES_ALL, PolicyRequirementRule.MATCHES_NONE));
        rule.setId("Test");
        rule.initialize();
        assertEquals(rule.matches(DataSources.unPopulatedFilterContext()), Tristate.FALSE);

        rule = newAndPolicyRule(Arrays.asList(PolicyRequirementRule.MATCHES_ALL, PolicyRequirementRule.MATCHES_ALL));
        rule.setId("Test");
        rule.initialize();
        assertEquals(rule.matches(DataSources.unPopulatedFilterContext()), Tristate.TRUE);

        rule = newAndPolicyRule(Arrays.asList(PolicyRequirementRule.MATCHES_ALL, PolicyRequirementRule.REQUIREMENT_RULE_FAILS));
        rule.setId("Test");
        rule.initialize();
        assertEquals(rule.matches(DataSources.unPopulatedFilterContext()), Tristate.FAIL);
    }
    
    @Test public void testSingletons() throws ComponentInitializationException {
        AndPolicyRule rule = newAndPolicyRule(Collections.singletonList(PolicyRequirementRule.MATCHES_NONE));
        rule.setId("Test");
        rule.initialize();
        assertEquals(rule.matches(DataSources.unPopulatedFilterContext()), Tristate.FALSE);

        rule = newAndPolicyRule(Collections.singletonList(PolicyRequirementRule.REQUIREMENT_RULE_FAILS));
        rule.setId("Test");
        rule.initialize();
        assertEquals(rule.matches(DataSources.unPopulatedFilterContext()), Tristate.FAIL);

        rule = newAndPolicyRule(Collections.singletonList(PolicyRequirementRule.MATCHES_ALL));
        rule.setId("Test");
        rule.initialize();
        assertEquals(rule.matches(DataSources.unPopulatedFilterContext()), Tristate.TRUE);
    }
    
    public static AndPolicyRule newAndPolicyRule(@Nullable @NullableElements 
            final Collection<PolicyRequirementRule> composedRules) {
        final AndPolicyRule  rule = new AndPolicyRule();
        rule.setSubsidiaries(composedRules);
        return rule;
    }
}
