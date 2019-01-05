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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue.EmptyType;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AbstractAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.LegacyPrincipalDecoder;
import net.shibboleth.idp.attribute.resolver.MockAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.MockDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolverAttributeDefinitionDependency;
import net.shibboleth.idp.attribute.resolver.ResolverDataConnectorDependency;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.idp.attribute.resolver.ad.impl.SimpleAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;

/** Test case for {@link AttributeResolverImpl}. */
public class AttributeResolverImplTest {
    private final Logger log = LoggerFactory.getLogger(AttributeResolverImplTest.class);

    /** Test post-instantiation state. */
    @Test public void initDestroy() throws Exception {
        final MockAttributeDefinition attrDef = new MockAttributeDefinition("foo", new IdPAttribute("test"));
        final MockDataConnector dataCon = new MockDataConnector("bar", (Map) null);
        dataCon.initialize();
        final AttributeResolverImpl resolver =
                newAttributeResolverImpl("toto", Collections.singleton((AttributeDefinition) attrDef),
                        Collections.singleton((DataConnector) dataCon));

        Assert.assertFalse(attrDef.isInitialized());
        Assert.assertFalse(attrDef.isDestroyed());
        Assert.assertFalse(dataCon.isDestroyed());

        attrDef.initialize();
        resolver.initialize();
        Assert.assertTrue(attrDef.isInitialized());
        Assert.assertFalse(attrDef.isDestroyed());
        Assert.assertTrue(dataCon.isInitialized());
        Assert.assertFalse(dataCon.isDestroyed());

        Assert.assertEquals(resolver.getId(), "toto");
        Assert.assertEquals(resolver.getAttributeDefinitions().size(), 1);
        Assert.assertTrue(resolver.getAttributeDefinitions().containsKey("foo"));
        Assert.assertEquals(resolver.getDataConnectors().size(), 1);
        Assert.assertTrue(resolver.getDataConnectors().containsKey("bar"));

        attrDef.destroy();
        resolver.destroy();
        dataCon.destroy();
        Assert.assertTrue(attrDef.isInitialized());
        Assert.assertTrue(attrDef.isDestroyed());
        Assert.assertTrue(dataCon.isInitialized());
        Assert.assertTrue(dataCon.isDestroyed());

        try {
            resolver.initialize();
            Assert.fail();
        } catch (final DestroyedComponentException e) {
            // OK
        }

    }

    /** Test getting, setting, overwriting, defensive collection copy. */
    @Test public void setAttributeDefinitions() throws Exception {
        final ArrayList<AttributeDefinition> definitions = new ArrayList<>();
        definitions.add(new MockAttributeDefinition("foo", new IdPAttribute("test")));
        definitions.add(null);
        definitions.add(new MockAttributeDefinition("bar", new IdPAttribute("test")));

        final AttributeResolverImpl resolver = newAttributeResolverImpl(" foo ", definitions, null);
        resolver.initialize();
        Assert.assertNotNull(resolver.getAttributeDefinitions());
        Assert.assertEquals(resolver.getAttributeDefinitions().size(), 2);

        definitions.add(new MockAttributeDefinition("foo", new IdPAttribute("test")));
        try {
            newAttributeResolverImpl(" foo ", definitions, null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // OK
        }
    }

    /** Test getting, setting, overwriting, defensive collection copy. */
    @Test public void setDataConnectors() throws Exception {
        final ArrayList<DataConnector> connectors = new ArrayList<>();
        connectors.add(new MockDataConnector("foo", (Map) null));
        connectors.add(null);
        connectors.add(new MockDataConnector("bar", (Map) null));

        final AttributeResolver resolver = newAttributeResolverImpl("foo", null, connectors);
        Assert.assertNotNull(resolver.getDataConnectors());
        Assert.assertEquals(resolver.getDataConnectors().size(), 2);

        connectors.add(new MockDataConnector("foo", (Map) null));
        try {
            newAttributeResolverImpl(" foo ", null, connectors);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // OK
        }
    }

    /** Test that a simple resolve returns the expected results. */
    @Test public void resolve() throws Exception {
        final IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.setValues(Collections.singletonList(new StringAttributeValue("value1")));

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(new MockAttributeDefinition("ad1", attribute));
        definitions.iterator().next().initialize();

        final AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, null);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertEquals(context.getResolvedIdPAttributes().size(), 1);
        Assert.assertEquals(context.getResolvedIdPAttributes().get("ad1"), attribute);
    }

    /** Test that a simple resolve returns the expected results. */
    @Test public void resolveSpecificAttribute() throws Exception {
        final IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.setValues(Collections.singletonList(new StringAttributeValue("value1")));

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(new MockAttributeDefinition("ad1", attribute));
        definitions.iterator().next().initialize();

        final AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, null);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        context.setRequestedIdPAttributeNames(Collections.singleton("ad1"));
        resolver.resolveAttributes(context);

        Assert.assertEquals(context.getResolvedIdPAttributes().size(), 1);
        Assert.assertEquals(context.getResolvedIdPAttributes().get("ad1"), attribute);

        context = new AttributeResolutionContext();
        context.setRequestedIdPAttributeNames(Collections.singleton("1da"));
        resolver.resolveAttributes(context);

        Assert.assertTrue(context.getResolvedIdPAttributes().isEmpty());
    }

    /** Test that a simple resolve returns the expected results. */
    @Test public void resolveFails() throws Exception {
        log.debug("Log Resolve fails");
        final IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.setValues(Collections.singletonList(new StringAttributeValue("value1")));

        LazySet<AttributeDefinition> definitions = new LazySet<>();
        AbstractAttributeDefinition attrDef = new MockAttributeDefinition("ad1", new ResolutionException());
        attrDef.setPropagateResolutionExceptions(true);
        definitions.add(attrDef);

        AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, null);
        attrDef.initialize();
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
            Assert.fail();
        } catch (final ResolutionException e) {
            // OK
        }

        definitions = new LazySet<>();
        attrDef = new MockAttributeDefinition("ad1", new ResolutionException());
        attrDef.setPropagateResolutionExceptions(false);
        definitions.add(attrDef);

        resolver = newAttributeResolverImpl("foo", definitions, null);
        resolver.initialize();
        attrDef.initialize();

        context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertTrue(context.getResolvedIdPAttributes().isEmpty());
        log.debug("Logged Resolve fails");
    }

    /** Test that a resolve with no definitions returns nothing the expected results. */
    @Test public void resolveEmpty() throws Exception {
        final LazySet<AttributeDefinition> definitions = new LazySet<>();

        final AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, null);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertTrue(context.getResolvedIdPAttributes().isEmpty());
    }

    /** Test that resolve w/ dependencies returns the expected results. */

    @Test public void resolveWithDependenciesOld() throws Exception {
        final MockDataConnector dc1 = new MockDataConnector("dc1", (Map) null);
        dc1.initialize();

        final IdPAttribute attr = new IdPAttribute("test");
        attr.setValues(Arrays.asList(new StringAttributeValue("a"), new StringAttributeValue("b")));
        
        final ResolverPluginDependency dep1 = new ResolverPluginDependency("dc1");
        final MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", attr);
        ad1.setDependencies(Collections.singleton(dep1));
        ad1.initialize();

        final MockAttributeDefinition ad2 = new MockAttributeDefinition("ad2", attr);

        final ResolverPluginDependency dep2 = TestSources.makeResolverPluginDependency("ad1");
        final ResolverPluginDependency dep3 = TestSources.makeResolverPluginDependency("ad2");
        final MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", attr);
        ad0.setDependencies(new HashSet<>(Arrays.asList(dep2, dep3)));
        ad0.initialize();

        final LazySet<DataConnector> connectors = new LazySet<>();
        connectors.add(dc1);

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(ad0);
        definitions.add(ad1);
        definitions.add(ad2);
        ad2.initialize();

        final AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, connectors);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertEquals(context.getResolvedIdPAttributes().size(), 1);
        Assert.assertEquals(context.getResolvedIdPAttributes().get("test").getValues().size(), 2);
    }
    
    @Test public void resolveWithDependencies() throws Exception {
        final MockDataConnector dc1 = new MockDataConnector("dc1", (Map) null);
        dc1.initialize();

        final IdPAttribute attr = new IdPAttribute("test");
        attr.setValues(Arrays.asList(new StringAttributeValue("a"), new StringAttributeValue("b")));
        
        final ResolverDataConnectorDependency dep1 = new ResolverDataConnectorDependency("dc1");
        dep1.setAllAttributes(true);
        final MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", attr);
        ad1.setDependencies(Collections.singleton((ResolverPluginDependency) dep1));
        ad1.initialize();

        final MockAttributeDefinition ad2 = new MockAttributeDefinition("ad2", attr);

        final ResolverPluginDependency dep2 = new ResolverAttributeDefinitionDependency("ad1");
        final ResolverPluginDependency dep3 = new ResolverAttributeDefinitionDependency("ad2");
        final MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", attr);
        ad0.setDependencies(new HashSet<>(Arrays.asList(dep2, dep3)));
        ad0.initialize();

        final LazySet<DataConnector> connectors = new LazySet<>();
        connectors.add(dc1);

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(ad0);
        definitions.add(ad1);
        definitions.add(ad2);
        ad2.initialize();

        final AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, connectors);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertEquals(context.getResolvedIdPAttributes().size(), 1);
        Assert.assertEquals(context.getResolvedIdPAttributes().get("test").getValues().size(), 2);
    }

    /**
     * Test that resolve w/ dependencies returns the expected results.
     * 
     * @throws ComponentInitializationException if badness happens
     * @throws ResolutionException if badness happens in attribute resolution
     */
    @Test public void resolveWithDependencyFail1() throws ComponentInitializationException, ResolutionException
    /* throws Exception */{
        final MockDataConnector dc1 = new MockDataConnector("dc1", new HashMap<String, IdPAttribute>());
        dc1.setFailoverDataConnectorId("dc2");
        dc1.initialize();
        //

        final ResolverPluginDependency dep1 = TestSources.makeResolverPluginDependency("dc1", null);
        final MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new IdPAttribute("test"));
        ad1.setDependencies(Collections.singleton(dep1));
        ad1.initialize();

        final LazySet<DataConnector> connectors = new LazySet<>();
        connectors.add(dc1);

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(ad1);

        final AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, connectors);
        dc1.initialize();
        ad1.initialize();
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        //
        // This will pass since we failover to a nonexistant connector, but we silently allow that through.
        // The error in configuration is tested by validate()
        //
        resolver.resolveAttributes(context);
    }
    
    @Test(enabled=true) public void resolveWithTimeout() throws Exception {
        
        final IdPAttribute i1Val = new IdPAttribute("Atr");
        i1Val.setValues(Collections.singleton(new StringAttributeValue("value1")));
        
        final MockDataConnector dc1 = new MockDataConnector("dc1", Collections.singletonMap("Atr", i1Val));
        final SimpleAttributeDefinition ad = new SimpleAttributeDefinition();
        ad.setId("output");
        final ResolverPluginDependency dep1 = TestSources.makeResolverPluginDependency("dc1","Atr");
        ad.setDependencies(Collections.singleton(dep1));
        ad.initialize();
        
        final IdPAttribute i2Val = new IdPAttribute("Atr");
        final HashSet<IdPAttributeValue<?>>vals = new HashSet<>();
        vals.add(new StringAttributeValue("value1"));
        vals.add(new StringAttributeValue("value2"));
        i2Val.setValues(vals);
        final MockDataConnector dc2 = new MockDataConnector("dc2", Collections.singletonMap("Atr", i2Val));
        dc2.initialize();
        dc1.setFailoverDataConnectorId("dc2");
        dc1.setNoRetryDelay(3000);
        dc1.initialize();
        
        final HashSet<DataConnector> connectors = new HashSet<>(2);
        connectors.add(dc2);
        connectors.add(dc1);
        
        final AttributeResolverImpl resolver = newAttributeResolverImpl("foo", Collections.singleton((AttributeDefinition)ad), connectors);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        
        resolver.resolveAttributes(context);
        Assert.assertEquals(context.getResolvedIdPAttributes().get("output").getValues().size(), 1);
        
        dc1.setFailure(true);
        
        context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);
        
        Assert.assertEquals(context.getResolvedIdPAttributes().get("output").getValues().size(), 2);

        dc1.setFailure(false);

        context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);
        Assert.assertEquals(context.getResolvedIdPAttributes().get("output").getValues().size(), 2);
        
        Thread.sleep(6200);
        context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);
        Assert.assertEquals(context.getResolvedIdPAttributes().get("output").getValues().size(), 1);
    }

    /**
     * Test that resolve w/ dependencies returns the expected results.
     * 
     * @throws ComponentInitializationException if badness happens
     * @throws ResolutionException if badness happens in attribute resolution
     */
    @Test public void resolveDataConnectorFail() throws ComponentInitializationException, ResolutionException {
        final MockDataConnector dc1 = new MockDataConnector("dc1", new HashMap<String, IdPAttribute>());
        dc1.setFailure(true);
        dc1.initialize();

        final ResolverPluginDependency dep1 = TestSources.makeResolverPluginDependency("dc1", null);
        final MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new IdPAttribute("test"));
        ad1.setDependencies(Collections.singleton(dep1));

        final LazySet<DataConnector> connectors = new LazySet<>();
        connectors.add(dc1);

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(ad1);

        final AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, connectors);
        dc1.initialize();
        ad1.initialize();
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
            Assert.fail();
        } catch (final ResolutionException e) {
            //
            // OK
        }
    }

    @Test public void cachedDataConnectorDependency() throws ComponentInitializationException, ResolutionException {
        final MockDataConnector dc1 = new MockDataConnector("dc1", (Map) null);
        dc1.initialize();

        final ResolverPluginDependency dep1 = TestSources.makeResolverPluginDependency("dc1", null);
        IdPAttribute attr = new IdPAttribute("test1");
        attr.setValues(Collections.singletonList(new StringAttributeValue("value1")));
        final MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", attr);
        ad1.setDependencies(Collections.singleton(dep1));
        ad1.initialize();

        attr = new IdPAttribute("test2");
        attr.setValues(Collections.singletonList(new StringAttributeValue("value2")));
        final MockAttributeDefinition ad2 = new MockAttributeDefinition("ad2", attr);
        ad2.setDependencies(Collections.singleton(TestSources.makeResolverPluginDependency("dc1", null)));
        ad2.initialize();

        LazySet<DataConnector> connectors = new LazySet<>();
        connectors.add(dc1);

        LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(ad1);
        definitions.add(ad2);

        AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, connectors);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertEquals(context.getResolvedIdPAttributes().size(), 2);

        final MockDataConnector dcfail1 = new MockDataConnector("failer1", new HashMap<String, IdPAttribute>());
        dcfail1.setFailure(true);
        dcfail1.setPropagateResolutionExceptions(false);
        dcfail1.initialize();
        final ResolverPluginDependency depFail1 = new ResolverPluginDependency("failer1");
        final MockDataConnector dcfail2 = new MockDataConnector("failer2", new HashMap<String, IdPAttribute>());
        dcfail2.setFailure(true);
        dcfail2.setFailoverDataConnectorId("failer1");
        dcfail2.initialize();
        final ResolverPluginDependency depFail2 = new ResolverPluginDependency("failer2");

        connectors = new LazySet<>();
        connectors.add(dcfail1);
        connectors.add(dcfail2);

        final MockAttributeDefinition ad10 = new MockAttributeDefinition("ad10", new IdPAttribute("ten"));
        ad10.setDependencies(Collections.singleton(depFail1));
        ad10.setPropagateResolutionExceptions(false);
        ad10.initialize();


        final MockAttributeDefinition ad11 = new MockAttributeDefinition("ad11", new IdPAttribute("eleven"));
        ad11.setDependencies(Collections.singleton(depFail2));
        ad11.setPropagateResolutionExceptions(false);
        ad11.initialize();

        definitions = new LazySet<>();
        definitions.add(ad10);
        definitions.add(ad11);

        resolver = newAttributeResolverImpl("failoverTest", definitions, connectors);
        resolver.initialize();

    }

    @Test public void dataConnectorWithDataDependencyOld() throws ComponentInitializationException, ResolutionException {
        final Map<String, IdPAttribute> values = new HashMap<>(1);
        IdPAttribute attr = new IdPAttribute("SubAttribute");
        attr.setValues(Collections.singletonList(new StringAttributeValue("SubValue1")));

        values.put("SubAttribute", attr);
        final MockDataConnector dc1 = new MockDataConnector("dc1", values);
        dc1.initialize();

        final ResolverPluginDependency dep1 = TestSources.makeResolverPluginDependency("dc1", "SubAttribute");
        attr = new IdPAttribute("test1");
        attr.setValues(Collections.singletonList(new StringAttributeValue("value1")));
        final MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", attr);
        ad1.setDependencies(Collections.singleton(dep1));
        ad1.initialize();

        final LazySet<DataConnector> connectors = new LazySet<>();
        connectors.add(dc1);

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(ad1);
        final AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, connectors);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertEquals(context.getResolvedIdPAttributes().size(), 1);
    }
    
    @Test public void dataConnectorWithDataDependency() throws ComponentInitializationException, ResolutionException {
        final Map<String, IdPAttribute> values = new HashMap<>(1);
        IdPAttribute attr = new IdPAttribute("SubAttribute");
        attr.setValues(Collections.singletonList(new StringAttributeValue("SubValue1")));

        values.put("SubAttribute", attr);
        final MockDataConnector dc1 = new MockDataConnector("dc1", values);
        dc1.initialize();

        final ResolverDataConnectorDependency dep1 = new ResolverDataConnectorDependency("dc1");
        dep1.setAttributeNames(Collections.singleton("SubAttribute"));
        attr = new IdPAttribute("test1");
        attr.setValues(Collections.singletonList(new StringAttributeValue("value1")));
        final MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", attr);
        ad1.setDependencies(Collections.singleton((ResolverPluginDependency)dep1));
        ad1.initialize();

        final LazySet<DataConnector> connectors = new LazySet<>();
        connectors.add(dc1);

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(ad1);
        final AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, connectors);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertEquals(context.getResolvedIdPAttributes().size(), 1);
    }

    /**
     * Test that after resolution attribute definitions which returned null values don't have their results show up in
     * the resolved attribute set.
     */
    @Test public void resolveCleanNullAttributes() throws Exception {
        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(new MockAttributeDefinition("ad1", new IdPAttribute("test")));
        definitions.iterator().next().initialize();
        
        final AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, null);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertTrue(context.getResolvedIdPAttributes().isEmpty());
    }

    /**
     * Test that after resolution attribute definitions which are marked as dependency only don't have their results
     * show up in the resolved attribute set.
     */
    @Test public void resolveCleanDependencyOnly() throws Exception {
        final IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.setValues(Collections.singletonList(new StringAttributeValue("value1")));

        final MockAttributeDefinition definition = new MockAttributeDefinition("ad1", attribute);
        definition.setDependencyOnly(true);

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(definition);
        definition.initialize();

        final AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, null);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertTrue(context.getResolvedIdPAttributes().isEmpty());
    }

    /** Test that after resolution that the values for a resolved attribute are deduped. */
    @Test public void resolveCleanDuplicateValues() throws Exception {
        final IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.setValues(Arrays.asList(new StringAttributeValue("value1"), new StringAttributeValue("value1")));

        final MockAttributeDefinition definition = new MockAttributeDefinition("ad1", attribute);

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(definition);
        definition.initialize();

        final AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, null);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertTrue(context.getResolvedIdPAttributes().containsKey("ad1"));
        Assert.assertEquals(context.getResolvedIdPAttributes().get("ad1").getValues().size(), 1);
    }
    
    /** Test that after resolution that the values for a resolved attribute are deduped. */
    @Test public void resolveNullValues() throws Exception {
        final IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.setValues(Arrays.asList(new EmptyAttributeValue(EmptyType.NULL_VALUE), new EmptyAttributeValue(EmptyType.ZERO_LENGTH_VALUE), null));

        final MockAttributeDefinition definition = new MockAttributeDefinition("ad1", attribute);

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(definition);
        definition.initialize();

        AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, null);
        resolver.initialize();

        AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertTrue(context.getResolvedIdPAttributes().containsKey("ad1"));
        Assert.assertEquals(context.getResolvedIdPAttributes().get("ad1").getValues().size(),2);
        
        resolver = newAttributeResolverImpl("foo", definitions, null);
        resolver.setStripNulls(true);
        resolver.initialize();

        context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertTrue(context.getResolvedIdPAttributes().isEmpty());
    }


    /**
     * Test that after resolution attribute definitions whose resultant attribute contains no value don't have their
     * results show up in the resolved attribute set.
     */
    @Test public void resolveCleanEmptyValueAttributes() throws Exception {
        final IdPAttribute attribute = new IdPAttribute("ad1");

        final MockAttributeDefinition definition = new MockAttributeDefinition("ad1", attribute);
        definition.setDependencyOnly(true);

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(definition);
        definition.initialize();

        final AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, null);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        Assert.assertTrue(context.getResolvedIdPAttributes().isEmpty());
    }

    /** Test that validation fails when a plugin depends on a non-existent plugin. */
    @Test public void badPluginIdInitialize() throws Exception {
        final ResolverPluginDependency dep1 = new ResolverDataConnectorDependency("dc1");
        final MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new IdPAttribute("test"));
        ad1.setDependencies(Collections.singleton(dep1));
        ad1.initialize();

        final ResolverPluginDependency dep2 = new ResolverAttributeDefinitionDependency("ad1");
        MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", new IdPAttribute("test"));
        ad0.setDependencies(Collections.singleton(dep2));
        ad0.initialize();

        LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(ad0);
        definitions.add(ad1);

        AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, null);
        try {
            resolver.initialize();
            Assert.fail("invalid resolver configuration didn't fail initialization");
        } catch (final ComponentInitializationException e) {
            // OK
        }

        final ResolverPluginDependency dep3 = new ResolverAttributeDefinitionDependency("ad0");
        ad0 = new MockAttributeDefinition("ad0", new IdPAttribute("test"));
        ad0.setDependencies(Collections.singleton(dep3));
        definitions = new LazySet<>();
        definitions.add(ad0);
        ad0.initialize();

        resolver = newAttributeResolverImpl("foo", definitions, null);

        try {
            resolver.initialize();
            Assert.fail("invalid resolver configuration didn't fail initialization");
        } catch (final ComponentInitializationException e) {
            // expected this
        }
    }
    
    /** Test that validation fails when a plugin depends on a non-existent plugin. */
    @Test public void badPluginIdInitializeOld() throws Exception {
        final ResolverPluginDependency dep1 = TestSources.makeResolverPluginDependency("dc1", "test");
        final MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new IdPAttribute("test"));
        ad1.setDependencies(Collections.singleton(dep1));
        ad1.initialize();

        final ResolverPluginDependency dep2 = TestSources.makeResolverPluginDependency("ad1");
        MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", new IdPAttribute("test"));
        ad0.setDependencies(Collections.singleton(dep2));
        ad0.initialize();

        LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(ad0);
        definitions.add(ad1);

        AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, null);
        try {
            resolver.initialize();
            Assert.fail("invalid resolver configuration didn't fail initialization");
        } catch (final ComponentInitializationException e) {
            // OK
        }

        final ResolverPluginDependency dep3 = TestSources.makeResolverPluginDependency("ad0");
        ad0 = new MockAttributeDefinition("ad0", new IdPAttribute("test"));
        ad0.setDependencies(Collections.singleton(dep3));
        definitions = new LazySet<>();
        definitions.add(ad0);
        ad0.initialize();

        resolver = newAttributeResolverImpl("foo", definitions, null);

        try {
            resolver.initialize();
            Assert.fail("invalid resolver configuration didn't fail initialization");
        } catch (final ComponentInitializationException e) {
            // expected this
        }
    }

    /** Test that validation fails when there are circular dependencies between plugins. */
    @Test public void circularDependencyInitializeOld() throws Exception {
        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new IdPAttribute("test"));
        ad1.setDependencies(Collections.singleton(TestSources.makeResolverPluginDependency("ad1")));

        LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(ad1);
        ad1.initialize();
        AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, null);

        try {
            resolver.initialize();
            Assert.fail("invalid resolver configuration didn't fail initialization.");
        } catch (final ComponentInitializationException e) {
            // OK
        }

        final MockDataConnector dc1 = new MockDataConnector("dc1", null, Collections.singleton(TestSources.makeResolverPluginDependency("ad0")));

        ad1 = new MockAttributeDefinition("ad1", new IdPAttribute("test"));
        ad1.setDependencies(Collections.singleton(TestSources.makeResolverPluginDependency("dc1")));
        ad1.initialize();

        final MockAttributeDefinition ad2 = new MockAttributeDefinition("ad2", new IdPAttribute("test"));
        ad2.setDependencies(Collections.singleton(TestSources.makeResolverPluginDependency("dc1")));
        ad2.initialize();

        final MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", new IdPAttribute("test"));
        ad0.setDependencies(new HashSet<>(Arrays.asList(TestSources.makeResolverPluginDependency("ad1"), TestSources.makeResolverPluginDependency("ad2"))));
        ad0.initialize();

        final LazySet<DataConnector> connectors = new LazySet<>();
        connectors.add(dc1);

        definitions = new LazySet<>();
        definitions.add(ad0);
        definitions.add(ad1);
        definitions.add(ad2);

        resolver = newAttributeResolverImpl("foo", definitions, connectors);
        try {
            resolver.initialize();
            Assert.fail("invalid resolver configuration didn't fail initialization");
        } catch (final ComponentInitializationException e) {
            // expected this
        }
    }


    /** Test that validation fails when there are circular dependencies between plugins. */
    @Test public void circularDependencyInitialize() throws Exception {
        MockAttributeDefinition ad1 = new MockAttributeDefinition("ad1", new IdPAttribute("test"));
        ad1.setDependencies(Collections.singleton((ResolverPluginDependency)new ResolverAttributeDefinitionDependency("ad1")));

        LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(ad1);
        ad1.initialize();
        AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, null);

        try {
            resolver.initialize();
            Assert.fail("invalid resolver configuration didn't fail initialization.");
        } catch (final ComponentInitializationException e) {
            // OK
        }

        final MockDataConnector dc1 = new MockDataConnector("dc1", null, Collections.singleton((ResolverPluginDependency)new ResolverAttributeDefinitionDependency("ad0")));

        ad1 = new MockAttributeDefinition("ad1", new IdPAttribute("test"));
        ad1.setDependencies(Collections.singleton((ResolverPluginDependency)new ResolverDataConnectorDependency("dc1")));
        ad1.initialize();

        final MockAttributeDefinition ad2 = new MockAttributeDefinition("ad2", new IdPAttribute("test"));
        ad2.setDependencies(Collections.singleton((ResolverPluginDependency)new ResolverDataConnectorDependency("dc1")));
        ad2.initialize();

        final MockAttributeDefinition ad0 = new MockAttributeDefinition("ad0", new IdPAttribute("test"));
        ad0.setDependencies(new HashSet<>(Arrays.asList((ResolverPluginDependency)new ResolverAttributeDefinitionDependency("ad1"), new ResolverAttributeDefinitionDependency("ad2"))));
        ad0.initialize();

        final LazySet<DataConnector> connectors = new LazySet<>();
        connectors.add(dc1);

        definitions = new LazySet<>();
        definitions.add(ad0);
        definitions.add(ad1);
        definitions.add(ad2);

        resolver = newAttributeResolverImpl("foo", definitions, connectors);
        try {
            resolver.initialize();
            Assert.fail("invalid resolver configuration didn't fail initialization");
        } catch (final ComponentInitializationException e) {
            // expected this
        }
    }

    @Test public void testPrincipalConnector() throws ComponentInitializationException, ResolutionException {
        final LazySet<AttributeDefinition> definitions = new LazySet<>();

        AttributeResolverImpl resolver = newAttributeResolverImpl("foo", definitions, null);
        resolver.initialize();

        Assert.assertNull(resolver.canonicalize(null));

        resolver =
                newAttributeResolverWithPrincipalConnector("foo", definitions, null,
                        new LegacyPrincipalDecoder() {

                            @Override @Nullable public String canonicalize(final SubjectCanonicalizationContext context)
                                    throws ResolutionException {
                                return "Principal";
                            }

                            @Override public boolean hasValidConnectors() {
                                return true;
                            }
                        });
        resolver.initialize();
        Assert.assertEquals(resolver.canonicalize(null), "Principal");
    }

    public static AttributeResolverImpl newAttributeResolverWithPrincipalConnector(@Nonnull @NotEmpty final String resolverId,
            @Nullable @NullableElements final Collection<AttributeDefinition> definitions,
            @Nullable @NullableElements final Collection<DataConnector> connectors,
            @Nullable final LegacyPrincipalDecoder principalResolver) {
        final AttributeResolverImpl result = new AttributeResolverImpl();
        result.setId(resolverId);
        result.setAttributeDefinitions(definitions);
        result.setDataConnectors(connectors);
        result.setPrincipalDecoder(principalResolver);
        return result;
    }

    public static AttributeResolverImpl newAttributeResolverImpl(@Nonnull @NotEmpty final String resolverId,
            @Nullable @NullableElements final Collection<AttributeDefinition> definitions,
            @Nullable @NullableElements final Collection<DataConnector> connectors) {
        return newAttributeResolverWithPrincipalConnector(resolverId, definitions, connectors, null);
    }

}