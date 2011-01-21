/*
 * Copyright 2011 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.resolver;

import java.util.HashMap;

import net.shibboleth.idp.attribute.Attribute;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link ResolverPluginDependency}. */
public class ResolverPluginDependencyTest {

    /** Tests the state of a newly instantiated object. */
    @Test
    public void testInstantiation() {
        ResolverPluginDependency dep = new ResolverPluginDependency(" foo ", " bar ");
        Assert.assertEquals(dep.getDependencyPluginId(), "foo");
        Assert.assertEquals(dep.getDependencyAttributeId(), "bar");

        dep = new ResolverPluginDependency("foo ", "");
        Assert.assertEquals(dep.getDependencyPluginId(), "foo");
        Assert.assertEquals(dep.getDependencyAttributeId(), null);

        dep = new ResolverPluginDependency("foo ", null);
        Assert.assertEquals(dep.getDependencyPluginId(), "foo");
        Assert.assertEquals(dep.getDependencyAttributeId(), null);

        try {
            dep = new ResolverPluginDependency(null, null);
            Assert.fail("able to set null dependency ID");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        try {
            dep = new ResolverPluginDependency(" ", null);
            Assert.fail("able to set empty dependency ID");
        } catch (IllegalArgumentException e) {
            // expected this
        }
    }
    
    /** Test getting a dependent attribute from a resolution context. */
    @Test
    public void testGetDependentAttribute() throws Exception {
        MockAttributeDefinition definition = new MockAttributeDefinition("foo", null);
        MockDataConnector connector = new MockDataConnector("bar", null);
        
        AttributeResolutionContext context = new AttributeResolutionContext(null);
        context.recordAttributeDefinitionResolution(definition, null);
        context.recordDataConnectorResolution(connector, null);
        
        ResolverPluginDependency dep = new ResolverPluginDependency("foo", null);
        Assert.assertNull(dep.getDependentAttribute(context));
        
        dep = new ResolverPluginDependency("bar", null);
        Assert.assertNull(dep.getDependentAttribute(context));
        
        
        Attribute<String> attribute = new Attribute<String>("foo");
        definition = new MockAttributeDefinition("foo", attribute);
        context = new AttributeResolutionContext(null);
        context.recordAttributeDefinitionResolution(definition, attribute);
        
        dep = new ResolverPluginDependency("foo", null);
        Assert.assertTrue(dep.getDependentAttribute(context) == attribute);
        
        attribute = new Attribute<String>("foo");
        HashMap<String, Attribute<?>> values = new HashMap<String, Attribute<?>>();
        values.put(attribute.getId(), attribute);
        
        connector = new MockDataConnector("bar", values);
        context = new AttributeResolutionContext(null);
        context.recordDataConnectorResolution(connector, values);
        
        dep = new ResolverPluginDependency("bar", "foo");
        Assert.assertTrue(dep.getDependentAttribute(context) == attribute);
        
        dep = new ResolverPluginDependency("bar", "baz");
        Assert.assertNull(dep.getDependentAttribute(context));
    }
}