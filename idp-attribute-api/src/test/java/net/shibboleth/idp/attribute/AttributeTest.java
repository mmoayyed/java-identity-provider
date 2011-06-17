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

package net.shibboleth.idp.attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.opensaml.util.collections.LazySet;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link Attribute} class. */
public class AttributeTest {

    /** Tests that the attribute has its expected state after instantiation. */
    @Test
    public void testInstantiation() {
        Attribute<String> attrib = new Attribute<String>("foo");

        Assert.assertEquals(attrib.getId(), "foo");

        Assert.assertNotNull(attrib.getDisplayDescriptions());
        Assert.assertTrue(attrib.getDisplayDescriptions().isEmpty());

        Assert.assertNotNull(attrib.getDisplayNames());
        Assert.assertTrue(attrib.getDisplayNames().isEmpty());

        Assert.assertNotNull(attrib.getEncoders());
        Assert.assertTrue(attrib.getEncoders().isEmpty());

        Assert.assertNotNull(attrib.getValues());
        Assert.assertTrue(attrib.getValues().isEmpty());

        Assert.assertNotNull(attrib.hashCode());

        Assert.assertTrue(attrib.equals(new Attribute<String>("foo")));
    }

    /** Tests that null/empty IDs aren't accepted. */
    @Test
    public void testNullEmptyId() {
        try {
            new Attribute(null);
            Assert.fail("able to create attribute with null ID");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        try {
            new Attribute("");
            Assert.fail("able to create attribute with empty ID");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        try {
            new Attribute(" ");
            Assert.fail("able to create attribute with empty ID");
        } catch (IllegalArgumentException e) {
            // expected this
        }
    }

    /** Tests that display names are properly added and modified. */
    @Test
    public void testDisplayNames() {
        Locale en = new Locale("en");
        Locale enbr = new Locale("en", "br");

        Attribute attrib = new Attribute<String>("foo");

        // test adding one entry
        attrib.addDisplayName(en, " english ");
        Assert.assertFalse(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 1);
        Assert.assertTrue(attrib.getDisplayNames().containsKey(en));
        Assert.assertEquals(attrib.getDisplayNames().get(en), "english");

        // test adding another entry
        attrib.addDisplayName(enbr, "british");
        Assert.assertFalse(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 2);
        Assert.assertTrue(attrib.getDisplayNames().containsKey(enbr));
        Assert.assertEquals(attrib.getDisplayNames().get(enbr), "british");

        // test replacing an entry
        String replacedName = attrib.addDisplayName(en, "english ");
        Assert.assertEquals(replacedName, "english");
        Assert.assertFalse(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 2);
        Assert.assertTrue(attrib.getDisplayNames().containsKey(en));
        Assert.assertEquals(attrib.getDisplayNames().get(en), "english");

        // test removing an entry
        attrib.removeDisplayName(en);
        Assert.assertFalse(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 1);
        Assert.assertFalse(attrib.getDisplayNames().containsKey(en));
        Assert.assertTrue(attrib.getDisplayNames().containsKey(enbr));
        Assert.assertEquals(attrib.getDisplayNames().get(enbr), "british");

        // test removing the same entry
        attrib.removeDisplayName(en);
        Assert.assertFalse(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 1);
        Assert.assertFalse(attrib.getDisplayNames().containsKey(en));
        Assert.assertTrue(attrib.getDisplayNames().containsKey(enbr));
        Assert.assertEquals(attrib.getDisplayNames().get(enbr), "british");

        // test removing null
        attrib.removeDisplayName(null);
        Assert.assertFalse(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 1);
        Assert.assertFalse(attrib.getDisplayNames().containsKey(en));
        Assert.assertTrue(attrib.getDisplayNames().containsKey(enbr));
        Assert.assertEquals(attrib.getDisplayNames().get(enbr), "british");

        // test removing the second entry
        attrib.removeDisplayName(enbr);
        Assert.assertTrue(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 0);
        Assert.assertFalse(attrib.getDisplayNames().containsKey(en));
        Assert.assertFalse(attrib.getDisplayNames().containsKey(enbr));

        // test adding something once the collection has been drained
        attrib.addDisplayName(en, " english ");
        Assert.assertFalse(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 1);
        Assert.assertTrue(attrib.getDisplayNames().containsKey(en));
        Assert.assertEquals(attrib.getDisplayNames().get(en), "english");

        // test replacing all entries
        Map<Locale, String> names = new HashMap<Locale, String>();
        names.put(enbr, " british");
        attrib.setDisplayNames(names);
        Assert.assertFalse(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 1);
        Assert.assertFalse(attrib.getDisplayNames().containsKey(en));
        Assert.assertTrue(attrib.getDisplayNames().containsKey(enbr));
        Assert.assertEquals(attrib.getDisplayNames().get(enbr), "british");

        try {
            attrib.addDisplayName(null, "foo");
            Assert.fail("able to add name with null locale");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        try {
            attrib.addDisplayName(en, null);
            Assert.fail("able to add name with null name");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        try {
            attrib.addDisplayName(en, "");
            Assert.fail("able to add name with empty name");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        names = attrib.getDisplayNames();
        try {
            names.put(en, "foo");
            Assert.fail("able to add value to supposedly unmodifiable collection");
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }

    /** Tests that display descriptions are properly added and modified. */
    @Test
    public void testDisplayDescriptions() {
        Locale en = new Locale("en");
        Locale enbr = new Locale("en", "br");

        Attribute attrib = new Attribute<String>("foo");

        // test adding one entry
        attrib.addDisplayDescription(en, " english ");
        Assert.assertFalse(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 1);
        Assert.assertTrue(attrib.getDisplayDescriptions().containsKey(en));
        Assert.assertEquals(attrib.getDisplayDescriptions().get(en), "english");

        // test adding another entry
        attrib.addDisplayDescription(enbr, "british");
        Assert.assertFalse(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 2);
        Assert.assertTrue(attrib.getDisplayDescriptions().containsKey(enbr));
        Assert.assertEquals(attrib.getDisplayDescriptions().get(enbr), "british");

        // test replacing an entry
        String replacedDescription = attrib.addDisplayDescription(en, "english ");
        Assert.assertEquals(replacedDescription, "english");
        Assert.assertFalse(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 2);
        Assert.assertTrue(attrib.getDisplayDescriptions().containsKey(en));
        Assert.assertEquals(attrib.getDisplayDescriptions().get(en), "english");

        // test removing an entry
        attrib.removeDisplayDescription(en);
        Assert.assertFalse(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 1);
        Assert.assertFalse(attrib.getDisplayDescriptions().containsKey(en));
        Assert.assertTrue(attrib.getDisplayDescriptions().containsKey(enbr));
        Assert.assertEquals(attrib.getDisplayDescriptions().get(enbr), "british");

        // test removing the same entry
        attrib.removeDisplayDescription(en);
        Assert.assertFalse(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 1);
        Assert.assertFalse(attrib.getDisplayDescriptions().containsKey(en));
        Assert.assertTrue(attrib.getDisplayDescriptions().containsKey(enbr));
        Assert.assertEquals(attrib.getDisplayDescriptions().get(enbr), "british");

        // test removing null
        attrib.removeDisplayDescription(null);
        Assert.assertFalse(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 1);
        Assert.assertFalse(attrib.getDisplayDescriptions().containsKey(en));
        Assert.assertTrue(attrib.getDisplayDescriptions().containsKey(enbr));
        Assert.assertEquals(attrib.getDisplayDescriptions().get(enbr), "british");

        // test removing the second entry
        attrib.removeDisplayDescription(enbr);
        Assert.assertTrue(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 0);
        Assert.assertFalse(attrib.getDisplayDescriptions().containsKey(en));
        Assert.assertFalse(attrib.getDisplayDescriptions().containsKey(enbr));

        // test adding something once the collection has been drained
        attrib.addDisplayDescription(en, " english ");
        Assert.assertFalse(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 1);
        Assert.assertTrue(attrib.getDisplayDescriptions().containsKey(en));
        Assert.assertEquals("english", attrib.getDisplayDescriptions().get(en));

        // test replacing all entries
        Map<Locale, String> descriptions = new HashMap<Locale, String>();
        descriptions.put(enbr, " british");
        attrib.setDisplayDescriptions(descriptions);
        Assert.assertFalse(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 1);
        Assert.assertFalse(attrib.getDisplayDescriptions().containsKey(en));
        Assert.assertTrue(attrib.getDisplayDescriptions().containsKey(enbr));
        Assert.assertEquals(attrib.getDisplayDescriptions().get(enbr), "british");

        try {
            attrib.addDisplayDescription(null, "foo");
            Assert.fail("able to add description with null locale");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        try {
            attrib.addDisplayDescription(en, null);
            Assert.fail("able to add description with null description");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        try {
            attrib.addDisplayDescription(en, "");
            Assert.fail("able to add description with empty description");
        } catch (IllegalArgumentException e) {
            // expected this
        }

        descriptions = attrib.getDisplayDescriptions();
        try {
            descriptions.put(en, "foo");
            Assert.fail("able to add value to supposedly unmodifiable collection");
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }

    /** Tests that values are properly added and modified. */
    @Test
    public void testValues() {
        String value1 = "value1";
        String value2 = "value2";

        Attribute attrib = new Attribute<String>("foo");

        // test adding one entry
        Assert.assertTrue(attrib.addValue(value1));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 1);
        Assert.assertTrue(attrib.getValues().contains(value1));

        // test adding another entry
        Assert.assertTrue(attrib.addValue(value2));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 2);
        Assert.assertTrue(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test adding null
        Assert.assertFalse(attrib.addValue(null));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 2);
        Assert.assertTrue(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test adding an existing value (now have two value2s)
        Assert.assertTrue(attrib.addValue(value2));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 3);
        Assert.assertTrue(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test removing an entry
        Assert.assertTrue(attrib.removeValue(value1));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 2);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test removing the same entry
        Assert.assertFalse(attrib.removeValue(value1));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 2);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test removing null
        Assert.assertFalse(attrib.removeValue(null));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 2);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test removing the second entry (first value2 entry)
        Assert.assertTrue(attrib.removeValue(value2));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 1);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test removing the second entry (second value2 entry)
        Assert.assertTrue(attrib.removeValue(value2));
        Assert.assertTrue(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 0);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertFalse(attrib.getValues().contains(value2));

        // test adding something once the collection has been drained
        Assert.assertTrue(attrib.addValue(value1));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 1);
        Assert.assertTrue(attrib.getValues().contains(value1));
        Assert.assertFalse(attrib.getValues().contains(value2));

        // test replacing all entries
        Collection<String> values = new ArrayList<String>();
        values.add(value2);
        attrib.setValues(values);
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 1);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        values = attrib.getValues();
        try {
            values.add("foo");
            Assert.fail("able to add value to supposedly unmodifiable collection");
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }

    /** Tests that values are properly added and modified. */
    @Test
    public void testEncoders() {
        AttributeEncoder<String> enc1 = new MockEncoder<String>();
        AttributeEncoder<String> enc2 = new MockEncoder<String>();

        Attribute attrib = new Attribute<String>("foo");

        // test adding one entry
        Assert.assertTrue(attrib.addEncoder(enc1));
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 1);
        Assert.assertTrue(attrib.getEncoders().contains(enc1));

        // test adding another entry
        Assert.assertTrue(attrib.addEncoder(enc2));
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 2);
        Assert.assertTrue(attrib.getEncoders().contains(enc1));
        Assert.assertTrue(attrib.getEncoders().contains(enc2));

        // test adding null
        Assert.assertFalse(attrib.addEncoder(null));
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 2);
        Assert.assertTrue(attrib.getEncoders().contains(enc1));
        Assert.assertTrue(attrib.getEncoders().contains(enc2));

        // test adding an existing Encoder
        Assert.assertFalse(attrib.addEncoder(enc2));
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 2);
        Assert.assertTrue(attrib.getEncoders().contains(enc1));
        Assert.assertTrue(attrib.getEncoders().contains(enc2));

        // test removing an entry
        Assert.assertTrue(attrib.removeEncoder(enc1));
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 1);
        Assert.assertFalse(attrib.getEncoders().contains(enc1));
        Assert.assertTrue(attrib.getEncoders().contains(enc2));

        // test removing the same entry
        Assert.assertFalse(attrib.removeEncoder(enc1));
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 1);
        Assert.assertFalse(attrib.getEncoders().contains(enc1));
        Assert.assertTrue(attrib.getEncoders().contains(enc2));

        // test removing null
        Assert.assertFalse(attrib.removeEncoder(null));
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 1);
        Assert.assertFalse(attrib.getEncoders().contains(enc1));
        Assert.assertTrue(attrib.getEncoders().contains(enc2));

        // test removing the second entry
        Assert.assertTrue(attrib.removeEncoder(enc2));
        Assert.assertTrue(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 0);
        Assert.assertFalse(attrib.getEncoders().contains(enc1));
        Assert.assertFalse(attrib.getEncoders().contains(enc2));

        // test adding something once the collection has been drained
        Assert.assertTrue(attrib.addEncoder(enc1));
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 1);
        Assert.assertTrue(attrib.getEncoders().contains(enc1));
        Assert.assertFalse(attrib.getEncoders().contains(enc2));

        // test replacing all entries
        Set<AttributeEncoder<?>> encoders = new LazySet<AttributeEncoder<?>>();
        encoders.add(enc2);
        attrib.setEncoders(encoders);
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 1);
        Assert.assertFalse(attrib.getEncoders().contains(enc1));
        Assert.assertTrue(attrib.getEncoders().contains(enc2));

        encoders = attrib.getEncoders();
        try {
            encoders.add(enc1);
            Assert.fail("able to add Encoder to supposedly unmodifiable collection");
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }
}