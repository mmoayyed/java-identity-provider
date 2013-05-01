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

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
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
 * Tester for {@link ScopedAttributeDefinition}.
 */
public class ScopedAttributeTest {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "scoped";

    /** The scope. */
    private static final String TEST_SCOPE = "scope";

    /**
     * Test resolution of the scoped attribute resolver.
     * 
     * @throws ResolutionException if resolution failed.
     * @throws ComponentInitializationException if any of our initializations failed (which it shouldn't)
     */
    @Test public void scopes() throws ResolutionException, ComponentInitializationException {

        // Set the dependency on the data connector
        final Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(TestSources.makeResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR));

        final ScopedAttributeDefinition scoped = new ScopedAttributeDefinition();
        scoped.setScope(TEST_SCOPE);
        scoped.setId(TEST_ATTRIBUTE_NAME);
        scoped.setDependencies(dependencySet);
        scoped.initialize();

        // And resolve
        final Set<BaseDataConnector> connectorSet = new LazySet<BaseDataConnector>();
        connectorSet.add(TestSources.populatedStaticConnector());

        final Set<BaseAttributeDefinition> attributeSet = new LazySet<BaseAttributeDefinition>();
        attributeSet.add(scoped);

        final AttributeResolver resolver = new AttributeResolver("foo", attributeSet, connectorSet);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected - two scoped attributes
        final Collection<?> f = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(f.size(), 2);
        Assert.assertTrue(
                f.contains(new ScopedStringAttributeValue(TestSources.COMMON_ATTRIBUTE_VALUE_STRING, TEST_SCOPE)),
                "looking for COMMON_ATTRIBUTE_VALUE");
        Assert.assertTrue(
                f.contains(new ScopedStringAttributeValue(TestSources.COMMON_ATTRIBUTE_VALUE_STRING, TEST_SCOPE)),
                "looking for CONNECTOR_ATTRIBUTE_VALUE");

    }

    @Test public void invalidValueType() throws ComponentInitializationException {
        Attribute attr = new Attribute(ResolverTestSupport.EPA_ATTRIB_ID);
        attr.setValues(Collections.singleton((AttributeValue) new ByteAttributeValue(new byte[] {1, 2, 3})));

        AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1", attr));

        final ScopedAttributeDefinition attrDef = new ScopedAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        attrDef.setScope(TEST_SCOPE);
        attrDef.setDependencies(Sets.newHashSet(TestSources.makeResolverPluginDependency("connector1",
                ResolverTestSupport.EPA_ATTRIB_ID)));
        attrDef.initialize();

        try {
            attrDef.doAttributeDefinitionResolve(resolutionContext);
            Assert.fail("Invalid type");
        } catch (ResolutionException e) {
            //
        }
    }

    @Test public void initDestroyParms() throws ResolutionException, ComponentInitializationException {

        ScopedAttributeDefinition attrDef = new ScopedAttributeDefinition();
        Set<ResolverPluginDependency> pluginDependencies =
                Sets.newHashSet(TestSources.makeResolverPluginDependency("connector1", ResolverTestSupport.EPA_ATTRIB_ID));
        attrDef.setDependencies(pluginDependencies);
        attrDef.setId(TEST_ATTRIBUTE_NAME);

        try {
            attrDef.setScope(null);
            Assert.fail("set null delimiter");
        } catch (ConstraintViolationException e) {
            // OK
        }

        attrDef = new ScopedAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        Assert.assertNull(attrDef.getScope());
        attrDef.setScope(TEST_SCOPE);
        try {
            attrDef.initialize();
            Assert.fail("no Dependency - should fail");
        } catch (ComponentInitializationException e) {
            // OK
        }
        attrDef = new ScopedAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        Assert.assertNull(attrDef.getScope());
        attrDef.setDependencies(pluginDependencies);
        try {
            attrDef.initialize();
            Assert.fail("no Scope - should fail");
        } catch (ComponentInitializationException e) {
            // OK
        }

        try {
            attrDef.doAttributeDefinitionResolve(new AttributeResolutionContext());
            Assert.fail("resolve not initialized");
        } catch (UninitializedComponentException e) {
            // OK
        }

        attrDef.setScope(TEST_SCOPE);
        attrDef.initialize();

        Assert.assertEquals(attrDef.getScope(), TEST_SCOPE);

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
            attrDef.setScope(TEST_SCOPE);
            Assert.fail("Set Delimiter after destroy");
        } catch (DestroyedComponentException e) {
            // OK
        }
    }

}
