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

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Collection;
import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

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
    private static final String TEST_SIMPLE_SCRIPT = "importPackage(Packages.net.shibboleth.idp.attribute);\n"
            + TEST_ATTRIBUTE_NAME + " = res = new Attribute(\"" + TEST_ATTRIBUTE_NAME + "\");\n" + TEST_ATTRIBUTE_NAME
            + ".addValue(\"" + SIMPLE_VALUE + "\");\n";

    /** A simple script to set a value based on input values. */
    private static final String TEST_ATTRIBUTES_SCRIPT = "importPackage(Packages.net.shibboleth.idp.attribute);\n"
            + TEST_ATTRIBUTE_NAME + " = res = new Attribute(\"" + TEST_ATTRIBUTE_NAME + "\");\n" + "values = "
            + TestSources.DEPENDS_ON_ATTRIBUTE_NAME + ".getValues().iterator();\n" + "while (values.hasNext()) {\n"
            + "  val = values.next();\n" + "  " + TEST_ATTRIBUTE_NAME + ".addValue(val);\n}\n";

    /** Something to look at the requestContext. */
    private static final String TEST_REQUEST_SCRIPT = "importPackage(Packages.net.shibboleth.idp.attribute);\n"
            + TEST_ATTRIBUTE_NAME + " = res = new Attribute(\"" + TEST_ATTRIBUTE_NAME + "\");\n"
            + "clazloader = requestContext.getClass().getClassLoader();\n"
            + "claz = clazloader.loadClass(\"net.shibboleth.idp.attribute.resolver.AttributeResolutionContext\");\n"
            + "parent = requestContext.getOwner();\n" + "child = parent.getSubcontext(claz);\n" + TEST_ATTRIBUTE_NAME
            + ".addValue(child);\n";

    /**
     * Test Invalid syntax.
     * 
     * @throws ComponentInitializationException only if bad things happens
     */
    @Test public void testInvalid() throws ComponentInitializationException {

        boolean threw = false;

        final ScriptedAttributeDefinition attr = new ScriptedAttributeDefinition();
        attr.setId(TEST_ATTRIBUTE_NAME);
        attr.setScriptLanguage("COBOL");
        attr.setScript(TEST_SIMPLE_SCRIPT);
        try {
            attr.initialize();
        } catch (ComponentInitializationException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "invalid language threw a initialization  error");

        threw = false;
        attr.setScriptLanguage(SCRIPT_LANGUAGE);
        attr.setScript("badSyntox.");
        attr.initialize();
        try {
            attr.doAttributeResolution(new AttributeResolutionContext());
        } catch (AttributeResolutionException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "bad syntax threw a initialization error");

    }

    /**
     * Test resolution of an simple script (statically generated data).
     * 
     * @throws AttributeResolutionException
     * @throws ComponentInitializationException only if the test will fail
     */
    @Test public void testSimple() throws AttributeResolutionException, ComponentInitializationException {

        final Attribute test = new Attribute(TEST_ATTRIBUTE_NAME);

        test.addValue(SIMPLE_VALUE);

        final ScriptedAttributeDefinition attr = new ScriptedAttributeDefinition();
        attr.setId(TEST_ATTRIBUTE_NAME);
        attr.setScriptLanguage(SCRIPT_LANGUAGE);
        attr.setScript(TEST_SIMPLE_SCRIPT);
        attr.initialize();

        final Attribute val = attr.doAttributeResolution(new AttributeResolutionContext());
        final Collection<?> results = val.getValues();

        Assert.assertTrue(test.equals(val), "Scripted result is the same as bases");
        Assert.assertEquals(results.size(), 1, "Scripted result value count");
        Assert.assertTrue(results.contains(SIMPLE_VALUE), "Scripted result contains known value");
    }

    /**
     * Test resolution of an script which looks at the provided attributes.
     * 
     * @throws AttributeResolutionException if the resolve fails
     * @throws ComponentInitializationException only if things go wrong
     */
    @Test public void testWithAttributes() throws AttributeResolutionException, ComponentInitializationException {

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(new ResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId(TEST_ATTRIBUTE_NAME);
        scripted.setScriptLanguage(SCRIPT_LANGUAGE);
        scripted.setScript(TEST_ATTRIBUTES_SCRIPT);
        scripted.setDependencies(ds);
        scripted.initialize();

        // And resolve
        final Set<BaseAttributeDefinition> attrDefinitions = new LazySet<BaseAttributeDefinition>();
        attrDefinitions.add(scripted);
        attrDefinitions.add(TestSources.populatedStaticAttribute());

        final Set<BaseDataConnector> dataDefinitions = new LazySet<BaseDataConnector>();
        dataDefinitions.add(TestSources.populatedStaticConnectior());

        final AttributeResolver resolver = new AttributeResolver();
        resolver.setId("foo");
        resolver.setDataConnectors(dataDefinitions);
        resolver.setAttributeDefinition(attrDefinitions);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }
        final Attribute attribute = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME);
        final Collection values = attribute.getValues();

        Assert.assertEquals(values.size(), 2);
        Assert.assertTrue(values.contains(TestSources.COMMON_ATTRIBUTE_VALUE),
                "looking for value COMMON_ATTRIBUTE_VALUE");
        Assert.assertTrue(values.contains(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE),
                "looking for value ATTRIBUTE_ATTRIBUTE_VALUE");
    }

    /**
     * Test resolution of an script which looks at the provided request context.
     * 
     * @throws AttributeResolutionException if the resolve fails
     * @throws ComponentInitializationException only if the test has gone wrong
     */
    @Test public void testRequestContext() throws AttributeResolutionException, ComponentInitializationException {

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME));

        final ScriptedAttributeDefinition scripted = new ScriptedAttributeDefinition();
        scripted.setId(TEST_ATTRIBUTE_NAME);
        scripted.setScriptLanguage(SCRIPT_LANGUAGE);
        scripted.setScript(TEST_REQUEST_SCRIPT);
        scripted.setDependencies(ds);
        scripted.initialize();

        // And resolve
        final Set<BaseAttributeDefinition> attrDefinitions = new LazySet<BaseAttributeDefinition>();
        attrDefinitions.add(scripted);
        attrDefinitions.add(TestSources.populatedStaticAttribute());

        final Set<BaseDataConnector> dataDefinitions = new LazySet<BaseDataConnector>();
        dataDefinitions.add(TestSources.populatedStaticConnectior());

        final AttributeResolver resolver = new AttributeResolver();
        resolver.setId("foo");
        resolver.setDataConnectors(dataDefinitions);
        resolver.setAttributeDefinition(attrDefinitions);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }

        // The script just put the resolution context in as the attribute value. Yea it makes
        // no sense but it is easy to test.
        final Attribute attribute = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME);
        final Collection values = attribute.getValues();

        Assert.assertTrue(values.contains(context), "looking for context");
    }

}
