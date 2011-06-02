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
import java.util.Set;

import net.shibboleth.idp.attribute.ScopedAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;

import org.opensaml.util.collections.LazySet;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tester for {@link ScopedAttributeDefinition}.
 */
public class ScopedAttributeTest {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "scoped";
    /** The scope. */
    private static final String TEST_SCOPE = "scope";

    /**
     * Test resolution.
     * 
     * @throws AttributeResolutionException if resolution failed.
     */
    @Test
    public void testScopes() throws AttributeResolutionException {
        ScopedAttributeDefinition scoped = new ScopedAttributeDefinition(TEST_ATTRIBUTE_NAME, TEST_SCOPE);
        ScopedAttributeValue res1 = new ScopedAttributeValue(TestSources.COMMON_ATTRIBUTE_VALUE, TEST_SCOPE);
        ScopedAttributeValue res2 = new ScopedAttributeValue(TestSources.COMMON_ATTRIBUTE_VALUE, TEST_SCOPE);

        //
        // Set the dependency on the data connector
        //
        Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        scoped.setDependencies(dependencySet);

        //
        // And resolve
        //
        AttributeResolver resolver = new AttributeResolver("foo");
        Set<BaseDataConnector> connectorSet = new LazySet<BaseDataConnector>();
        connectorSet.add(TestSources.populatedStaticConnectior());

        Set<BaseAttributeDefinition> attributeSet = new LazySet<BaseAttributeDefinition>();
        attributeSet.add(scoped);
        resolver.setDataConnectors(connectorSet);
        resolver.setAttributeDefinition(attributeSet);

        AttributeResolutionContext context = new AttributeResolutionContext(null);
        resolver.resolveAttributes(context);

        //
        // Now test that we got exactly what we expected - two scoped attributes
        //
        Collection<?> f = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(f.size(), 2);
        Assert.assertTrue(f.contains(res1), "looking for COMMON_ATTRIBUTE_VALUE");
        Assert.assertTrue(f.contains(res2), "looking for CONNECTOR_ATTRIBUTE_VALUE");

    }
}
