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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;

import org.opensaml.util.collections.LazySet;
import org.opensaml.util.component.ComponentInitializationException;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Test the mapped attribute type. */
public class MappedAttributeTester {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "mapped";

    /**
     * Test with no mapping provided.
     * 
     * @throws ComponentInitializationException only in an error situation.
     */
    @Test
    public void testEmptyMap() throws ComponentInitializationException {
        final MappedAttributeDefinition mapped = new MappedAttributeDefinition();
        mapped.setId(TEST_ATTRIBUTE_NAME);

        boolean thrown = false;
        try {
            mapped.initialize();
        } catch (ComponentInitializationException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "Initialize with no valuemap");

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));

        mapped.setDependencies(dependencySet);
        mapped.setValueMaps(Collections.EMPTY_SET);
        mapped.initialize();

        //
        // And resolve
        //
        final Set<BaseDataConnector> connectorSet = new LazySet<BaseDataConnector>();
        connectorSet.add(TestSources.populatedStaticConnectior());

        final Set<BaseAttributeDefinition> attributeSet = new LazySet<BaseAttributeDefinition>();
        attributeSet.add(mapped);
        final AttributeResolver resolver = new AttributeResolver();
        resolver.setId("foo");
        resolver.setDataConnectors(connectorSet);
        resolver.setAttributeDefinition(attributeSet);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext(null);
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }

        Assert.assertNull(context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME));
    }

    /**
     * Test no mapping but with a default.
     * 
     * @throws ComponentInitializationException only in the case of a test failure.
     */
    @Test
    public void testNoMapDefault() throws ComponentInitializationException {
        final String mapResult = "result";

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));

        final MappedAttributeDefinition mapped = new MappedAttributeDefinition();
        mapped.setId(TEST_ATTRIBUTE_NAME);
        mapped.setValueMaps(Collections.EMPTY_SET);
        mapped.setDefaultValue(mapResult);
        mapped.setDependencies(dependencySet);
        mapped.initialize();

        // And resolve
        final Set<BaseDataConnector> connectorSet = new LazySet<BaseDataConnector>();
        connectorSet.add(TestSources.populatedStaticConnectior());

        final Set<BaseAttributeDefinition> attributeSet = new LazySet<BaseAttributeDefinition>();
        attributeSet.add(mapped);
        final AttributeResolver resolver = new AttributeResolver();
        resolver.setId("foo");
        resolver.setDataConnectors(connectorSet);
        resolver.setAttributeDefinition(attributeSet);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext(null);
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }

        final Collection f = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(f.size(), 1);
        Assert.assertTrue(f.contains(mapResult), "looking for value TEST_DEFAULT_VALUE");
    }

    /**
     * Test no mapping but with passthrough.
     * 
     * @throws ComponentInitializationException only in case of error
     */
    @Test
    public void testNoMapPassThru() throws ComponentInitializationException {
        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        final MappedAttributeDefinition mapped = new MappedAttributeDefinition();
        mapped.setId(TEST_ATTRIBUTE_NAME);
        mapped.setValueMaps(Collections.EMPTY_SET);
        mapped.setPassThru(true);
        mapped.setDependencies(dependencySet);
        mapped.initialize();

        // And resolve
        final Set<BaseDataConnector> connectorSet = new LazySet<BaseDataConnector>();
        connectorSet.add(TestSources.populatedStaticConnectior());

        final Set<BaseAttributeDefinition> attributeSet = new LazySet<BaseAttributeDefinition>();
        attributeSet.add(mapped);
        final AttributeResolver resolver = new AttributeResolver();
        resolver.setId("foo");
        resolver.setDataConnectors(connectorSet);
        resolver.setAttributeDefinition(attributeSet);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext(null);
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }

        final Collection f = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(f.size(), 2);
        Assert.assertTrue(f.contains(TestSources.COMMON_ATTRIBUTE_VALUE), "looking for value COMMON_ATTRIBUTE_VALUE");
        Assert.assertTrue(f.contains(TestSources.CONNECTOR_ATTRIBUTE_VALUE),
                "looking for value CONNECTOR_ATTRIBUTE_VALUE");
    }

    /**
     * Test mapping. Set up some values, pass them through a map. Do we get what we expect?
     * 
     * @throws ComponentInitializationException only in case of error.
     */
    @Test
    public void testMap() throws ComponentInitializationException {
        final String mapResult = "result";
        final Collection<ValueMap> map = new LazySet<ValueMap>();
        final HashSet<ValueMap.SourceValue> valueSet = new HashSet<ValueMap.SourceValue>();
        valueSet.add(new ValueMap.SourceValue(TestSources.COMMON_ATTRIBUTE_VALUE, false, false));

        map.add(new ValueMap(valueSet, mapResult));

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));

        final MappedAttributeDefinition mapped = new MappedAttributeDefinition();
        mapped.setId(TEST_ATTRIBUTE_NAME);
        mapped.setValueMaps(map);
        mapped.setPassThru(false);
        mapped.setDependencies(dependencySet);
        mapped.initialize();

        // And resolve
        final Set<BaseDataConnector> connectorSet = new LazySet<BaseDataConnector>();
        connectorSet.add(TestSources.populatedStaticConnectior());

        final Set<BaseAttributeDefinition> attributeSet = new LazySet<BaseAttributeDefinition>();
        attributeSet.add(mapped);
        final AttributeResolver resolver = new AttributeResolver();
        resolver.setId("foo");
        resolver.setDataConnectors(connectorSet);
        resolver.setAttributeDefinition(attributeSet);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext(null);
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }

        final Collection f = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();
        Assert.assertEquals(f.size(), 1);
        Assert.assertTrue(f.contains(mapResult), "looking for value TEST_DEFAULT_VALUE");
    }

    /**
     * Test mapping with multiple mappings and use of regexps.
     * 
     * @throws ComponentInitializationException
     */
    @Test
    public void testMultiMap() throws ComponentInitializationException {
        Collection<ValueMap> map = new HashSet<ValueMap>();
        final String mapResult1 = "result1";
        final String mapResult2 = "result2";

        HashSet<ValueMap.SourceValue> valueSet = new HashSet<ValueMap.SourceValue>();
        String trunc = TestSources.COMMON_ATTRIBUTE_VALUE;
        trunc = trunc.substring(0, trunc.length() - 1);
        trunc = trunc.toUpperCase();
        valueSet.add(new ValueMap.SourceValue(trunc, true, true));
        map.add(new ValueMap(valueSet, mapResult1));

        valueSet = new HashSet<ValueMap.SourceValue>();
        valueSet.add(new ValueMap.SourceValue(TestSources.CONNECTOR_ATTRIBUTE_VALUE_REGEXP.toUpperCase(), true, false));
        map.add(new ValueMap(valueSet, mapResult2));

        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));

        final MappedAttributeDefinition mapped = new MappedAttributeDefinition();
        mapped.setId(TEST_ATTRIBUTE_NAME);
        mapped.setValueMaps(map);
        mapped.setPassThru(false);
        mapped.setDependencies(dependencySet);
        mapped.initialize();

        final Set<BaseDataConnector> connectorSet = new LazySet<BaseDataConnector>();
        connectorSet.add(TestSources.populatedStaticConnectior());

        final Set<BaseAttributeDefinition> attributeSet = new LazySet<BaseAttributeDefinition>();
        attributeSet.add(mapped);
        final AttributeResolver resolver = new AttributeResolver();
        resolver.setId("foo");
        resolver.setDataConnectors(connectorSet);
        resolver.setAttributeDefinition(attributeSet);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext(null);
        try {
            resolver.resolveAttributes(context);
        } catch (AttributeResolutionException e) {
            Assert.fail("resolution failed", e);
        }

        final Collection f = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertTrue(f.contains(mapResult1), "looking for value mapResult1");
        Assert.assertTrue(f.contains(mapResult2), "looking for value mapResult2");
        Assert.assertEquals(f.size(), 2);
    }
}
