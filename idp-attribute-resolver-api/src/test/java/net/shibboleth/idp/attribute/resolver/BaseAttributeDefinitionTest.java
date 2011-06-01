/*
 * Licensed to the University Corporation for Advanced Internet Development, Inc.
 * under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache 
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeEncoder;

import org.testng.Assert;
import org.testng.annotations.Test;

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
    public void testDependecyOnly() {
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

        ArrayList<AttributeEncoder> encoders = new ArrayList<AttributeEncoder>();

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

        definition.removeAttributeEndoer(enc2);
        Assert.assertNotNull(definition.getAttributeEncoders());
        Assert.assertTrue(definition.getAttributeEncoders().isEmpty());

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

        // test adding one entry
        definition.addDisplayDescription(en, " english ");
        Assert.assertFalse(definition.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(definition.getDisplayDescriptions().size(), 1);
        Assert.assertTrue(definition.getDisplayDescriptions().containsKey(en));
        Assert.assertEquals(definition.getDisplayDescriptions().get(en), "english");

        // test adding another entry
        definition.addDisplayDescription(enbr, "british");
        Assert.assertFalse(definition.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(definition.getDisplayDescriptions().size(), 2);
        Assert.assertTrue(definition.getDisplayDescriptions().containsKey(enbr));
        Assert.assertEquals(definition.getDisplayDescriptions().get(enbr), "british");

        // test replacing an entry
        String replacedDescription = definition.addDisplayDescription(en, "english ");
        Assert.assertEquals(replacedDescription, "english");
        Assert.assertFalse(definition.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(definition.getDisplayDescriptions().size(), 2);
        Assert.assertTrue(definition.getDisplayDescriptions().containsKey(en));
        Assert.assertEquals(definition.getDisplayDescriptions().get(en), "english");

        // test removing an entry
        definition.removeDisplayDescription(en);
        Assert.assertFalse(definition.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(definition.getDisplayDescriptions().size(), 1);
        Assert.assertFalse(definition.getDisplayDescriptions().containsKey(en));
        Assert.assertTrue(definition.getDisplayDescriptions().containsKey(enbr));
        Assert.assertEquals(definition.getDisplayDescriptions().get(enbr), "british");

        // test removing the same entry
        definition.removeDisplayDescription(en);
        Assert.assertFalse(definition.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(definition.getDisplayDescriptions().size(), 1);
        Assert.assertFalse(definition.getDisplayDescriptions().containsKey(en));
        Assert.assertTrue(definition.getDisplayDescriptions().containsKey(enbr));
        Assert.assertEquals(definition.getDisplayDescriptions().get(enbr), "british");

        // test removing null
        definition.removeDisplayDescription(null);
        Assert.assertFalse(definition.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(definition.getDisplayDescriptions().size(), 1);
        Assert.assertFalse(definition.getDisplayDescriptions().containsKey(en));
        Assert.assertTrue(definition.getDisplayDescriptions().containsKey(enbr));
        Assert.assertEquals(definition.getDisplayDescriptions().get(enbr), "british");

        // test removing the second entry
        definition.removeDisplayDescription(enbr);
        Assert.assertTrue(definition.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(definition.getDisplayDescriptions().size(), 0);
        Assert.assertFalse(definition.getDisplayDescriptions().containsKey(en));
        Assert.assertFalse(definition.getDisplayDescriptions().containsKey(enbr));

        // test adding something once the collection has been drained
        definition.addDisplayDescription(en, " english ");
        Assert.assertFalse(definition.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(definition.getDisplayDescriptions().size(), 1);
        Assert.assertTrue(definition.getDisplayDescriptions().containsKey(en));
        Assert.assertEquals("english", definition.getDisplayDescriptions().get(en));

        // test replacing all entries
        Map<Locale, String> descriptions = new HashMap<Locale, String>();
        descriptions.put(enbr, " british");
        definition.setDisplayDescriptions(descriptions);
        Assert.assertFalse(definition.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(definition.getDisplayDescriptions().size(), 1);
        Assert.assertFalse(definition.getDisplayDescriptions().containsKey(en));
        Assert.assertTrue(definition.getDisplayDescriptions().containsKey(enbr));
        Assert.assertEquals(definition.getDisplayDescriptions().get(enbr), "british");

        try {
            definition.addDisplayDescription(null, "foo");
            Assert.fail("able to add description with null locale");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        try {
            definition.addDisplayDescription(en, null);
            Assert.fail("able to add description with null description");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        try {
            definition.addDisplayDescription(en, "");
            Assert.fail("able to add description with empty description");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        descriptions = definition.getDisplayDescriptions();
        try {
            descriptions.put(en, "foo");
            Assert.fail("able to add value to supposedly unmodifiable collection");
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

        // test adding one entry
        definition.addDisplayName(en, " english ");
        Assert.assertFalse(definition.getDisplayNames().isEmpty());
        Assert.assertEquals(definition.getDisplayNames().size(), 1);
        Assert.assertTrue(definition.getDisplayNames().containsKey(en));
        Assert.assertEquals(definition.getDisplayNames().get(en), "english");

        // test adding another entry
        definition.addDisplayName(enbr, "british");
        Assert.assertFalse(definition.getDisplayNames().isEmpty());
        Assert.assertEquals(definition.getDisplayNames().size(), 2);
        Assert.assertTrue(definition.getDisplayNames().containsKey(enbr));
        Assert.assertEquals(definition.getDisplayNames().get(enbr), "british");

        // test replacing an entry
        String replacedName = definition.addDisplayName(en, "english ");
        Assert.assertEquals(replacedName, "english");
        Assert.assertFalse(definition.getDisplayNames().isEmpty());
        Assert.assertEquals(definition.getDisplayNames().size(), 2);
        Assert.assertTrue(definition.getDisplayNames().containsKey(en));
        Assert.assertEquals(definition.getDisplayNames().get(en), "english");

        // test removing an entry
        definition.removeDisplayName(en);
        Assert.assertFalse(definition.getDisplayNames().isEmpty());
        Assert.assertEquals(definition.getDisplayNames().size(), 1);
        Assert.assertFalse(definition.getDisplayNames().containsKey(en));
        Assert.assertTrue(definition.getDisplayNames().containsKey(enbr));
        Assert.assertEquals(definition.getDisplayNames().get(enbr), "british");

        // test removing the same entry
        definition.removeDisplayName(en);
        Assert.assertFalse(definition.getDisplayNames().isEmpty());
        Assert.assertEquals(definition.getDisplayNames().size(), 1);
        Assert.assertFalse(definition.getDisplayNames().containsKey(en));
        Assert.assertTrue(definition.getDisplayNames().containsKey(enbr));
        Assert.assertEquals(definition.getDisplayNames().get(enbr), "british");

        // test removing null
        definition.removeDisplayName(null);
        Assert.assertFalse(definition.getDisplayNames().isEmpty());
        Assert.assertEquals(definition.getDisplayNames().size(), 1);
        Assert.assertFalse(definition.getDisplayNames().containsKey(en));
        Assert.assertTrue(definition.getDisplayNames().containsKey(enbr));
        Assert.assertEquals(definition.getDisplayNames().get(enbr), "british");

        // test removing the second entry
        definition.removeDisplayName(enbr);
        Assert.assertTrue(definition.getDisplayNames().isEmpty());
        Assert.assertEquals(definition.getDisplayNames().size(), 0);
        Assert.assertFalse(definition.getDisplayNames().containsKey(en));
        Assert.assertFalse(definition.getDisplayNames().containsKey(enbr));

        // test adding something once the collection has been drained
        definition.addDisplayName(en, " english ");
        Assert.assertFalse(definition.getDisplayNames().isEmpty());
        Assert.assertEquals(definition.getDisplayNames().size(), 1);
        Assert.assertTrue(definition.getDisplayNames().containsKey(en));
        Assert.assertEquals(definition.getDisplayNames().get(en), "english");

        // test replacing all entries
        Map<Locale, String> names = new HashMap<Locale, String>();
        names.put(enbr, " british");
        definition.setDisplayNames(names);
        Assert.assertFalse(definition.getDisplayNames().isEmpty());
        Assert.assertEquals(definition.getDisplayNames().size(), 1);
        Assert.assertFalse(definition.getDisplayNames().containsKey(en));
        Assert.assertTrue(definition.getDisplayNames().containsKey(enbr));
        Assert.assertEquals(definition.getDisplayNames().get(enbr), "british");

        try {
            definition.addDisplayName(null, "foo");
            Assert.fail("able to add name with null locale");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        try {
            definition.addDisplayName(en, null);
            Assert.fail("able to add name with null name");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        try {
            definition.addDisplayName(en, "");
            Assert.fail("able to add name with empty name");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        names = definition.getDisplayNames();
        try {
            names.put(en, "foo");
            Assert.fail("able to add value to supposedly unmodifiable collection");
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }

    /** Test resolve an attribute. */
    @Test
    public void testResolve() throws Exception {
        AttributeResolutionContext context = new AttributeResolutionContext(null);

        MockAttributeDefinition definition = new MockAttributeDefinition("foo", (Attribute) null);
        Assert.assertNull(definition.resolve(context));

        Attribute<?> attribute = new Attribute<String>("foo");
        definition = new MockAttributeDefinition("foo", attribute);
        Assert.assertEquals(definition.resolve(context), attribute);

        Locale en = new Locale("en");
        definition = new MockAttributeDefinition("foo", attribute);
        definition.addAttributeEncoder(new MockAttributeEncoder(null, null));
        definition.addDisplayDescription(en, "foo");
        definition.addDisplayName(en, "bar");

        attribute = definition.resolve(context);
        Assert.assertEquals(attribute.getEncoders().size(), 1);
        Assert.assertTrue(attribute.getDisplayDescriptions().containsKey(en));
        Assert.assertTrue(attribute.getDisplayNames().containsKey(en));
    }

    /**
     * This class implements the minimal level of functionality and is meant only as a means of testing the abstract
     * {@link BaseAttributeDefinition}.
     */
    private static final class MockBaseAttributeDefinition extends BaseAttributeDefinition {

        /** Static attribute value returned from resolution. */
        private Attribute staticAttribute;

        /**
         * Constructor.
         * 
         * @param id id of the attribute definition, never null or empty
         * @param attribute value returned from the resolution of this attribute, may be null
         */
        public MockBaseAttributeDefinition(String id, Attribute<?> attribute) {
            super(id);
            staticAttribute = attribute;
        }

        /** {@inheritDoc} */
        protected Attribute<?> doAttributeResolution(AttributeResolutionContext resolutionContext)
                throws AttributeResolutionException {
            return staticAttribute;
        }
    }
}