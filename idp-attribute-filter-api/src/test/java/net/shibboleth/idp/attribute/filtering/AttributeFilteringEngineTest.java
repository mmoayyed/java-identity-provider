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

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

/** Unit test for {@link AttributeFilteringEngine}. */
public class AttributeFilteringEngineTest {

    /** Test that post-construction state is what is expected. */
    @Test public void testPostConstructionState() throws Exception {
        AttributeFilteringEngine engine = new AttributeFilteringEngine("engine", null);
        Assert.assertNotNull(engine.getFilterPolicies());
        Assert.assertTrue(engine.getFilterPolicies().isEmpty());
        Assert.assertEquals(engine.getId(), "engine");

        try {
            new AttributeFilteringEngine("  ", null);
            Assert.fail();
        } catch (AssertionError e) {
            // expected
        }
        
        try {
            new AttributeFilteringEngine("", null);
            Assert.fail();
        } catch (AssertionError e) {
            // expected
        }
        
        try {
            new AttributeFilteringEngine(null, null);
            Assert.fail();
        } catch (AssertionError e) {
            // expected
        }
    }

    /** Test setting and retrieving filter policies. */
    @Test public void testFilterPolicies() throws Exception {
        AttributeFilterPolicy policy1 = new AttributeFilterPolicy("policy1", Predicates.alwaysFalse(), null);
        AttributeFilterPolicy policy2 = new AttributeFilterPolicy("policy2", Predicates.alwaysFalse(), null);
        AttributeFilterPolicy policy3 = new AttributeFilterPolicy("policy3", Predicates.alwaysFalse(), null);
        
        AttributeFilteringEngine engine = new AttributeFilteringEngine("engine", Lists.<AttributeFilterPolicy>newArrayList(policy1, policy1, policy2));
        engine.initialize();

        Assert.assertTrue(engine.isInitialized());
        Assert.assertEquals(engine.getFilterPolicies().size(), 2);
        Assert.assertTrue(engine.getFilterPolicies().contains(policy1));
        Assert.assertTrue(policy1.isInitialized());
        Assert.assertTrue(engine.getFilterPolicies().contains(policy2));
        Assert.assertTrue(policy2.isInitialized());
        Assert.assertFalse(engine.getFilterPolicies().contains(policy3));
        Assert.assertFalse(policy3.isInitialized());

        engine = new AttributeFilteringEngine("engine", Lists.<AttributeFilterPolicy>newArrayList(policy1, policy2));
        engine.initialize();

        Assert.assertEquals(engine.getFilterPolicies().size(), 2);
        Assert.assertFalse(engine.getFilterPolicies().contains(policy1));
        Assert.assertTrue(engine.getFilterPolicies().contains(policy2));
        Assert.assertTrue(engine.getFilterPolicies().contains(policy3));

        try {
            engine.getFilterPolicies().add(policy1);
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }

    /** Test filtering attributes. */
    @Test public void testFilterAttributes() throws Exception {
        MockAttributeValueMatcher attribute1Matcher = new MockAttributeValueMatcher();
        attribute1Matcher.setMatchingAttribute("attribute1");
        attribute1Matcher.setMatchingValues(null);

        AttributeValueFilterPolicy attribute1Policy = new AttributeValueFilterPolicy();
        attribute1Policy.setAttributeId("attribute1");
        attribute1Policy.setValueMatcher(attribute1Matcher);

        AttributeFilterPolicy policy = new AttributeFilterPolicy("attribute1Policy", Predicates.alwaysTrue(), Lists.newArrayList(attribute1Policy));

        AttributeFilterContext filterContext = new AttributeFilterContext();

        Attribute attribute1 = new Attribute("attribute1");
        attribute1.setValues(Lists.<AttributeValue>newArrayList(new StringAttributeValue("one"), new StringAttributeValue("two")));
        filterContext.getPrefilteredAttributes().put(attribute1.getId(), attribute1);

        Attribute attribute2 = new Attribute("attribute2");
        attribute2.setValues(Lists.<AttributeValue>newArrayList(new StringAttributeValue("a"), new StringAttributeValue("b")));
        filterContext.getPrefilteredAttributes().put(attribute2.getId(), attribute2);

        AttributeFilteringEngine engine = new AttributeFilteringEngine("engine", Lists.newArrayList(policy));
        engine.initialize();

        engine.filterAttributes(filterContext);
        Assert.assertEquals(filterContext.getFilteredAttributes().size(), 1);
    }

    @Test public void testInitDestroy() throws ComponentInitializationException {
        MockAttributeValueMatcher matcher = new MockAttributeValueMatcher();
        AttributeValueFilterPolicy filterPolicy = new AttributeValueFilterPolicy();
        filterPolicy.setAttributeId("attribute1");
        filterPolicy.setValueMatcher(matcher);

        MockPredicate predicate = new MockPredicate();
        AttributeFilterPolicy policy = new AttributeFilterPolicy("policy", predicate, Arrays.asList(filterPolicy));

        Assert.assertFalse(predicate.isInitialized());
        Assert.assertFalse(predicate.isDestroyed());
        Assert.assertFalse(matcher.isInitialized());
        Assert.assertFalse(matcher.isDestroyed());

        AttributeFilteringEngine engine = new AttributeFilteringEngine("engine", Lists.newArrayList(policy));
        engine.initialize();

        Assert.assertTrue(predicate.isInitialized());
        Assert.assertFalse(predicate.isDestroyed());
        Assert.assertTrue(matcher.isInitialized());
        Assert.assertFalse(matcher.isDestroyed());

        engine.destroy();
        Assert.assertTrue(predicate.isInitialized());
        Assert.assertTrue(predicate.isDestroyed());
        Assert.assertTrue(matcher.isInitialized());
        Assert.assertTrue(matcher.isDestroyed());

        try {
            engine.initialize();
            Assert.fail();
        } catch (DestroyedComponentException e) {
            // OK
        }
    }

    @Test public void testValidate() throws ComponentInitializationException, ComponentValidationException {
        MockAttributeValueMatcher matcher = new MockAttributeValueMatcher();
        AttributeValueFilterPolicy filterPolicy = new AttributeValueFilterPolicy();
        filterPolicy.setAttributeId("attribute1");
        filterPolicy.setValueMatcher(matcher);

        MockPredicate predicate = new MockPredicate();
        AttributeFilterPolicy policy = new AttributeFilterPolicy("Id", predicate, Arrays.asList(filterPolicy));

        AttributeFilteringEngine engine = new AttributeFilteringEngine("engine", Lists.newArrayList(policy));
        Assert.assertFalse(predicate.getValidated());
        Assert.assertFalse(matcher.getValidated());
        
        try {
            engine.validate();
            Assert.fail();
        } catch (UninitializedComponentException e) {
            // OK
        }
        Assert.assertFalse(predicate.getValidated());
        Assert.assertFalse(matcher.getValidated());

        engine.initialize();
        engine.validate();
        Assert.assertTrue(predicate.getValidated());
        Assert.assertTrue(matcher.getValidated());

        predicate.setFailValidate(true);
        try {
            engine.validate();
            Assert.fail();
        } catch (ComponentValidationException e) {
            // OK
        }
        
    }
}