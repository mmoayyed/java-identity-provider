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

package net.shibboleth.idp.attribute.filtering.impl.matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;

import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.DestructableComponent;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for class {@link AndMatcher}.
 * 
 */
public class TestAndMatcher {

    /**
     * test {@link AndMatcher}.
     * 
     * @throws AttributeFilteringException if anything dies horribly.
     * @throws ComponentInitializationException init initialize fails
     */
    @Test
    public void andMatcherTest() throws AttributeFilteringException, ComponentInitializationException {
        final Attribute<String> attribute = new Attribute<String>("attribute");
        final Collection<String> values = CollectionSupport.toList("a", "b", "c", "d");
        attribute.setValues(values);

        AndMatcher filter = new AndMatcher();
        boolean threw = false;
        try {
            filter.getMatchingValues(attribute, null);
            Assert.assertTrue(false, "unreachable code (init)");
        } catch (AttributeFilteringException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "Non initialized filter should throw");
        filter.initialize();

        Assert.assertTrue(filter.getMatchingValues(attribute, null).isEmpty(), "null filter gives empty result");

        final List<AttributeValueMatcher> list = new ArrayList<AttributeValueMatcher>(1);
        list.add((AttributeValueMatcher) null);

        filter.setSubMatchers(list);
        
        Assert.assertTrue(filter.getSubMatchers().isEmpty(), "empty elements are nulled out");
        Assert.assertTrue(filter.getMatchingValues(attribute, null).isEmpty(), "empty filter gives empty result");

        filter.setSubMatchers(CollectionSupport.toList((AttributeValueMatcher) new AnyMatcher()));
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), values.size(), "AND of ANY is ANY");
        
        // Construct three or filters to give {"a","b","c"}, {"b", "c"}, {"a", "b", "d"}

        list.add(new AttributeValueStringMatcher("a", false));
        list.add(new AttributeValueStringMatcher("b", true));
        list.add(new AttributeValueStringMatcher("c", true));
        OrMatcher or1 = new OrMatcher();
        or1.setSubMatchers(list);
        or1.initialize();
        
        list.clear();
        list.add(new AttributeValueStringMatcher("c", false));
        list.add(new AttributeValueStringMatcher("b", true));
        OrMatcher or2 = new OrMatcher();
        or2.setSubMatchers(list);
        or2.initialize();

        list.clear();
        list.add(new AttributeValueStringMatcher("a", false));
        list.add(new AttributeValueStringMatcher("b", true));
        DestroyableAttributeValueStringMatcher destroyTester = new DestroyableAttributeValueStringMatcher("d", true);
        list.add(destroyTester);
        OrMatcher or3 = new OrMatcher();
        or3.setSubMatchers(list);
        or3.initialize();

        filter.setSubMatchers(CollectionSupport.toList((AttributeValueMatcher)or1, or2, or3));
        Collection c = filter.getMatchingValues(attribute, null);
        Assert.assertEquals(c.size(), 1, "Match complex AND");
        Assert.assertTrue(c.contains("b"));
        
        threw = false;
        filter.destroy();
        Assert.assertTrue(destroyTester.isDestroyed(), "Has destroy been passed down (via ORmatcher)");
        try {
            filter.getMatchingValues(attribute, null);
            Assert.assertTrue(false, "unreachable code (destroy)");
        } catch (AttributeFilteringException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "unreachable code (destroy)");
        
    }
}
