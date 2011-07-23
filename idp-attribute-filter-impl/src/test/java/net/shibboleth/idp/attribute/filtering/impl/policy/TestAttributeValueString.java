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
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;

import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.criteria.EvaluationException;
import org.testng.Assert;
import org.testng.annotations.Test;

/** tests for the {@link AttributeValueStringCriterion}. */
public class TestAttributeValueString {

    /** name used throughout the tests for the attribute. */
    private static final String ATTR_NAME = "attributeName";

    /**
     * Test various combinations of bad parameters.
     * 
     * @throws EvaluationException to keep the compiler happy.
     */
    @Test
    public void attributeValueStringCriterionBadParamsTest() throws EvaluationException {
        try {
            new AttributeValueStringCriterion("", true, ATTR_NAME);
            Assert.assertTrue(false, "testing bad constructor (empty match): unreacahble code");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true, "testing bad constructor (empty match): usual case");
        }

        try {
            new AttributeValueStringCriterion("match", true, null);
            Assert.assertTrue(false, "testing bad constructor (null attribute name): unreacahble code");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true, "testing bad constructor (null attribute name): usual case");
        }
        //
        // mismatched attribute
        //
        AttributeFilterContext filterContext = new AttributeFilterContext(null);
        AttributeValueStringCriterion filter = new AttributeValueStringCriterion("match", true, ATTR_NAME);

        Assert.assertFalse(filter.evaluate(filterContext), "missed attribute should return false");

    }

    /**
     * test usual operation.
     * 
     * @throws EvaluationException to keep the compiler happy.
     */
    @Test
    public void attributeValueCriterionStringTest() throws EvaluationException {
        Attribute<String> attribute = new Attribute<String>(ATTR_NAME);

        attribute.setValues(CollectionSupport.toSet("val1", "val2", "VAL3"));
        AttributeFilterContext filterContext = new AttributeFilterContext(null);

        Set s = CollectionSupport.toSet(attribute);
        filterContext.setPrefilteredAttributes(s);

        AttributeValueStringCriterion filter = new AttributeValueStringCriterion("match", true, ATTR_NAME);
        Assert.assertFalse(filter.evaluate(filterContext), "match for the string \"match\"");

        filter = new AttributeValueStringCriterion("VAL3", true, ATTR_NAME);
        Assert.assertTrue(filter.evaluate(filterContext), "match for the string \"VAL3\"");

        filter = new AttributeValueStringCriterion("val3", true, ATTR_NAME);
        Assert.assertFalse(filter.evaluate(filterContext), "case sensitive match for the string \"val3\"");

        filter = new AttributeValueStringCriterion("val3", false, ATTR_NAME);
        Assert.assertTrue(filter.evaluate(filterContext), "case sensitive match for the string \"val3\"");
    }

}
