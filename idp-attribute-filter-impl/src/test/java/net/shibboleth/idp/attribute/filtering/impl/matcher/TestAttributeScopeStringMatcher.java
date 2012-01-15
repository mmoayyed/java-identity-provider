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
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

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
     * @throws ComponentInitializationException never.
     */
    @Test public void attributeScopeStringMatcherTest() throws AttributeFilteringException,
            ComponentInitializationException {
        // Bad Parameters are handles in the base class and tested in @link(TestAttributeValueStringMatcher

        AttributeScopeStringMatcher filter;
        final Attribute<?> attribute = new Attribute<String>(ATTR_NAME);
        // set up "a", "a@foo", "b@FOO", "foo@a"
        final Collection values =
                Lists.newArrayList((Object) "a", new ScopedAttributeValue("a", "foo"), new ScopedAttributeValue("b",
                        "FOO"), new ScopedAttributeValue("foo", "A"));
        attribute.setValues(values);

        filter = new AttributeScopeStringMatcher();
        filter.setCaseSentitive(true);
        filter.setMatchString("foo");
        filter.initialize();
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), 1, "counts of '@foo' (case sensitive)");

        filter = new AttributeScopeStringMatcher();
        filter.setCaseSentitive(false);
        filter.setMatchString("foo");
        filter.initialize();
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), 2, "counts of '@foo' (case insensitive)");

        filter = new AttributeScopeStringMatcher();
        filter.setCaseSentitive(true);
        filter.setMatchString("a");
        filter.initialize();
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), 0, "counts of 'ONE' (case sensitive)");

    }

}
