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

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.opensaml.util.criteria.EvaluationException;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

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
     * @throws ComponentInitializationException e
     */
    @Test public void scriptedCriterionTest() throws EvaluationException, ComponentInitializationException {

        AttributeFilterContext filterContext = new AttributeFilterContext(null);
        ScriptedCriterion criterion;
        boolean threw = false;

        criterion = new ScriptedCriterion();
        try {
            criterion.evaluate(filterContext);
        } catch (EvaluationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "evaluate of an unitialized should throw");

        criterion.setScript(TEST_INVALID_SCRIPT);
        threw = false;
        try {
            criterion.initialize();
        } catch (ComponentInitializationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "empty language should throw an initialization exception");

        criterion.setLanguage("imp77");
        threw = false;
        try {
            criterion.initialize();
        } catch (ComponentInitializationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "invalid language should throw an initialization");

        criterion.setLanguage(TEST_SCRIPT_LANGUAGE);
        criterion.initialize();

        Assert.assertNull(criterion.getCompiledScript(), "Invalid script should compile to null");

        threw = false;
        try {
            criterion.doEvaluate(null);
        } catch (EvaluationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "bad syntax should throw a resolution error");

        threw = false;
        try {
            criterion.setLanguage("fortran4");
        } catch (UnmodifiableComponentException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "post initialize setting should fail");

        threw = false;
        try {
            criterion.setScript("fortran4");
        } catch (UnmodifiableComponentException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "post initialize setting should fail");

        criterion = new ScriptedCriterion();
        criterion.setLanguage(TEST_SCRIPT_LANGUAGE);
        criterion.setScript(TEST_TRIVIAL_SCRIPT);
        criterion.initialize();
        Assert.assertTrue(criterion.evaluate(null), "Trvial script");

        Attribute<String> attribute = new Attribute<String>("attribute");

        attribute.setValues(Lists.newArrayList("val1", KNOWN_VALUE, "VAL3"));

        filterContext.setPrefilteredAttributes((List) Lists.newArrayList(attribute));

        criterion = new ScriptedCriterion();
        criterion.setLanguage(TEST_SCRIPT_LANGUAGE);
        criterion.setScript(TEST_SCRIPT_PARAMETERS);
        criterion.initialize();
        Assert.assertTrue(criterion.evaluate(filterContext), "Parameter script");

    }

}
