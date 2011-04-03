/*
 * Copyright 2011 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.filtering;

import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.util.collections.CollectionSupport;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link AttributeFilteringEngine}. */
public class AttributeFilteringEngineTest {

    /** Test that post-construction state is what is expected. */
    @Test
    public void testPostConstructionState() {
        AttributeFilteringEngine engine = new AttributeFilteringEngine("engine");
        Assert.assertNotNull(engine.getFilterPolicies());
        Assert.assertTrue(engine.getFilterPolicies().isEmpty());
        Assert.assertEquals(engine.getId(), "engine");
    }

    /** Test setting and retrieving filter policies. */
    @Test
    public void testFilterPolicies() {

        AttributeFilterPolicy policy1 =
                new AttributeFilterPolicy("policy1", new MockAttributeFilterPolicyRequirementRule(), null);
        AttributeFilterPolicy policy2 =
                new AttributeFilterPolicy("policy2", new MockAttributeFilterPolicyRequirementRule(), null);
        AttributeFilterPolicy policy3 =
                new AttributeFilterPolicy("policy3", new MockAttributeFilterPolicyRequirementRule(), null);

        AttributeFilteringEngine engine = new AttributeFilteringEngine("engine");

        engine.setFilterPolicies(CollectionSupport.toList(policy1, policy1, policy2));
        Assert.assertEquals(engine.getFilterPolicies().size(), 2);
        Assert.assertTrue(engine.getFilterPolicies().contains(policy1));
        Assert.assertTrue(engine.getFilterPolicies().contains(policy2));
        Assert.assertFalse(engine.getFilterPolicies().contains(policy3));

        engine.setFilterPolicies(CollectionSupport.toList(policy2, policy3));
        Assert.assertEquals(engine.getFilterPolicies().size(), 2);
        Assert.assertFalse(engine.getFilterPolicies().contains(policy1));
        Assert.assertTrue(engine.getFilterPolicies().contains(policy2));
        Assert.assertTrue(engine.getFilterPolicies().contains(policy3));

        engine.setFilterPolicies(null);
        Assert.assertEquals(engine.getFilterPolicies().size(), 0);

        try {
            engine.getFilterPolicies().add(policy1);
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }
    
    /** Test filtering attributes. */
    @Test
    public void testFilterAttributes() throws Exception{
        MockAttributeValueMatcher attribute1Matcher = new MockAttributeValueMatcher();
        attribute1Matcher.setMatchingAttribute("attribute1");
        attribute1Matcher.setMatchingValues(null);
        AttributeValueFilterPolicy attribute1Policy = new AttributeValueFilterPolicy("attribute1",attribute1Matcher);
        
        MockAttributeFilterPolicyRequirementRule requirementRule = new MockAttributeFilterPolicyRequirementRule();
        requirementRule.setSatisfied(true);
        AttributeFilterPolicy policy = new AttributeFilterPolicy("attribute1Policy", requirementRule, CollectionSupport.toList(attribute1Policy));
                
        AttributeFilterContext filterContext = new AttributeFilterContext(null);
        
        Attribute<String> attribute1 = new Attribute<String>("attribute1");
        attribute1.setValues(CollectionSupport.toList("one", "two"));
        filterContext.addPrefilteredAttribute(attribute1);
        
        Attribute<String> attribute2 = new Attribute<String>("attribute2");
        attribute2.setValues(CollectionSupport.toList("a", "b"));
        filterContext.addPrefilteredAttribute(attribute2);
        
        AttributeFilteringEngine engine = new AttributeFilteringEngine("engine");
        engine.setFilterPolicies(CollectionSupport.toList(policy));

        engine.filterAttributes(filterContext);
        Assert.assertEquals(filterContext.getFilteredAttributes().size(), 1);
    }
}