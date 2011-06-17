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

import org.opensaml.util.collections.LazySet;
import org.testng.Assert;
import org.testng.annotations.Test;

/** test for {@link net.shibboleth.idp.attribute.resolver.impl.SimpleAttribute}. */
public class SimpleAttributeTest {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "simple";

    /**
     * Test resolution of an empty definition to nothing.
     * 
     * @throws AttributeResolutionException if resolution failed.
     */
    @Test
    public void testEmpty() throws AttributeResolutionException {
        SimpleAttributeDefinition simple = new SimpleAttributeDefinition(TEST_ATTRIBUTE_NAME);

        Attribute<?> result = simple.doAttributeResolution(null);

        Assert.assertNotNull(result.getValues());
    }

    /** Test when dependent on a data connector. */
    @Test
    public void testDataConnector() {

        SimpleAttributeDefinition simple = new SimpleAttributeDefinition(TEST_ATTRIBUTE_NAME);
        //
        // Set the dependency on the data connector
        //
        Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        simple.setDependencies(dependencySet);

        //
        // And resolve
        //
        AttributeResolver resolver = new AttributeResolver("foo");
        Set<BaseDataConnector> connectorSet = new LazySet<BaseDataConnector>();
        connectorSet.add(TestSources.populatedStaticConnectior());

        Set<BaseAttributeDefinition> attributeSet = new LazySet<BaseAttributeDefinition>();
        attributeSet.add(simple);
        resolver.setDataConnectors(connectorSet);
        resolver.setAttributeDefinition(attributeSet);

        AttributeResolutionContext context = new AttributeResolutionContext(null);
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }
        Collection f = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(f.size(), 2);
        Assert.assertTrue(f.contains(TestSources.COMMON_ATTRIBUTE_VALUE), "looking for COMMON_ATTRIBUTE_VALUE");
        Assert.assertTrue(f.contains(TestSources.CONNECTOR_ATTRIBUTE_VALUE), "looking for CONNECTOR_ATTRIBUTE_VALUE");
    }

    /** Test when dependent on another attribute. */
    @Test
    public void testAttribute() {

        SimpleAttributeDefinition simple = new SimpleAttributeDefinition(TEST_ATTRIBUTE_NAME);
        //
        // Set the dependency on the data connector
        //
        Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(new ResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        simple.setDependencies(ds);

        //
        // And resolve
        //
        AttributeResolver resolver = new AttributeResolver("foo");

        Set<BaseAttributeDefinition> am = new LazySet<BaseAttributeDefinition>();
        am.add(simple);
        am.add(TestSources.populatedStaticAttribute());
        resolver.setAttributeDefinition(am);

        AttributeResolutionContext context = new AttributeResolutionContext(null);
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }
        Collection f = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(f.size(), 2);
        Assert.assertTrue(f.contains(TestSources.COMMON_ATTRIBUTE_VALUE), "looking for value COMMON_ATTRIBUTE_VALUE");
        Assert.assertTrue(f.contains(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE),
                "looking for value ATTRIBUTE_ATTRIBUTE_VALUE");
    }

    /** Test when dependent on a data connector and another attribute. */
    @Test
    public void testBoth() {

        SimpleAttributeDefinition simple = new SimpleAttributeDefinition(TEST_ATTRIBUTE_NAME);
        //
        // Set the dependency on the data connector
        //
        Set<ResolverPluginDependency> ds = new LazySet<ResolverPluginDependency>();
        ds.add(new ResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        ds.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        simple.setDependencies(ds);

        //
        // And resolve
        //
        AttributeResolver resolver = new AttributeResolver("foo");

        Set<BaseAttributeDefinition> attrDefinitions = new LazySet<BaseAttributeDefinition>();
        attrDefinitions.add(simple);
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
        Collection f = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(f.size(), 3);
        Assert.assertTrue(f.contains(TestSources.COMMON_ATTRIBUTE_VALUE), "looking for value COMMON_ATTRIBUTE_VALUE");
        Assert.assertTrue(f.contains(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE),
                "looking for value ATTRIBUTE_ATTRIBUTE_VALUE");
        Assert.assertTrue(f.contains(TestSources.CONNECTOR_ATTRIBUTE_VALUE),
                "looking for value CONNECTOR_ATTRIBUTE_VALUE");
    }
}
