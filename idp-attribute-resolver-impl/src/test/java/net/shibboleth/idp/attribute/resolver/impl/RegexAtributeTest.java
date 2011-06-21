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
 * Test for regex attribute definitions.
 */
public class RegexAtributeTest {
    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "simple";

    /**
     * Test regexp.  We set up an attribute called 'at1-Connector', we throw this
     * at 'at1-(.+)or' and look for group 1 'Connect'.
     * 
     * @throws AttributeResolutionException on resolution issues.
     */
    @Test
    public void testRegex() throws AttributeResolutionException {

        final BaseAttributeDefinition attrDef =
                new RegexSplitAttributeDefinition(TEST_ATTRIBUTE_NAME, TestSources.CONNECTOR_ATTRIBUTE_VALUE_REGEXP,
                        false);
        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        attrDef.setDependencies(dependencySet);

        // And resolve
        final Set<BaseDataConnector> connectorSet = new LazySet<BaseDataConnector>();
        connectorSet.add(TestSources.populatedStaticConnectior());

        final Set<BaseAttributeDefinition> attributeSet = new LazySet<BaseAttributeDefinition>();
        attributeSet.add(attrDef);
        
        final AttributeResolver resolver = new AttributeResolver("foo");
        resolver.setDataConnectors(connectorSet);
        resolver.setAttributeDefinition(attributeSet);

        final AttributeResolutionContext context = new AttributeResolutionContext(null);
        resolver.resolveAttributes(context);
        Collection f = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(f.size(), 1);
        Assert.assertTrue(f.contains(TestSources.CONNECTOR_ATTRIBUTE_VALUE_REGEXP_RESULT), "looking for regexp result");
    }

}
