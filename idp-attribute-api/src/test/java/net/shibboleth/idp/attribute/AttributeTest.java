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

import net.shibboleth.utilities.java.support.collection.LazySet;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link Attribute} class. */
public class AttributeTest {

    /** Tests that the attribute has its expected state after instantiation. */
    @Test public void testInstantiation() {
        Attribute attrib = new Attribute("foo");

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

        Assert.assertTrue(attrib.equals(new Attribute("foo")));
    }

    /** Tests that null/empty IDs aren't accepted. */
    @Test public void testNullEmptyId() {
        try {
            new Attribute(null);
            Assert.fail("able to create attribute with null ID");
        } catch (AssertionError e) {
            // expected this
        }

        try {
            new Attribute("");
            Assert.fail("able to create attribute with empty ID");
        } catch (AssertionError e) {
            // expected this
        }

        try {
            new Attribute(" ");
            Assert.fail("able to create attribute with empty ID");
        } catch (AssertionError e) {
            // expected this
        }
    }

    /** Tests that display names are properly added and modified. */
    @Test public void testDisplayNames() {
        Locale en = new Locale("en");
        Locale enbr = new Locale("en", "br");

        Attribute attrib = new Attribute("foo");
        Map<Locale, String> diplayNames = attrib.getDisplayNames();

        // test adding one entry
        diplayNames.put(en, " english ");
        Assert.assertFalse(diplayNames.isEmpty());
        Assert.assertEquals(diplayNames.size(), 1);
        Assert.assertTrue(diplayNames.containsKey(en));
        Assert.assertEquals(diplayNames.get(en), "english");

        // test adding another entry
        diplayNames.put(enbr, "british");
        Assert.assertFalse(diplayNames.isEmpty());
        Assert.assertEquals(diplayNames.size(), 2);
        Assert.assertTrue(diplayNames.containsKey(enbr));
        Assert.assertEquals(diplayNames.get(enbr), "british");

        // test replacing an entry
        String replacedName = diplayNames.put(en, "english ");
        Assert.assertEquals(replacedName, "english");
        Assert.assertFalse(diplayNames.isEmpty());
        Assert.assertEquals(diplayNames.size(), 2);
        Assert.assertTrue(diplayNames.containsKey(en));
        Assert.assertEquals(diplayNames.get(en), "english");

        // test removing an entry
        diplayNames.remove(en);
        Assert.assertFalse(diplayNames.isEmpty());
        Assert.assertEquals(diplayNames.size(), 1);
        Assert.assertFalse(diplayNames.containsKey(en));
        Assert.assertTrue(diplayNames.containsKey(enbr));
        Assert.assertEquals(diplayNames.get(enbr), "british");

        // test removing the same entry
        diplayNames.remove(en);
        Assert.assertFalse(diplayNames.isEmpty());
        Assert.assertEquals(diplayNames.size(), 1);
        Assert.assertFalse(diplayNames.containsKey(en));
        Assert.assertTrue(diplayNames.containsKey(enbr));
        Assert.assertEquals(diplayNames.get(enbr), "british");

        // test removing null
        diplayNames.remove(null);
        Assert.assertFalse(diplayNames.isEmpty());
        Assert.assertEquals(diplayNames.size(), 1);
        Assert.assertFalse(diplayNames.containsKey(en));
        Assert.assertTrue(diplayNames.containsKey(enbr));
        Assert.assertEquals(diplayNames.get(enbr), "british");

        // test removing the second entry
        diplayNames.remove(enbr);
        Assert.assertTrue(diplayNames.isEmpty());
        Assert.assertEquals(diplayNames.size(), 0);
        Assert.assertFalse(diplayNames.containsKey(en));
        Assert.assertFalse(diplayNames.containsKey(enbr));

        // test adding something once the collection has been drained
        diplayNames.put(en, " english ");
        Assert.assertFalse(diplayNames.isEmpty());
        Assert.assertEquals(diplayNames.size(), 1);
        Assert.assertTrue(diplayNames.containsKey(en));
        Assert.assertEquals(diplayNames.get(en), "english");

        // test replacing all entries
        Map<Locale, String> names = new HashMap<Locale, String>();
        names.put(enbr, " british");
        attrib.setDisplayNames(names);
        Assert.assertFalse(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 1);
        Assert.assertFalse(attrib.getDisplayNames().containsKey(en));
        Assert.assertTrue(attrib.getDisplayNames().containsKey(enbr));
        Assert.assertEquals(attrib.getDisplayNames().get(enbr), "british");

        attrib.getDisplayNames().put(null, "foo");
        Assert.assertFalse(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 1);

        attrib.getDisplayNames().put(en, null);
        Assert.assertFalse(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 1);

        attrib.getDisplayNames().put(en, "");
        Assert.assertFalse(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 1);
    }

    /** Tests that display descriptions are properly added and modified. */
    @Test public void testDisplayDescriptions() {
        Locale en = new Locale("en");
        Locale enbr = new Locale("en", "br");

        Attribute attrib = new Attribute("foo");
        Map<Locale, String> descriptions = attrib.getDisplayDescriptions();

        // test adding one entry
        descriptions.put(en, " english ");
        Assert.assertFalse(descriptions.isEmpty());
        Assert.assertEquals(descriptions.size(), 1);
        Assert.assertTrue(descriptions.containsKey(en));
        Assert.assertEquals(descriptions.get(en), "english");

        // test adding another entry
        descriptions.put(enbr, "british");
        Assert.assertFalse(descriptions.isEmpty());
        Assert.assertEquals(descriptions.size(), 2);
        Assert.assertTrue(descriptions.containsKey(enbr));
        Assert.assertEquals(descriptions.get(enbr), "british");

        // test replacing an entry
        String replacedDescription = descriptions.put(en, "english ");
        Assert.assertEquals(replacedDescription, "english");
        Assert.assertFalse(descriptions.isEmpty());
        Assert.assertEquals(descriptions.size(), 2);
        Assert.assertTrue(descriptions.containsKey(en));
        Assert.assertEquals(descriptions.get(en), "english");

        // test removing an entry
        descriptions.remove(en);
        Assert.assertFalse(descriptions.isEmpty());
        Assert.assertEquals(descriptions.size(), 1);
        Assert.assertFalse(descriptions.containsKey(en));
        Assert.assertTrue(descriptions.containsKey(enbr));
        Assert.assertEquals(descriptions.get(enbr), "british");

        // test removing the same entry
        descriptions.remove(en);
        Assert.assertFalse(descriptions.isEmpty());
        Assert.assertEquals(descriptions.size(), 1);
        Assert.assertFalse(descriptions.containsKey(en));
        Assert.assertTrue(descriptions.containsKey(enbr));
        Assert.assertEquals(descriptions.get(enbr), "british");

        // test removing null
        descriptions.remove(null);
        Assert.assertFalse(descriptions.isEmpty());
        Assert.assertEquals(descriptions.size(), 1);
        Assert.assertFalse(descriptions.containsKey(en));
        Assert.assertTrue(descriptions.containsKey(enbr));
        Assert.assertEquals(descriptions.get(enbr), "british");

        // test removing the second entry
        descriptions.remove(enbr);
        Assert.assertTrue(descriptions.isEmpty());
        Assert.assertEquals(descriptions.size(), 0);
        Assert.assertFalse(descriptions.containsKey(en));
        Assert.assertFalse(descriptions.containsKey(enbr));

        // test adding something once the collection has been drained
        descriptions.put(en, " english ");
        Assert.assertFalse(descriptions.isEmpty());
        Assert.assertEquals(descriptions.size(), 1);
        Assert.assertTrue(descriptions.containsKey(en));
        Assert.assertEquals("english", descriptions.get(en));

        // test replacing all entries
        Map<Locale, String> newDescriptions = new HashMap<Locale, String>();
        newDescriptions.put(enbr, " british");
        attrib.setDisplayDescriptions(newDescriptions);
        Assert.assertFalse(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 1);
        Assert.assertFalse(attrib.getDisplayDescriptions().containsKey(en));
        Assert.assertTrue(attrib.getDisplayDescriptions().containsKey(enbr));
        Assert.assertEquals(attrib.getDisplayDescriptions().get(enbr), "british");

        newDescriptions.put(null, "foo");
        Assert.assertFalse(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 1);

        newDescriptions.put(en, null);
        Assert.assertFalse(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 1);

        newDescriptions.put(en, "");
        Assert.assertFalse(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 1);
    }

    /** Tests that values are properly added and modified. */
    @Test public void testValues() {
        LocalizedStringAttributeValue value1 = new LocalizedStringAttributeValue("value1", null);
        LocalizedStringAttributeValue value2 = new LocalizedStringAttributeValue("value2", null);

        Attribute attrib = new Attribute("foo");
        Collection attribValues = attrib.getValues();

        // test adding one entry
        Assert.assertTrue(attribValues.add(value1));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 1);
        Assert.assertTrue(attrib.getValues().contains(value1));

        // test adding another entry
        Assert.assertTrue(attribValues.add(value2));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 2);
        Assert.assertTrue(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test adding null
        Assert.assertFalse(attribValues.add(null));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 2);
        Assert.assertTrue(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test adding an existing value
        Assert.assertFalse(attribValues.add(value2));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 2);
        Assert.assertTrue(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test removing an entry
        Assert.assertTrue(attribValues.remove(value1));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 1);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test removing the same entry
        Assert.assertFalse(attribValues.remove(value1));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 1);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test removing null
        Assert.assertFalse(attribValues.remove(null));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 1);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test removing the second entry
        Assert.assertTrue(attribValues.remove(value2));
        Assert.assertTrue(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 0);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertFalse(attrib.getValues().contains(value2));

        // test adding something once the collection has been drained
        Assert.assertTrue(attribValues.add(value1));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 1);
        Assert.assertTrue(attrib.getValues().contains(value1));
        Assert.assertFalse(attrib.getValues().contains(value2));

        // test replacing all entries
        Collection<AttributeValue> values = new ArrayList<AttributeValue>();
        values.add(value2);
        attrib.setValues(values);
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 1);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));
    }

    /** Tests that values are properly added and modified. */
    @Test public void testEncoders() {
        AttributeEncoder<String> enc1 = new MockEncoder<String>();
        AttributeEncoder<String> enc2 = new MockEncoder<String>();

        Attribute attrib = new Attribute("foo");
        Set<AttributeEncoder<?>> attribEncoders = attrib.getEncoders();

        // test adding one entry
        Assert.assertTrue(attribEncoders.add(enc1));
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 1);
        Assert.assertTrue(attrib.getEncoders().contains(enc1));

        // test adding another entry
        Assert.assertTrue(attribEncoders.add(enc2));
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 2);
        Assert.assertTrue(attrib.getEncoders().contains(enc1));
        Assert.assertTrue(attrib.getEncoders().contains(enc2));

        // test adding null
        Assert.assertFalse(attribEncoders.add(null));
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 2);
        Assert.assertTrue(attrib.getEncoders().contains(enc1));
        Assert.assertTrue(attrib.getEncoders().contains(enc2));

        // test adding an existing Encoder
        Assert.assertFalse(attribEncoders.add(enc2));
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 2);
        Assert.assertTrue(attrib.getEncoders().contains(enc1));
        Assert.assertTrue(attrib.getEncoders().contains(enc2));

        // test removing an entry
        Assert.assertTrue(attribEncoders.remove(enc1));
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 1);
        Assert.assertFalse(attrib.getEncoders().contains(enc1));
        Assert.assertTrue(attrib.getEncoders().contains(enc2));

        // test removing the same entry
        Assert.assertFalse(attribEncoders.remove(enc1));
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 1);
        Assert.assertFalse(attrib.getEncoders().contains(enc1));
        Assert.assertTrue(attrib.getEncoders().contains(enc2));

        // test removing null
        Assert.assertFalse(attribEncoders.remove(null));
        Assert.assertFalse(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 1);
        Assert.assertFalse(attrib.getEncoders().contains(enc1));
        Assert.assertTrue(attrib.getEncoders().contains(enc2));

        // test removing the second entry
        Assert.assertTrue(attribEncoders.remove(enc2));
        Assert.assertTrue(attrib.getEncoders().isEmpty());
        Assert.assertEquals(attrib.getEncoders().size(), 0);
        Assert.assertFalse(attrib.getEncoders().contains(enc1));
        Assert.assertFalse(attrib.getEncoders().contains(enc2));

        // test adding something once the collection has been drained
        Assert.assertTrue(attribEncoders.add(enc1));
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
    }
}