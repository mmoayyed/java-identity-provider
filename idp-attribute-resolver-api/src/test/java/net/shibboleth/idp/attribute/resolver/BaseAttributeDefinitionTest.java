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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeEncoder;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Optional;

/**
 * Unit test for {@link BaseAttributeDefinition}. This test does not test any methods inherited from
 * {@link BaseResolverPlugin}, those are covered in {@link BaseResolverPluginTest}.
 */
public class BaseAttributeDefinitionTest {

    /** Tests the state of a newly instantiated object. */
    @Test
    public void testInstantiation() {
        MockBaseAttributeDefinition definition = new MockBaseAttributeDefinition("foo", null);

        Assert.assertEquals(definition.getId(), "foo");
        Assert.assertFalse(definition.isDependencyOnly());
        Assert.assertNotNull(definition.getAttributeEncoders());
        Assert.assertTrue(definition.getAttributeEncoders().isEmpty());
        Assert.assertNotNull(definition.getDisplayDescriptions());
        Assert.assertTrue(definition.getDisplayDescriptions().isEmpty());
        Assert.assertNotNull(definition.getDisplayNames());
        Assert.assertTrue(definition.getDisplayNames().isEmpty());
    }

    /** Tests setting and retrieving the dependency only option. */
    @Test
    public void testDependencyOnly() {
        MockBaseAttributeDefinition definition = new MockBaseAttributeDefinition("foo", null);
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

    /** Tests setting and retrieving encoders. */
    @Test
    public void testEncoders() {
        MockBaseAttributeDefinition definition = new MockBaseAttributeDefinition("foo", null);

        MockAttributeEncoder enc1 = new MockAttributeEncoder(null, null);
        MockAttributeEncoder enc2 = new MockAttributeEncoder(null, null);

        Set<AttributeEncoder<?>> encoders = new HashSet<AttributeEncoder<?>>(2);

        definition.setAttributeEncoders(null);
        Assert.assertNotNull(definition.getAttributeEncoders());
        Assert.assertTrue(definition.getAttributeEncoders().isEmpty());

        definition.setAttributeEncoders(encoders);
        Assert.assertNotNull(definition.getAttributeEncoders());
        Assert.assertTrue(definition.getAttributeEncoders().isEmpty());

        encoders.add(enc1);
        encoders.add(null);
        encoders.add(enc2);
        Assert.assertNotNull(definition.getAttributeEncoders());
        Assert.assertTrue(definition.getAttributeEncoders().isEmpty());

        definition.setAttributeEncoders(encoders);
        Assert.assertEquals(definition.getAttributeEncoders().size(), 2);
        Assert.assertTrue(definition.getAttributeEncoders().contains(enc1));
        Assert.assertTrue(definition.getAttributeEncoders().contains(enc2));

        encoders.clear();
        encoders.add(enc2);
        definition.setAttributeEncoders(encoders);
        Assert.assertEquals(definition.getAttributeEncoders().size(), 1);
        Assert.assertFalse(definition.getAttributeEncoders().contains(enc1));
        Assert.assertTrue(definition.getAttributeEncoders().contains(enc2));

        try {
            definition.getAttributeEncoders().add(enc2);
            Assert.fail("able to add entry to supposedly unmodifiable collection");
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }

    /** Tests that display descriptions are properly added and modified. */
    @Test
    public void testDisplayDescriptions() {
        Locale en = new Locale("en");
        Locale enbr = new Locale("en", "br");

        MockBaseAttributeDefinition definition = new MockBaseAttributeDefinition("foo", null);
        
        Map<Locale, String> descriptions = new HashMap<Locale, String>();
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
    public void testDisplayNames() {
        Locale en = new Locale("en");
        Locale enbr = new Locale("en", "br");

        MockBaseAttributeDefinition definition = new MockBaseAttributeDefinition("foo", null);

        Map<Locale, String> names = new HashMap<Locale, String>();
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
    public void testResolve() throws Exception {
        AttributeResolutionContext context = new AttributeResolutionContext();

        MockAttributeDefinition definition = new MockAttributeDefinition("foo", (Attribute) null);
        definition.initialize();
        Assert.assertTrue(!definition.resolve(context).isPresent());

        Attribute attribute = new Attribute("foo");
        definition = new MockAttributeDefinition("foo", attribute);
        definition.initialize();
        Assert.assertEquals(definition.resolve(context).get(), attribute);

    }
    
    @Test public void testInitDestroyValidate() throws ComponentInitializationException, ComponentValidationException {
        MockAttributeEncoder encoder = new MockAttributeEncoder("foo", "baz");
        MockBaseAttributeDefinition definition = new MockBaseAttributeDefinition("foo", (Attribute) null);
        
        Set<AttributeEncoder<?>> encoders = new HashSet<AttributeEncoder<?>>(1);
        encoders.add(encoder);
        definition.setAttributeEncoders(encoders);
        
        Assert.assertFalse(encoder.isInitialized());
        Assert.assertFalse(encoder.getValidateCount() > 0);
        Assert.assertFalse(encoder.isDestroyed());

        definition.initialize();
        Assert.assertTrue(encoder.isInitialized());
        Assert.assertFalse(encoder.getValidateCount() > 0);
        Assert.assertFalse(encoder.isDestroyed());

        definition.validate();
        Assert.assertTrue(encoder.isInitialized());
        Assert.assertTrue(encoder.getValidateCount() > 0);
        Assert.assertFalse(encoder.isDestroyed());

        definition.destroy();
        Assert.assertTrue(encoder.isInitialized());
        Assert.assertTrue(encoder.getValidateCount() > 0);
        Assert.assertTrue(encoder.isDestroyed());
        
        
    }

    /**
     * This class implements the minimal level of functionality and is meant only as a means of testing the abstract
     * {@link BaseAttributeDefinition}.
     */
    private static final class MockBaseAttributeDefinition extends BaseAttributeDefinition {

        /** Static attribute value returned from resolution. */
        private Optional<Attribute> staticAttribute;

        /**
         * Constructor.
         * 
         * @param id id of the attribute definition, never null or empty
         * @param attribute value returned from the resolution of this attribute, may be null
         */
        public MockBaseAttributeDefinition(String id, Attribute attribute) {
            setId(id);
            staticAttribute = Optional.<Attribute>fromNullable(attribute);
        }

        /** {@inheritDoc} */
        protected Optional<Attribute> doAttributeDefinitionResolve(AttributeResolutionContext resolutionContext)
                throws ResolutionException {
            return staticAttribute;
        }
    }
}