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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;

import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.component.ComponentInitializationException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for class {@link OrMatcher}.
 * 
 */
public class TestOrMatcher {

    /**
     * test {@link OrMatcher}.
     * 
     * @throws AttributeFilteringException if anything dies horribly.
     * @throws ComponentInitializationException  if the initialize fails
     */
    @Test
    public void orMatcherTest() throws AttributeFilteringException, ComponentInitializationException {
        final Attribute<String> attribute = new Attribute<String>("attribute");
        final Collection<String> values = CollectionSupport.toList("zero", "one", "two", "three");
        attribute.setValues(values);

        OrMatcher filter = new OrMatcher();
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
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), values.size(), "or of ANY is ANY");

        list.add(new AttributeValueStringMatcher("ONE", false));
        list.add(new AttributeValueStringMatcher("three", true));
        filter.setSubMatchers(list);
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), 2, "Match OR of two element values");

        DestroyableAttributeValueStringMatcher destroyTester = new DestroyableAttributeValueStringMatcher("ONE", false);
        list.add(destroyTester);
        list.add(new AnyMatcher());
        
        filter.setSubMatchers(list);
        // Force into set to make the testNG Collection comparison work.
        final Set result = new HashSet(filter.getMatchingValues(attribute, null));
        final Set vals = new HashSet(values);

        Assert.assertEquals(result, vals, "Match OR of anything and ANY the same as ANY");
        
        // test destroy
        filter.destroy();
        Assert.assertTrue(destroyTester.isDestroyed(), "destroyable sub matcher not destroyed");
        try {
            filter.getMatchingValues(attribute, null);
            Assert.assertTrue(false, "unreachable code (destroy)");
        } catch (AttributeFilteringException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "should not be able to match after destrouction");

    }
}
