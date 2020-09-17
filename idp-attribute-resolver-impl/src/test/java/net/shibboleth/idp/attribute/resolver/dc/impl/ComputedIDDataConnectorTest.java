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

package net.shibboleth.idp.attribute.resolver.dc.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue.EmptyType;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.impl.ComputedPairwiseIdStore;
import net.shibboleth.idp.attribute.impl.JDBCPairwiseIdStore;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolverDataConnectorDependency;
import net.shibboleth.idp.attribute.resolver.ResolverTestSupport;
import net.shibboleth.idp.attribute.resolver.ad.impl.SimpleAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImpl;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImplTest;
import net.shibboleth.idp.saml.impl.testing.TestSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.InitializableComponent;

/**
 * Test for {@link PairwiseIdDataConnector} with computed store.
 */
@SuppressWarnings("javadoc")
public class ComputedIDDataConnectorTest extends OpenSAMLInitBaseTestCase {

    /** The attribute name. */
    private static final String TEST_ATTRIBUTE_NAME = "computedAttribute";

    /** The connector name. */
    private static final String TEST_CONNECTOR_NAME = "computedAttributeConnector";

    /** What we end up looking at */
    protected static final String OUTPUT_ATTRIBUTE_NAME = "outputAttribute";

    /** Value calculated using V2 version. DO NOT CHANGE WITHOUT TESTING AGAINST 2.0 */
    protected static final String RESULT = "Vl6z6K70iLc4AuBoNeb59Dj1rGw=";

    protected static final byte salt[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    protected static final byte smallSalt[] = {0, 1, 2};

    private static void testInit(final PairwiseIdDataConnector connector, final String failMessage) {
        try {
            connector.initialize();
            fail(failMessage);
        } catch (final ComponentInitializationException e) {
            // OK
        }
    }

    @Test public void dataConnector() throws ComponentInitializationException, ResolutionException {
        
        final ComputedPairwiseIdStore store = new ComputedPairwiseIdStore();
        store.setSalt(salt);
        store.initialize();
        
        final PairwiseIdDataConnector connector = new PairwiseIdDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);
        connector.setAttributeDependencies(Collections.singleton(TestSources.makeAttributeDefinitionDependency(
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR)));
        testInit(connector, "No salt");
        connector.setPairwiseIdStore(store);
        connector.setGeneratedAttributeId(TEST_ATTRIBUTE_NAME);
        connector.initialize();

        final SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(OUTPUT_ATTRIBUTE_NAME);
        simple.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency(TEST_CONNECTOR_NAME,
        		TEST_ATTRIBUTE_NAME)));

        final Set<AttributeDefinition> set = new HashSet<>(2);
        set.add(simple);
        set.add(TestSources.populatedStaticAttribute(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR, 1));

        final AttributeResolverImpl resolver =
                AttributeResolverImplTest.newAttributeResolverImpl("atresolver", set, Collections.singleton((DataConnector) connector));

        simple.initialize();
        resolver.initialize();
        connector.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected - two scoped attributes
        final List<IdPAttributeValue> resultValues =
                context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME).getValues();
        assertEquals(resultValues.size(), 1);
        assertEquals(((StringAttributeValue) resultValues.iterator().next()).getValue(), RESULT);
    }

    private AttributeResolverImpl constructResolver(final int values) throws ComponentInitializationException {
        final PairwiseIdDataConnector connector = new PairwiseIdDataConnector();        
        return constructResolver(connector, values, false);
    }

    protected static AttributeResolverImpl constructResolver(final PairwiseIdDataConnector connector, final int values, final boolean noSalt)
            throws ComponentInitializationException {
        
        if (!noSalt) {
            final ComputedPairwiseIdStore store = new ComputedPairwiseIdStore();
            store.setSalt(salt);
            store.initialize();
            if (connector.getPairwiseIdStore() != null) {
                ((JDBCPairwiseIdStore) connector.getPairwiseIdStore()).setInitialValueStore(store);
            } else {
                connector.setPairwiseIdStore(store);
            }
        }
        
        if (connector.getPairwiseIdStore() != null) {
            ((InitializableComponent) connector.getPairwiseIdStore()).initialize();
        }
        
        connector.setId(TEST_CONNECTOR_NAME);
        connector.setAttributeDependencies(Collections.singleton(TestSources.makeAttributeDefinitionDependency(
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR)));

        connector.initialize();
        
        final SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(OUTPUT_ATTRIBUTE_NAME);
        simple.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency(TEST_CONNECTOR_NAME,
                TEST_CONNECTOR_NAME)));
        simple.initialize();

        final Set<AttributeDefinition> set = new HashSet<>(2);
        set.add(simple);
        set.add(TestSources.populatedStaticAttribute(
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR, values));

        return AttributeResolverImplTest.newAttributeResolverImpl("atresolver", set, Collections.singleton((DataConnector) connector));
    }

    private AttributeResolverImpl constructResolverWithNonString(final String dependantOn)
            throws ComponentInitializationException {
        return constructResolverWithNonString(new PairwiseIdDataConnector(), dependantOn);
    }

    protected static AttributeResolverImpl constructResolverWithNonString(final PairwiseIdDataConnector connector,
            final String dependantOn) throws ComponentInitializationException {
        
        final ComputedPairwiseIdStore store = new ComputedPairwiseIdStore();
        store.setSalt(salt);
        
        if (connector.getPairwiseIdStore() != null) {
            ((JDBCPairwiseIdStore) connector.getPairwiseIdStore()).setInitialValueStore(store);
        } else {
            connector.setPairwiseIdStore(store);
        }
        ((InitializableComponent) connector.getPairwiseIdStore()).initialize();
        
        connector.setId(TEST_CONNECTOR_NAME);
        connector.setAttributeDependencies(Collections.singleton(TestSources.makeAttributeDefinitionDependency(dependantOn)));
        connector.initialize();

        final SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(OUTPUT_ATTRIBUTE_NAME);
        simple.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency(TEST_CONNECTOR_NAME,
                TEST_CONNECTOR_NAME)));
        simple.initialize();
        final Set<AttributeDefinition> set = new HashSet<>(3);
        set.add(simple);
        set.add(TestSources.populatedStaticAttribute(
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR, 1));
        set.add(TestSources.nonStringAttributeDefiniton(dependantOn));

        return AttributeResolverImplTest.newAttributeResolverImpl("atresolver", set, Collections.singleton((DataConnector) connector));
    }

    protected static PairwiseIdDataConnector connectorFromResolver(final AttributeResolverImpl resolver) {
        return (PairwiseIdDataConnector) resolver.getDataConnectors().get(TEST_CONNECTOR_NAME);
    }
    
    @Test(expectedExceptions={ComponentInitializationException.class,}) public void noStore() throws ComponentInitializationException {
        connectorFromResolver(constructResolver(new PairwiseIdDataConnector(), 1, true)).initialize();
    }

    @Test public void altDataConnector() throws ComponentInitializationException, ResolutionException {
        AttributeResolverImpl resolver = constructResolver(1);
        connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);

        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected
        List<IdPAttributeValue> resultValues =
                context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME).getValues();
        assertEquals(resultValues.size(), 1);
        assertEquals(((StringAttributeValue) resultValues.iterator().next()).getValue(), RESULT);

        //
        // now do it again with more values
        //
        resolver = constructResolver(3);
        connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);

        context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected
        // No equality test since we don't know which attribute will be returned
        resultValues = context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME).getValues();
        assertEquals(resultValues.size(), 1);

        //
        // And again with different values
        //
        resolver = constructResolver(1);

        connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);

        context = TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID, "foo");
        resolver.resolveAttributes(context);

        // Now test that we got exactly what we expected - two scoped attributes
        resultValues = context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME).getValues();
        assertEquals(resultValues.size(), 1);
        assertNotEquals(((StringAttributeValue) resultValues.iterator().next()).getValue(), RESULT);

    }

    @Test public void attributeFails() throws ComponentInitializationException, ResolutionException {
        AttributeResolverImpl resolver = constructResolver(3);

        connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);

        AttributeResolutionContext context = TestSources.createResolutionContext(null, null, TestSources.SP_ENTITY_ID);;
        resolver.resolveAttributes(context);
        assertNull(context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME));

        resolver = constructResolver(0);
        connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);

        context = TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);
        assertNull(context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME));

        resolver = constructResolver(1);

        connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);

        resolver.resolveAttributes(TestSources.createResolutionContext(null, null, null));
        assertNull(context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME));

        resolver = constructResolverWithNonString("nonString");
        connectorFromResolver(resolver).initialize();
        ComponentSupport.initialize(resolver);

        context = TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);
        assertNull(context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME));
    }

    @Test public void case425() throws ComponentInitializationException, ResolutionException {

        final ComputedPairwiseIdStore store = new ComputedPairwiseIdStore();
        store.setSalt(salt);
        store.initialize();

        final PairwiseIdDataConnector connector = new PairwiseIdDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);
        connector.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency(
                TestSources.STATIC_CONNECTOR_NAME, TestSources.DEPENDS_ON_ATTRIBUTE_NAME_CONNECTOR)));
        connector.setGeneratedAttributeId("wibble");
        connector.setPairwiseIdStore(store);
        connector.initialize();

        final Set<DataConnector> set = new HashSet<>(2);
        set.add(TestSources.populatedStaticConnector());
        set.add(connector);

        final SimpleAttributeDefinition simple = new SimpleAttributeDefinition();
        simple.setId(OUTPUT_ATTRIBUTE_NAME);
        simple.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency(TEST_CONNECTOR_NAME,
                "wibble")));
        simple.initialize();

        final AttributeResolverImpl resolver =
                AttributeResolverImplTest.newAttributeResolverImpl("atresolver", Collections.singleton((AttributeDefinition) simple), set);
        ComponentSupport.initialize(resolver);

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);

        final List<IdPAttributeValue> resultValues =
                context.getResolvedIdPAttributes().get(OUTPUT_ATTRIBUTE_NAME).getValues();
        assertEquals(resultValues.size(), 1);

    }

    @Test public void nullValue() throws ComponentInitializationException, ResolutionException {
        
        final List<IdPAttributeValue> values = new ArrayList<>(2);
        values.add(new EmptyAttributeValue(EmptyType.NULL_VALUE));
        values.add(new StringAttributeValue("calue"));
        final IdPAttribute attr = new IdPAttribute(ResolverTestSupport.EPA_ATTRIB_ID);

        attr.setValues(values);

        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1", attr));
        
        resolutionContext.setAttributeIssuerID(TestSources.IDP_ENTITY_ID);
        resolutionContext.setAttributeRecipientID(TestSources.SP_ENTITY_ID);
        resolutionContext.setPrincipal(TestSources.PRINCIPAL_ID);

        final ResolverDataConnectorDependency depend = TestSources.makeDataConnectorDependency("connector1", ResolverTestSupport.EPA_ATTRIB_ID);

        final ComputedPairwiseIdStore store = new ComputedPairwiseIdStore();
        store.setSalt(salt);
        store.initialize();

        final PairwiseIdDataConnector connector = new PairwiseIdDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);
        connector.setDataConnectorDependencies(Collections.singleton(depend));
        connector.setGeneratedAttributeId("wibble");
        connector.setPairwiseIdStore(store);
        connector.initialize();

        
        assertNull(connector.resolve(resolutionContext));
    }

    @Test public void emptyValue() throws ComponentInitializationException, ResolutionException {
        
        final List<IdPAttributeValue> values = new ArrayList<>(1);
        values.add(new EmptyAttributeValue(EmptyType.ZERO_LENGTH_VALUE));
        final IdPAttribute attr = new IdPAttribute(ResolverTestSupport.EPA_ATTRIB_ID);

        attr.setValues(values);

        final AttributeResolutionContext resolutionContext =
                ResolverTestSupport.buildResolutionContext(ResolverTestSupport.buildDataConnector("connector1", attr));
        
        resolutionContext.setAttributeIssuerID(TestSources.IDP_ENTITY_ID);
        resolutionContext.setAttributeRecipientID(TestSources.SP_ENTITY_ID);
        resolutionContext.setPrincipal(TestSources.PRINCIPAL_ID);

        final ResolverDataConnectorDependency depend = TestSources.makeDataConnectorDependency("connector1", ResolverTestSupport.EPA_ATTRIB_ID);

        final ComputedPairwiseIdStore store = new ComputedPairwiseIdStore();
        store.setSalt(salt);
        store.initialize();

        final PairwiseIdDataConnector connector = new PairwiseIdDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);
        connector.setDataConnectorDependencies(Collections.singleton(depend));
        connector.setGeneratedAttributeId("wibble");
        connector.setPairwiseIdStore(store);
        connector.initialize();

        final Map<String, IdPAttribute> result = connector.resolve(resolutionContext);
            
        assertNull(result);

    }

    
}
