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

/** test for {@link net.shibboleth.idp.attribute.resolver.impl.SimpleAttribute}. */
public class SimpleAttributeTest {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "simple";

    /**
     * Test resolution of an empty definition to nothing.
     * 
     * @throws AttributeResolutionException if resolution failed.
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     */
    @Test public void testEmpty() throws AttributeResolutionException, ComponentInitializationException {
        final SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(TEST_ATTRIBUTE_NAME);
        simple.initialize();

        final Attribute result = simple.doAttributeResolution();

        Assert.assertNotNull(result.getValues());
    }

    /**
     * Test when dependent on a data connector.
     * 
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     */
    @Test public void testDataConnector() throws ComponentInitializationException {

        // Set the dependency on the data connector
        final SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(TEST_ATTRIBUTE_NAME);

        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        simple.setDependencies(dependencySet);
        simple.initialize();

        // And resolve
        final Set<BaseDataConnector> connectorSet = new LazySet<BaseDataConnector>();
        connectorSet.add(TestSources.populatedStaticConnectior());

        final Set<BaseAttributeDefinition> attributeSet = new LazySet<BaseAttributeDefinition>();
        attributeSet.add(simple);

        final AttributeResolver resolver = new AttributeResolver();
        resolver.setId("foo");

        resolver.setDataConnectors(connectorSet);
        resolver.setAttributeDefinition(attributeSet);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }

        final Collection values = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();
        Assert.assertEquals(values.size(), 2);
        Assert.assertTrue(values.contains(TestSources.COMMON_ATTRIBUTE_VALUE), "looking for COMMON_ATTRIBUTE_VALUE");
        Assert.assertTrue(values.contains(TestSources.CONNECTOR_ATTRIBUTE_VALUE),
                "looking for CONNECTOR_ATTRIBUTE_VALUE");
    }

    /**
     * Test when dependent on another attribute.
     * 
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     */
    @Test public void testAttribute() throws ComponentInitializationException {

        final SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(TEST_ATTRIBUTE_NAME);

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        simple.setDependencies(dependencySet);
        simple.initialize();

        // And resolve
        final Set<BaseAttributeDefinition> am = new LazySet<BaseAttributeDefinition>();
        am.add(simple);
        am.add(TestSources.populatedStaticAttribute());

        final AttributeResolver resolver = new AttributeResolver();
        resolver.setId("foo");
        resolver.setAttributeDefinition(am);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }
        final Collection values = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(values.size(), 2);
        Assert.assertTrue(values.contains(TestSources.COMMON_ATTRIBUTE_VALUE),
                "looking for value COMMON_ATTRIBUTE_VALUE");
        Assert.assertTrue(values.contains(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE),
                "looking for value ATTRIBUTE_ATTRIBUTE_VALUE");
    }

    /**
     * Test when dependent on a data connector and another attribute.
     * 
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     */
    @Test public void testBoth() throws ComponentInitializationException {

        final SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(TEST_ATTRIBUTE_NAME);

        Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_ATTRIBUTE_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        simple.setDependencies(dependencySet);
        simple.initialize();

        // And resolve
        final Set<BaseAttributeDefinition> attrDefinitions = new LazySet<BaseAttributeDefinition>();
        attrDefinitions.add(simple);
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

        final Collection values = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();
        Assert.assertEquals(values.size(), 3);
        Assert.assertTrue(values.contains(TestSources.COMMON_ATTRIBUTE_VALUE),
                "looking for value COMMON_ATTRIBUTE_VALUE");
        Assert.assertTrue(values.contains(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE),
                "looking for value ATTRIBUTE_ATTRIBUTE_VALUE");
        Assert.assertTrue(values.contains(TestSources.CONNECTOR_ATTRIBUTE_VALUE),
                "looking for value CONNECTOR_ATTRIBUTE_VALUE");
    }
}
