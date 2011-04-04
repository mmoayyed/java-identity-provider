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
import java.util.HashSet;
import java.util.Map;

import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.messaging.context.impl.BasicMessageContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link AttributeResolutionContext}. */
public class AttributeResolutionContextTest {

    /** Test instantiation and post-instantiation state. */
    @Test
    public void testInstantiation() {

        AttributeResolutionContext context = new AttributeResolutionContext(null);
        Assert.assertNull(context.getOwner());
        Assert.assertNotNull(context.getRequestedAttributes());
        Assert.assertTrue(context.getRequestedAttributes().isEmpty());
        Assert.assertNotNull(context.getResolvedAttributeDefinitions());
        Assert.assertTrue(context.getResolvedAttributeDefinitions().isEmpty());
        Assert.assertNotNull(context.getResolvedDataConnectors());
        Assert.assertTrue(context.getResolvedDataConnectors().isEmpty());

        context = new AttributeResolutionContext(new BasicMessageContext<String>());
        Assert.assertNotNull(context.getOwner());
        Assert.assertNotNull(context.getRequestedAttributes());
        Assert.assertTrue(context.getRequestedAttributes().isEmpty());
        Assert.assertNotNull(context.getResolvedAttributeDefinitions());
        Assert.assertTrue(context.getResolvedAttributeDefinitions().isEmpty());
        Assert.assertNotNull(context.getResolvedDataConnectors());
        Assert.assertTrue(context.getResolvedDataConnectors().isEmpty());
    }

    /** Test {@link AttributeResolutionContext#setRequestedAttributes(java.util.Set)}. */
    @Test
    public void testSetRequesedAttributes(){
        AttributeResolutionContext context = new AttributeResolutionContext(null);
        
        context.setRequestedAttributes(null);
        Assert.assertNotNull(context.getRequestedAttributes());
        Assert.assertTrue(context.getRequestedAttributes().isEmpty());
        
        HashSet<Attribute<?>> attributes = new HashSet<Attribute<?>>();
        context.setRequestedAttributes(attributes);
        Assert.assertNotNull(context.getRequestedAttributes());
        Assert.assertTrue(context.getRequestedAttributes().isEmpty());
        
        attributes.add(null);
        context.setRequestedAttributes(attributes);
        Assert.assertNotNull(context.getRequestedAttributes());
        Assert.assertTrue(context.getRequestedAttributes().isEmpty());
        
        attributes.add(new Attribute<String>("foo"));
        attributes.add(null);
        attributes.add(new Attribute<String>("bar"));
        context.setRequestedAttributes(attributes);
        Assert.assertNotNull(context.getRequestedAttributes());
        Assert.assertEquals(context.getRequestedAttributes().size(), 2);
        
        attributes.clear();
        attributes.add(new Attribute<String>("baz"));
        context.setRequestedAttributes(attributes);
        Assert.assertNotNull(context.getRequestedAttributes());
        Assert.assertEquals(context.getRequestedAttributes().size(), 1);
    }
    
    /** Test {@link AttributeResolutionContext#setRequestedAttributes(java.util.Set)}. */
    @Test
    public void testSetResolvedAttributes(){
        AttributeResolutionContext context = new AttributeResolutionContext(null);
        
        context.setResolvedAttributes(null);
        Assert.assertNotNull(context.getResolvedAttributes());
        Assert.assertTrue(context.getResolvedAttributes().isEmpty());
        
        HashSet<Attribute<?>> attributes = new HashSet<Attribute<?>>();
        context.setResolvedAttributes(attributes);
        Assert.assertNotNull(context.getResolvedAttributes());
        Assert.assertTrue(context.getResolvedAttributes().isEmpty());
        
        attributes.add(null);
        context.setResolvedAttributes(attributes);
        Assert.assertNotNull(context.getResolvedAttributes());
        Assert.assertTrue(context.getResolvedAttributes().isEmpty());
        
        attributes.add(new Attribute<String>("foo"));
        attributes.add(null);
        attributes.add(new Attribute<String>("bar"));
        context.setResolvedAttributes(attributes);
        Assert.assertNotNull(context.getResolvedAttributes());
        Assert.assertEquals(context.getResolvedAttributes().size(), 2);
        
        attributes.clear();
        attributes.add(new Attribute<String>("baz"));
        context.setResolvedAttributes(attributes);
        Assert.assertNotNull(context.getResolvedAttributes());
        Assert.assertEquals(context.getResolvedAttributes().size(), 1);
    }
    
    /** Test adding and retrieving attribute definitions. */
    @Test
    public void testResolvedAttributeDefinitions() throws Exception {
        AttributeResolutionContext context = new AttributeResolutionContext(null);
        
        Assert.assertNotNull(context.getResolvedAttributeDefinitions());
        Assert.assertNull(context.getResolvedAttributeDefinition("foo"));

        Attribute attribute = new Attribute<String>("foo");
        MockAttributeDefinition definition = new MockAttributeDefinition("foo", attribute);
        
        context.recordAttributeDefinitionResolution(definition, attribute);
        Assert.assertNotNull(context.getResolvedAttributeDefinitions());
        Assert.assertEquals(context.getResolvedAttributeDefinitions().size(), 1);
        Assert.assertNotNull(context.getResolvedAttributeDefinition("foo"));
        Assert.assertTrue(context.getResolvedAttributeDefinition("foo") instanceof ResolvedAttributeDefinition);
        Assert.assertTrue(context.getResolvedAttributeDefinition("foo").unwrap() == definition);
        Assert.assertTrue(context.getResolvedAttributeDefinition("foo").resolve(context) == attribute);
        
        try{
            context.recordAttributeDefinitionResolution(definition, attribute);
            Assert.fail("able to record a second resolution for a single attribute definition");
        }catch(AttributeResolutionException e){
            //expected this
        }
        
        definition = new MockAttributeDefinition("bar", (Attribute)null);
        
        context.recordAttributeDefinitionResolution(definition, null);
        Assert.assertNotNull(context.getResolvedAttributeDefinitions());
        Assert.assertEquals(context.getResolvedAttributeDefinitions().size(), 2);
        Assert.assertNotNull(context.getResolvedAttributeDefinition("foo"));
        Assert.assertTrue(context.getResolvedAttributeDefinition("foo") instanceof ResolvedAttributeDefinition);        
        Assert.assertNotNull(context.getResolvedAttributeDefinition("bar"));
        Assert.assertTrue(context.getResolvedAttributeDefinition("bar") instanceof ResolvedAttributeDefinition);
        Assert.assertTrue(context.getResolvedAttributeDefinition("bar").unwrap() == definition);
        Assert.assertTrue(context.getResolvedAttributeDefinition("bar").resolve(context) == null);
    }
    
    /** Test adding and retrieving data connectors. */
    @Test
    public void testResolvedDataConnectors() throws Exception {
        AttributeResolutionContext context = new AttributeResolutionContext(null);
        
        Assert.assertNotNull(context.getResolvedDataConnectors());
        Assert.assertNull(context.getResolvedDataConnector("foo"));

        
        Attribute attribute = new Attribute<String>("foo");
        
        Map<String, Attribute<?>> attributes = new HashMap<String, Attribute<?>>();
        attributes.put(attribute.getId(), attribute);
        
        MockDataConnector connector = new MockDataConnector("foo", attributes);
        
        context.recordDataConnectorResolution(connector, attributes);
        Assert.assertNotNull(context.getResolvedDataConnectors());
        Assert.assertEquals(context.getResolvedDataConnectors().size(), 1);
        Assert.assertNotNull(context.getResolvedDataConnector("foo"));
        Assert.assertTrue(context.getResolvedDataConnector("foo") instanceof ResolvedDataConnector);
        Assert.assertTrue(context.getResolvedDataConnector("foo").unwrap() == connector);
        Assert.assertTrue(context.getResolvedDataConnector("foo").resolve(context) == attributes);
        
        try{
            context.recordDataConnectorResolution(connector, attributes);
            Assert.fail("able to record a second resolution for a single data connector");
        }catch(AttributeResolutionException e){
            //expected this
        }
        
        connector = new MockDataConnector("bar", (Map)null);
        
        context.recordDataConnectorResolution(connector, null);
        Assert.assertNotNull(context.getResolvedDataConnectors());
        Assert.assertEquals(context.getResolvedDataConnectors().size(), 2);
        Assert.assertNotNull(context.getResolvedDataConnector("foo"));
        Assert.assertTrue(context.getResolvedDataConnector("foo") instanceof ResolvedDataConnector);        
        Assert.assertNotNull(context.getResolvedDataConnector("bar"));
        Assert.assertTrue(context.getResolvedDataConnector("bar") instanceof ResolvedDataConnector);
        Assert.assertTrue(context.getResolvedDataConnector("bar").unwrap() == connector);
        Assert.assertTrue(context.getResolvedDataConnector("bar").resolve(context) == null);
    }
}