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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue.EmptyType;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolverAttributeDefinitionDependency;
import net.shibboleth.idp.attribute.resolver.ResolverDataConnectorDependency;
import net.shibboleth.idp.attribute.resolver.ResolverTestSupport;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImpl;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImplTest;
import net.shibboleth.idp.saml.attribute.resolver.impl.SAML1NameIdentifierAttributeDefinition;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Test for {@link SAML1NameIdentifierAttributeDefinition}.
 */
public class SAML1NameIdentifierAttributeDefinitionTest extends OpenSAMLInitBaseTestCase {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "simple";

    private static final String SECOND_ATTRIBUTE_NAME = "second";

    private static final String IDP_ENTITY_ID = "https://idp.example.org/idp";

    private static final String ALTERNATIVE_FORMAT = "ALTERNATE_FORMAT";

    private static final String ALTERNATE_QUALIFIER = "ALTERNATE_QUALIFIER";

    @Test public void empty() throws ResolutionException, ComponentInitializationException {
        final SAML1NameIdentifierAttributeDefinition defn = new SAML1NameIdentifierAttributeDefinition();
        defn.setId(TEST_ATTRIBUTE_NAME);
        defn.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("foo", "bar")));
        defn.initialize();

        final IdPAttribute result =
                defn.resolve(TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, null));

        assertTrue(result.getValues().isEmpty());
    }

    @Test public void simple() throws ResolutionException, ComponentInitializationException {
        final SAML1NameIdentifierAttributeDefinition defn = new SAML1NameIdentifierAttributeDefinition();
        defn.setId(TEST_ATTRIBUTE_NAME);
        // Set the dependency on the data connector
        final Set<ResolverAttributeDefinitionDependency> dependencySet = new LazySet<>();
        dependencySet.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        defn.setAttributeDependencies(dependencySet);
        defn.initialize();

        // And resolve
        final Set<AttributeDefinition> am = new LazySet<>();
        am.add(defn);
        am.add(TestSources.populatedStaticAttribute());

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", am, null);
        resolver.initialize();

        final AttributeResolutionContext context = TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, null);
        try {
            resolver.resolveAttributes(context);
        } catch (final ResolutionException e) {
            fail("resolution failed", e);
        }
        final Collection<IdPAttributeValue> values = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        assertEquals(values.size(), 2);
        final Collection<String> nameValues = new HashSet<>(2);
        for (final IdPAttributeValue val : values) {
            final NameIdentifier id = (NameIdentifier) val.getValue();
            assertEquals(id.getFormat(),  "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
            assertEquals(id.getNameQualifier(), IDP_ENTITY_ID);
            nameValues.add(id.getValue());
        }
        assertTrue(nameValues.contains(TestSources.COMMON_ATTRIBUTE_VALUE_STRING));
        assertTrue(nameValues.contains(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_STRING));
    }
    
    @Test public void nullValueType() throws ComponentInitializationException, ResolutionException {
        final List<IdPAttributeValue> values = new ArrayList<>(3);
        values.add(new StringAttributeValue(TestSources.COMMON_ATTRIBUTE_VALUE_STRING));
        values.add(new EmptyAttributeValue(EmptyType.NULL_VALUE));
        values.add(new StringAttributeValue(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_STRING));
        final IdPAttribute attr = new IdPAttribute(ResolverTestSupport.EPA_ATTRIB_ID);
        attr.setValues(values);
        
        final ResolverDataConnectorDependency depend = TestSources.makeDataConnectorDependency("connector1", ResolverTestSupport.EPA_ATTRIB_ID);

        
        final SAML1NameIdentifierAttributeDefinition defn = new SAML1NameIdentifierAttributeDefinition();
        defn.setId(TEST_ATTRIBUTE_NAME);
        // Set the dependency on the data connector
        defn.setDataConnectorDependencies(Collections.singleton(depend));
        defn.initialize();

        // And resolve
        final Set<AttributeDefinition> am = new LazySet<>();
        am.add(defn);

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", am, Collections.singleton((DataConnector)ResolverTestSupport.buildDataConnector("connector1", attr)));
        resolver.initialize();

        final AttributeResolutionContext context = TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, null);
        try {
            resolver.resolveAttributes(context);
        } catch (final ResolutionException e) {
            fail("resolution failed", e);
        }
        final Collection<IdPAttributeValue> outValues = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        assertEquals(outValues.size(), 2);
        final Collection<String> nameValues = new HashSet<>(2);
        for (final IdPAttributeValue val : outValues) {
            final NameIdentifier id = (NameIdentifier) val.getValue();
            assertEquals(id.getFormat(),  "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
            assertEquals(id.getNameQualifier(), IDP_ENTITY_ID);
            nameValues.add(id.getValue());
        }
        assertTrue(nameValues.contains(TestSources.COMMON_ATTRIBUTE_VALUE_STRING));
        assertTrue(nameValues.contains(TestSources.ATTRIBUTE_ATTRIBUTE_VALUE_STRING));
    }

    @Test public void nullIssuerID() throws ComponentInitializationException {
        final SAML1NameIdentifierAttributeDefinition defn = new SAML1NameIdentifierAttributeDefinition();
        defn.setId(TEST_ATTRIBUTE_NAME);
        defn.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("foo", "bar")));

        // Set the dependency on the data connector
        final Set<ResolverAttributeDefinitionDependency> dependencySet = new LazySet<>();
        dependencySet.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        defn.setAttributeDependencies(dependencySet);
        defn.initialize();
        final Set<AttributeDefinition> am = new LazySet<>();
        am.add(defn);
        am.add(TestSources.populatedStaticAttribute());
        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", am, null);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
            fail("resolution Should have failed");
        } catch (final ResolutionException e) {
            // OK
        }
    }

    @Test public void badValue() throws ResolutionException, ComponentInitializationException {
        final AttributeDefinition defn = TestSources.nonStringAttributeDefiniton(TEST_ATTRIBUTE_NAME);

        final SAML1NameIdentifierAttributeDefinition defn2 = new SAML1NameIdentifierAttributeDefinition();
        defn2.setId(SECOND_ATTRIBUTE_NAME);
        defn2.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("foo", "bar")));

        // Set the dependency on the data connector
        final Set<ResolverAttributeDefinitionDependency> dependencySet = new LazySet<>();
        dependencySet.add(TestSources.makeAttributeDefinitionDependency(TEST_ATTRIBUTE_NAME));
        defn2.setAttributeDependencies(dependencySet);
        defn2.initialize();

        // And resolve
        final Set<AttributeDefinition> am = new LazySet<>();
        am.add(defn);
        am.add(TestSources.populatedStaticAttribute());
        am.add(defn2);

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", am, null);
        resolver.initialize();

        //
        // Attribute recipient needed because the nonStringAttributeDefinition needs it
        //
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, TestSources.SP_ENTITY_ID);
        resolver.resolveAttributes(context);
        assertNull(context.getResolvedIdPAttributes().get(SECOND_ATTRIBUTE_NAME));
    }

    @Test public void singleValueWithOptions() throws ResolutionException, ComponentInitializationException {
        final SAML1NameIdentifierAttributeDefinition defn = new SAML1NameIdentifierAttributeDefinition();
        defn.setId(TEST_ATTRIBUTE_NAME);
        defn.setDataConnectorDependencies(Collections.singleton(TestSources.makeDataConnectorDependency("foo", "bar")));

        // Set the dependency on the data connector
        final Set<ResolverAttributeDefinitionDependency> dependencySet = new LazySet<>();
        dependencySet.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        defn.setAttributeDependencies(dependencySet);
        defn.setNameIdFormat(ALTERNATIVE_FORMAT);
        defn.setNameIdQualifier(ALTERNATE_QUALIFIER);
        defn.initialize();

        // And resolve
        final Set<AttributeDefinition> am = new LazySet<>();
        am.add(defn);
        am.add(TestSources.populatedStaticAttribute(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR, 1));

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", am, null);
        resolver.initialize();

        final AttributeResolutionContext context = TestSources.createResolutionContext(null, TestSources.IDP_ENTITY_ID, null);
        try {
            resolver.resolveAttributes(context);
        } catch (final ResolutionException e) {
            fail("resolution failed", e);
        }
        final Collection<IdPAttributeValue> values = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        assertEquals(values.size(), 1);
        final NameIdentifier id = (NameIdentifier) values.iterator().next().getValue();
        assertEquals(id.getFormat(), ALTERNATIVE_FORMAT);
        assertEquals(defn.getNameIdFormat(), id.getFormat());
        assertEquals(id.getNameQualifier(), ALTERNATE_QUALIFIER);
        assertEquals(defn.getNameIdQualifier(), id.getNameQualifier());
        assertEquals(id.getValue(), TestSources.COMMON_ATTRIBUTE_VALUE_STRING);

    }
}
