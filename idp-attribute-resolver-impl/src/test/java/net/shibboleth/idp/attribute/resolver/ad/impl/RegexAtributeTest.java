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

package net.shibboleth.idp.attribute.resolver.ad.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue.EmptyType;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolverDataConnectorDependency;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.idp.attribute.resolver.ResolverTestSupport;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImpl;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImplTest;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

/**
 * Test for regex attribute definitions.
 */
@SuppressWarnings("deprecation")
public class RegexAtributeTest {
    
    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "regex";

    /**
     * Test regexp. We set up an attribute with values including one 'at1-Connector', 
     * we throw this at 'at1-(.+)or' and look for 'Connect'.
     * 
     * @throws ResolutionException on resolution issues.
     * @throws ComponentInitializationException only if things went bad.
     */
    @Test public void regex() throws ResolutionException, ComponentInitializationException {

        // Set the dependency on the data connector
        final Set<ResolverDataConnectorDependency> dependencySet = new LazySet<>();
        final  ResolverDataConnectorDependency depend = TestSources.makeResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR);
        dependencySet.add(depend);
        final RegexSplitAttributeDefinition attrDef = new RegexSplitAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        attrDef.setRegularExpression(TestSources.CONNECTOR_ATTRIBUTE_VALUE_REGEXP_PATTERN);
        attrDef.setDataConnectorDependencies(dependencySet);
        attrDef.initialize();

        // And resolve
        final Set<DataConnector> connectorSet = new LazySet<>();
        connectorSet.add(TestSources.populatedStaticConnector());

        final Set<AttributeDefinition> attributeSet = new LazySet<>();
        attributeSet.add(attrDef);

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", attributeSet, connectorSet);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);
        final Collection f = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(f.size(), 1);
        Assert.assertTrue(f.contains(TestSources.CONNECTOR_ATTRIBUTE_VALUE_REGEXP_RESULT), "looking for regexp result");
    }
    
    @Test public void nullValueType() throws ComponentInitializationException, ResolutionException {
        final List<IdPAttributeValue<?>> values = new ArrayList<>(4);
        values.add(new StringAttributeValue(TestSources.CONNECTOR_ATTRIBUTE_VALUE_STRING));
        values.add(new EmptyAttributeValue(EmptyType.NULL_VALUE));
        values.add(new StringAttributeValue("three"));
        values.add(new EmptyAttributeValue(EmptyType.ZERO_LENGTH_VALUE));
        final IdPAttribute attr = new IdPAttribute(ResolverTestSupport.EPA_ATTRIB_ID);

        attr.setValues(values);

        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1", attr));
        final ResolverDataConnectorDependency depend = TestSources.makeResolverPluginDependency("connector1", ResolverTestSupport.EPA_ATTRIB_ID);


        final RegexSplitAttributeDefinition attrDef = new RegexSplitAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        attrDef.setRegularExpression(TestSources.CONNECTOR_ATTRIBUTE_VALUE_REGEXP_PATTERN);
        attrDef.setDataConnectorDependencies(Collections.singleton(depend));
        attrDef.initialize();

        final IdPAttribute result = attrDef.resolve(resolutionContext);
        
        final Collection f = result.getValues();

        Assert.assertEquals(f.size(), 1);
        Assert.assertTrue(f.contains(new StringAttributeValue("Connect")));
    }


    @Test public void invalidValueType() throws ComponentInitializationException {
        final IdPAttribute attr = new IdPAttribute(ResolverTestSupport.EPA_ATTRIB_ID);
        attr.setValues(Collections.singletonList(new ByteAttributeValue(new byte[] {1, 2, 3})));

        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1", attr));

        final RegexSplitAttributeDefinition attrDef = new RegexSplitAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        attrDef.setRegularExpression(TestSources.CONNECTOR_ATTRIBUTE_VALUE_REGEXP_PATTERN);
        final ResolverDataConnectorDependency depend = TestSources.makeResolverPluginDependency("connector1", ResolverTestSupport.EPA_ATTRIB_ID);
        attrDef.setDataConnectorDependencies(Collections.singleton(depend));
        attrDef.initialize();

        try {
            attrDef.resolve(resolutionContext);
            Assert.fail("Invalid type");
        } catch (final ResolutionException e) {
            //
        }
    }

    @Test public void emptyValueType() throws ResolutionException, ComponentInitializationException {
        // Set the dependency on the data connector
        final Set<ResolverDataConnectorDependency> dependencySet = new LazySet<>();
        final ResolverDataConnectorDependency depend = 
                TestSources.makeResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR);
        dependencySet.add(depend);
        final RegexSplitAttributeDefinition attrDef = new RegexSplitAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        // regex where the first group doesn't match
        attrDef.setRegularExpression(Pattern.compile("([zZ]*)at1-(.+)or"));
        attrDef.setDataConnectorDependencies(dependencySet);
        attrDef.initialize();

        // And resolve
        final Set<DataConnector> connectorSet = new LazySet<>();
        connectorSet.add(TestSources.populatedStaticConnector());

        final Set<AttributeDefinition> attributeSet = new LazySet<>();
        attributeSet.add(attrDef);

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", attributeSet, connectorSet);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);
        final Collection f = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(f.size(), 1);
        Assert.assertEquals(f.iterator().next(), EmptyAttributeValue.ZERO_LENGTH);
    }

    @Test public void initDestroyParms() throws ResolutionException, ComponentInitializationException {

        RegexSplitAttributeDefinition attrDef = new RegexSplitAttributeDefinition();
        final ResolverDataConnectorDependency depend = 
                TestSources.makeResolverPluginDependency("connector1", ResolverTestSupport.EPA_ATTRIB_ID);
        final Set<ResolverDataConnectorDependency> pluginDependencies = Collections.singleton(depend);
        attrDef.setDataConnectorDependencies(pluginDependencies);
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        try {
            attrDef.initialize();
            Assert.fail("no regexp - should fail");
        } catch (final ComponentInitializationException e) {
            // OK
        }
        try {
            attrDef.setRegularExpression(null);
            Assert.fail("set null regexp");
        } catch (final ConstraintViolationException e) {
            // OK
        }

        attrDef = new RegexSplitAttributeDefinition();
        attrDef.setId(TEST_ATTRIBUTE_NAME);
        Assert.assertNull(attrDef.getRegularExpression());
        attrDef.setRegularExpression(TestSources.CONNECTOR_ATTRIBUTE_VALUE_REGEXP_PATTERN);
        try {
            attrDef.initialize();
            Assert.fail("no Dependency - should fail");
        } catch (final ComponentInitializationException e) {
            // OK
        }
        attrDef.setDataConnectorDependencies(pluginDependencies);

        try {
            attrDef.resolve(new AttributeResolutionContext());
            Assert.fail("resolve not initialized");
        } catch (final UninitializedComponentException e) {
            // OK
        }
        attrDef.initialize();

        Assert.assertEquals(attrDef.getRegularExpression(), TestSources.CONNECTOR_ATTRIBUTE_VALUE_REGEXP_PATTERN);

        try {
            attrDef.resolve(null);
            Assert.fail("Null context not allowed");
        } catch (final ConstraintViolationException e) {
            // OK
        }

        attrDef.destroy();
        try {
            attrDef.initialize();
            Assert.fail("Init after destroy");
        } catch (final DestroyedComponentException e) {
            // OK
        }
        try {
            attrDef.setRegularExpression(TestSources.CONNECTOR_ATTRIBUTE_VALUE_REGEXP_PATTERN);
            Assert.fail("setRegExp after destroy");
        } catch (final UnmodifiableComponentException e) {
            // OK
        }
        try {
            attrDef.resolve(new AttributeResolutionContext());
            Assert.fail("Resolve after destroy");
        } catch (final DestroyedComponentException e) {
            // OK
        }
    }
}