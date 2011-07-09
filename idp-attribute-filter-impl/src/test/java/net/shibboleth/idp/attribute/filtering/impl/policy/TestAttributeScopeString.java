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

package net.shibboleth.idp.attribute.filtering.impl.policy;

import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.ScopedAttributeValue;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;

import org.opensaml.util.collections.CollectionSupport;
import org.testng.Assert;
import org.testng.annotations.Test;

/** tests for the Attribute Scope String criterion. */
public class TestAttributeScopeString {

    /** name used throughout the tests for the attribute. */
    private static final String ATTR_NAME = "attributeName";

    /** Test various combinations of bad parameters. */
    @Test
    public void attributeScopeStringCriterionBadParamsTest() {
        try {
            new AttributeScopeStringCriterion("", true, ATTR_NAME);
            Assert.assertTrue(false, "testing bad constructor (empty match): unreacahble code");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true, "testing bad constructor (empty match): usual case");
        }

        try {
            new AttributeScopeStringCriterion("scope", true, null);
            Assert.assertTrue(false, "testing bad constructor (null attribute name): unreacahble code");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true, "testing bad constructor (null attribute name): usual case");
        }
        //
        // mismatched attribute
        //
        AttributeFilterContext filterContext = new AttributeFilterContext(null);
        AttributeScopeStringCriterion filter = new AttributeScopeStringCriterion("scope", true, ATTR_NAME);

        Assert.assertFalse(filter.evaluate(filterContext), "missed attribute should return false");

    }

    /** test usual operation. */
    @Test
    public void attributeScopeStringCriterionTest() {
        Attribute<Object> attribute = new Attribute<Object>(ATTR_NAME);

        attribute.setValues(CollectionSupport.toSet("val1", new ScopedAttributeValue("foo", "bar"),
                new ScopedAttributeValue("BAR", "foo")));
        AttributeFilterContext filterContext = new AttributeFilterContext(null);

        Set s = CollectionSupport.toSet(attribute);
        filterContext.setPrefilteredAttributes(s);

        AttributeScopeStringCriterion filter = new AttributeScopeStringCriterion("FRED", true, ATTR_NAME);
        Assert.assertFalse(filter.evaluate(filterContext), "match for the scope \"FRED\"");

        filter = new AttributeScopeStringCriterion("bar", true, ATTR_NAME);
        Assert.assertTrue(filter.evaluate(filterContext), "match for the scope \"bar\"");

        filter = new AttributeScopeStringCriterion("BAR", true, ATTR_NAME);
        Assert.assertFalse(filter.evaluate(filterContext), "case sensitive match for the scope \"BAR\"");

        filter = new AttributeScopeStringCriterion("BAR", false, ATTR_NAME);
        Assert.assertTrue(filter.evaluate(filterContext), "case sensitive match for the scope \"BAR\"");
    }

}
