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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.idp.attribute.resolver.ResolverTestSupport;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

/** Test the mapped attribute type. */
public class MappedAttributeTester {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "mapped";

    @Test public void testInstantiation() {
        MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(TEST_ATTRIBUTE_NAME);

        try {
            definition.initialize();
            Assert.fail("Initialized without dependencies and value mappings");
        } catch (ComponentInitializationException e) {
            // expected this
        }

        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        definition.setDependencies(dependencySet);

        try {
            definition.initialize();
            Assert.fail("Initialized without value mappings");
        } catch (ComponentInitializationException e) {
            // expected this
        }

        Collection<ValueMapping> valueMappings = new ArrayList<ValueMapping>();
        valueMappings.add(new SubstringValueMapping("foo", false, "foo"));
        definition.setValueMappings(valueMappings);

        try {
            definition.initialize();
        } catch (ComponentInitializationException e) {
            Assert.fail();
        }
    }

    @Test public void testNoAttributeValues() throws Exception {
        AttributeResolutionContext resolutionContext = ResolverTestSupport.buildResolutionContext();

        Collection<ValueMapping> valueMappings = new ArrayList<ValueMapping>();
        valueMappings.add(new SubstringValueMapping("foo", false, "foo"));

        MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(TEST_ATTRIBUTE_NAME);
        definition.setDependencies(Sets.newHashSet(new ResolverPluginDependency(ResolverTestSupport.ATTRIB3_NAME, "NoSuchAttribute")));        
        definition.setValueMappings(valueMappings);
        definition.initialize();
        
        Optional<Attribute> optionalResult = definition.resolve(resolutionContext);
        Assert.assertNotNull(optionalResult);
        Assert.assertTrue(optionalResult.isPresent());
        
        Attribute result = optionalResult.get();
        Assert.assertEquals(result.getId(), TEST_ATTRIBUTE_NAME);
        Assert.assertTrue(result.getValues().isEmpty());
    }

    @Test public void testInvalidValueType() {
        //TODO
    }

    @Test public void testValidValueType() throws Exception {
        AttributeResolutionContext resolutionContext = ResolverTestSupport.buildResolutionContext();

        Collection<ValueMapping> valueMappings = new ArrayList<ValueMapping>();
        valueMappings.add(new SubstringValueMapping("attr3", false, "foo"));

        MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(TEST_ATTRIBUTE_NAME);
        definition.setDependencies(Sets.newHashSet(new ResolverPluginDependency(ResolverTestSupport.ATTRIB3_NAME, ResolverTestSupport.ATTRIB3_NAME)));        
        definition.setValueMappings(valueMappings);
        definition.initialize();
        
        Optional<Attribute> optionalResult = definition.resolve(resolutionContext);
        Assert.assertNotNull(optionalResult);
        Assert.assertTrue(optionalResult.isPresent());
        
        Attribute result = optionalResult.get();
        Assert.assertEquals(result.getId(), TEST_ATTRIBUTE_NAME);
        Assert.assertFalse(result.getValues().isEmpty());
        Assert.assertEquals(result.getValues().size(), 1);
        Assert.assertTrue(result.getValues().contains("foo"));
    }
}
