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

package net.shibboleth.idp.attribute.filtering;

import java.util.Arrays;
import java.util.Collection;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 */
public class AttributeFilterPolicyTest {

    private MockPredicate predicate;

    private AttributeValueFilterPolicy valuePolicy;

    private MockAttributeValueMatcher matcher;

    private AttributeFilterPolicy policy;

    private final String ATTR_NAME = "foo";
    private final String ATTR_NAME_2 = "Bar";

    private final String ID = "foo";

    @BeforeMethod public void setUp() {
        predicate = new MockPredicate();
        matcher = new MockAttributeValueMatcher();
        policy = new AttributeFilterPolicy();
        valuePolicy = new AttributeValueFilterPolicy();
        valuePolicy.setAttributeId(ATTR_NAME);
        valuePolicy.setValueMatcher(matcher);
    }

    @Test public void testInitDestroy() throws ComponentInitializationException {
        policy.setActivationCriteria(predicate);
        policy.setAttributeValuePolicies(Arrays.asList(valuePolicy));

        Assert.assertFalse(policy.isInitialized(), "Created");
        Assert.assertFalse(predicate.isInitialized(), "Created");
        Assert.assertFalse(matcher.isInitialized(), "Created");

        Assert.assertFalse(policy.isDestroyed(), "Created");
        Assert.assertFalse(predicate.isDestroyed(), "Created");
        Assert.assertFalse(matcher.isDestroyed(), "Created");

        policy.setId(ID);
        policy.initialize();
        Assert.assertTrue(policy.isInitialized(), "Initialized");
        Assert.assertTrue(predicate.isInitialized(), "Initialized");
        Assert.assertTrue(matcher.isInitialized(), "Initialized");

        Assert.assertFalse(policy.isDestroyed(), "Initialized");
        Assert.assertFalse(predicate.isDestroyed(), "Initialized");
        Assert.assertFalse(matcher.isDestroyed(), "Initialized");

        policy.destroy();
        Assert.assertTrue(policy.isDestroyed(), "Destroyed");
        Assert.assertTrue(predicate.isDestroyed(), "Destroyed");
        Assert.assertTrue(matcher.isDestroyed(), "Destroyed");

        boolean thrown = false;
        try {
            policy.initialize();
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "Destroyed");
    }

    @Test public void testId() throws ComponentInitializationException {
        Assert.assertNotNull(policy.getId(), "created");
        policy.setId(ID);
        Assert.assertEquals(policy.getId(), ID);
        policy.initialize();
        Assert.assertEquals(policy.getId(), ID);
        boolean thrown = false;
        try {
            policy.setId("thing");
        } catch (UnmodifiableComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);
        policy.destroy();
        thrown = false;
        try {
            policy.setId("thing");
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
    }

    @Test public void testActivationCriteria() throws ComponentInitializationException {
        policy.setId(ID);
        Assert.assertNotNull(policy.getActivationCriteria());
        boolean thrown = false;
        try {
            policy.setActivationCriteria(null);
        } catch (AssertionError e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "set null Activation criteria");

        policy.setActivationCriteria(predicate);
        Assert.assertEquals(policy.getActivationCriteria(), predicate);
        policy.initialize();
        Assert.assertEquals(policy.getActivationCriteria(), predicate);
        thrown = false;
        try {
            policy.setActivationCriteria(new MockPredicate());
        } catch (UnmodifiableComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);
        policy = new AttributeFilterPolicy();
        policy.destroy();
        thrown = false;
        try {
            policy.setActivationCriteria(new MockPredicate());
        } catch (DestroyedComponentException e) {
            thrown = true;
        }

    }

    @Test public void testAttributeValuePolicies() throws ComponentInitializationException {
        policy.setId(ID);
        Assert.assertTrue(policy.getAttributeValuePolicies().isEmpty());

        policy.setAttributeValuePolicies(null);
        Assert.assertTrue(policy.getAttributeValuePolicies().isEmpty());

        boolean thrown = false;
        try {
            policy.getAttributeValuePolicies().add(new AttributeValueFilterPolicy());
        } catch (UnsupportedOperationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

        policy.setAttributeValuePolicies(Arrays.asList(valuePolicy));
        Assert.assertEquals(policy.getAttributeValuePolicies().size(), 1);

        policy.initialize();
        Assert.assertEquals(policy.getAttributeValuePolicies().size(), 1);
        Assert.assertTrue(policy.getAttributeValuePolicies().contains(valuePolicy));

        thrown = false;
        try {
            policy.setAttributeValuePolicies(Arrays.asList(new AttributeValueFilterPolicy()));
        } catch (UnmodifiableComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

        policy = new AttributeFilterPolicy();
        policy.destroy();

        thrown = false;
        try {
            policy.setAttributeValuePolicies(Arrays.asList(new AttributeValueFilterPolicy()));
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
    }

    @Test public void testValidate() throws ComponentInitializationException, ComponentValidationException {
        policy.setId(ID);
        policy.setAttributeValuePolicies(Arrays.asList(valuePolicy));
        policy.setActivationCriteria(predicate);

        boolean thrown = false;
        try {
            policy.validate();
        } catch (UninitializedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

        policy.initialize();
        policy.validate();
        Assert.assertTrue(predicate.getValidated());
        Assert.assertTrue(matcher.getValidated());

        thrown = false;
        predicate.setFailValidate(true);
        try {
            policy.validate();
        } catch (ComponentValidationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

    }

    @Test public void testApplicable() throws ComponentInitializationException, AttributeFilteringException {
        policy.setId(ID);
        policy.setAttributeValuePolicies(Arrays.asList(valuePolicy));
        policy.setActivationCriteria(predicate);

        boolean thrown = false;
        try {
            policy.isApplicable(new AttributeFilterContext());
        } catch (UninitializedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

        policy.initialize();

        thrown = false;
        try {
            policy.isApplicable(null);
        } catch (AssertionError e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

        predicate.setRetVal(true);
        AttributeFilterContext context = new AttributeFilterContext();
        Assert.assertNull(predicate.getContextUsedAndReset());
        Assert.assertTrue(policy.isApplicable(context));
        Assert.assertEquals(predicate.getContextUsedAndReset(), context);
        //
        // Test that the reset worked
        //
        Assert.assertNull(predicate.getContextUsedAndReset());
        predicate.setRetVal(false);
        Assert.assertFalse(policy.isApplicable(context));
        Assert.assertEquals(predicate.getContextUsedAndReset(), context);
    }

    @Test public void testApply() throws ComponentInitializationException, AttributeFilteringException {
        policy.setId(ID);
        policy.setAttributeValuePolicies(Arrays.asList(valuePolicy));
        policy.setActivationCriteria(predicate);

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
        } catch (AssertionError e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

        AttributeFilterContext context = new AttributeFilterContext();
        Attribute attribute = new Attribute(ATTR_NAME);

        attribute.setValues(Arrays.asList(new StringAttributeValue("one"), new StringAttributeValue("two"),
                new StringAttributeValue("three")));

        Attribute attribute2 = new Attribute(ATTR_NAME_2);
        attribute2.setValues(Arrays.asList(new StringAttributeValue("45")));
        context.setPrefilteredAttributes(Arrays.asList(attribute, attribute2));

        predicate.setRetVal(true);
        matcher.setMatchingAttribute(ATTR_NAME);
        matcher.setMatchingValues(Arrays.asList(new StringAttributeValue("one"), new StringAttributeValue("three")));

        policy.apply(context);

        Collection values = context.getPermittedAttributeValues().get(ATTR_NAME);

        Assert.assertEquals(values.size(), 2);
        Assert.assertTrue(values.containsAll(Arrays.asList(new StringAttributeValue("one"), new StringAttributeValue(
                "three"))));
        
        Assert.assertNull(context.getPermittedAttributeValues().get(ATTR_NAME_2));

    }

}
