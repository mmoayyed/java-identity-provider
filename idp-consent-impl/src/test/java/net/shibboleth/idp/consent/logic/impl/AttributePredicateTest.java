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

package net.shibboleth.idp.consent.logic.impl;

import java.util.Arrays;
import java.util.regex.Pattern;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link AttributePredicate} unit test. */
@SuppressWarnings("javadoc")
public class AttributePredicateTest {

    private AttributePredicate p;

    private IdPAttribute attribute1;

    private IdPAttribute attribute2;

    @BeforeMethod public void setUp() {
        attribute1 = new IdPAttribute("attribute1");
        attribute1.setValues(Arrays.asList(new StringAttributeValue("value1")));
        attribute2 = new IdPAttribute("attribute2");
        attribute2.setValues(Arrays.asList(new StringAttributeValue("value2"), new StringAttributeValue("value3")));
        p = new AttributePredicate();
    }

    @Test public void testWhitelist() {
        p.setPromptedAttributeIds(Arrays.asList("attribute1"));
        Assert.assertTrue(p.test(attribute1));
        Assert.assertFalse(p.test(attribute2));
    }

    @Test public void testBlacklist() {
        p.setIgnoredAttributeIds(Arrays.asList("attribute1"));
        Assert.assertFalse(p.test(attribute1));
        Assert.assertTrue(p.test(attribute2));
    }

    @Test public void testMatchExpression() {
        p.setAttributeIdMatchExpression(Pattern.compile(".*1"));
        Assert.assertTrue(p.test(attribute1));
        Assert.assertFalse(p.test(attribute2));
    }

    @Test public void testWhitelistAndBlacklist() {
        p.setPromptedAttributeIds(Arrays.asList("attribute1"));
        p.setIgnoredAttributeIds(Arrays.asList("attribute1"));
        Assert.assertFalse(p.test(attribute1));
        Assert.assertFalse(p.test(attribute2));

        p.setIgnoredAttributeIds(Arrays.asList("attribute2"));
        Assert.assertTrue(p.test(attribute1));
        Assert.assertFalse(p.test(attribute2));
    }

    @Test public void testWhitelistAndMatchExpression() {
        p.setPromptedAttributeIds(Arrays.asList("attribute1"));
        p.setAttributeIdMatchExpression(Pattern.compile(".*1"));
        Assert.assertTrue(p.test(attribute1));
        Assert.assertFalse(p.test(attribute2));

        p.setAttributeIdMatchExpression(Pattern.compile(".*2"));
        Assert.assertFalse(p.test(attribute1));
        Assert.assertTrue(p.test(attribute2));
    }

    @Test public void testBlacklistAndMatchExpression() {
        p.setIgnoredAttributeIds(Arrays.asList("attribute1"));
        p.setAttributeIdMatchExpression(Pattern.compile(".*1"));
        Assert.assertFalse(p.test(attribute1));
        Assert.assertFalse(p.test(attribute2));

        p.setAttributeIdMatchExpression(Pattern.compile(".*2"));
        Assert.assertFalse(p.test(attribute1));
        Assert.assertTrue(p.test(attribute2));
    }

    @Test public void testWhitelistAndBlacklistAndMatchExpression() {
        p.setPromptedAttributeIds(Arrays.asList("attribute1"));
        p.setIgnoredAttributeIds(Arrays.asList("attribute1"));
        p.setAttributeIdMatchExpression(Pattern.compile(".*1"));
        Assert.assertFalse(p.test(attribute1));
        Assert.assertFalse(p.test(attribute2));

        p.setPromptedAttributeIds(Arrays.asList("attribute1"));
        p.setIgnoredAttributeIds(Arrays.asList("attribute2"));
        p.setAttributeIdMatchExpression(Pattern.compile(".*1"));
        Assert.assertTrue(p.test(attribute1));
        Assert.assertFalse(p.test(attribute2));

        p.setPromptedAttributeIds(Arrays.asList("attribute1"));
        p.setIgnoredAttributeIds(Arrays.asList("attribute1"));
        p.setAttributeIdMatchExpression(Pattern.compile(".*2"));
        Assert.assertFalse(p.test(attribute1));
        Assert.assertTrue(p.test(attribute2));

        p.setPromptedAttributeIds(Arrays.asList("attribute1"));
        p.setIgnoredAttributeIds(Arrays.asList("attribute2"));
        p.setAttributeIdMatchExpression(Pattern.compile(".*2"));
        Assert.assertFalse(p.test(attribute1));
        Assert.assertTrue(p.test(attribute2));
    }

    @Test public void testNullInput() {
        p = new AttributePredicate();
        Assert.assertFalse(p.test(null));
    }

    @Test public void testEmptyAttribute() {
        // no values
        final IdPAttribute emptyAttribute = new IdPAttribute("emptyAttribute");
        Assert.assertFalse(p.test(emptyAttribute));

        // empty values
        emptyAttribute.setValues(Arrays.asList(StringAttributeValue.valueOf(""), StringAttributeValue.valueOf(null)));
        Assert.assertFalse(p.test(emptyAttribute));

        // empty and non-empty values
        emptyAttribute.setValues(Arrays.asList(StringAttributeValue.valueOf("1"), StringAttributeValue.valueOf("")));
        Assert.assertTrue(p.test(emptyAttribute));
    }
}
