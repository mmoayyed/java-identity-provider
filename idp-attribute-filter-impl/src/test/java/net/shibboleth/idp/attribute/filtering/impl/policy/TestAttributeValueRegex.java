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

/** tests for the Attribute Value Regex criterion. */

public class TestAttributeValueRegex {

    /** name used throughout the tests for the attribute. */
    private static final String ATTR_NAME = "attributeName";

    /**
     * Test various combinations of bad parameters.
     * 
     * @throws EvaluationException to keep the compiler happy.
     * @throws ComponentInitializationException never
     */
    @Test public void attributeValueRegexCriterionBadParamsTest() throws EvaluationException,
            ComponentInitializationException {
        AttributeFilterContext filterContext = new AttributeFilterContext(null);
        AttributeValueRegexCriterion filter;

        boolean threw = false;
        filter = new AttributeValueRegexCriterion();
        filter.setAttributeName(ATTR_NAME);
        filter.setRegularExpression("");
        try {
            filter.evaluate(filterContext);
        } catch (EvaluationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "filtering before initialization");

        threw = true;
        try {
            filter.initialize();
        } catch (ComponentInitializationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "initialize with null regex");

        filter.setRegularExpression("r.x.");
        filter.setAttributeName(null);
        threw = true;
        try {
            filter.initialize();
        } catch (ComponentInitializationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "initialize with null attreibute");

        //
        // mismatched attribute
        //
        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        threw = false;
        try {
            filter.evaluate(filterContext);
        } catch (EvaluationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "missed attribute should throw an EvaluationException");

    }

    /**
     * test usual operation.
     * 
     * @throws EvaluationException to keep the compiler happy.
     * @throws ComponentInitializationException never
     */
    @Test public void attributeValueCriterionStringTest() throws EvaluationException, ComponentInitializationException {
        Attribute<String> attribute = new Attribute<String>(ATTR_NAME);

        attribute.setValues(Lists.newArrayList("one", "two", "three"));
        AttributeFilterContext filterContext = new AttributeFilterContext(null);

        filterContext.setPrefilteredAttributes((List) Lists.newArrayList(attribute));

        AttributeValueRegexCriterion filter = new AttributeValueRegexCriterion();
        filter.setRegularExpression("t.e");
        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        Assert.assertFalse(filter.evaluate(filterContext), "match for the string \"t.e\"");

        filter = new AttributeValueRegexCriterion();
        filter.setRegularExpression("t.*e");
        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        Assert.assertTrue(filter.evaluate(filterContext), "match for the string \"t.*e\" (against 'three')");

    }

}
