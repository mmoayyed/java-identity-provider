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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Sets;

//TODO test during resolve: failover, requested attributes, bad pluginIDs, plugin throwing error, error propagation

/** Test case for {@link AttributeResolver}. */
public class AttributeResolverTest {
    private final Logger log = LoggerFactory.getLogger(AttributeResolverTest.class);

    /** Test post-instantiation state. */
    @Test public void initVerifyDestroy() throws Exception {
        MockAttributeDefinition attrDef = new MockAttributeDefinition("foo", new Attribute("test"));
        MockDataConnector dataCon = new MockDataConnector("bar", (Map) null);
        AttributeResolver resolver =
                new AttributeResolver("toto", Collections.singleton((BaseAttributeDefinition) attrDef),
                        Collections.singleton((BaseDataConnector) dataCon));

        Assert.assertFalse(attrDef.isInitialized());
        Assert.assertFalse(attrDef.getValidateCount() > 0);
        Assert.assertFalse(attrDef.isDestroyed());
        Assert.assertFalse(dataCon.isInitialized());
        Assert.assertFalse(dataCon.getValidateCount() > 0);
        Assert.assertFalse(dataCon.isDestroyed());

        try {
            resolver.validate();
            Assert.fail();
        } catch (UninitializedComponentException e) {
            // OK
        }
        Assert.assertFalse(attrDef.isInitialized());
        Assert.assertFalse(attrDef.getValidateCount() > 0);
        Assert.assertFalse(attrDef.isDestroyed());
        Assert.assertFalse(dataCon.isInitialized());
        Assert.assertFalse(dataCon.getValidateCount() > 0);
        Assert.assertFalse(dataCon.isDestroyed());

        resolver.initialize();
        Assert.assertTrue(attrDef.isInitialized());
        Assert.assertFalse(attrDef.getValidateCount() > 0);
        Assert.assertFalse(attrDef.isDestroyed());
        Assert.assertTrue(dataCon.isInitialized());
        Assert.assertFalse(dataCon.getValidateCount() > 0);
        Assert.assertFalse(dataCon.isDestroyed());

        Assert.assertEquals(resolver.getId(), "toto");
        Assert.assertEquals(resolver.getAttributeDefinitions().size(), 1);
        Assert.assertTrue(resolver.getAttributeDefinitions().containsKey("foo"));
        Assert.assertEquals(resolver.getDataConnectors().size(), 1);
        Assert.assertTrue(resolver.getDataConnectors().containsKey("bar"));

        resolver.validate();
        Assert.assertTrue(attrDef.isInitialized());
        Assert.assertTrue(attrDef.getValidateCount() > 0);
        Assert.assertFalse(attrDef.isDestroyed());
        Assert.assertTrue(dataCon.isInitialized());
        Assert.assertTrue(dataCon.getValidateCount() > 0);
        Assert.assertFalse(dataCon.isDestroyed());

        resolver.destroy();
        Assert.assertTrue(attrDef.isInitialized());
        Assert.assertTrue(attrDef.getValidateCount() > 0);
        Assert.assertTrue(attrDef.isDestroyed());
        Assert.assertTrue(dataCon.isInitialized());
        Assert.assertTrue(dataCon.getValidateCount() > 0);
        Assert.assertTrue(dataCon.isDestroyed());

        try {
            resolver.initialize();
            Assert.fail();
        } catch (DestroyedComponentException e) {
            // OK
        }

        try {
            resolver.validate();
            Assert.fail();
        } catch (DestroyedComponentException e) {
            // OK
        }
    }

    /** Test getting, setting, overwriting, defensive collection copy. */
    @Test public void setAttributeDefinitions() throws Exception {
        ArrayList<BaseAttributeDefinition> definitions = new ArrayList<BaseAttributeDefinition>();
        definitions.add(new MockAttributeDefinition("foo", new Attribute("test")));
        definitions.add(null);
        definitions.add(new MockAttributeDefinition("bar", new Attribute("test")));

        AttributeResolver resolver = new AttributeResolver(" foo ", definitions, null);
        resolver.initialize();
        Assert.assertNotNull(resolver.getAttributeDefinitions());
        Assert.assertEquals(resolver.getAttributeDefinitions().size(), 2);
        
        definitions.add(new MockAttributeDefinition("foo", new Attribute("test")));
        try {
            new AttributeResolver(" foo ", definitions, null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    /** Test getting, setting, overwriting, defensive collection copy. */
    @Test public void setDataConnectors() throws Exception {
        ArrayList<BaseDataConnector> connectors = new ArrayList<BaseDataConnector>();
        connectors.add(new MockDataConnector("foo", (Map) null));
        connectors.add(null);
        connectors.add(new MockDataConnector("bar", (Map) null));

        AttributeResolver resolver = new AttributeResolver("foo", null, connectors);
        Assert.assertNotNull(resolver.getDataConnectors());
        Assert.assertEquals(resolver.getDataConnectors().size(), 2);

        connectors.add(new MockDataConnector("foo", (Map) null));
        try {
            new AttributeResolver(" foo ", null, connectors);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    /** Test that a simple resolve returns the expected results. */
    @Test public void resolve() throws Exception {
        Attribute attribute = new Attribute("ad1");
        attribute.getValues().add(new StringAttributeValue("value1"));

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(new MockAttributeDefinition("ad1", attribute));

        AttributeResolver resolver = new AttributeResolver("foo", definitions, null);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertNotNull(context.getResolvedAttributeDefinitions().get("ad1"));
        Assert.assertEquals(context.getResolvedAttributes().size(), 1);
        Assert.assertEquals(context.getResolvedAttributes().get("ad1"), attribute);
    }

    /** Test that a simple resolve returns the expected results. */
    @Test public void resolveSpecificAttribute() throws Exception {
        Attribute attribute = new Attribute("ad1");
        attribute.getValues().add(new StringAttributeValue("value1"));

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(new MockAttributeDefinition("ad1", attribute));

        AttributeResolver resolver = new AttributeResolver("foo", definitions, null);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        context.setRequestedAttributes(Collections.singleton(new Attribute("ad1")));
        resolver.resolveAttributes(context);

        Assert.assertNotNull(context.getResolvedAttributeDefinitions().get("ad1"));
        Assert.assertEquals(context.getResolvedAttributes().size(), 1);
        Assert.assertEquals(context.getResolvedAttributes().get("ad1"), attribute);

        context = new AttributeResolutionContext();
        context.setRequestedAttributes(Collections.singleton(new Attribute("1da")));
        resolver.resolveAttributes(context);

        Assert.assertTrue(context.getResolvedAttributeDefinitions().isEmpty());
    }

    /** Test that a simple resolve returns the expected results. */
    @Test public void resolveFails() throws Exception {
        log.debug("Log Resolve fails");
        Attribute attribute = new Attribute("ad1");
        attribute.getValues().add(new StringAttributeValue("value1"));

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        BaseAttributeDefinition attrDef = new MockAttributeDefinition("ad1", new ResolutionException());
        attrDef.setPropagateResolutionExceptions(true);
        definitions.add(attrDef);

        AttributeResolver resolver = new AttributeResolver("foo", definitions, null);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
            Assert.fail();
        } catch (ResolutionException e) {
            // OK
        }

        definitions = new LazySet<BaseAttributeDefinition>();
        attrDef = new MockAttributeDefinition("ad1", new ResolutionException());
        attrDef.setPropagateResolutionExceptions(false);
        definitions.add(attrDef);

        resolver = new AttributeResolver("foo", definitions, null);
        resolver.initialize();

        context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertTrue(context.getResolvedAttributes().isEmpty());
        log.debug("Logged Resolve fails");
    }

    /** Test that a resolve with no definitions returns nothing the expected results. */
    @Test public void resolveEmpty() throws Exception {
        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();

        AttributeResolver resolver = new AttributeResolver("foo", definitions, null);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertTrue(context.getResolvedAttributeDefinitions().isEmpty());
    }

    /** Test that resolve w/ dependencies returns the expected results. */
    @Test public void resolveWithDependencies() throws Exception {
        MockDataConnector dc1 = new MockDataConnector("dc1", (Map) null);

        ResolverPluginDependency dep1 = new ResolverPluginDependency("dc1");
        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new Attribute("test"));
        ad1.setDependencies(Sets.newHashSet(dep1));

        MockAttributeDefinition ad2 = new MockAttributeDefinition("ad2", new Attribute("test"));

        ResolverPluginDependency dep2 = new ResolverPluginDependency("ad1");
        ResolverPluginDependency dep3 = new ResolverPluginDependency("ad2");
        MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", new Attribute("test"));
        ad0.setDependencies(Sets.newHashSet(dep2, dep3));

        LazySet<BaseDataConnector> connectors = new LazySet<BaseDataConnector>();
        connectors.add(dc1);

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad0);
        definitions.add(ad1);
        definitions.add(ad2);

        AttributeResolver resolver = new AttributeResolver("foo", definitions, connectors);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertNotNull(context.getResolvedAttributeDefinitions().get("ad0"));
        Assert.assertNotNull(context.getResolvedAttributeDefinitions().get("ad1"));
        Assert.assertNotNull(context.getResolvedAttributeDefinitions().get("ad2"));

        Assert.assertNotNull(context.getResolvedDataConnectors().get("dc1"));
    }

    /**
     * Test that resolve w/ dependencies returns the expected results.
     * 
     * @throws ComponentInitializationException if badness happens
     * @throws ResolutionException if badness happens in attribute resolution
     */
    @Test public void resolveWithDependencyFail1() throws ComponentInitializationException, ResolutionException
    /* throws Exception */{
        MockDataConnector dc1 = new MockDataConnector("dc1", new ResolutionException());
        dc1.setFailoverDataConnectorId("dc2");

        ResolverPluginDependency dep1 = new ResolverPluginDependency("dc1");
        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new Attribute("test"));
        ad1.setDependencies(Sets.newHashSet(dep1));

        LazySet<BaseDataConnector> connectors = new LazySet<BaseDataConnector>();
        connectors.add(dc1);

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad1);

        AttributeResolver resolver = new AttributeResolver("foo", definitions, connectors);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        //
        // This will pass since we failover to a nonexistant connector, but we silently allow that through.
        // The error in configuration is tested by validate()
        //
        resolver.resolveAttributes(context);
    }

    /**
     * Test that resolve w/ dependencies returns the expected results.
     * 
     * @throws ComponentInitializationException if badness happens
     * @throws ResolutionException if badness happens in attribute resolution
     */
    @Test public void resolveDataConnectorFail() throws ComponentInitializationException, ResolutionException {
        MockDataConnector dc1 = new MockDataConnector("dc1", new ResolutionException());

        ResolverPluginDependency dep1 = new ResolverPluginDependency("dc1");
        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new Attribute("test"));
        ad1.setDependencies(Sets.newHashSet(dep1));

        LazySet<BaseDataConnector> connectors = new LazySet<BaseDataConnector>();
        connectors.add(dc1);

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad1);

        AttributeResolver resolver = new AttributeResolver("foo", definitions, connectors);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
            Assert.fail();
        } catch (ResolutionException e) {
            //
            // OK
        }
    }

    @Test public void cachedDataConnectorDependency() throws ComponentInitializationException, ResolutionException {
        MockDataConnector dc1 = new MockDataConnector("dc1", (Map) null);

        ResolverPluginDependency dep1 = new ResolverPluginDependency("dc1");
        Attribute attr = new Attribute("test1");
        attr.getValues().add(new StringAttributeValue("value1"));
        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", attr);
        ad1.setDependencies(Sets.newHashSet(dep1));

        attr = new Attribute("test2");
        attr.getValues().add(new StringAttributeValue("value2"));
        MockAttributeDefinition ad2 = new MockAttributeDefinition("ad2", attr);
        ad2.setDependencies(Sets.newHashSet(new ResolverPluginDependency("dc1")));

        LazySet<BaseDataConnector> connectors = new LazySet<BaseDataConnector>();
        connectors.add(dc1);

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad1);
        definitions.add(ad2);

        AttributeResolver resolver = new AttributeResolver("foo", definitions, connectors);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertNotNull(context.getResolvedAttributeDefinitions().get("ad1"));
        Assert.assertNotNull(context.getResolvedAttributeDefinitions().get("ad2"));
        Assert.assertEquals(context.getResolvedAttributes().size(), 2);

        MockDataConnector dcfail1 = new MockDataConnector("failer1", new ResolutionException());
        dcfail1.setInvalid(true);
        dcfail1.setPropagateResolutionExceptions(false);
        ResolverPluginDependency depFail1 = new ResolverPluginDependency("failer1");
        MockDataConnector dcfail2 = new MockDataConnector("failer2", new ResolutionException());
        dcfail2.setFailoverDataConnectorId("failer1");
        dcfail2.setInvalid(true);
        ResolverPluginDependency depFail2 = new ResolverPluginDependency("failer2");

        connectors = new LazySet<BaseDataConnector>();
        connectors.add(dcfail1);
        connectors.add(dcfail2);

        MockAttributeDefinition ad10 = new MockAttributeDefinition("ad10", new Attribute("ten"));
        ad10.setDependencies(Sets.newHashSet(depFail1));
        ad10.setPropagateResolutionExceptions(false);

        MockAttributeDefinition ad11 = new MockAttributeDefinition("ad11", new Attribute("eleven"));
        ad11.setDependencies(Sets.newHashSet(depFail2));
        ad11.setPropagateResolutionExceptions(false);

        definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad10);
        definitions.add(ad11);

        resolver = new AttributeResolver("failoverTest", definitions, connectors);
        resolver.initialize();

        try {
            resolver.validate();
            Assert.fail();
        } catch (ComponentValidationException e) {
            // OK
        }

    }

    @Test public void dataConnectorWithDataDependency() throws ComponentInitializationException,
            ResolutionException {
        Map<String, Attribute> values = new HashMap<String, Attribute>(1);
        Attribute attr = new Attribute("SubAttribute");
        attr.getValues().add(new StringAttributeValue("SubValue1"));

        values.put("SubAttribute", attr);
        MockDataConnector dc1 = new MockDataConnector("dc1", values);

        ResolverPluginDependency dep1 = new ResolverPluginDependency("dc1");
        dep1.setDependencyAttributeId("SubAttribute");
        attr = new Attribute("test1");
        attr.getValues().add(new StringAttributeValue("value1"));
        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", attr);
        ad1.setDependencies(Sets.newHashSet(dep1));

        LazySet<BaseDataConnector> connectors = new LazySet<BaseDataConnector>();
        connectors.add(dc1);

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad1);
        AttributeResolver resolver = new AttributeResolver("foo", definitions, connectors);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertNotNull(context.getResolvedAttributeDefinitions().get("ad1"));
        Assert.assertEquals(context.getResolvedAttributes().size(), 1);
        Assert.assertEquals(context.getResolvedDataConnectors().size(), 1);
    }

    /**
     * Test that after resolution attribute definitions which returned null values don't have their results show up in
     * the resolved attribute set.
     */
    @Test public void resolveCleanNullAttributes() throws Exception {
        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(new MockAttributeDefinition("ad1", new Attribute("test")));

        AttributeResolver resolver = new AttributeResolver("foo", definitions, null);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertNotNull(context.getResolvedAttributeDefinitions().get("ad1"));
        Assert.assertTrue(context.getResolvedAttributes().isEmpty());
    }

    /**
     * Test that after resolution attribute definitions which are marked as dependency only don't have their results
     * show up in the resolved attribute set.
     */
    @Test public void resolveCleanDependencyOnly() throws Exception {
        Attribute attribute = new Attribute("ad1");
        attribute.getValues().add(new StringAttributeValue("value1"));

        MockAttributeDefinition definition = new MockAttributeDefinition("ad1", attribute);
        definition.setDependencyOnly(true);

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(definition);

        AttributeResolver resolver = new AttributeResolver("foo", definitions, null);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertNotNull(context.getResolvedAttributeDefinitions().get("ad1"));
        Assert.assertTrue(context.getResolvedAttributes().isEmpty());
    }

    /** Test that after resolution that the values for a resolved attribute are deduped. */
    @Test public void resolveCleanDuplicateValues() throws Exception {
        Attribute attribute = new Attribute("ad1");
        attribute.getValues().addAll(
                Sets.newHashSet(new StringAttributeValue("value1"), new StringAttributeValue("value1")));

        MockAttributeDefinition definition = new MockAttributeDefinition("ad1", attribute);

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(definition);

        AttributeResolver resolver = new AttributeResolver("foo", definitions, null);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertNotNull(context.getResolvedAttributeDefinitions().get("ad1"));
        Assert.assertTrue(context.getResolvedAttributes().containsKey("ad1"));
        Assert.assertEquals(context.getResolvedAttributes().get("ad1").getValues().size(), 1);
    }

    /**
     * Test that after resolution attribute definitions whose resultant attribute contains no value don't have their
     * results show up in the resolved attribute set.
     */
    @Test public void resolveCleanEmptyValueAttributes() throws Exception {
        Attribute attribute = new Attribute("ad1");

        MockAttributeDefinition definition = new MockAttributeDefinition("ad1", attribute);
        definition.setDependencyOnly(true);

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(definition);

        AttributeResolver resolver = new AttributeResolver("foo", definitions, null);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertNotNull(context.getResolvedAttributeDefinitions().get("ad1"));
        Assert.assertTrue(context.getResolvedAttributes().isEmpty());
    }

    /** Test a simple, expected-to-be-valid, configuration. */
    @Test public void simpleValidate() throws Exception {
        MockDataConnector dc1 = new MockDataConnector("dc1", (Map) null);

        ResolverPluginDependency dep1 = new ResolverPluginDependency("dc1");
        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new Attribute("test"));
        ad1.setDependencies(Sets.newHashSet(dep1));

        MockAttributeDefinition ad2 = new MockAttributeDefinition("ad2", new Attribute("test"));

        ResolverPluginDependency dep2 = new ResolverPluginDependency("ad1");
        ResolverPluginDependency dep3 = new ResolverPluginDependency("ad2");
        MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", new Attribute("test"));
        ad0.setDependencies(Sets.newHashSet(dep2, dep3));

        LazySet<BaseDataConnector> connectors = new LazySet<BaseDataConnector>();
        connectors.add(dc1);

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad0);
        definitions.add(ad1);
        definitions.add(ad2);

        AttributeResolver resolver = new AttributeResolver("foo", definitions, connectors);
        resolver.initialize();

        resolver.validate();
    }

    /** Test validation when a plugin throws a validation exception. */
    @Test public void invalidPluginValidate() throws Exception {
        MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", new Attribute("test"));
        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", (Attribute) null);
        ad1.setInvalid(true);

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad0);
        definitions.add(ad1);

        AttributeResolver resolver = new AttributeResolver("foo", definitions, null);
        resolver.initialize();

        try {
            resolver.validate();
            Assert.fail("resolver with invalid plugin didn't fail validation");
        } catch (ComponentValidationException e) {
            // expected this
        }
    }

    /** Tests that an invalid data connector fails over to the failover connector if its invalid. */
    @Test public void dataConnectorFailoverDuringValidate() throws Exception {
        MockDataConnector dc0 = new MockDataConnector("dc0", (Map) null);
        MockDataConnector dc1 = new MockDataConnector("dc1", (Map) null);
        dc1.setInvalid(true);
        dc1.setFailoverDataConnectorId("dc0");

        LazySet<BaseDataConnector> connectors = new LazySet<BaseDataConnector>();
        connectors.add(dc0);
        connectors.add(dc1);

        AttributeResolver resolver = new AttributeResolver("foo", null, connectors);
        resolver.initialize();

        resolver.validate();
    }

    @Test public void dataConnectorFailovertoInvalid() throws Exception {
        MockDataConnector dc1 = new MockDataConnector("dc1", (Map) null);
        dc1.setInvalid(true);
        dc1.setFailoverDataConnectorId("dc0");

        LazySet<BaseDataConnector> connectors = new LazySet<BaseDataConnector>();
        connectors.add(dc1);

        AttributeResolver resolver = new AttributeResolver("foo", null, connectors);
        resolver.initialize();

        try {
            resolver.validate();
            Assert.fail("resolver with invalid plugin didn't fail validation");
        } catch (ComponentValidationException e) {
            // expected this
        }

        MockDataConnector dc0 = new MockDataConnector("dc0", (Map) null);
        dc0.setInvalid(true);
        dc1 = new MockDataConnector("dc1", (Map) null);
        dc1.setInvalid(true);
        dc1.setFailoverDataConnectorId("dc0");

        connectors = new LazySet<BaseDataConnector>();
        connectors.add(dc1);
        connectors.add(dc0);

        resolver = new AttributeResolver("foo", null, connectors);
        resolver.initialize();

        try {
            resolver.validate();
            Assert.fail("resolver with invalid plugin didn't fail validation");
        } catch (ComponentValidationException e) {
            // expected this
        }

        //
        // try again to provoke a different error path
        //
        try {
            resolver.validate();
            Assert.fail("resolver with invalid plugin didn't fail validation");
        } catch (ComponentValidationException e) {
            // expected this
        }
    }

    /** Test that validation fails when a plugin depends on a non-existent plugin. */
    @Test public void badPluginIdInitialize() throws Exception {
        ResolverPluginDependency dep1 = new ResolverPluginDependency("dc1");
        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new Attribute("test"));
        ad1.setDependencies(Sets.newHashSet(dep1));

        ResolverPluginDependency dep2 = new ResolverPluginDependency("ad1");
        MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", new Attribute("test"));
        ad0.setDependencies(Sets.newHashSet(dep2));

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad0);
        definitions.add(ad1);

        AttributeResolver resolver = new AttributeResolver("foo", definitions, null);
        try {
            resolver.initialize();
            Assert.fail("invalid resolver configuration didn't fail initialization");
        } catch (ComponentInitializationException e) {
            // OK
        }

        ResolverPluginDependency dep3 = new ResolverPluginDependency("ad0");
        ad0 = new MockAttributeDefinition("ad0", new Attribute("test"));
        ad0.setDependencies(Sets.newHashSet(dep3));
        definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad0);

        resolver = new AttributeResolver("foo", definitions, null);

        try {
            resolver.initialize();
            Assert.fail("invalid resolver configuration didn't fail initialization");
        } catch (ComponentInitializationException e) {
            // expected this
        }
    }

    /** Test that validation fails when there are circular dependencies between plugins. */
    @Test public void circularDependencyInitialize() throws Exception {
        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new Attribute("test"));
        ad1.setDependencies(Sets.newHashSet(new ResolverPluginDependency("ad1")));

        LazySet<BaseAttributeDefinition> definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad1);
        AttributeResolver resolver = new AttributeResolver("foo", definitions, null);

        try {
            resolver.initialize();
            Assert.fail("invalid resolver configuration didn't fail initialization.");
        } catch (ComponentInitializationException e) {
            // OK
        }

        MockDataConnector dc1 = new MockDataConnector("dc1", (Map) null);
        dc1.setDependencies(Sets.newHashSet(new ResolverPluginDependency("ad0")));

        ad1 = new MockAttributeDefinition("ad1", new Attribute("test"));
        ad1.setDependencies(Sets.newHashSet(new ResolverPluginDependency("dc1")));

        MockAttributeDefinition ad2 = new MockAttributeDefinition("ad2", new Attribute("test"));
        ad2.setDependencies(Sets.newHashSet(new ResolverPluginDependency("dc1")));

        MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", new Attribute("test"));
        ad0.setDependencies(Sets.newHashSet(new ResolverPluginDependency("ad1"), new ResolverPluginDependency(
                "ad2")));

        LazySet<BaseDataConnector> connectors = new LazySet<BaseDataConnector>();
        connectors.add(dc1);

        definitions = new LazySet<BaseAttributeDefinition>();
        definitions.add(ad0);
        definitions.add(ad1);
        definitions.add(ad2);

        resolver = new AttributeResolver("foo", definitions, connectors);
        try {
            resolver.initialize();
            Assert.fail("invalid resolver configuration didn't fail initialization");
        } catch (ComponentInitializationException e) {
            // expected this
        }
    }
}