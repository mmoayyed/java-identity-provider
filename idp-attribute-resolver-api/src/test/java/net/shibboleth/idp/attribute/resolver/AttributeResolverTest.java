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

import java.util.Collections;
import java.util.Map;

import net.shibboleth.idp.ComponentValidationException;
import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.collections.LazyList;
import org.opensaml.util.collections.LazySet;
import org.testng.Assert;
import org.testng.annotations.Test;

//TODO test during resolve: failover, requested attributes, bad pluginIDs, plugin throwing error, error propagation

/** Test case for {@link AttributeResolver}. */
public class AttributeResolverTest {

    /** Test post-instantiation state. */
    @Test
    public void testInstantiation() {
        AttributeResolver resolver = new AttributeResolver(" foo ");

        Assert.assertEquals(resolver.getId(), "foo");
        Assert.assertNotNull(resolver.getAttributeDefinitions());
        Assert.assertTrue(resolver.getAttributeDefinitions().isEmpty());
        Assert.assertNotNull(resolver.getDataConnectors());
        Assert.assertTrue(resolver.getDataConnectors().isEmpty());
    }

    /** Test getting, setting, overwriting, defensive collection copy. */
    @Test
    public void testSetAttributeDefinitions() {
        AttributeResolver resolver = new AttributeResolver("foo");

        LazySet<BaseAttributeDefinition> definitions = null;
        resolver.setAttributeDefinition(definitions);
        Assert.assertNotNull(resolver.getAttributeDefinitions());
        Assert.assertTrue(resolver.getAttributeDefinitions().isEmpty());

        definitions = new LazySet<BaseAttributeDefinition>();
        resolver.setAttributeDefinition(definitions);
        Assert.assertNotNull(resolver.getAttributeDefinitions());
        Assert.assertTrue(resolver.getAttributeDefinitions().isEmpty());

        definitions.add(new MockAttributeDefinition("foo", new Attribute<String>("test")));
        definitions.add(null);
        definitions.add(new MockAttributeDefinition("bar", new Attribute<String>("test")));
        definitions.add(new MockAttributeDefinition("foo", new Attribute<String>("test")));
        resolver.setAttributeDefinition(definitions);
        Assert.assertNotNull(resolver.getAttributeDefinitions());
        Assert.assertEquals(resolver.getAttributeDefinitions().size(), 2);

        definitions.clear();
        Assert.assertNotNull(resolver.getAttributeDefinitions());
        Assert.assertEquals(resolver.getAttributeDefinitions().size(), 2);

        resolver.setAttributeDefinition(definitions);
        Assert.assertNotNull(resolver.getAttributeDefinitions());
        Assert.assertTrue(resolver.getAttributeDefinitions().isEmpty());
    }

    /** Test getting, setting, overwriting, defensive collection copy. */
    @Test
    public void testSetDataConnectors() {
        AttributeResolver resolver = new AttributeResolver("foo");

        LazySet<BaseDataConnector> connectors = null;
        resolver.setDataConnectors(connectors);
        Assert.assertNotNull(resolver.getDataConnectors());
        Assert.assertTrue(resolver.getDataConnectors().isEmpty());

        connectors = new LazySet<BaseDataConnector>();
        resolver.setDataConnectors(connectors);
        Assert.assertNotNull(resolver.getDataConnectors());
        Assert.assertTrue(resolver.getDataConnectors().isEmpty());

        connectors.add(new MockDataConnector("foo", (Map)null));
        connectors.add(null);
        connectors.add(new MockDataConnector("bar", (Map)null));
        connectors.add(new MockDataConnector("foo", (Map)null));
        resolver.setDataConnectors(connectors);
        Assert.assertNotNull(resolver.getDataConnectors());
        Assert.assertEquals(resolver.getDataConnectors().size(), 2);

        connectors.clear();
        Assert.assertNotNull(resolver.getDataConnectors());
        Assert.assertEquals(resolver.getDataConnectors().size(), 2);

        resolver.setDataConnectors(connectors);
        Assert.assertNotNull(resolver.getDataConnectors());
        Assert.assertTrue(resolver.getDataConnectors().isEmpty());
    }

    /** Test that a simple resolve returns the expected results. */
    @Test
    public void testResolve() throws AttributeResolutionException {
        AttributeResolver resolver = new AttributeResolver("foo");

        Attribute<String> attribute = new Attribute<String>("ad1");
        attribute.setValues(Collections.singletonList("value1"));

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(new MockAttributeDefinition("ad1", attribute));
        resolver.setAttributeDefinition(definitions);

        AttributeResolutionContext context = new AttributeResolutionContext(null);
        resolver.resolveAttributes(context);

        Assert.assertNotNull(context.getResolvedAttributeDefinition("ad1"));
        Assert.assertEquals(context.getResolvedAttributes().size(), 1);
        Assert.assertEquals(context.getResolvedAttributes().get("ad1"), attribute);
    }
    
    /** Test that resolve w/ dependencies returns the expected results. */
    @Test
    public void testResolveWithDependencies() throws AttributeResolutionException {
        AttributeResolver resolver = new AttributeResolver("foo");

        MockDataConnector dc1 = new MockDataConnector("dc1", (Map)null);

        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new Attribute<String>("test"));
        ad1.addDependency(new ResolverPluginDependency("dc1", null));

        MockAttributeDefinition ad2 = new MockAttributeDefinition("ad2", new Attribute<String>("test"));

        MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", new Attribute<String>("test"));
        ad0.addDependency(new ResolverPluginDependency("ad1", null));
        ad0.addDependency(new ResolverPluginDependency("ad2", null));

        LazySet<BaseDataConnector> connectors = new LazySet<BaseDataConnector>();
        connectors.add(dc1);
        resolver.setDataConnectors(connectors);

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad0);
        definitions.add(ad1);
        definitions.add(ad2);
        resolver.setAttributeDefinition(definitions);

        AttributeResolutionContext context = new AttributeResolutionContext(null);
        resolver.resolveAttributes(context);

        Assert.assertNotNull(context.getResolvedAttributeDefinition("ad0"));
        Assert.assertNotNull(context.getResolvedAttributeDefinition("ad1"));
        Assert.assertNotNull(context.getResolvedAttributeDefinition("ad2"));
        
        Assert.assertNotNull(context.getResolvedDataConnector("dc1"));
    }

    /**
     * Test that after resolution attribute definitions which returned null values don't have their results show up in
     * the resolved attribute set.
     */
    @Test
    public void testResolveCleanNullAttributes() throws AttributeResolutionException {
        AttributeResolver resolver = new AttributeResolver("foo");

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(new MockAttributeDefinition("ad1", new Attribute<String>("test")));
        resolver.setAttributeDefinition(definitions);

        AttributeResolutionContext context = new AttributeResolutionContext(null);
        resolver.resolveAttributes(context);

        Assert.assertNotNull(context.getResolvedAttributeDefinition("ad1"));
        Assert.assertTrue(context.getResolvedAttributes().isEmpty());
    }

    /**
     * Test that after resolution attribute definitions which are marked as dependency only don't have their results
     * show up in the resolved attribute set.
     */
    @Test
    public void testResolveCleanDependencyOnly() throws AttributeResolutionException {
        AttributeResolver resolver = new AttributeResolver("foo");

        Attribute<String> attribute = new Attribute<String>("ad1");
        attribute.setValues(Collections.singletonList("value1"));

        MockAttributeDefinition definition = new MockAttributeDefinition("ad1", attribute);
        definition.setDependencyOnly(true);

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(definition);
        resolver.setAttributeDefinition(definitions);

        AttributeResolutionContext context = new AttributeResolutionContext(null);
        resolver.resolveAttributes(context);

        Assert.assertNotNull(context.getResolvedAttributeDefinition("ad1"));
        Assert.assertTrue(context.getResolvedAttributes().isEmpty());
    }
    
    /** Test that after resolution that the values for a resolved attribute are deduped. */
    @Test
    public void testResolveCleanDuplicateValues() throws AttributeResolutionException {
        AttributeResolver resolver = new AttributeResolver("foo");

        LazyList<String> values = new LazyList<String>();
        values.add("value1");
        values.add("value1");
        
        Attribute<String> attribute = new Attribute<String>("ad1");
        attribute.setValues(values);

        MockAttributeDefinition definition = new MockAttributeDefinition("ad1", attribute);

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(definition);
        resolver.setAttributeDefinition(definitions);

        AttributeResolutionContext context = new AttributeResolutionContext(null);
        resolver.resolveAttributes(context);

        Assert.assertNotNull(context.getResolvedAttributeDefinition("ad1"));
        Assert.assertTrue(context.getResolvedAttributes().containsKey("ad1"));
        Assert.assertEquals(context.getResolvedAttributes().get("ad1").getValues().size(), 1);
    }

    /**
     * Test that after resolution attribute definitions whose resultant attribute contains no value don't have their
     * results show up in the resolved attribute set.
     */
    @Test
    public void testResolveCleanEmptyValueAttributes() throws AttributeResolutionException {
        AttributeResolver resolver = new AttributeResolver("foo");

        Attribute<String> attribute = new Attribute<String>("ad1");
        attribute.setValues(Collections.EMPTY_LIST);

        MockAttributeDefinition definition = new MockAttributeDefinition("ad1", attribute);
        definition.setDependencyOnly(true);

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(definition);
        resolver.setAttributeDefinition(definitions);

        AttributeResolutionContext context = new AttributeResolutionContext(null);
        resolver.resolveAttributes(context);

        Assert.assertNotNull(context.getResolvedAttributeDefinition("ad1"));
        Assert.assertTrue(context.getResolvedAttributes().isEmpty());
    }

    /** Test a simple, expected-to-be-valid, configuration. */
    @Test
    public void testSimpleValidate() throws ComponentValidationException {
        AttributeResolver resolver = new AttributeResolver("foo");

        MockDataConnector dc1 = new MockDataConnector("dc1", (Map)null);

        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new Attribute<String>("test"));
        ad1.addDependency(new ResolverPluginDependency("dc1", null));

        MockAttributeDefinition ad2 = new MockAttributeDefinition("ad2", new Attribute<String>("test"));
        ad2.addDependency(new ResolverPluginDependency("dc1", null));

        MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", new Attribute<String>("test"));
        ad0.addDependency(new ResolverPluginDependency("ad1", null));
        ad0.addDependency(new ResolverPluginDependency("ad2", null));

        LazySet<BaseDataConnector> connectors = new LazySet<BaseDataConnector>();
        connectors.add(dc1);
        resolver.setDataConnectors(connectors);

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad0);
        definitions.add(ad1);
        definitions.add(ad2);
        resolver.setAttributeDefinition(definitions);

        resolver.validate();
    }
    
    /** Test validation when a plugin throws a validation exception. */
    @Test
    public void testInvalidPluginValidate() {
        AttributeResolver resolver = new AttributeResolver("foo");
        
        MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", new Attribute<String>("test"));
        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", (Attribute)null);
        ad1.setInvalid(true);
        
        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad0);
        definitions.add(ad1);
        resolver.setAttributeDefinition(definitions);
        
        try{
            resolver.validate();
            Assert.fail("resolver with invalid plugin didn't fail validation");
        }catch(ComponentValidationException e){
            // expected this
        }
    }

    /** Tests that an invalid data connector fails over to the failover connector if its invalid. */
    @Test
    public void testDataConnectorFailoverDuringValidate() throws ComponentValidationException{
        AttributeResolver resolver = new AttributeResolver("foo");
        
        MockDataConnector dc0 = new MockDataConnector("dc0", (Map)null);
        MockDataConnector dc1 = new MockDataConnector("dc1", (Map)null);
        dc1.setInvalid(true);
        dc1.setFailoverDataConnectorId("dc0");
        
        LazySet<BaseDataConnector> connectors = new LazySet<BaseDataConnector>();
        connectors.add(dc0);
        connectors.add(dc1);
        resolver.setDataConnectors(connectors);
        
        resolver.validate();
    }
    
    /** Test that validation fails when a plugin depends on a non-existent plugin. */
    @Test
    public void testBadPluginIdValidate() throws ComponentValidationException {
        AttributeResolver resolver = new AttributeResolver("foo");

        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new Attribute<String>("test"));
        ad1.addDependency(new ResolverPluginDependency("dc1", null));

        MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", new Attribute<String>("test"));
        ad0.addDependency(new ResolverPluginDependency("ad1", null));

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad0);
        definitions.add(ad1);
        resolver.setAttributeDefinition(definitions);

        try {
            resolver.validate();
            Assert.fail("invalid resolver configuration didn't fail validation");
        } catch (ComponentValidationException e) {
            // expected this
        }

        ad0 = new MockAttributeDefinition("ad0", new Attribute<String>("test"));
        ad0.addDependency(new ResolverPluginDependency("ad0", null));
        definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad0);
        resolver.setAttributeDefinition(definitions);

        try {
            resolver.validate();
            Assert.fail("invalid resolver configuration didn't fail validation");
        } catch (ComponentValidationException e) {
            // expected this
        }
    }

    /** Test that validation fails when there are circular dependencies between plugins. */
    @Test
    public void testCircularDependencyValidation() {
        AttributeResolver resolver = new AttributeResolver("foo");

        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new Attribute<String>("test"));
        ad1.addDependency(new ResolverPluginDependency("ad1", null));

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad1);
        resolver.setAttributeDefinition(definitions);

        try {
            resolver.validate();
            Assert.fail("invalid resolver configuration didn't fail validation");
        } catch (ComponentValidationException e) {
            // expected this
        }

        MockDataConnector dc1 = new MockDataConnector("dc1", (Map)null);
        dc1.addDependency(new ResolverPluginDependency("ad0", null));

        ad1 = new MockAttributeDefinition("ad1", new Attribute<String>("test"));
        ad1.addDependency(new ResolverPluginDependency("dc1", null));

        MockAttributeDefinition ad2 = new MockAttributeDefinition("ad2", new Attribute<String>("test"));
        ad2.addDependency(new ResolverPluginDependency("dc1", null));

        MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", new Attribute<String>("test"));
        ad0.addDependency(new ResolverPluginDependency("ad1", null));
        ad0.addDependency(new ResolverPluginDependency("ad2", null));

        LazySet<BaseDataConnector> connectors = new LazySet<BaseDataConnector>();
        connectors.add(dc1);
        resolver.setDataConnectors(connectors);

        definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad0);
        definitions.add(ad1);
        definitions.add(ad2);
        resolver.setAttributeDefinition(definitions);

        try {
            resolver.validate();
            Assert.fail("invalid resolver configuration didn't fail validation");
        } catch (ComponentValidationException e) {
            // expected this
        }
    }
}