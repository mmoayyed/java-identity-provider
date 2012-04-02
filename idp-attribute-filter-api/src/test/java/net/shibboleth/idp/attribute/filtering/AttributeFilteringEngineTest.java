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
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

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
        } catch (ConstraintViolationException e) {
            // expected
        }

        try {
            new AttributeFilteringEngine("", null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected
        }

        try {
            new AttributeFilteringEngine(null, null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected
        }
    }

    /** Test setting and retrieving filter policies. */
    @Test public void testFilterPolicies() throws Exception {
        AttributeFilterPolicy policy1 = new AttributeFilterPolicy("policy1", Predicates.alwaysFalse(), null);
        AttributeFilterPolicy policy2 = new AttributeFilterPolicy("policy2", Predicates.alwaysFalse(), null);
        AttributeFilterPolicy policy3 = new AttributeFilterPolicy("policy3", Predicates.alwaysFalse(), null);

        AttributeFilteringEngine engine =
                new AttributeFilteringEngine("engine", Lists.<AttributeFilterPolicy> newArrayList(policy1, policy1,
                        policy2));
        engine.initialize();

        Assert.assertTrue(engine.isInitialized());
        Assert.assertEquals(engine.getFilterPolicies().size(), 3);
        Assert.assertTrue(engine.getFilterPolicies().contains(policy1));
        Assert.assertTrue(policy1.isInitialized());
        Assert.assertTrue(engine.getFilterPolicies().contains(policy2));
        Assert.assertTrue(policy2.isInitialized());
        Assert.assertFalse(engine.getFilterPolicies().contains(policy3));
        Assert.assertFalse(policy3.isInitialized());

        engine = new AttributeFilteringEngine("engine", Lists.<AttributeFilterPolicy> newArrayList(policy1, policy2));
        engine.initialize();

        Assert.assertEquals(engine.getFilterPolicies().size(), 2);
        List<AttributeFilterPolicy> contents = engine.getFilterPolicies();
        Assert.assertEquals(contents.get(0).getId(), "policy1");
        Assert.assertEquals(contents.get(1).getId(), "policy2");

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

        AttributeFilterPolicy policy =
                new AttributeFilterPolicy("attribute1Policy", Predicates.alwaysTrue(),
                        Lists.newArrayList(attribute1Policy));

        AttributeFilterContext filterContext = new AttributeFilterContext();

        Attribute attribute1 = new Attribute("attribute1");
        attribute1.setValues(Lists.<AttributeValue> newArrayList(new StringAttributeValue("one"),
                new StringAttributeValue("two")));
        filterContext.getPrefilteredAttributes().put(attribute1.getId(), attribute1);

        Attribute attribute2 = new Attribute("attribute2");
        attribute2.setValues(Lists.<AttributeValue> newArrayList(new StringAttributeValue("a"),
                new StringAttributeValue("b")));
        filterContext.getPrefilteredAttributes().put(attribute2.getId(), attribute2);

        AttributeFilteringEngine engine = new AttributeFilteringEngine("engine", Lists.newArrayList(policy));
        engine.initialize();

        engine.filterAttributes(filterContext);
        Map<String, Attribute> resultAttrs = filterContext.getFilteredAttributes();
        Assert.assertEquals(resultAttrs.size(), 1);
        Set<AttributeValue> result = resultAttrs.get("attribute1").getValues();
        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.contains(new StringAttributeValue("one")));
        Assert.assertTrue(result.contains(new StringAttributeValue("two")));
    }

    @Test public void testAllMatcher() throws Exception {

        AttributeValueFilterPolicy attribute1Policy = new AttributeValueFilterPolicy();
        attribute1Policy.setAttributeId("attribute1");
        attribute1Policy.setValueMatcher(AttributeValueMatcher.MATCHES_ALL);

        AttributeFilterPolicy policy =
                new AttributeFilterPolicy("attribute1Policy", Predicates.alwaysTrue(),
                        Lists.newArrayList(attribute1Policy));

        AttributeFilterContext filterContext = new AttributeFilterContext();

        Attribute attribute1 = new Attribute("attribute1");
        attribute1.setValues(Lists.<AttributeValue> newArrayList(new StringAttributeValue("one"),
                new StringAttributeValue("two")));
        filterContext.getPrefilteredAttributes().put(attribute1.getId(), attribute1);

        AttributeFilteringEngine engine = new AttributeFilteringEngine("engine", Lists.newArrayList(policy));
        engine.initialize();

        engine.filterAttributes(filterContext);
        Set<AttributeValue> result = filterContext.getFilteredAttributes().get("attribute1").getValues();
        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.contains(new StringAttributeValue("one")));
        Assert.assertTrue(result.contains(new StringAttributeValue("two")));
    }

    @Test public void testNoneMatcher() throws Exception {

        AttributeValueFilterPolicy attribute1Policy = new AttributeValueFilterPolicy();
        attribute1Policy.setAttributeId("attribute1");
        attribute1Policy.setValueMatcher(AttributeValueMatcher.MATCHES_NONE);

        AttributeFilterPolicy policy =
                new AttributeFilterPolicy("attribute1Policy", Predicates.alwaysTrue(),
                        Lists.newArrayList(attribute1Policy));

        AttributeFilterContext filterContext = new AttributeFilterContext();

        Attribute attribute1 = new Attribute("attribute1");
        attribute1.setValues(Lists.<AttributeValue> newArrayList(new StringAttributeValue("one"),
                new StringAttributeValue("two")));
        filterContext.getPrefilteredAttributes().put(attribute1.getId(), attribute1);

        AttributeFilteringEngine engine = new AttributeFilteringEngine("engine", Lists.newArrayList(policy));
        engine.initialize();

        engine.filterAttributes(filterContext);
        Assert.assertTrue(filterContext.getFilteredAttributes().isEmpty());
    }

    @Test public void testDenyFilterAttributes() throws Exception {
        MockAttributeValueMatcher deny = new MockAttributeValueMatcher();
        deny.setMatchingAttribute("attribute1");
        deny.setMatchingValues(Arrays.asList(new StringAttributeValue("one")));

        AttributeValueFilterPolicy denyPolicy = new AttributeValueFilterPolicy();
        denyPolicy.setAttributeId("attribute1");
        denyPolicy.setMatchingPermittedValues(false);
        denyPolicy.setValueMatcher(deny);

        AttributeValueFilterPolicy allowPolicy = new AttributeValueFilterPolicy();
        allowPolicy.setAttributeId("attribute1");
        allowPolicy.setValueMatcher(AttributeValueMatcher.MATCHES_ALL);

        AttributeFilterPolicy policy =
                new AttributeFilterPolicy("attribute1Policy", Predicates.alwaysTrue(), Lists.newArrayList(denyPolicy,
                        allowPolicy));

        AttributeFilterContext filterContext = new AttributeFilterContext();

        Attribute attribute1 = new Attribute("attribute1");
        attribute1.setValues(Lists.<AttributeValue> newArrayList(new StringAttributeValue("one"),
                new StringAttributeValue("two")));
        filterContext.getPrefilteredAttributes().put(attribute1.getId(), attribute1);

        AttributeFilteringEngine engine = new AttributeFilteringEngine("engine", Lists.newArrayList(policy));
        engine.initialize();

        engine.filterAttributes(filterContext);
        Map<String, Attribute> resultAttrs = filterContext.getFilteredAttributes();
        Assert.assertEquals(resultAttrs.size(), 1);
        Set<AttributeValue> result = resultAttrs.get("attribute1").getValues();
        Assert.assertEquals(result.size(), 1);
        Assert.assertTrue(result.contains(new StringAttributeValue("two")));
    }

    @Test public void testNoPolicy() throws Exception {
        AttributeValueFilterPolicy allowPolicy = new AttributeValueFilterPolicy();
        allowPolicy.setAttributeId("attribute1");
        allowPolicy.setValueMatcher(AttributeValueMatcher.MATCHES_ALL);

        AttributeFilterPolicy policy =
                new AttributeFilterPolicy("attribute1Policy", Predicates.alwaysFalse(), Lists.newArrayList(allowPolicy));

        AttributeFilterContext filterContext = new AttributeFilterContext();

        Attribute attribute1 = new Attribute("attribute1");
        attribute1.setValues(Lists.<AttributeValue> newArrayList(new StringAttributeValue("one"),
                new StringAttributeValue("two")));
        filterContext.getPrefilteredAttributes().put(attribute1.getId(), attribute1);

        AttributeFilteringEngine engine = new AttributeFilteringEngine("engine", Lists.newArrayList(policy));
        engine.initialize();

        engine.filterAttributes(filterContext);
        Assert.assertTrue(filterContext.getFilteredAttributes().isEmpty());
    }

    @Test public void testDenyAllFilterAttributes() throws Exception {
        AttributeValueFilterPolicy denyPolicy = new AttributeValueFilterPolicy();
        denyPolicy.setAttributeId("attribute1");
        denyPolicy.setMatchingPermittedValues(false);
        denyPolicy.setValueMatcher(AttributeValueMatcher.MATCHES_ALL);

        AttributeValueFilterPolicy allowPolicy = new AttributeValueFilterPolicy();
        allowPolicy.setAttributeId("attribute1");
        allowPolicy.setValueMatcher(AttributeValueMatcher.MATCHES_ALL);

        AttributeFilterPolicy policy =
                new AttributeFilterPolicy("attribute1Policy", Predicates.alwaysTrue(), Lists.newArrayList(denyPolicy,
                        allowPolicy));

        AttributeFilterContext filterContext = new AttributeFilterContext();

        Attribute attribute1 = new Attribute("attribute1");
        attribute1.setValues(Lists.<AttributeValue> newArrayList(new StringAttributeValue("one"),
                new StringAttributeValue("two")));
        filterContext.getPrefilteredAttributes().put(attribute1.getId(), attribute1);

        AttributeFilteringEngine engine = new AttributeFilteringEngine("engine", Lists.newArrayList(policy));
        engine.initialize();

        engine.filterAttributes(filterContext);
        Map<String, Attribute> resultAttrs = filterContext.getFilteredAttributes();
        Assert.assertTrue(resultAttrs.isEmpty());
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