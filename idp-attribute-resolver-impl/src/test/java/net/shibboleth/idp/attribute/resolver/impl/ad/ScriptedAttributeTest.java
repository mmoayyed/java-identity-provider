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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import java.util.Collection;
import java.util.Set;

import javax.script.ScriptException;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.idp.attribute.resolver.impl.TestSources;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

import org.testng.Assert;
import org.testng.annotations.Test;

/** test for {@link net.shibboleth.idp.attribute.resolver.impl.ScriptedAttribute}. */
public class ScriptedAttributeTest {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "Scripted";

    /** The language */
    private static final String SCRIPT_LANGUAGE = "JavaScript";

    /** Simple result. */
    private static final String SIMPLE_VALUE = "simple";

    /** A simple script to set a constant value. */
    private static final String TEST_SIMPLE_SCRIPT =
            "importPackage(Packages.net.shibboleth.idp.attribute.resolver.impl.ad);\n" + TEST_ATTRIBUTE_NAME
                    + " = res = new JscriptAttribute(\"" + TEST_ATTRIBUTE_NAME + "\");\n" + TEST_ATTRIBUTE_NAME
                    + ".addValue(\"" + SIMPLE_VALUE + "\");\n";

    /** A simple script to set a value based on input values. */
    private static final String TEST_ATTRIBUTES_SCRIPT =
            "importPackage(Packages.net.shibboleth.idp.attribute.resolver.impl.ad);\n" + TEST_ATTRIBUTE_NAME
                    + " = res = new JscriptAttribute(\"" + TEST_ATTRIBUTE_NAME + "\");\n" + "values = "
                    + TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR + ".iterator();\n" + "while (values.hasNext()) {\n"
                    + "  val = values.next();\n" + "  " + TEST_ATTRIBUTE_NAME + ".addValue(val);\n}\n";

    /** Something to look at the requestContext. */
    private static final String TEST_REQUEST_SCRIPT =
            "importPackage(Packages.net.shibboleth.idp.attribute.resolver.impl.ad);\n"
                    + TEST_ATTRIBUTE_NAME
                    + " = res = new JscriptAttribute(\""
                    + TEST_ATTRIBUTE_NAME
                    + "\");\n"
                    + "clazloader = requestContext.getClass().getClassLoader();\n"
                    + "claz = clazloader.loadClass(\"net.shibboleth.idp.attribute.resolver.AttributeResolutionContext\");\n"
                    + "parent = requestContext.getParent();\n" + "child = parent.getSubcontext(claz);\n"
                    + TEST_ATTRIBUTE_NAME + ".addValue(child);\n";
    
    private static final String TEST_FAIL_SCRIPT =
            "importPackage(Packages.net.shibboleth.idp.attribute.resolver.impl.ad);\n"
                    + " flibby.nonexistant();";


    /**
     * Test resolution of an simple script (statically generated data).
     * 
     * @throws AttributeResolutionException
     * @throws ComponentInitializationException only if the test will fail
     * @throws ScriptException
     */
    @Test public void testSimple() throws AttributeResolutionException, ComponentInitializationException,
            ScriptException {

        final Attribute test = new Attribute(TEST_ATTRIBUTE_NAME);

        test.getValues().add(new StringAttributeValue(SIMPLE_VALUE));

        final ScriptedAttributeDefinition attr = new ScriptedAttributeDefinition();
        Assert.assertNull(attr.getScript());
        attr.setId(TEST_ATTRIBUTE_NAME);
        attr.setScript(new EvaluableScript(SCRIPT_LANGUAGE, TEST_SIMPLE_SCRIPT));
        attr.initialize();
        Assert.assertNotNull(attr.getScript());
        
        final Attribute val = attr.doAttributeDefinitionResolve(new AttributeResolutionContext()).get();
        final Set<AttributeValue> results = val.getValues();

        Assert.assertTrue(test.equals(val), "Scripted result is the same as bases");
        Assert.assertEquals(results.size(), 1, "Scripted result value count");
        Assert.assertEquals(results.iterator().next().getValue(), SIMPLE_VALUE, "Scripted result contains known value");
    }

    @Test public void testFails() throws AttributeResolutionException, ComponentInitializationException,
            ScriptException {

        final Attribute test = new Attribute(TEST_ATTRIBUTE_NAME);

        test.getValues().add(new StringAttributeValue(SIMPLE_VALUE));

        final ScriptedAttributeDefinition attr = new ScriptedAttributeDefinition();
        attr.setId(TEST_ATTRIBUTE_NAME);
        try {
            attr.initialize();
            Assert.fail("No script defined");
        } catch (ComponentInitializationException ex) {
            // OK
        }
        
        attr.setScript(new EvaluableScript(SCRIPT_LANGUAGE, TEST_FAIL_SCRIPT));
        attr.initialize();

        try {
            attr.doAttributeDefinitionResolve(new AttributeResolutionContext());
            Assert.fail("Should have thrown an exception");
        } catch (AttributeResolutionException ex) {
            //OK
        }
    }

    /**
     * Test resolution of an script which looks at the provided attributes.
     * 
     * @throws AttributeResolutionException if the resolve fails
     * @throws ComponentInitializationException only if things go wrong
     * @throws ScriptException
     */
    @Test public void testWithAttributes() throws AttributeResolutionException, ComponentInitializationException,
            ScriptException {

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(new ResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId(TEST_ATTRIBUTE_NAME);
        scripted.setScript(new EvaluableScript(SCRIPT_LANGUAGE, TEST_ATTRIBUTES_SCRIPT));
        scripted.setDependencies(ds);
        scripted.initialize();

        // And resolve
        final Set<BaseAttributeDefinition> attrDefinitions = new LazySet<BaseAttributeDefinition>();
        attrDefinitions.add(scripted);
        attrDefinitions.add(TestSources.populatedStaticAttribute());

        final Set<BaseDataConnector> dataDefinitions = new LazySet<BaseDataConnector>();
        dataDefinitions.add(TestSources.populatedStaticConnector());

        final AttributeResolver resolver = new AttributeResolver("foo", attrDefinitions, dataDefinitions);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);
        final Attribute attribute = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME);
        final Collection<AttributeValue> values = attribute.getValues();

        Assert.assertEquals(values.size(), 2);
        for (AttributeValue value : values) {
            Assert.assertTrue(value.getValue().equals(TestSources.COMMON_ATTRIBUTE_VALUE_RESULT)
                    || value.getValue().equals(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_RESULT), "looking for value "
                    + TestSources.COMMON_ATTRIBUTE_VALUE_STRING + " or " + TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_STRING
                    + ", found:" + value.toString());
        }
    }

    /**
     * Test resolution of an script which looks at the provided request context.
     * 
     * @throws AttributeResolutionException if the resolve fails
     * @throws ComponentInitializationException only if the test has gone wrong
     * @throws ScriptException
     */
    @Test public void testRequestContext() throws AttributeResolutionException, ComponentInitializationException,
            ScriptException {

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR));

        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId(TEST_ATTRIBUTE_NAME);
        scripted.setScript(new EvaluableScript(SCRIPT_LANGUAGE, TEST_REQUEST_SCRIPT));
        scripted.setDependencies(ds);
        scripted.initialize();

        // And resolve
        final Set<BaseAttributeDefinition> attrDefinitions = new LazySet<BaseAttributeDefinition>();
        attrDefinitions.add(scripted);
        attrDefinitions.add(TestSources.populatedStaticAttribute());

        final Set<BaseDataConnector> dataDefinitions = new LazySet<BaseDataConnector>();
        dataDefinitions.add(TestSources.populatedStaticConnector());

        final AttributeResolver resolver = new AttributeResolver("foo", attrDefinitions, dataDefinitions);
        resolver.initialize();

        TestContextContainer container = new TestContextContainer();
        final AttributeResolutionContext context = new AttributeResolutionContext();
        container.addSubcontext(context);
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }

        // The script just put the resolution context in as the attribute value. Yea it makes
        // no sense but it is easy to test.
        final Attribute attribute = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME);
        final Collection values = attribute.getValues();

        Assert.assertEquals(values.size(), 1, "looking for context");
    }

}
