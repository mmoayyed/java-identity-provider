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

import java.util.Collection;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;

import org.opensaml.util.collections.CollectionSupport;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for {@link AttributeValueStringMatcher}.
 */
public class TestAttributeValueStringMatcher {

    /** name used throughout the tests for the attribute. */
    private static final String ATTR_NAME = "attributeName";

    /** Test the getMatchingValue. 
     * @throws AttributeFilteringException if the filter fails
     */
    @Test
    public void attributeValueStringMatcherTest() throws AttributeFilteringException {
        // Bad Parameters
        try {
            new AttributeValueStringMatcher("", true);
            Assert.assertTrue(false, "testing bad constructor (empty match): unreacahble code");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true, "testing bad constructor (empty match): usual case");
        }

        final Attribute<String> attribute = new Attribute<String>(ATTR_NAME);
        final Collection<String> values = CollectionSupport.toList("zero", "one", "two", "three");
        attribute.setValues(values);

        AttributeValueStringMatcher filter = new AttributeValueStringMatcher("one", true);
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), 1, "counts of 'one' (case sensitive)");
        
        filter  = new AttributeValueStringMatcher("one", false);
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), 1, "counts of 'one' (case insensitive)");
        
        filter  = new AttributeValueStringMatcher("ONE", true);
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), 0, "counts of 'ONE' (case sensitive)");
        
        filter  = new AttributeValueStringMatcher("TWO", false);
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), 1, "counts of 'TWO' (case insensitive)");

    }


}
