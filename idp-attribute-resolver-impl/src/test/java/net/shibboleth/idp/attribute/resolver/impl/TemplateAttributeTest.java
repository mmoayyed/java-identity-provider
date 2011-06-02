/*
 * Licensed to the University Corporation for Advanced Internet Development, Inc.
 * under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache 
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
import java.util.List;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;

import org.apache.velocity.app.VelocityEngine;
import org.opensaml.util.collections.LazySet;
import org.opensaml.xml.util.LazyList;
import org.testng.Assert;
import org.testng.annotations.Test;

/** test for {@link net.shibboleth.idp.attribute.resolver.impl.TemplateAttribute}. */
@ThreadSafe
public class TemplateAttributeTest {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "TEMPLATE";

    /** Simple result. */
    private static final String SIMPLE_VALUE = "simple";

    /** A simple script to set a constant value. */
    private static final String TEST_SIMPLE_TEMPLATE = SIMPLE_VALUE;

    /** A simple script to set a value based on input values. */
    private static final String TEST_ATTRIBUTES_TEMPLATE = "Att " + "${" + TestSources.DEPENDS_ON_ATTRIBUTE_NAME + "}-"
            + "${" + TestSources.DEPENDS_ON_SECOND_ATTRIBUTE_NAME + "}";

    /** Our singleton engine. */
    private static VelocityEngine engineSingleton;

    /**
     * Create new or return the velocity engine with enough hardwired properties to get us going.
     * 
     * @return a new engine suitable groomed
     */
    private VelocityEngine getEngine() {
        if (null == engineSingleton) {
            engineSingleton = new VelocityEngine();
            try {
                engineSingleton.addProperty("string.resource.loader.class",
                        "org.apache.velocity.runtime.resource.loader.StringResourceLoader");
                engineSingleton.addProperty("classpath.resource.loader.class",
                        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
                engineSingleton.addProperty("resource.loader", "classpath, string");
                engineSingleton.init();
            } catch (Exception e) {
                Assert.fail("couldn't create engine", e);
            }
        }
        return engineSingleton;
    }

    /**
     * Test resolution of an template script (statically generated data).
     * 
     * @throws AttributeResolutionException id resolution fails
     */
    @Test
    public void testSimple() throws AttributeResolutionException {

        TemplateAttributeDefinition attr =
                new TemplateAttributeDefinition(TEST_ATTRIBUTE_NAME, getEngine(), TEST_ATTRIBUTES_TEMPLATE,
                        new LazyList<String>());

        Attribute<?> val = attr.doAttributeResolution(new AttributeResolutionContext(null));
        Collection<?> results = val.getValues();

        Assert.assertEquals(results.size(), 0, "Templated value count");
    }

    /**
     * Test resolution of an template script (statically generated data). By giving it attributes we create some values.
     * 
     * @throws AttributeResolutionException if resolution fails
     */
    @Test
    public void testSimpleWithValues() throws AttributeResolutionException {

        List<String> sources = new LazyList<String>();
        sources.add(TestSources.DEPENDS_ON_ATTRIBUTE_NAME);

        TemplateAttributeDefinition templateDef =
                new TemplateAttributeDefinition(TEST_ATTRIBUTE_NAME, getEngine(), TEST_SIMPLE_TEMPLATE, sources);
        Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(new ResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        templateDef.setDependencies(ds);

        AttributeResolver resolver = new AttributeResolver("foo");

        Set<BaseAttributeDefinition> attrDefinitions = new LazySet<BaseAttributeDefinition>();
        attrDefinitions.add(templateDef);
        attrDefinitions.add(TestSources.populatedStaticAttribute());
        Set<BaseDataConnector> dataDefinitions = new LazySet<BaseDataConnector>();
        dataDefinitions.add(TestSources.populatedStaticConnectior());
        resolver.setDataConnectors(dataDefinitions);
        resolver.setAttributeDefinition(attrDefinitions);

        AttributeResolutionContext context = new AttributeResolutionContext(null);
        resolver.resolveAttributes(context);

        Attribute<?> a = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME);
        Collection results = a.getValues();
        Assert.assertEquals(results.size(), 1, "Templated value count");
        Assert.assertTrue(results.contains(SIMPLE_VALUE), "Single value context is correct");

    }

    /**
     * Test resolution of an template script with data generated from the attributes.
     * 
     * @throws AttributeResolutionException if it goes wrong.
     */
    @Test
    public void testTemplateWithValues() throws AttributeResolutionException {

        List<String> sources = new LazyList<String>();
        sources.add(TestSources.DEPENDS_ON_ATTRIBUTE_NAME);
        sources.add(TestSources.DEPENDS_ON_SECOND_ATTRIBUTE_NAME);

        TemplateAttributeDefinition templateDef =
                new TemplateAttributeDefinition(TEST_ATTRIBUTE_NAME, getEngine(), TEST_ATTRIBUTES_TEMPLATE, sources);
        Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(new ResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        ds.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_SECOND_ATTRIBUTE_NAME));
        templateDef.setDependencies(ds);

        AttributeResolver resolver = new AttributeResolver("foo");

        Set<BaseAttributeDefinition> attrDefinitions = new LazySet<BaseAttributeDefinition>();
        attrDefinitions.add(templateDef);
        attrDefinitions.add(TestSources.populatedStaticAttribute());
        Set<BaseDataConnector> dataDefinitions = new LazySet<BaseDataConnector>();
        dataDefinitions.add(TestSources.populatedStaticConnectior());
        resolver.setDataConnectors(dataDefinitions);
        resolver.setAttributeDefinition(attrDefinitions);

        AttributeResolutionContext context = new AttributeResolutionContext(null);
        resolver.resolveAttributes(context);

        Attribute<?> a = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME);
        Collection results = a.getValues();
        Assert.assertEquals(results.size(), 2, "Templated value count");
        String s = "Att " + TestSources.COMMON_ATTRIBUTE_VALUE + "-" + TestSources.SECOND_ATTRIBUTE_VALUES[0];
        Assert.assertTrue(results.contains(s), "First Match");
        s = "Att " + TestSources.ATTRIBUTE_ATTRIBUTE_VALUE + "-" + TestSources.SECOND_ATTRIBUTE_VALUES[1];
        Assert.assertTrue(results.contains(s), "Second Match");
    }

}
