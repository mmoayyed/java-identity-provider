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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.idp.attribute.resolver.ResolverTestSupport;
import net.shibboleth.idp.attribute.resolver.impl.TestSources;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Sets;

/**
 * Test for prescoped attribute definitions.
 */
public class PrescopedAtributeTest {
    /** The name. resolve to */
    private static final String TEST_ATTRIBUTE_NAME = "prescoped";

    private static final String DELIMITER = "@";

    /**
     * Test regexp. The test Data Connector provides an input attribute "at1" with values at1-Data and at1-Connector. We
     * can feed these into the prescoped, looking for '-'
     * 
     * @throws ResolutionException on resolution issues.
     * @throws ComponentInitializationException if any of our initializtions failed (which it shouldn't)
     */
    @Test public void preScoped() throws ResolutionException, ComponentInitializationException {

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        ResolverPluginDependency depend = new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME);
        depend.setDependencyAttributeId(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR);
        dependencySet.add(depend);
        final PrescopedAttributeDefinition attrDef = new PrescopedAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        attrDef.setScopeDelimiter("-");
        attrDef.setDependencies(dependencySet);
        attrDef.initialize();

        // And resolve
        final Set<DataConnector> connectorSet = new LazySet<DataConnector>();
        connectorSet.add(TestSources.populatedStaticConnector());

        final Set<AttributeDefinition> attributeSet = new LazySet<AttributeDefinition>();
        attributeSet.add(attrDef);

        final AttributeResolver resolver = new AttributeResolver("foo", attributeSet, connectorSet);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);
        final Collection f = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(f.size(), 2);
        Assert.assertTrue(f.contains(new ScopedStringAttributeValue("at1", "Data")));
        Assert.assertTrue(f.contains(new ScopedStringAttributeValue("at1", "Connector")));
    }

    /**
     * Test the prescoped attribute resolve when there are no matches.
     * 
     * @throws ResolutionException if resolution fails.
     * @throws ComponentInitializationException if any of our initializations failed (which it shouldn't)
     */
    @Test public void preScopedNoValues() throws ResolutionException, ComponentInitializationException {

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        ResolverPluginDependency depend = new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME);
        depend.setDependencyAttributeId(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR);
        dependencySet.add(depend);
        final PrescopedAttributeDefinition attrDef = new PrescopedAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        attrDef.setScopeDelimiter(DELIMITER);
        attrDef.setDependencies(dependencySet);
        attrDef.initialize();

        // And resolve
        final Set<DataConnector> connectorSet = new LazySet<DataConnector>();
        connectorSet.add(TestSources.populatedStaticConnector());

        final Set<AttributeDefinition> attributeSet = new LazySet<AttributeDefinition>();
        attributeSet.add(attrDef);

        final AttributeResolver resolver = new AttributeResolver("foo", attributeSet, connectorSet);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
            Assert.fail();
        } catch (ResolutionException e) {
            // OK
        }
    }

    @Test public void invalidValueType() throws ComponentInitializationException {
        IdPAttribute attr = new IdPAttribute(ResolverTestSupport.EPA_ATTRIB_ID);
        attr.setValues(Collections.singleton(new ByteAttributeValue(new byte[] {1, 2, 3})));

        AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1", attr));

        final PrescopedAttributeDefinition attrDef = new PrescopedAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        attrDef.setScopeDelimiter("@");
        ResolverPluginDependency depend = new ResolverPluginDependency("connector1");
        depend.setDependencyAttributeId(ResolverTestSupport.EPA_ATTRIB_ID);
        attrDef.setDependencies(Sets.newHashSet(depend));
        attrDef.initialize();

        try {
            attrDef.doAttributeDefinitionResolve(resolutionContext);
            Assert.fail("Invalid type");
        } catch (ResolutionException e) {
            //
        }
    }

    @Test public void initDestroyParms() throws ResolutionException, ComponentInitializationException {

        PrescopedAttributeDefinition attrDef = new PrescopedAttributeDefinition();
        ResolverPluginDependency depend = new ResolverPluginDependency("connector1");
        depend.setDependencyAttributeId(ResolverTestSupport.EPA_ATTRIB_ID);
        Set<ResolverPluginDependency> pluginDependencies =
                Sets.newHashSet(depend);
        attrDef.setDependencies(pluginDependencies);
        attrDef.setId(TEST_ATTRIBUTE_NAME);

        try {
            attrDef.setScopeDelimiter(null);
            Assert.fail("set null delimiter");
        } catch (ConstraintViolationException e) {
            // OK
        }

        attrDef = new PrescopedAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        Assert.assertNotNull(attrDef.getScopeDelimiter());
        attrDef.setScopeDelimiter(DELIMITER);
        try {
            attrDef.initialize();
            Assert.fail("no Dependency - should fail");
        } catch (ComponentInitializationException e) {
            // OK
        }
        attrDef.setDependencies(pluginDependencies);

        try {
            attrDef.doAttributeDefinitionResolve(new AttributeResolutionContext());
            Assert.fail("resolve not initialized");
        } catch (UninitializedComponentException e) {
            // OK
        }
        attrDef.initialize();

        Assert.assertEquals(attrDef.getScopeDelimiter(), DELIMITER);

        try {
            attrDef.doAttributeDefinitionResolve(null);
            Assert.fail("Null context not allowed");
        } catch (ConstraintViolationException e) {
            // OK
        }

        attrDef.destroy();
        try {
            attrDef.initialize();
            Assert.fail("Init after destroy");
        } catch (DestroyedComponentException e) {
            // OK
        }
        try {
            attrDef.doAttributeDefinitionResolve(new AttributeResolutionContext());
            Assert.fail("Resolve after destroy");
        } catch (DestroyedComponentException e) {
            // OK
        }
        try {
            attrDef.setScopeDelimiter(DELIMITER);
            Assert.fail("Set Delimiter after destroy");
        } catch (DestroyedComponentException e) {
            // OK
        }
    }
}
