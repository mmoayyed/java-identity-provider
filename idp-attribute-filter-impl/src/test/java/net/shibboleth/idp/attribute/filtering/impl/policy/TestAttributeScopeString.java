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

/** tests for the Attribute Scope String criterion. */
public class TestAttributeScopeString {

    /** name used throughout the tests for the attribute. */
    private static final String ATTR_NAME = "attributeName";

    /**
     * Test various combinations of bad parameters.
     * 
     * @throws EvaluationException to keep the compiler happy.
     * @throws ComponentInitializationException if the only non bracketed initialize throws an error
     */
    @Test public void attributeScopeStringCriterionBadParamsTest() throws EvaluationException,
            ComponentInitializationException {
        AttributeScopeStringCriterion filter;
        AttributeFilterContext filterContext = new AttributeFilterContext(null);

        boolean threw = false;
        filter = new AttributeScopeStringCriterion();
        filter.setMatchString("match");
        filter.setAttributeName(ATTR_NAME);
        try {
            filter.evaluate(filterContext);
        } catch (EvaluationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "testing an evaluate before initialize");

        threw = false;
        try {
            filter.initialize();
        } catch (ComponentInitializationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "testing bad initialize case sensitivity not defaulted");

        filter.setCaseSensitive(true);
        filter.setMatchString("");
        threw = false;
        try {
            filter.initialize();
        } catch (ComponentInitializationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "testing bad initialize (empty match)");

        filter.setMatchString("match");
        filter.setAttributeName(null);
        threw = false;
        try {
            filter.initialize();
        } catch (ComponentInitializationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "testing bad initialize (null attribute name)");

        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        //
        // mismatched attribute
        //
        threw = false;
        try {
            filter.evaluate(filterContext);
        } catch (EvaluationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "missed attribute should return throw an exception");

    }

    /**
     * test usual operation.
     * 
     * @throws EvaluationException to keep the compiler happy.
     * @throws ComponentInitializationException never
     */
    @Test public void attributeScopeStringCriterionTest() throws EvaluationException, ComponentInitializationException {
        Attribute<Object> attribute = new Attribute<Object>(ATTR_NAME);

        attribute.setValues(Lists.newArrayList("val1", new ScopedAttributeValue("foo", "bar"),
                new ScopedAttributeValue("BAR", "foo")));
        AttributeFilterContext filterContext = new AttributeFilterContext(null);

        filterContext.setPrefilteredAttributes((List) Lists.newArrayList(attribute));

        AttributeScopeStringCriterion filter = new AttributeScopeStringCriterion();
        filter.setMatchString("FRED");
        filter.setCaseSensitive(true);
        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        Assert.assertFalse(filter.evaluate(filterContext), "match for the scope \"FRED\"");

        filter = new AttributeScopeStringCriterion();
        filter.setMatchString("bar");
        filter.setCaseSensitive(true);
        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        Assert.assertTrue(filter.evaluate(filterContext), "match for the scope \"bar\"");

        filter = new AttributeScopeStringCriterion();
        filter.setMatchString("BAR");
        filter.setCaseSensitive(true);
        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        Assert.assertFalse(filter.evaluate(filterContext), "case sensitive match for the scope \"BAR\"");

        filter = new AttributeScopeStringCriterion();
        filter.setMatchString("BAR");
        filter.setCaseSensitive(false);
        filter.setAttributeName(ATTR_NAME);
        filter.initialize();
        Assert.assertTrue(filter.evaluate(filterContext), "case sensitive match for the scope \"BAR\"");
    }

}
