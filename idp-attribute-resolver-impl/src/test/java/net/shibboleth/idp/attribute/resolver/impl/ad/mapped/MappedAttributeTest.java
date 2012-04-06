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

package net.shibboleth.idp.attribute.resolver.impl.ad.mapped;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.idp.attribute.resolver.ResolverTestSupport;
import net.shibboleth.idp.attribute.resolver.impl.TestSources;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

/** Test the mapped attribute type. */
public class MappedAttributeTest {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "mapped";

    @Test public void testInstantiation() throws ComponentInitializationException, AttributeResolutionException {
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
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR));
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

        definition.initialize();
        
        definition.destroy();
        try {
            definition.initialize();
            Assert.fail("init a torn down mapper?");
        } catch (DestroyedComponentException e) {
            // expected this
        }
        
        try {
            definition.doAttributeDefinitionResolve(new AttributeResolutionContext());
            Assert.fail("resolve a torn down mapper?");
        } catch (DestroyedComponentException e) {
            // expected this
        }

        
    }

    @Test public void testNoAttributeValues() throws Exception {
        AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1",
                        ResolverTestSupport.buildAttribute(ResolverTestSupport.EPE_ATTRIB_ID,
                                ResolverTestSupport.EPE1_VALUES), ResolverTestSupport.buildAttribute(
                                ResolverTestSupport.EPA_ATTRIB_ID, ResolverTestSupport.EPA1_VALUES)));

        Collection<ValueMapping> valueMappings = new ArrayList<ValueMapping>();
        valueMappings.add(new SubstringValueMapping("foo", false, "foo"));

        MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(TEST_ATTRIBUTE_NAME);
        definition.setDependencies(Sets.newHashSet(new ResolverPluginDependency("connector1", "NoSuchAttribute")));
        definition.setValueMappings(valueMappings);
        definition.initialize();

        Optional<Attribute> optionalResult = definition.resolve(resolutionContext);
        Assert.assertNotNull(optionalResult);
        Assert.assertTrue(optionalResult.isPresent());

        Attribute result = optionalResult.get();
        Assert.assertEquals(result.getId(), TEST_ATTRIBUTE_NAME);
        Assert.assertTrue(result.getValues().isEmpty());
    }

    @Test public void testInvalidValueType() throws ComponentInitializationException {
        Attribute attr = new Attribute(ResolverTestSupport.EPA_ATTRIB_ID);
        attr.setValues(Collections.singleton((AttributeValue) new ByteAttributeValue(new byte[] {1,2,3})));
        
        AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1", attr));

        Collection<ValueMapping> valueMappings = new ArrayList<ValueMapping>();
        valueMappings.add(new SubstringValueMapping("student", false, "student"));

        MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(TEST_ATTRIBUTE_NAME);
        definition.setDependencies(Sets.newHashSet(new ResolverPluginDependency("connector1",
                ResolverTestSupport.EPA_ATTRIB_ID)));
        definition.setValueMappings(valueMappings);
        definition.initialize();
        
        try {
            definition.doAttributeDefinitionResolve(resolutionContext);
            Assert.fail("invalid types");
        } catch (AttributeResolutionException e) {
            //
        }
        
    }

    @Test public void testValidValueType() throws Exception {
        AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1",
                        ResolverTestSupport.buildAttribute(ResolverTestSupport.EPE_ATTRIB_ID,
                                ResolverTestSupport.EPE1_VALUES), ResolverTestSupport.buildAttribute(
                                ResolverTestSupport.EPA_ATTRIB_ID, ResolverTestSupport.EPA3_VALUES)));

        Collection<ValueMapping> valueMappings = new ArrayList<ValueMapping>();
        valueMappings.add(new SubstringValueMapping("student", false, "student"));

        MappedAttributeDefinition definition = new MappedAttributeDefinition();
        definition.setId(TEST_ATTRIBUTE_NAME);
        definition.setDependencies(Sets.newHashSet(new ResolverPluginDependency("connector1",
                ResolverTestSupport.EPA_ATTRIB_ID)));
        Assert.assertTrue(definition.getValueMappings().isEmpty());
        definition.setValueMappings(valueMappings);
        Assert.assertEquals(definition.getValueMappings().size(), 1);
        definition.initialize();

        Optional<Attribute> optionalResult = definition.resolve(resolutionContext);
        Assert.assertNotNull(optionalResult);
        Assert.assertTrue(optionalResult.isPresent());

        Attribute result = optionalResult.get();
        Assert.assertEquals(result.getId(), TEST_ATTRIBUTE_NAME);
        Assert.assertFalse(result.getValues().isEmpty());
        Assert.assertEquals(result.getValues().size(), 1);
        Assert.assertTrue(result.getValues().contains(new StringAttributeValue("student")));
    }
}
