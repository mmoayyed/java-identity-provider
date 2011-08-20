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
import net.shibboleth.idp.attribute.ScopedAttributeValue;
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

/**
 * Test for prescoped attribute definitions.
 */
public class PrecscopedAtributeTest {
    /** The name. resolve to */
    private static final String TEST_ATTRIBUTE_NAME = "prescoped";

    /**
     * Test regexp. The test Data Connector provides an input attribute "at1" with values at1-Data and at1-Connector. We
     * can feed these into the prescoped, looking for '-'
     * 
     * @throws AttributeResolutionException on resolution issues.
     * @throws ComponentInitializationException if any of our initializtions failed (which it shouldn't)
     */
    @Test
    public void testPreScoped() throws AttributeResolutionException, ComponentInitializationException {

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        final PrescopedAttributeDefinition attrDef = new PrescopedAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        attrDef.setScopeDelimiter("-");
        attrDef.setDependencies(dependencySet);
        attrDef.initialize();

        // And resolve
        final Set<BaseDataConnector> connectorSet = new LazySet<BaseDataConnector>();
        connectorSet.add(TestSources.populatedStaticConnectior());

        final Set<BaseAttributeDefinition> attributeSet = new LazySet<BaseAttributeDefinition>();
        attributeSet.add(attrDef);

        final AttributeResolver resolver = new AttributeResolver();
        resolver.setId("foo");
        resolver.setDataConnectors(connectorSet);
        resolver.setDataConnectors(connectorSet);
        resolver.setAttributeDefinition(attributeSet);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext(null);
        resolver.resolveAttributes(context);
        final Collection f = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(f.size(), 2);
        Assert.assertTrue(f.contains(new ScopedAttributeValue("at1", "Data")));
        Assert.assertTrue(f.contains(new ScopedAttributeValue("at1", "Connector")));
    }

    /**
     * Test the prescoped attribute resolve when there are no matches.
     * 
     * @throws AttributeResolutionException if resolution fails.
     * @throws ComponentInitializationException if any of our initializtions failed (which it shouldn't)
     */
    @Test
    public void testPreScopedNoValues() throws AttributeResolutionException, ComponentInitializationException {

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        final PrescopedAttributeDefinition attrDef = new PrescopedAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        attrDef.setScopeDelimiter("@");
        attrDef.setDependencies(dependencySet);
        attrDef.initialize();

        // And resolve
        final Set<BaseDataConnector> connectorSet = new LazySet<BaseDataConnector>();
        connectorSet.add(TestSources.populatedStaticConnectior());

        final Set<BaseAttributeDefinition> attributeSet = new LazySet<BaseAttributeDefinition>();
        attributeSet.add(attrDef);

        final AttributeResolver resolver = new AttributeResolver();
        resolver.setId("foo");
        resolver.setDataConnectors(connectorSet);
        resolver.setDataConnectors(connectorSet);
        resolver.setAttributeDefinition(attributeSet);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext(null);
        resolver.resolveAttributes(context);

        final Attribute<?> resultAttribute = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME);
        Assert.assertNull(resultAttribute);
    }
}
