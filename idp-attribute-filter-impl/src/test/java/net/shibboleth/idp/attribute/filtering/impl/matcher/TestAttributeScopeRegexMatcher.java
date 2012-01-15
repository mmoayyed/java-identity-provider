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
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/**
 * Rest for the {@link AttributeScopeRegexMatcher} class.
 */
public class TestAttributeScopeRegexMatcher {

    /**
     * test the {@link AttributeScopeRegexMatcher} filtering and constructor. 
     * @throws AttributeFilteringException if the filtering fails.
     * @throws ComponentInitializationException never.
     */
    @Test
    public void attributeValueRegexMatcherTest() throws AttributeFilteringException, ComponentInitializationException {
        AttributeScopeRegexMatcher filter; 
        boolean thrown = false;
        try {
            filter = new AttributeScopeRegexMatcher();
            filter.setRegularExpression("");
            filter.initialize();
            Assert.assertTrue(false, "testing bad constructor (empty regexp): unreacahble code");
        } catch (ComponentInitializationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "testing bad constructor (empty regexp): usual case");
        
        thrown = false;
        // multiple sets
        try {
            filter = new AttributeScopeRegexMatcher();
            filter.setRegularExpression("string");
            filter.initialize();
            filter.setRegularExpression("string");
            Assert.assertTrue(false, "testing bad constructor (missing initialize): unreacahble code");
        } catch (UnmodifiableComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "testing bad constructor (missing initialize): usual case");

        final Attribute<?> attribute = new Attribute("Attribute");
        // set up "foo", "a@foo", "a@foobar", "a@flo", "foo@a"
        ScopedAttributeValue aAtfoo = new ScopedAttributeValue("a", "foo");
        ScopedAttributeValue aAtfoobar = new ScopedAttributeValue("a", "fooobar");
        ScopedAttributeValue aAtflo = new ScopedAttributeValue("a", "flo");
        ScopedAttributeValue fooAta = new ScopedAttributeValue("foo", "a");
        final Collection values = Lists.newArrayList((Object) "foo", aAtfoo, aAtfoobar, aAtflo, fooAta);
        attribute.setValues(values);

        thrown = false;
        // Missing initialize
        try {
            filter = new AttributeScopeRegexMatcher();
            filter.setRegularExpression("string");
            filter.getMatchingValues(attribute, null);
            Assert.assertTrue(false, "testing bad constructor (missing initialize): unreacahble code");
        } catch (AttributeFilteringException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "testing bad constructor (missing initialize): usual case");

        filter = new AttributeScopeRegexMatcher();
        filter.setRegularExpression("f.o");
        filter.initialize();
        Collection res = filter.getMatchingValues(attribute, null);
        
        Assert.assertEquals(res.size(), 2, "f.o matches a@foo and a@flo");
        Assert.assertTrue(res.contains(aAtfoo), "f.o matches a@foo");
        Assert.assertTrue(res.contains(aAtflo), "f.o matches a@flo");

        filter = new AttributeScopeRegexMatcher();
        filter.setRegularExpression("fo.*");
        filter.initialize();
        res = filter.getMatchingValues(attribute, null);

        Assert.assertEquals(res.size(), 2, "fo.* matches a@foo, a@foobar ");
        Assert.assertTrue(res.contains(aAtfoo), "f.o matches a@foo");
        Assert.assertTrue(res.contains(aAtfoobar), "f.o matches a@foobar");
        Assert.assertFalse(res.contains(aAtflo), "f.o does not match a@flo");
        Assert.assertFalse(res.contains(fooAta), "f.o does not match foo@a");
        Assert.assertFalse(res.contains("foo"), "f.o does not match foo");

    }
}
