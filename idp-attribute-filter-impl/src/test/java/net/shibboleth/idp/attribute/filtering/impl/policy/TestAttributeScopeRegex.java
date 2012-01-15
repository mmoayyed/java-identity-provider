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
import net.shibboleth.idp.attribute.ScopedAttributeValue;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.util.criteria.EvaluationException;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/** tests for the Attribute Scope Regex criterion. */
public class TestAttributeScopeRegex {

    /** name used throughout the tests for the attribute. */
    private static final String ATTR_NAME = "attributeName";

    /**
     * Test various combinations of bad parameters.
     * 
     * @throws EvaluationException to keep the compiler happy.
     * @throws ComponentInitializationException never
     */
    @Test public void attributeScopeRegexCriterionBadParamsTest() throws EvaluationException,
            ComponentInitializationException {
        boolean threw = false;
        try {
            new AttributeScopeRegexCriterion().initialize();
        } catch (ComponentInitializationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "testing bad constructor (empty match): expected an exception");

        threw = false;
        AttributeScopeRegexCriterion filter = new AttributeScopeRegexCriterion();
        try {
            filter.setAttributeName("r.e");
            filter.initialize();
        } catch (ComponentInitializationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "testing bad constructor (null attribute name): expected an exception");
        //
        // mismatched attribute
        //
        AttributeFilterContext filterContext = new AttributeFilterContext(null);
        filter = new AttributeScopeRegexCriterion();
        filter.setAttributeName(ATTR_NAME);
        filter.setRegularExpression("r.e");

        threw = false;
        try {
            filter.evaluate(filterContext);
        } catch (EvaluationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "missed initialize should throw");

        filter.initialize();
        threw = false;
        try {
            filter.evaluate(filterContext);
        } catch (EvaluationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "missed attribute should throw");
    }

    /**
     * test usual operation.
     * 
     * @throws EvaluationException to keep the compiler happy.
     * @throws ComponentInitializationException never
     */
    @Test public void attributeScopeStringCriterionTest() throws EvaluationException, ComponentInitializationException {
        Attribute<Object> attribute = new Attribute<Object>(ATTR_NAME);

        // Attribute values "foo", "foo@bar", "BAR@three".
        // should not match foo
        // should not match t.e
        // should match t.* e
        attribute.setValues(Lists.newArrayList("foo", new ScopedAttributeValue("foo", "two"), new ScopedAttributeValue(
                "BAR", "three")));
        AttributeFilterContext filterContext = new AttributeFilterContext(null);

        filterContext.setPrefilteredAttributes((List) Lists.newArrayList(attribute));

        AttributeScopeRegexCriterion filter = new AttributeScopeRegexCriterion();
        filter.setRegularExpression("foo");
        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        Assert.assertFalse(filter.evaluate(filterContext), "match for the scope regex \"foo\"");

        filter = new AttributeScopeRegexCriterion();
        filter.setRegularExpression("t.e");
        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        Assert.assertFalse(filter.evaluate(filterContext), "match for the scope \"t.e\"");

        filter = new AttributeScopeRegexCriterion();
        filter.setRegularExpression("t.*e");
        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        Assert.assertTrue(filter.evaluate(filterContext), "match for the scope \"t.*e\" (against 'three')");

    }

}
