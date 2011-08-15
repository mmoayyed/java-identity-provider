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

import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.UnmodifiableComponentException;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;

/** Tests for the {@link ScriptedMatcher} class. */
@ThreadSafe
public class TestScriptedMatcher {

    /** Bad syntax. */
    private static final String TEST_INVALID_SCRIPT = "@";

    /** A script to always return true. */
    private static final String TEST_TRIVIAL_SCRIPT = "importPackage(Packages.java.util);\n" + "x = new HashSet();\n"
            + "x.add(\"a\");\n" + "x.add(\"b\");\n" + "x.add(\"c\");\n" + "x;\n";

    /** A script to always return true. */
    private static final String TEST_COMPLEX_SCRIPT = "importPackage(Packages.java.util);\n"
            + "var x = new HashSet();\n" + "var i = 0;\n" + "y = attribute.getValues().iterator();\n"
            + "while (y.hasNext()) {\n" + "  var v = y.next();\n" + "  if (i&1) x.add(v);\n" + "  i++;\n" + " }\n"
            + "x;\n";

    /** The language that all (valid language) scripts are in : JavaScript. */
    private static final String TEST_SCRIPT_LANGUAGE = "JavaScript";

    /**
     * Tests for {@link ScriptedMatcher}. <br />
     * Invalid language and script <br />
     * Trivial function. <br />
     * Function to return data from the provided input.
     * 
     * @throws AttributeFilteringException if the resolution fails when it shouldn't.
     * @throws ComponentInitializationException never.
     */
    @Test
    public void scriptedMatcherTest() throws AttributeFilteringException, ComponentInitializationException {
        boolean threw = false;
        ScriptedMatcher matcher;
        
        try {
            matcher = new ScriptedMatcher();
            matcher.setScript(TEST_INVALID_SCRIPT);
            matcher.setLanguage("");
            matcher.initialize();
            Assert.assertTrue(false, "unreachable code path");
        } catch (ComponentInitializationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "empty language should throw an initialization error");

        threw = false;
        try {
            matcher = new ScriptedMatcher();
            matcher.setScript(TEST_INVALID_SCRIPT);
            matcher.setLanguage("imp");
            matcher.initialize();
            Assert.assertTrue(false, "unreachable code path");
        } catch (ComponentInitializationException  e) {
            threw = true;
        }
        Assert.assertTrue(threw, "invalid language should throw a initialization error");

        threw = false;
        try {
            matcher = new ScriptedMatcher();
            matcher.setScript(TEST_INVALID_SCRIPT);
            matcher.setLanguage(TEST_SCRIPT_LANGUAGE);
            matcher.initialize();
            matcher.setLanguage(TEST_SCRIPT_LANGUAGE);
            Assert.assertTrue(false, "unreachable code path");
        } catch (UnmodifiableComponentException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "invalid language should throw a UnmodifiableComponentException");
        
        matcher = new ScriptedMatcher();
        matcher.setLanguage(TEST_SCRIPT_LANGUAGE);
        matcher.setScript(TEST_INVALID_SCRIPT);
        matcher.initialize();
        Assert.assertNull(matcher.getCompiledScript(), "Invalid script should compile to null");

        threw = false;
        try {
            matcher.initialize();
            Assert.assertTrue(false, "unreachable code path");
        } catch (ComponentInitializationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "double initialize should throw a UnmodifiableComponentException");
        
        threw = false;
        try {
            matcher.getMatchingValues(null, null);
            Assert.assertTrue(false, "unreachable code path");
        } catch (AttributeFilteringException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "bad syntax should throw a resolution error");

        matcher = new ScriptedMatcher();
        matcher.setLanguage(TEST_SCRIPT_LANGUAGE);
        matcher.setScript(TEST_TRIVIAL_SCRIPT);
        matcher.initialize();

        Collection result = matcher.getMatchingValues(null, null);
        Assert.assertEquals(result.size(), 3, "Result of trivial Script");
        Assert.assertTrue(result.contains("a") && result.contains("b") && result.contains("c"),
                "Result of trivial script");

        Attribute<String> attribute = new Attribute<String>("attribute");
        attribute.setValues(CollectionSupport.toList("zero", "one", "two", "three", "four", "five"));

        matcher = new ScriptedMatcher();
        matcher.setLanguage(TEST_SCRIPT_LANGUAGE);
        matcher.setScript(TEST_COMPLEX_SCRIPT);
        matcher.initialize();

        result = matcher.getMatchingValues(attribute, null);
        Assert.assertEquals(result.size(), 3, "Result of complex Script");
        Assert.assertTrue(result.contains("one") && result.contains("three") && result.contains("five"),
                "Result of complex script");
    }
}
