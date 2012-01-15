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
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/**
 * Test for {@link AttributeValueStringMatcher}.
 */
public class TestAttributeValueStringMatcher {

    /** name used throughout the tests for the attribute. */
    private static final String ATTR_NAME = "attributeName";

    /**
     * Test the getMatchingValue.
     * 
     * @throws AttributeFilteringException if the filter fails
     * @throws ComponentInitializationException never
     */
    @Test public void attributeValueStringMatcherTest() throws AttributeFilteringException,
            ComponentInitializationException {
        AttributeValueStringMatcher filter;
        boolean thrown = false;
        // Bad Parameters
        try {
            filter = new AttributeValueStringMatcher();
            filter.setCaseSentitive(true);
            filter.setMatchString("");
            filter.initialize();
            Assert.assertTrue(false, "testing bad constructor (empty match): unreacahble code");
        } catch (ComponentInitializationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "testing bad constructor (empty match): usual case");

        thrown = false;
        try {
            filter = new AttributeValueStringMatcher();
            filter.setMatchString("string");
            filter.initialize();
            Assert.assertTrue(false, "testing bad constructor (empty case sensitivity): unreacahble code");
        } catch (ComponentInitializationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "testing bad constructor (empty case sensitivity): usual case");

        thrown = false;
        // multiple sets
        try {
            filter = new AttributeValueStringMatcher();
            filter.setCaseSentitive(true);
            filter.setMatchString("string");
            filter.initialize();
            filter.setCaseSentitive(false);
            Assert.assertTrue(false, "testing bad constructor (missing initialize): unreacahble code");
        } catch (UnmodifiableComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "testing bad constructor (missing initialize): usual case");

        final Attribute<String> attribute = new Attribute<String>(ATTR_NAME);
        final Collection<String> values = Lists.newArrayList("zero", "one", "two", "three");
        attribute.setValues(values);

        thrown = false;
        // Missing initialize
        try {
            filter = new AttributeValueStringMatcher();
            filter.setCaseSentitive(true);
            filter.setMatchString("string");
            filter.getMatchingValues(attribute, null);
            Assert.assertTrue(false, "testing bad constructor (missing initialize): unreacahble code");
        } catch (AttributeFilteringException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "testing bad constructor (missing initialize): usual case");

        filter = new AttributeValueStringMatcher();
        filter.setCaseSentitive(true);
        filter.setMatchString("one");
        filter.initialize();
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), 1, "counts of 'one' (case sensitive)");

        filter = new AttributeValueStringMatcher();
        filter.setCaseSentitive(false);
        filter.setMatchString("one");
        filter.initialize();
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), 1, "counts of 'one' (case insensitive)");

        filter = new AttributeValueStringMatcher();
        filter.setCaseSentitive(true);
        filter.setMatchString("ONE");
        filter.initialize();
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), 0, "counts of 'ONE' (case sensitive)");

        filter = new AttributeValueStringMatcher();
        filter.setCaseSentitive(false);
        filter.setMatchString("TWO");
        filter.initialize();
        Assert.assertEquals(filter.getMatchingValues(attribute, null).size(), 1, "counts of 'TWO' (case insensitive)");

    }

}
