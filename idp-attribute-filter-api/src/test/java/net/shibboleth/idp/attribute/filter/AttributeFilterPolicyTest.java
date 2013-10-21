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

package net.shibboleth.idp.attribute.filter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/** {@link AttributeFilterPolicy} unit test. */
public class AttributeFilterPolicyTest {

    private MockPolicyRequirementRule policyMatcher;

    private AttributeRule valuePolicy;

    private MockMatcher valueMatcher;

    private final String ATTR_NAME = "foo";

    private final String ATTR_NAME_2 = "Bar";

    private final String ID = "foo";

    @BeforeMethod public void setUp() {
        policyMatcher = new MockPolicyRequirementRule();
        valueMatcher = new MockMatcher();
        valuePolicy = new AttributeRule();
        valuePolicy.setId("valuePolicy");
        valuePolicy.setAttributeId(ATTR_NAME);
        valuePolicy.setMatcher(valueMatcher);
        valuePolicy.setIsDenyRule(false);
    }

    @Test public void testPostConstructionState() {
        AttributeFilterPolicy policy = new AttributeFilterPolicy(ID, policyMatcher, Arrays.asList(valuePolicy));
        Assert.assertEquals(policy.getId(), ID);
        Assert.assertEquals(policy.getPolicyRequirementRule(), policyMatcher);
        Assert.assertTrue(policy.getAttributeRules().contains(valuePolicy));

        policy = new AttributeFilterPolicy(ID, policyMatcher, null);
        Assert.assertEquals(policy.getId(), ID);
        Assert.assertEquals(policy.getPolicyRequirementRule(), policyMatcher);
        Assert.assertTrue(policy.getAttributeRules().isEmpty());

        try {
            new AttributeFilterPolicy(null, policyMatcher, Arrays.asList(valuePolicy));
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected
        }

        try {
            new AttributeFilterPolicy("", policyMatcher, Arrays.asList(valuePolicy));
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected
        }

        try {
            new AttributeFilterPolicy("  ", policyMatcher, Arrays.asList(valuePolicy));
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected
        }

        try {
            new AttributeFilterPolicy("engine", null, Arrays.asList(valuePolicy));
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected
        }
    }

    @Test public void testInitDestroy() throws ComponentInitializationException {
        AttributeFilterPolicy policy = new AttributeFilterPolicy(ID, policyMatcher, Arrays.asList(valuePolicy));
        Assert.assertFalse(policy.isInitialized(), "Created");
        Assert.assertFalse(policyMatcher.isInitialized(), "Created");
        Assert.assertFalse(valueMatcher.isInitialized(), "Created");

        Assert.assertFalse(policy.isDestroyed(), "Created");
        Assert.assertFalse(policyMatcher.isDestroyed(), "Created");
        Assert.assertFalse(valueMatcher.isDestroyed(), "Created");

        policy = new AttributeFilterPolicy(ID, policyMatcher, Arrays.asList(valuePolicy));
        policy.initialize();
        Assert.assertTrue(policy.isInitialized(), "Initialized");
        Assert.assertTrue(policyMatcher.isInitialized(), "Initialized");
        Assert.assertTrue(valueMatcher.isInitialized(), "Initialized");

        Assert.assertFalse(policy.isDestroyed(), "Initialized");
        Assert.assertFalse(policyMatcher.isDestroyed(), "Initialized");
        Assert.assertFalse(valueMatcher.isDestroyed(), "Initialized");

        policy.destroy();
        Assert.assertTrue(policy.isDestroyed(), "Destroyed");
        Assert.assertTrue(policyMatcher.isDestroyed(), "Destroyed");
        Assert.assertTrue(valueMatcher.isDestroyed(), "Destroyed");

        boolean thrown = false;
        try {
            policy.initialize();
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "Destroyed");
    }

    @Test public void testAttributeValuePolicies() throws ComponentInitializationException {
        AttributeFilterPolicy policy = new AttributeFilterPolicy(ID, policyMatcher, null);
        Assert.assertTrue(policy.getAttributeRules().isEmpty());

        try {
            policy.getAttributeRules().add(new AttributeRule());
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            // expected
        }

        policy = new AttributeFilterPolicy(ID, policyMatcher, Arrays.asList(valuePolicy));
        Assert.assertEquals(policy.getAttributeRules().size(), 1);

        policy.initialize();
        Assert.assertEquals(policy.getAttributeRules().size(), 1);
        Assert.assertTrue(policy.getAttributeRules().contains(valuePolicy));
    }

    @Test public void testValidate() throws ComponentInitializationException, ComponentValidationException {
        AttributeFilterPolicy policy = new AttributeFilterPolicy(ID, policyMatcher, Arrays.asList(valuePolicy));

        boolean thrown = false;
        try {
            policy.validate();
        } catch (UninitializedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

        policy.initialize();
        policy.validate();
        Assert.assertTrue(policyMatcher.getValidated());
        Assert.assertTrue(valueMatcher.getValidated());

        thrown = false;
        policyMatcher.setFailValidate(true);
        try {
            policy.validate();
        } catch (ComponentValidationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

    }

    private AttributeFilterContext apply(Tristate state) throws AttributeFilterException, ComponentInitializationException {

        AttributeFilterPolicy policy = new AttributeFilterPolicy(ID, policyMatcher, Arrays.asList(valuePolicy));

        boolean thrown = false;
        try {
            policy.apply(new AttributeFilterContext());
        } catch (UninitializedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

        policy.initialize();

        thrown = false;
        try {
            policy.apply(null);
        } catch (ConstraintViolationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

        AttributeFilterContext context = new AttributeFilterContext();
        IdPAttribute attribute = new IdPAttribute(ATTR_NAME);

        attribute.setValues(Lists.<AttributeValue> newArrayList(new StringAttributeValue("one"),
                new StringAttributeValue("two"), new StringAttributeValue("three")));

        IdPAttribute attribute2 = new IdPAttribute(ATTR_NAME_2);
        attribute2.setValues(Lists.<AttributeValue> newArrayList(new StringAttributeValue("45")));
        context.setPrefilteredIdPAttributes(Arrays.asList(attribute, attribute2));

        policyMatcher.setRetVal(state);
        valueMatcher.setMatchingAttribute(ATTR_NAME);
        valueMatcher.setMatchingValues(Arrays
                .asList(new StringAttributeValue("one"), new StringAttributeValue("three")));

        policy.apply(context);
        return context;
    }

    @Test public void testApply() throws ComponentInitializationException, AttributeFilterException {
        
        AttributeFilterContext ctx = apply(Tristate.TRUE);
        
        Collection values = ctx.getPermittedAttributeValues().get(ATTR_NAME);

        Assert.assertEquals(values.size(), 2);
        Assert.assertTrue(values.containsAll(Arrays.asList(new StringAttributeValue("one"), new StringAttributeValue(
                "three"))));

        Assert.assertNull(ctx.getPermittedAttributeValues().get(ATTR_NAME_2));

        ctx = apply(Tristate.FALSE);
        Assert.assertNull(ctx.getPermittedAttributeValues().get(ATTR_NAME));

        ctx = apply(Tristate.FAIL);
        Assert.assertNull(ctx.getPermittedAttributeValues().get(ATTR_NAME));
}

    @Test public void testApplyToEmpty() throws ComponentInitializationException, AttributeFilterException {
        AttributeFilterPolicy policy = new AttributeFilterPolicy(ID, policyMatcher, Arrays.asList(valuePolicy));
        //
        // Empty attribute
        //
        AttributeFilterContext context = new AttributeFilterContext();
        IdPAttribute attribute = new IdPAttribute(ATTR_NAME);
        attribute.setValues(Collections.EMPTY_LIST);
        context.setPrefilteredIdPAttributes(Arrays.asList(attribute));
        policy.initialize();
        policy.apply(context);
        Assert.assertTrue(context.getPermittedAttributeValues().isEmpty());

    }
}