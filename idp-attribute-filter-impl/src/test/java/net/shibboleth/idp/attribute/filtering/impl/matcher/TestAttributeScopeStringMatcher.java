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
import net.shibboleth.idp.attribute.ScopedAttributeValue;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;

import org.opensaml.util.collections.CollectionSupport;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for {@link AttributeScopeStringMatcher }.
 */
public class TestAttributeScopeStringMatcher {

    /** name used throughout the tests for the attribute. */
    private static final String ATTR_NAME = "attributeName";

    /**
     * Test the getMatchingValue.
     * 
     * @throws AttributeFilteringException if the filter fails
     */
    @Test
    public void attributeScopeStringMatcherTest() throws AttributeFilteringException {
        // Bad Parameters
        try {
            new AttributeScopeStringMatcher("", true);
            Assert.assertTrue(false, "testing bad constructor (empty match): unreacahble code");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true, "testing bad constructor (empty match): usual case");
        }

        final Attribute<?> attribute = new Attribute<String>(ATTR_NAME);
        // set up "a", "a@foo", "b@FOO", "foo@a"
        final Collection values =
                CollectionSupport.toList((Object) "a", new ScopedAttributeValue("a", "foo"), new ScopedAttributeValue(
                        "b", "FOO"), new ScopedAttributeValue("foo", "A"));
        attribute.setValues(values);

        AttributeScopeStringMatcher filter = new AttributeScopeStringMatcher("foo", true);
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), 1, "counts of '@foo' (case sensitive)");

        filter = new AttributeScopeStringMatcher("foo", false);
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), 2, "counts of '@foo' (case insensitive)");

        filter = new AttributeScopeStringMatcher("a", true);
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), 0, "counts of 'ONE' (case sensitive)");


    }

}
