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
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;

import org.opensaml.util.collections.CollectionSupport;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Test the {@link NotMatcher} matcher. */
public class TestNotMatcher {

    /** Test the {@link NotMatcher} matcher in a variety of configurations.
     * @throws AttributeFilteringException if the filter has issues.
     */
    @Test
    public void notMatcherTest() throws AttributeFilteringException {
        final Attribute<String> attribute = new Attribute<String>("attribute");
        final Collection<String> values = CollectionSupport.toSet("zero", "one", "two", "three");
        attribute.setValues(values);

        try {
            new NotMatcher(null);
            Assert.assertFalse(true, "unreachable code");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true, "normal flow");
        }

        NotMatcher not = new NotMatcher(new AnyMatcher());
        Assert.assertTrue(not.getMatchingValues(attribute, null).isEmpty(), "Not of everything is nothing");

        final OrMatcher or =
                new OrMatcher(CollectionSupport.toList((AttributeValueMatcher) new AttributeValueStringMatcher("zero",
                        true), new AttributeValueStringMatcher("two", true)));
        not = new NotMatcher(or);
        
        Collection<String> expected = CollectionSupport.toSet("one", "three", "simple not");
        Assert.assertEquals(not.getMatchingValues(attribute, null), expected);
        
        expected = CollectionSupport.toSet("zero", "two");
        not = new NotMatcher(not);
        Assert.assertEquals(not.getMatchingValues(attribute, null), expected, "not of not");   
    }
}
