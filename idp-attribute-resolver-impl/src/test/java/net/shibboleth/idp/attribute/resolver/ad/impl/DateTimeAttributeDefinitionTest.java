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

import static org.testng.Assert.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.DateTimeAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AbstractAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ResolverAttributeDefinitionDependency;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImpl;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImplTest;
import net.shibboleth.idp.saml.impl.testing.TestSources;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/** Unit test for {@link DateTimeAttributeDefinition}. */
public class DateTimeAttributeDefinitionTest {

    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "datetime";
    private static final String STRING_SECS = "1659979872";
    private static final String STRING_MSECS = "1659979872969";
    private static final String STRING_ISO = "2022-08-08T17:31:12.969Z";

    /**
     * Test resolution of an empty definition to nothing.
     * 
     * @throws ResolutionException if resolution failed.
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     */
    @Test public void empty() throws ResolutionException, ComponentInitializationException {
        final DateTimeAttributeDefinition simple = new DateTimeAttributeDefinition();
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

    /**
     * Test handling of errors.
     * 
     * @throws ResolutionException
     * @throws ComponentInitializationException
     */
    @Test public void errors() throws ResolutionException, ComponentInitializationException {
        errors(true);
        errors(false);
    }

    private void errors(boolean ignore) throws ComponentInitializationException, ResolutionException {
        final AbstractAttributeDefinition sa = new AbstractAttributeDefinition() {

            protected IdPAttribute doAttributeDefinitionResolve(AttributeResolutionContext resolutionContext,
                    AttributeResolverWorkContext workContext) throws ResolutionException {
                final IdPAttribute result = new IdPAttribute(TEST_ATTRIBUTE_NAME+"in");
                result.setValues(List.of(EmptyAttributeValue.NULL, EmptyAttributeValue.ZERO_LENGTH, new StringAttributeValue(STRING_SECS)));
                return result;
            }
        };
        sa.setId(TEST_ATTRIBUTE_NAME+"in");
        sa.initialize();

        final DateTimeAttributeDefinition datetime = new DateTimeAttributeDefinition();
        datetime.setId(TEST_ATTRIBUTE_NAME);
        datetime.setAttributeDependencies(Set.of(TestSources.makeAttributeDefinitionDependency(TEST_ATTRIBUTE_NAME+"in")));
        datetime.setIgnoreConversionErrors(ignore);
        datetime.initialize();

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", Set.of(datetime, sa), Collections.emptySet());
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        context.getSubcontext(AttributeResolverWorkContext.class, true);
        
        try {
            resolver.resolveAttributes(context);
        } catch (final ResolutionException e) {
            if (ignore) {
                fail("Did not ignore errors");
            }
        }

        final IdPAttribute result = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME);
        if (ignore) {
            final int vals = result.getValues().size();
            assertEquals(vals, 1);
        } else {
            assertNull(result);
        }
    }

    /**
     * Test when dependent on another attribute.
     * 
     * @throws ComponentInitializationException if initialization fails (which it shouldn't).
     */
    @Test public void attribute() throws ComponentInitializationException {

        final DateTimeAttributeDefinition datetime = new DateTimeAttributeDefinition();
        datetime.setId(TEST_ATTRIBUTE_NAME);

        // Set the dependency on the attribute def.
        final Set<ResolverAttributeDefinitionDependency> dependencySet = new LazySet<>();
        dependencySet.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        datetime.setAttributeDependencies(dependencySet);
        datetime.initialize();

        final Instant now = Instant.now();
        
        final IdPAttribute attr = new IdPAttribute(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);
        attr.setValues(List.of(new DateTimeAttributeValue(now), StringAttributeValue.valueOf(STRING_SECS)));
        
        // And resolve
        final Set<AttributeDefinition> am = new LazySet<>();
        am.add(datetime);
        am.add(TestSources.populatedStaticAttribute(attr));

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
        assertTrue(values.contains(new DateTimeAttributeValue(now)));
        assertTrue(values.contains(new DateTimeAttributeValue(Instant.ofEpochSecond(Long.valueOf(STRING_SECS)))));
    }
    
    /**
     * Test when using ms units.
     * 
     * @throws ComponentInitializationException if initialization fails (which it shouldn't)
     */
    @Test public void millisecs() throws ComponentInitializationException {

        final DateTimeAttributeDefinition datetime = new DateTimeAttributeDefinition();
        datetime.setId(TEST_ATTRIBUTE_NAME);
        datetime.setEpochInSeconds(false);

        // Set the dependency on the attribute def.
        final Set<ResolverAttributeDefinitionDependency> dependencySet = new LazySet<>();
        dependencySet.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        datetime.setAttributeDependencies(dependencySet);
        datetime.initialize();
        
        final IdPAttribute attr = new IdPAttribute(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);
        attr.setValues(List.of(StringAttributeValue.valueOf(STRING_MSECS)));
        
        // And resolve
        final Set<AttributeDefinition> am = new LazySet<>();
        am.add(datetime);
        am.add(TestSources.populatedStaticAttribute(attr));

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", am, null);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
        } catch (final ResolutionException e) {
            fail("resolution failed", e);
        }
        final Collection<IdPAttributeValue> values = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        assertEquals(values.size(), 1);
        assertTrue(values.contains(new DateTimeAttributeValue(Instant.ofEpochMilli(Long.valueOf(STRING_MSECS)))));
    }

    /**
     * Test when using formatter.
     * 
     * @throws ComponentInitializationException if initialization fails (which it shouldn't)
     */
    @Test public void formatter() throws ComponentInitializationException {

        final DateTimeAttributeDefinition datetime = new DateTimeAttributeDefinition();
        datetime.setId(TEST_ATTRIBUTE_NAME);
        datetime.setDateTimeFormatter(DateTimeFormatter.ISO_INSTANT);

        // Set the dependency on the attribute def.
        final Set<ResolverAttributeDefinitionDependency> dependencySet = new LazySet<>();
        dependencySet.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        datetime.setAttributeDependencies(dependencySet);
        datetime.initialize();
        
        final IdPAttribute attr = new IdPAttribute(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);
        attr.setValues(List.of(StringAttributeValue.valueOf(STRING_ISO)));
        
        // And resolve
        final Set<AttributeDefinition> am = new LazySet<>();
        am.add(datetime);
        am.add(TestSources.populatedStaticAttribute(attr));

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", am, null);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        try {
            resolver.resolveAttributes(context);
        } catch (final ResolutionException e) {
            fail("resolution failed", e);
        }
        final Collection<IdPAttributeValue> values = context.getResolvedIdPAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        assertEquals(values.size(), 1);
        assertTrue(values.contains(new DateTimeAttributeValue(Instant.ofEpochMilli(Long.valueOf(STRING_MSECS)))));
    }

    /**
     * Test when using bad formatter.
     * 
     * @throws ComponentInitializationException if initialization fails (which it shouldn't)
     * @throws ResolutionException the expected outcome
     */
    @Test(expectedExceptions=ResolutionException.class)
    public void formatterError() throws ComponentInitializationException, ResolutionException {

        final DateTimeAttributeDefinition datetime = new DateTimeAttributeDefinition();
        datetime.setId(TEST_ATTRIBUTE_NAME);
        datetime.setDateTimeFormatter(DateTimeFormatter.BASIC_ISO_DATE);

        // Set the dependency on the attribute def.
        final Set<ResolverAttributeDefinitionDependency> dependencySet = new LazySet<>();
        dependencySet.add(TestSources.makeAttributeDefinitionDependency(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR));
        datetime.setAttributeDependencies(dependencySet);
        datetime.initialize();
        
        final IdPAttribute attr = new IdPAttribute(TestSources.DEPENDS_ON_ATTRIBUTE_NAME_ATTR);
        attr.setValues(List.of(StringAttributeValue.valueOf(STRING_ISO)));
        
        // And resolve
        final Set<AttributeDefinition> am = new LazySet<>();
        am.add(datetime);
        am.add(TestSources.populatedStaticAttribute(attr));

        final AttributeResolverImpl resolver = AttributeResolverImplTest.newAttributeResolverImpl("foo", am, null);
        resolver.initialize();

        final AttributeResolutionContext context = new AttributeResolutionContext();
        resolver.resolveAttributes(context);
    }

}