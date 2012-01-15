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

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

/** Unit test for {@link AttributeFilteringEngine}. */
public class AttributeFilteringEngineTest {

    /** Test that post-construction state is what is expected. */
    @Test public void testPostConstructionState() throws Exception {
        AttributeFilteringEngine engine = new AttributeFilteringEngine();
        engine.setId("engine");
        engine.initialize();

        Assert.assertNotNull(engine.getFilterPolicies());
        Assert.assertTrue(engine.getFilterPolicies().isEmpty());
        Assert.assertEquals(engine.getId(), "engine");
        Assert.assertTrue(engine.isInitialized());
    }

    /** Test setting and retrieving filter policies. */
    @Test public void testFilterPolicies() throws Exception {
        AttributeFilterPolicy policy1 = new AttributeFilterPolicy();
        policy1.setId("policy1");
        policy1.setActivationCriteria(Predicates.<AttributeFilterContext> alwaysFalse());

        AttributeFilterPolicy policy2 = new AttributeFilterPolicy();
        policy2.setId("policy2");
        policy2.setActivationCriteria(Predicates.<AttributeFilterContext> alwaysFalse());

        AttributeFilterPolicy policy3 = new AttributeFilterPolicy();
        policy3.setId("policy3");
        policy3.setActivationCriteria(Predicates.<AttributeFilterContext> alwaysFalse());

        AttributeFilteringEngine engine = new AttributeFilteringEngine();
        engine.setId("engine");
        engine.setFilterPolicies(Lists.newArrayList(policy1, policy1, policy2));
        engine.initialize();

        Assert.assertTrue(engine.isInitialized());
        Assert.assertEquals(engine.getFilterPolicies().size(), 2);
        Assert.assertTrue(engine.getFilterPolicies().contains(policy1));
        Assert.assertTrue(policy1.isInitialized());
        Assert.assertTrue(engine.getFilterPolicies().contains(policy2));
        Assert.assertTrue(policy2.isInitialized());
        Assert.assertFalse(engine.getFilterPolicies().contains(policy3));
        Assert.assertFalse(policy3.isInitialized());

        engine = new AttributeFilteringEngine();
        engine.setId("engine");
        engine.setFilterPolicies(Lists.newArrayList(policy2, policy3));
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

        try {
            engine.setId(null);
        } catch (UnmodifiableComponentException e) {
            // expected this
        }

        try {
            engine.setFilterPolicies(null);
        } catch (UnmodifiableComponentException e) {
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

        AttributeFilterPolicy policy = new AttributeFilterPolicy();
        policy.setId("attribute1Policy");
        policy.setActivationCriteria(Predicates.<AttributeFilterContext> alwaysTrue());
        policy.setAttributeValuePolicies(Lists.newArrayList(attribute1Policy));

        AttributeFilterContext filterContext = new AttributeFilterContext(null);

        Attribute<String> attribute1 = new Attribute<String>("attribute1");
        attribute1.setValues(Lists.newArrayList("one", "two"));
        filterContext.getPrefilteredAttributes().put(attribute1.getId(), attribute1);

        Attribute<String> attribute2 = new Attribute<String>("attribute2");
        attribute2.setValues(Lists.newArrayList("a", "b"));
        filterContext.getPrefilteredAttributes().put(attribute2.getId(), attribute2);

        AttributeFilteringEngine engine = new AttributeFilteringEngine();
        engine.setId("engine");
        engine.setFilterPolicies(Lists.newArrayList(policy));
        engine.initialize();

        engine.filterAttributes(filterContext);
        Assert.assertEquals(filterContext.getFilteredAttributes().size(), 1);
    }
}