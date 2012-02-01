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

package net.shibboleth.idp.attribute.resolver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.shibboleth.idp.attribute.Attribute;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Optional;

/** Unit test for {@link AttributeResolutionContext}. */
public class AttributeResolutionContextTest {

    /** Test instantiation and post-instantiation state. */
    @Test public void testInstantiation() {

        AttributeResolutionContext context = new AttributeResolutionContext();
        Assert.assertNull(context.getParent());
        Assert.assertNotNull(context.getRequestedAttributes());
        Assert.assertTrue(context.getRequestedAttributes().isEmpty());
        Assert.assertNotNull(context.getResolvedAttributeDefinitions());
        Assert.assertTrue(context.getResolvedAttributeDefinitions().isEmpty());
        Assert.assertNotNull(context.getResolvedDataConnectors());
        Assert.assertTrue(context.getResolvedDataConnectors().isEmpty());
    }

    /** Test {@link AttributeResolutionContext#setRequestedAttributes(java.util.Set)}. */
    @Test public void testSetRequesedAttributes() {
        AttributeResolutionContext context = new AttributeResolutionContext();

        context.setRequestedAttributes(null);
        Assert.assertNotNull(context.getRequestedAttributes());
        Assert.assertTrue(context.getRequestedAttributes().isEmpty());

        HashSet<Attribute> attributes = new HashSet<Attribute>();
        context.setRequestedAttributes(attributes);
        Assert.assertNotNull(context.getRequestedAttributes());
        Assert.assertTrue(context.getRequestedAttributes().isEmpty());

        attributes.add(null);
        context.setRequestedAttributes(attributes);
        Assert.assertNotNull(context.getRequestedAttributes());
        Assert.assertTrue(context.getRequestedAttributes().isEmpty());

        attributes.add(new Attribute("foo"));
        attributes.add(null);
        attributes.add(new Attribute("bar"));
        context.setRequestedAttributes(attributes);
        Assert.assertNotNull(context.getRequestedAttributes());
        Assert.assertEquals(context.getRequestedAttributes().size(), 2);

        attributes.clear();
        attributes.add(new Attribute("baz"));
        context.setRequestedAttributes(attributes);
        Assert.assertNotNull(context.getRequestedAttributes());
        Assert.assertEquals(context.getRequestedAttributes().size(), 1);
    }

    /** Test {@link AttributeResolutionContext#setRequestedAttributes(java.util.Set)}. */
    @Test public void testSetResolvedAttributes() {
        AttributeResolutionContext context = new AttributeResolutionContext();

        context.setResolvedAttributes(null);
        Assert.assertNotNull(context.getResolvedAttributes());
        Assert.assertTrue(context.getResolvedAttributes().isEmpty());

        HashSet<Attribute> attributes = new HashSet<Attribute>();
        context.setResolvedAttributes(attributes);
        Assert.assertNotNull(context.getResolvedAttributes());
        Assert.assertTrue(context.getResolvedAttributes().isEmpty());

        attributes.add(null);
        context.setResolvedAttributes(attributes);
        Assert.assertNotNull(context.getResolvedAttributes());
        Assert.assertTrue(context.getResolvedAttributes().isEmpty());

        attributes.add(new Attribute("foo"));
        attributes.add(null);
        attributes.add(new Attribute("bar"));
        context.setResolvedAttributes(attributes);
        Assert.assertNotNull(context.getResolvedAttributes());
        Assert.assertEquals(context.getResolvedAttributes().size(), 2);

        attributes.clear();
        attributes.add(new Attribute("baz"));
        context.setResolvedAttributes(attributes);
        Assert.assertNotNull(context.getResolvedAttributes());
        Assert.assertEquals(context.getResolvedAttributes().size(), 1);
    }

    /** Test adding and retrieving attribute definitions. */
    @Test public void testResolvedAttributeDefinitions() throws Exception {
        AttributeResolutionContext context = new AttributeResolutionContext();

        Assert.assertNotNull(context.getResolvedAttributeDefinitions());
        Assert.assertNull(context.getResolvedAttributeDefinitions().get("foo"));

        Attribute attribute = new Attribute("foo");
        MockAttributeDefinition definition = new MockAttributeDefinition("foo", attribute);

        context.recordAttributeDefinitionResolution(definition, Optional.<Attribute> fromNullable(attribute));
        Assert.assertNotNull(context.getResolvedAttributeDefinitions());
        Assert.assertEquals(context.getResolvedAttributeDefinitions().size(), 1);
        Assert.assertNotNull(context.getResolvedAttributeDefinitions().get("foo"));
        Assert.assertTrue(context.getResolvedAttributeDefinitions().get("foo") instanceof ResolvedAttributeDefinition);
        Assert.assertTrue(context.getResolvedAttributeDefinitions().get("foo").getResolvedDefinition() == definition);
        Assert.assertTrue(context.getResolvedAttributeDefinitions().get("foo").resolve(context).get() == attribute);

        try {
            context.recordAttributeDefinitionResolution(definition, Optional.<Attribute> fromNullable(attribute));
            Assert.fail("able to record a second resolution for a single attribute definition");
        } catch (AttributeResolutionException e) {
            // expected this
        }

        definition = new MockAttributeDefinition("bar", (Attribute) null);

        context.recordAttributeDefinitionResolution(definition, Optional.<Attribute> absent());
        Assert.assertNotNull(context.getResolvedAttributeDefinitions());
        Assert.assertEquals(context.getResolvedAttributeDefinitions().size(), 2);
        Assert.assertNotNull(context.getResolvedAttributeDefinitions().get("foo"));
        Assert.assertTrue(context.getResolvedAttributeDefinitions().get("foo") instanceof ResolvedAttributeDefinition);
        Assert.assertNotNull(context.getResolvedAttributeDefinitions().get("bar"));
        Assert.assertTrue(context.getResolvedAttributeDefinitions().get("bar") instanceof ResolvedAttributeDefinition);
        Assert.assertTrue(context.getResolvedAttributeDefinitions().get("bar").getResolvedDefinition() == definition);
        Assert.assertTrue(context.getResolvedAttributeDefinitions().get("bar").resolve(context)
                .equals(Optional.absent()));
    }

    /** Test adding and retrieving data connectors. */
    @Test public void testResolvedDataConnectors() throws Exception {
        AttributeResolutionContext context = new AttributeResolutionContext();

        Assert.assertNotNull(context.getResolvedDataConnectors());
        Assert.assertNull(context.getResolvedDataConnectors().get("foo"));

        Attribute attribute = new Attribute("foo");

        Map<String, Attribute> attributes = new HashMap<String, Attribute>();
        attributes.put(attribute.getId(), attribute);

        MockDataConnector connector = new MockDataConnector("foo", attributes);

        context.recordDataConnectorResolution(connector, Optional.of(attributes));
        Assert.assertNotNull(context.getResolvedDataConnectors());
        Assert.assertEquals(context.getResolvedDataConnectors().size(), 1);
        Assert.assertNotNull(context.getResolvedDataConnectors().get("foo"));
        Assert.assertTrue(context.getResolvedDataConnectors().get("foo") instanceof ResolvedDataConnector);
        Assert.assertTrue(context.getResolvedDataConnectors().get("foo").getResolvedConnector() == connector);
        Assert.assertTrue(context.getResolvedDataConnectors().get("foo").resolve(context).get() == attributes);

        try {
            context.recordDataConnectorResolution(connector, Optional.of(attributes));
            Assert.fail("able to record a second resolution for a single data connector");
        } catch (AttributeResolutionException e) {
            // expected this
        }

        connector = new MockDataConnector("bar", (Map) null);

        context.recordDataConnectorResolution(connector, Optional.<Map<String, Attribute>>absent());
        Assert.assertNotNull(context.getResolvedDataConnectors());
        Assert.assertEquals(context.getResolvedDataConnectors().size(), 2);
        Assert.assertNotNull(context.getResolvedDataConnectors().get("foo"));
        Assert.assertTrue(context.getResolvedDataConnectors().get("foo") instanceof ResolvedDataConnector);
        Assert.assertNotNull(context.getResolvedDataConnectors().get("bar"));
        Assert.assertTrue(context.getResolvedDataConnectors().get("bar") instanceof ResolvedDataConnector);
        Assert.assertTrue(context.getResolvedDataConnectors().get("bar").getResolvedConnector() == connector);
        Assert.assertTrue(context.getResolvedDataConnectors().get("bar").resolve(context) == Optional.<Map<String, Attribute>>absent());
    }
}