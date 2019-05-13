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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@link AttributeDefinition}. This test does not test any methods inherited from
 * {@link ResolverPlugin}, those are covered in {@link AbstractResolverPluginTest}.
 */
public class AbstractAttributeDefinitionTest {

    /** Tests the state of a newly instantiated object. */
    @Test
    public void instantiation() {
        MockAttributeDefinition definition = new MockAttributeDefinition("foo", null);

        Assert.assertEquals(definition.getId(), "foo");
        Assert.assertFalse(definition.isDependencyOnly());
        Assert.assertNotNull(definition.getDisplayDescriptions());
        Assert.assertTrue(definition.getDisplayDescriptions().isEmpty());
        Assert.assertNotNull(definition.getDisplayNames());
        Assert.assertTrue(definition.getDisplayNames().isEmpty());
    }

    /** Tests setting and retrieving the dependency only option. */
    @Test
    public void dependencyOnly() {
        MockAttributeDefinition definition = new MockAttributeDefinition("foo", null);
        Assert.assertFalse(definition.isDependencyOnly());

        definition.setDependencyOnly(true);
        Assert.assertTrue(definition.isDependencyOnly());

        definition.setDependencyOnly(true);
        Assert.assertTrue(definition.isDependencyOnly());

        definition.setDependencyOnly(false);
        Assert.assertFalse(definition.isDependencyOnly());

        definition.setDependencyOnly(false);
        Assert.assertFalse(definition.isDependencyOnly());
    }

    /** Tests that display descriptions are properly added and modified. */
    @Test
    public void displayDescriptions() {
        Locale en = new Locale("en");
        Locale enbr = new Locale("en", "br");

        MockAttributeDefinition definition = new MockAttributeDefinition("foo", null);
        
        Map<Locale, String> descriptions = new HashMap<>();
        descriptions.put(en, "english");
        descriptions.put(enbr, null);
        definition.setDisplayDescriptions(descriptions);
        
        Assert.assertFalse(definition.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(definition.getDisplayDescriptions().size(), 1);
        Assert.assertNotNull(definition.getDisplayDescriptions().get(en));

        descriptions = definition.getDisplayDescriptions();
        try {
            descriptions.put(enbr, "british");
            Assert.fail("able to add description to unmodifable map");
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }

    /** Tests that display names are properly added and modified. */
    @Test
    public void displayNames() {
        Locale en = new Locale("en");
        Locale enbr = new Locale("en", "br");

        MockAttributeDefinition definition = new MockAttributeDefinition("foo", null);

        Map<Locale, String> names = new HashMap<>();
        names.put(en, "english");
        names.put(enbr, null);
        definition.setDisplayNames(names);
        
        Assert.assertFalse(definition.getDisplayNames().isEmpty());
        Assert.assertEquals(definition.getDisplayNames().size(), 1);
        Assert.assertNotNull(definition.getDisplayNames().get(en));

        names = definition.getDisplayNames();
        try {
            names.put(enbr, "british");
            Assert.fail("able to add name to unmodifable map");
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }

    /** Test resolve an attribute. */
    @Test
    public void resolve() throws Exception {
        AttributeResolutionContext context = new AttributeResolutionContext();
        context.getSubcontext(AttributeResolverWorkContext.class, true);

        MockAttributeDefinition definition = new MockAttributeDefinition("foo", (IdPAttribute) null);
        definition.initialize();
        Assert.assertNull(definition.resolve(context));

        IdPAttribute attribute = new IdPAttribute("foo");
        definition = new MockAttributeDefinition("foo", attribute);
        definition.initialize();
        Assert.assertEquals(definition.resolve(context), attribute);

    }
    
    
    @Test
    public void dependencies() throws ComponentInitializationException {
        MockAttributeDefinition definition = new MockAttributeDefinition("foo", null);
        
        final ResolverDataConnectorDependency dc  = new ResolverDataConnectorDependency("dc");
        dc.setAttributeNames(Collections.singletonList("da"));
        definition.setDataConnectorDependencies(Collections.singleton(dc));
        definition.setAttributeDependencies(Collections.singleton(new ResolverAttributeDefinitionDependency("ad")));
        definition.initialize();
        
        final Set<ResolverDataConnectorDependency> dDepends = definition.getDataConnectorDependencies();
        
        Assert.assertEquals(dDepends.size(), 1);
        Assert.assertTrue(dDepends.iterator().next().getAttributeNames().contains("da"));
        Assert.assertEquals(dDepends.iterator().next().getDependencyPluginId(), "dc");

        final Set<ResolverAttributeDefinitionDependency> aDepends = definition.getAttributeDependencies();
        Assert.assertEquals(aDepends.size(), 1);
        Assert.assertEquals(aDepends.iterator().next().getDependencyPluginId(), "ad");

    }

    
    private void testInvalidName(@Nonnull MockAttributeDefinition attrdef) {
        try {
            attrdef.initialize();
            Assert.fail(attrdef.getId() +  "' Should not have initialized OK");
        }
        catch (ComponentInitializationException e) {
            // OK No actions
        }
    }
    
    @Test public void invalidName() {
        testInvalidName(new MockAttributeDefinition("Name With Space", (IdPAttribute) null));
        testInvalidName(new MockAttributeDefinition("Name\rWith\tnonprinters", (IdPAttribute) null));
    }
    
    /**
     * This class implements the minimal level of functionality and is meant only as a means of testing the abstract
     * {@link AttributeDefinition}.
     */
    private static final class MockAttributeDefinition extends AbstractAttributeDefinition {

        /** Static attribute value returned from resolution. */
        private IdPAttribute staticAttribute;

        /**
         * Constructor.
         * 
         * @param id id of the attribute definition, never null or empty
         * @param attribute value returned from the resolution of this attribute, may be null
         */
        public MockAttributeDefinition(String id, IdPAttribute attribute) {
            setId(id);
            staticAttribute = attribute;
        }

        /** {@inheritDoc} */
        @Override
        @Nullable protected IdPAttribute doAttributeDefinitionResolve(
                @Nonnull final AttributeResolutionContext resolutionContext,
                @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {
            return staticAttribute;
        }
    }
}