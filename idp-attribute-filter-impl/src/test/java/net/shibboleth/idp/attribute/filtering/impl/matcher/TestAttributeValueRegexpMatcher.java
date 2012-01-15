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
 * Test the {@link AttributeValueRegexMatcher} class.
 */
public class TestAttributeValueRegexpMatcher {

    /**
     * Test the regexp value matcher.
     * 
     * @throws AttributeFilteringException if the filter fails
     * @throws ComponentInitializationException never
     */
    @Test public void attributeValueRegexMatcherTest() throws AttributeFilteringException,
            ComponentInitializationException {

        // Initialize testing in implemented in the base class and tested in @link(TestAttributeScopeRegexMatcher)

        final Attribute<?> attribute = new Attribute("Attribute");
        // set up "X", "fot", "a@foo", "foobar", "foo", "foo@a"
        ScopedAttributeValue aAtfoo = new ScopedAttributeValue("a", "foo");
        ScopedAttributeValue fooAta = new ScopedAttributeValue("foo", "a");
        final Collection values = Lists.newArrayList((Object) "X", "fot", aAtfoo, "foobar", "foo", fooAta);
        attribute.setValues(values);

        AttributeValueRegexMatcher filter = new AttributeValueRegexMatcher();
        filter.setRegularExpression("f.o");
        filter.initialize();
        Collection res = filter.getMatchingValues(attribute, null);

        Assert.assertEquals(res.size(), 2, "f.o matches foo and foo@a");
        Assert.assertTrue(res.contains("foo"), "f.o matches foo");
        Assert.assertTrue(res.contains(fooAta), "f.o matches foo@a");

        filter = new AttributeValueRegexMatcher();
        filter.setRegularExpression("fo.*");
        filter.initialize();
        res = filter.getMatchingValues(attribute, null);

        Assert.assertEquals(res.size(), 4, "fo.* matches foo, fot, foobar, foo@a");
        Assert.assertTrue(res.contains("foo"), "f.o matches foo");
        Assert.assertTrue(res.contains("fot"), "f.o matches fot");
        Assert.assertTrue(res.contains("foobar"), "f.o matches foobar");
        Assert.assertTrue(res.contains(fooAta), "f.o matches foo@a");
    }
}
