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

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;

import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.criteria.EvaluationException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link ScriptedCriterion}.
 */
@ThreadSafe
public class TestScripted {

    /** A known value. */
    private static final String KNOWN_VALUE = "value";

    /** Attribute Name. */
    private static final String ATTRIBUTE_NAME = "attribute";

    /** Bad syntax. */
    private static final String TEST_INVALID_SCRIPT = "@";

    /** A script to always return true. */
    private static final String TEST_TRIVIAL_SCRIPT = "true;";

    /** A Script to inspect some parameters and return a value. */
    private static final String TEST_SCRIPT_PARAMETERS = "attributes = filterContext.getPrefilteredAttributes();"
            + "attribute = attributes.get(\"" + ATTRIBUTE_NAME + "\");" + "values = attribute.getValues();"
            + "values.contains(\"" + KNOWN_VALUE + "\");";

    /** The language that all (valid language) scripts are in : JavaScript. */
    private static final String TEST_SCRIPT_LANGUAGE = "JavaScript";

    /**
     * tests for {@link ScriptedCriterion}.
     * 
     * Null script <br />
     * Invalid script <br />
     * Trivial Script (return true) <br />
     * script to access parameters.
     * 
     * @throws EvaluationException to keep the compiler happy.
     */
    @Test
    public void scriptedCriterionTest() throws EvaluationException {

        boolean threw = false;

        try {
            new ScriptedCriterion("", TEST_INVALID_SCRIPT);
            Assert.assertTrue(false, "unreachable code path");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "empty language should throw a resolution error");

        threw = false;
        try {
            new ScriptedCriterion("imp", TEST_INVALID_SCRIPT);
            Assert.assertTrue(false, "unreachable code path");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "invalid language should throw a resolution error");

        ScriptedCriterion criterion = new ScriptedCriterion(TEST_SCRIPT_LANGUAGE, TEST_INVALID_SCRIPT);
        Assert.assertNull(criterion.getCompiledScript(), "Invalid script should compile to null");

        threw = false;
        try {
            criterion.doEvaluate(null);
            Assert.assertTrue(false, "unreachable code path");
        } catch (EvaluationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "bad syntax should throw a resolution error");

        criterion = new ScriptedCriterion(TEST_SCRIPT_LANGUAGE, TEST_TRIVIAL_SCRIPT);
        Assert.assertTrue(criterion.evaluate(null), "Trvial script");

        Attribute<String> attribute = new Attribute<String>("attribute");

        attribute.setValues(CollectionSupport.toSet("val1", KNOWN_VALUE, "VAL3"));
        AttributeFilterContext filterContext = new AttributeFilterContext(null);

        filterContext.setPrefilteredAttributes((Set) CollectionSupport.toSet(attribute));

        criterion = new ScriptedCriterion(TEST_SCRIPT_LANGUAGE, TEST_SCRIPT_PARAMETERS);
        Assert.assertTrue(criterion.evaluate(filterContext), "Parameter script");

    }

}
