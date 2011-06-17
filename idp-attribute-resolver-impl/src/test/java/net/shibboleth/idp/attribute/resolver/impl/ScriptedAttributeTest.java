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

import org.opensaml.messaging.context.impl.AbstractSubcontextContainer;
import org.opensaml.util.collections.LazySet;
import org.testng.Assert;
import org.testng.annotations.Test;

/** test for {@link net.shibboleth.idp.attribute.resolver.impl.ScriptedAttribute}. */
public class ScriptedAttributeTest {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "Scripted";
    
    /** Simple result. */
    private static final String SIMPLE_VALUE = "simple";

    /** A simple script to set a constant value. */
    private static final String TEST_SIMPLE_SCRIPT = "importPackage(Packages.net.shibboleth.idp.attribute);\n" +
       TEST_ATTRIBUTE_NAME + " = res = new Attribute(\"" + TEST_ATTRIBUTE_NAME + "\");\n" +
       TEST_ATTRIBUTE_NAME + ".addValue(\"" + SIMPLE_VALUE + "\");\n";
    
    /** A simple script to set a value based on input values. */
    private static final String TEST_ATTRIBUTES_SCRIPT = "importPackage(Packages.net.shibboleth.idp.attribute);\n" +
       TEST_ATTRIBUTE_NAME + " = res = new Attribute(\"" + TEST_ATTRIBUTE_NAME + "\");\n" +
       "values = " + TestSources.DEPENDS_ON_ATTRIBUTE_NAME + ".getValues().iterator();\n" +
       "while (values.hasNext()) {\n" +
       "  val = values.next();\n" +
       "  " + TEST_ATTRIBUTE_NAME + ".addValue(val);\n}\n";

    /** Something to look at the requestContext. */
    private static final String TEST_REQUEST_SCRIPT = "importPackage(Packages.net.shibboleth.idp.attribute);\n" +
       TEST_ATTRIBUTE_NAME + " = res = new Attribute(\"" + TEST_ATTRIBUTE_NAME + "\");\n" +
       "clazloader = requestContext.getClass().getClassLoader();\n" +
       "claz = clazloader.loadClass(\"net.shibboleth.idp.attribute.resolver.AttributeResolutionContext\");\n" +
       "parent = requestContext.getOwner();\n" +
       "child = parent.getSubcontext(claz);\n" +
       TEST_ATTRIBUTE_NAME + ".addValue(child);\n";
    
    /**
     * Test Invalid syntax.
     */
    @Test
    public void testInvalid() {
        
        boolean threw = false;
        
        ScriptedAttributeDefinition attr = 
            new ScriptedAttributeDefinition(TEST_ATTRIBUTE_NAME, "JavaScript", "badSyntox.");
        try {
            Attribute<?> val = attr.doAttributeResolution(new AttributeResolutionContext(null));
            //
            // The following lines should never get hit, bu
            Assert.assertNull(val, "unreachable code path");
        } catch (AttributeResolutionException e) {
            threw = true;
        }
        Assert.assertTrue(threw, "invalid syntax threw a resolution error");
    }
    
    /**
     * Test resolution of an simple script (statically generated data).
     * @throws AttributeResolutionException 
     */
    @Test
    public void testSimple() throws AttributeResolutionException {
        
        Attribute<String> test = new Attribute<String>(TEST_ATTRIBUTE_NAME);
        
        test.addValue(SIMPLE_VALUE);
        
        ScriptedAttributeDefinition attr = 
            new ScriptedAttributeDefinition(TEST_ATTRIBUTE_NAME, "JavaScript", TEST_SIMPLE_SCRIPT);
        
        Attribute<?> val = attr.doAttributeResolution(new AttributeResolutionContext(null));
        Collection<?> results = val.getValues();
        
        Assert.assertTrue(test.equals(val), "Scripted result is the same as bases");
        Assert.assertEquals(results.size(), 1, "Scripted result value count");
        Assert.assertTrue(results.contains(SIMPLE_VALUE), "Scripted result contains known value");
    }
        
    
    /** Test resolution of an script which looks at the provided attributes. 
     * @throws AttributeResolutionException if the resolve fails
     */
    @Test
    public void testWithAttributes() throws AttributeResolutionException {
        ScriptedAttributeDefinition scripted = 
            new ScriptedAttributeDefinition(TEST_ATTRIBUTE_NAME, "JavaScript", TEST_ATTRIBUTES_SCRIPT);
        //
        // Set the dependency on the data connector
        //
        Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(new ResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        scripted.setDependencies(ds);

        //
        // And resolve
        //
        AttributeResolver resolver = new AttributeResolver("foo");

        Set<BaseAttributeDefinition> attrDefinitions = new LazySet<BaseAttributeDefinition>();
        attrDefinitions.add(scripted);
        attrDefinitions.add(TestSources.populatedStaticAttribute());
        Set<BaseDataConnector> dataDefinitions = new LazySet<BaseDataConnector>();
        dataDefinitions.add(TestSources.populatedStaticConnectior());
        resolver.setDataConnectors(dataDefinitions);
        resolver.setAttributeDefinition(attrDefinitions);

        AttributeResolutionContext context = new AttributeResolutionContext(null);
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }
        Attribute<?> a = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME); 
        Collection f = a.getValues();
        
        Assert.assertEquals(f.size(), 2);
        Assert.assertTrue(f.contains(TestSources.COMMON_ATTRIBUTE_VALUE), "looking for value COMMON_ATTRIBUTE_VALUE");
        Assert.assertTrue(f.contains(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE),
                "looking for value ATTRIBUTE_ATTRIBUTE_VALUE");
    }

    /** Test resolution of an script which looks at the provided request context. 
     * @throws AttributeResolutionException if the resolve fails
     */
    @Test
    public void testRequestContext() throws AttributeResolutionException {

        ScriptedAttributeDefinition scripted = 
            new ScriptedAttributeDefinition(TEST_ATTRIBUTE_NAME, "JavaScript", TEST_REQUEST_SCRIPT);
        //
        // Set the dependency on the data connector
        //
        Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        scripted.setDependencies(ds);

        //
        // And resolve
        //
        AttributeResolver resolver = new AttributeResolver("foo");

        Set<BaseAttributeDefinition> attrDefinitions = new LazySet<BaseAttributeDefinition>();
        attrDefinitions.add(scripted);
        attrDefinitions.add(TestSources.populatedStaticAttribute());
        Set<BaseDataConnector> dataDefinitions = new LazySet<BaseDataConnector>();
        dataDefinitions.add(TestSources.populatedStaticConnectior());
        resolver.setDataConnectors(dataDefinitions);
        resolver.setAttributeDefinition(attrDefinitions);

        AttributeResolutionContext context = new AttributeResolutionContext(new TestContextContainer());
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }
        //
        // The script just put the resolution context in as the attribute value.  Yea it makes 
        // no sense but it is easy to test.
        //
        Attribute<?> a = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME); 
        Collection f = a.getValues();
        
        Assert.assertTrue(f.contains(context), "looking for context");
    }

    /** trivial context container to test the get something from a container. */
    class TestContextContainer extends AbstractSubcontextContainer {
        /** constructor. */
        public TestContextContainer() {
            super();
            //
            // Do not auto create subcontexts.
            //
            setAutoCreateSubcontexts(false);
        }
    }
}
