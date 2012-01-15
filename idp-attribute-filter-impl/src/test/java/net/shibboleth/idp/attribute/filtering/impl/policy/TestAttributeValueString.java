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

import java.util.List;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.util.criteria.EvaluationException;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/** tests for the {@link AttributeValueStringCriterion}. */
public class TestAttributeValueString {

    /** name used throughout the tests for the attribute. */
    private static final String ATTR_NAME = "attributeName";

    /**
     * Test various combinations of bad parameters.
     * 
     * @throws EvaluationException to keep the compiler happy.
     * @throws ComponentInitializationException never
     */
    @Test public void attributeValueStringCriterionBadParamsTest() throws EvaluationException,
            ComponentInitializationException {
        AttributeFilterContext filterContext = new AttributeFilterContext(null);
        AttributeValueStringCriterion filter = new AttributeValueStringCriterion();

        boolean threw = false;
        filter.setAttributeName(ATTR_NAME);
        filter.setMatchString("foo");
        try {
            filter.evaluate(filterContext);
        } catch (EvaluationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "evaluate on an unitialized filter");

        threw = false;
        try {
            filter.initialize();
        } catch (ComponentInitializationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "initialize without case sensitivity being set");

        filter.setCaseSensitive(true);
        filter.setMatchString("");
        threw = false;
        try {
            filter.initialize();
        } catch (ComponentInitializationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "initialize null match string being set");

        filter.setMatchString("match");
        filter.setAttributeName(null);
        threw = false;
        try {
            filter.initialize();
        } catch (ComponentInitializationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "initialize null attribute name being set");

        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        threw = false;
        try {
            filter.evaluate(filterContext);
        } catch (Exception e) {
            threw = true;
        }
        Assert.assertTrue(threw, "missed attribute should throw an exception");
    }

    /**
     * test usual operation.
     * 
     * @throws EvaluationException to keep the compiler happy.
     * @throws ComponentInitializationException never
     */
    @Test public void attributeValueCriterionStringTest() throws EvaluationException, ComponentInitializationException {
        Attribute<String> attribute = new Attribute<String>(ATTR_NAME);

        attribute.setValues(Lists.newArrayList("val1", "val2", "VAL3"));
        AttributeFilterContext filterContext = new AttributeFilterContext(null);

        filterContext.setPrefilteredAttributes((List) Lists.newArrayList(attribute));

        AttributeValueStringCriterion filter = new AttributeValueStringCriterion();
        filter.setMatchString("match");
        filter.setCaseSensitive(true);
        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        Assert.assertFalse(filter.evaluate(filterContext), "match for the string \"match\"");

        filter = new AttributeValueStringCriterion();
        filter.setMatchString("VAL3");
        filter.setCaseSensitive(true);
        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        Assert.assertTrue(filter.evaluate(filterContext), "match for the string \"VAL3\"");

        filter = new AttributeValueStringCriterion();
        filter.setMatchString("val3");
        filter.setCaseSensitive(true);
        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        Assert.assertFalse(filter.evaluate(filterContext), "case sensitive match for the string \"val3\"");

        filter = new AttributeValueStringCriterion();
        filter.setMatchString("val3");
        filter.setCaseSensitive(false);
        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        Assert.assertTrue(filter.evaluate(filterContext), "case sensitive match for the string \"val3\"");
    }

}
