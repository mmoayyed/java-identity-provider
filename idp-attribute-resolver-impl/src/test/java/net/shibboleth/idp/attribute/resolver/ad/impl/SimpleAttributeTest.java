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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue.EmptyType;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AbstractAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolverAttributeDefinitionDependency;
import net.shibboleth.idp.attribute.resolver.ResolverDataConnectorDependency;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImpl;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImplTest;
import net.shibboleth.idp.attribute.resolver.testing.ResolverTestSupport;
import net.shibboleth.idp.saml.impl.testing.TestSources;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/** Unit test for {@link SimpleAttributeDefinition}. */
@SuppressWarnings("javadoc")
public class SimpleAttributeTest {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "simple";

    /**
     * Test resolution of an empty definition to nothing.
     * 
     * @throws ResolutionException if resolution failed.
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     */
    @Test public void empty() throws ResolutionException, ComponentInitializationException {
        final SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(TEST_ATTRIBUTE_NAME);
        try {
            simple.initialize();
            fail("no dependencies");
        } catch (final ComponentInitializationException e) {
            //OK
        }
        simple.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("foo", "bar")));
        simple.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        context.getSubcontext(AttributeResolverWorkContext.class, true);
        final IdPAttribute result = simple.resolve(context);

        assertTrue(result.getValues().isEmpty());
    }

    @Test public void nulls() throws ResolutionException, ComponentInitializationException {
        nulls(true);
        nulls(false);
    }

    private void nulls(boolean strip) throws ComponentInitializationException, ResolutionException {
        final AbstractAttributeDefinition sa = new AbstractAttributeDefinition() {

            protected IdPAttribute doAttributeDefinitionResolve(AttributeResolutionContext resolutionContext,
                    AttributeResolverWorkContext workContext) throws ResolutionException {
                final IdPAttribute result = new IdPAttribute(TEST_ATTRIBUTE_NAME+"in");
                result.setValues(List.of(EmptyAttributeValue.NULL, EmptyAttributeValue.ZERO_LENGTH, new StringAttributeValue("foo")));
                return result;
            }
        };
        sa.setId(TEST_ATTRIBUTE_NAME+"in");
        sa.initialize();

        final SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(TEST_ATTRIBUTE_NAME);
        simple.setAttributeDependencies(Set.of(TestSources.makeAttributeDefinitionDependency(TEST_ATTRIBUTE_NAME+"in")));
        simple.setStripNulls(strip);
        simple.initialize();

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", Set.of(simple, sa), Collections.emptySet());
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        context.getSubcontext(AttributeResolverWorkContext.class, true);
        resolver.resolveAttributes(context);

        final IdPAttribute result = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME);
        final int vals = result.getValues().size();
        if (strip) {
            assertEquals(vals, 1);
        } else {
            assertEquals(vals, 3);
        }
    }

    /**
     * Test when dependent on a data connector.
     * 
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     */
    @Test public void dataConnector() throws ComponentInitializationException {

        // Set the dependency on the data connector
        final SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(TEST_ATTRIBUTE_NAME);

        final Set<ResolverDataConnectorDependency> dependencySet = new LazySet<>();
        dependencySet.add(TestSources.makeDataConnectorDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR));
        simple.setDataConnectorDependencies(dependencySet);
        simple.initialize();

        // And resolve
        final Set<DataConnector> connectorSet = new LazySet<>();
        connectorSet.add(TestSources.populatedStaticConnector());

        final Set<AttributeDefinition> attributeSet = new LazySet<>();
        attributeSet.add(simple);

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", attributeSet, connectorSet);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
        } catch (final ResolutionException e) {
            fail("resolution failed", e);
        }

        final Collection<?> values = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME).getValues();
        assertEquals(values.size(), 2);
        assertTrue(values.contains(TestSources.COMMON_ATTRIBUTE_VALUE_RESULT), "looking for " + TestSources.COMMON_ATTRIBUTE_VALUE_STRING);
        assertTrue(values.contains(TestSources.CONNECTOR_ATTRIBUTE_VALUE_RESULT),
                "looking for " + TestSources.CONNECTOR_ATTRIBUTE_VALUE_STRING);
    }

    /**
     * Test when dependent on another attribute.
     * 
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     */
    @Test public void attribute() throws ComponentInitializationException {

        final SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(TEST_ATTRIBUTE_NAME);

        // Set the dependency on the data connector
        final Set<ResolverAttributeDefinitionDependency> dependencySet = new LazySet<>();
        dependencySet.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        simple.setAttributeDependencies(dependencySet);
        simple.initialize();

        // And resolve
        final Set<AttributeDefinition> am = new LazySet<>();
        am.add(simple);
        am.add(TestSources.populatedStaticAttribute());

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", am, null);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
        } catch (final ResolutionException e) {
            fail("resolution failed", e);
        }
        final Collection<IdPAttributeValue> values = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        assertEquals(values.size(), 2);
        assertTrue(values.contains(TestSources.COMMON_ATTRIBUTE_VALUE_RESULT),
                "looking for value " + TestSources.COMMON_ATTRIBUTE_VALUE_STRING);
        assertTrue(values.contains(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_RESULT),
                "looking for value " + TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_STRING);
    }
    
    /**
     * Test resolution of an empty definition to nothing.
     * 
     * @throws ResolutionException if resolution failed.
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     */
    @Test public void nullValue() throws ResolutionException, ComponentInitializationException {
        final List<IdPAttributeValue> values = new ArrayList<>(3);
        values.add(TestSources.COMMON_ATTRIBUTE_VALUE_RESULT);
        values.add(new EmptyAttributeValue(EmptyType.NULL_VALUE));
        final IdPAttribute attr = new IdPAttribute(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);

        attr.setValues(values);

       final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1", attr));
        final ResolverDataConnectorDependency depend = TestSources.makeDataConnectorDependency("connector1", TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);

        final SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(TEST_ATTRIBUTE_NAME);
        simple.setDataConnectorDependencies(Collections.singleton(depend));
        simple.initialize();

        final IdPAttribute result = simple.resolve(resolutionContext);

       final List<IdPAttributeValue> outValues = result.getValues();
        assertEquals(outValues.size(), 2);
        assertTrue(outValues.contains(TestSources.COMMON_ATTRIBUTE_VALUE_RESULT));
        assertTrue(outValues.contains(new EmptyAttributeValue(EmptyType.NULL_VALUE)));

    }



    /**
     * Test when dependent on a data connector and another attribute.
     * 
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     */
    @Test public void both() throws ComponentInitializationException {

        final SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(TEST_ATTRIBUTE_NAME);

        simple.setAttributeDependencies(Collections.singleton(
                TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR)));
        simple.setDataConnectorDependencies(Collections.singleton(
                TestSources.makeDataConnectorDependency(TestSources.STATIC_CONNECTOR_NAME,
                        TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR)));
        simple.initialize();

        // And resolve
        final Set<AttributeDefinition> attrDefinitions = new LazySet<>();
        attrDefinitions.add(simple);
        attrDefinitions.add(TestSources.populatedStaticAttribute());

        final Set<DataConnector> dataDefinitions = new LazySet<>();
        dataDefinitions.add(TestSources.populatedStaticConnector());

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", attrDefinitions, dataDefinitions);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
        } catch (final ResolutionException e) {
            fail("resolution failed", e);
        }

        final Collection<?> values = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME).getValues();
        assertTrue(values.contains(TestSources.COMMON_ATTRIBUTE_VALUE_RESULT),
                "looking for value " + TestSources.COMMON_ATTRIBUTE_VALUE_STRING);
        assertTrue(values.contains(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_RESULT),
                "looking for value " + TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_STRING);
        assertTrue(values.contains(TestSources.CONNECTOR_ATTRIBUTE_VALUE_RESULT),
                "looking for value " + TestSources.CONNECTOR_ATTRIBUTE_VALUE_STRING);
        assertEquals(values.size(), 3);
    }
    
}
