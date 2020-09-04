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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.impl.EntityDescriptorBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.EmptyAttributeValue.EmptyType;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

/** Unit test for {@link IdPAttribute} class. */
@SuppressWarnings("javadoc")
public class AttributeTest extends OpenSAMLInitBaseTestCase{

    /** Tests that the attribute has its expected state after instantiation. */
    @Test public void instantiation() {
        IdPAttribute attrib = new IdPAttribute("foo");

        Assert.assertEquals(attrib.getId(), "foo");

        Assert.assertNotNull(attrib.getDisplayDescriptions());
        Assert.assertTrue(attrib.getDisplayDescriptions().isEmpty());

        Assert.assertNotNull(attrib.getDisplayNames());
        Assert.assertTrue(attrib.getDisplayNames().isEmpty());

        Assert.assertNotNull(attrib.getValues());
        Assert.assertTrue(attrib.getValues().isEmpty());

        Assert.assertNotNull(attrib.hashCode());

        Assert.assertTrue(attrib.equals(new IdPAttribute("foo")));
    }

    /** Tests that null/empty IDs aren't accepted. */
    @Test public void nullEmptyId() {
        try {
            new IdPAttribute(null);
            Assert.fail("able to create attribute with null ID");
        } catch (ConstraintViolationException e) {
            // expected this
        }

        try {
            new IdPAttribute("");
            Assert.fail("able to create attribute with empty ID");
        } catch (ConstraintViolationException e) {
            // expected this
        }

        try {
            new IdPAttribute(" ");
            Assert.fail("able to create attribute with empty ID");
        } catch (ConstraintViolationException e) {
            // expected this
        }
        
        try {
            new IdPAttribute("a b");
            Assert.fail("able to create attribute ID with spaces");
        } catch (ConstraintViolationException e) {
            // expected this
        }

        
    }

    /** Tests that display names are properly added and modified. */
    @Test public void displayNames() {
        Locale en = new Locale("en");
        Locale enbr = new Locale("en", "br");

        IdPAttribute attrib = new IdPAttribute("foo");
        attrib.setDisplayNames(Collections.EMPTY_MAP);
        Assert.assertTrue(attrib.getDisplayNames().isEmpty());

        attrib.setDisplayNames(Collections.emptyMap());
        Assert.assertTrue(attrib.getDisplayNames().isEmpty());

        Map<Locale, String> displayNames = new HashMap<>();
        // test adding one entry
        displayNames.put(en, " english ");
        attrib.setDisplayNames(displayNames);
        
        Assert.assertFalse(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 1);
        Assert.assertTrue(attrib.getDisplayNames().containsKey(en));
        Assert.assertEquals(attrib.getDisplayNames().get(en), "english");

        // test adding another entry
        displayNames.put(enbr, "british");
        displayNames.put(en, " englishX ");
        attrib.setDisplayNames(displayNames);
        Assert.assertFalse(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 2);
        Assert.assertTrue(attrib.getDisplayNames().containsKey(enbr));
        Assert.assertEquals(attrib.getDisplayNames().get(enbr), "british");
        Assert.assertEquals(attrib.getDisplayNames().get(en), "englishX");

        // test replacing an entry
        String replacedName = displayNames.put(en, "english ");
        Assert.assertEquals(replacedName, " englishX ");

        attrib.setDisplayNames(displayNames);
        Assert.assertFalse(attrib.getDisplayNames().isEmpty());
        Assert.assertEquals(attrib.getDisplayNames().size(), 2);
        Assert.assertTrue(attrib.getDisplayNames().containsKey(en));
        Assert.assertEquals(attrib.getDisplayNames().get(en), "english");

        try {
            // test removing an entry
            attrib.getDisplayNames().remove(en);
            Assert.fail();
        } catch (UnsupportedOperationException e) {
        }
        try {
            // test removing an entry
            attrib.getDisplayNames().put(en, "foo");
            Assert.fail();
        } catch (UnsupportedOperationException e) {
        }
    }

    /** Tests that display descriptions are properly added and modified. */
    @Test public void displayDescriptions() {
        Locale en = new Locale("en");
        Locale enbr = new Locale("en", "br");

        IdPAttribute attrib = new IdPAttribute("foo");
        attrib.setDisplayDescriptions(Collections.EMPTY_MAP);
        Assert.assertTrue(attrib.getDisplayNames().isEmpty());

        attrib.setDisplayNames(Collections.emptyMap());
        Assert.assertTrue(attrib.getDisplayDescriptions().isEmpty());

        Map<Locale, String> displayDescriptions = new HashMap<>();
        displayDescriptions.clear();
        attrib.setDisplayDescriptions(displayDescriptions);
        Assert.assertTrue(attrib.getDisplayDescriptions().isEmpty());

        displayDescriptions.clear();
        displayDescriptions.put(en, " english ");
        attrib.setDisplayDescriptions(displayDescriptions);
        
        Assert.assertFalse(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 1);
        Assert.assertTrue(attrib.getDisplayDescriptions().containsKey(en));
        Assert.assertEquals(attrib.getDisplayDescriptions().get(en), "english");

        // test adding another entry
        displayDescriptions.put(enbr, "british");
        displayDescriptions.put(en, " englishX ");
        attrib.setDisplayDescriptions(displayDescriptions);
        Assert.assertFalse(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 2);
        Assert.assertTrue(attrib.getDisplayDescriptions().containsKey(enbr));
        Assert.assertEquals(attrib.getDisplayDescriptions().get(enbr), "british");
        Assert.assertEquals(attrib.getDisplayDescriptions().get(en), "englishX");

        // test replacing an entry
        String replacedName = displayDescriptions.put(en, "english ");
        Assert.assertEquals(replacedName, " englishX ");

        attrib.setDisplayDescriptions(displayDescriptions);
        Assert.assertFalse(attrib.getDisplayDescriptions().isEmpty());
        Assert.assertEquals(attrib.getDisplayDescriptions().size(), 2);
        Assert.assertTrue(attrib.getDisplayDescriptions().containsKey(en));
        Assert.assertEquals(attrib.getDisplayDescriptions().get(en), "english");

        try {
            // test removing an entry
            attrib.getDisplayDescriptions().remove(en);
            Assert.fail();
        } catch (UnsupportedOperationException e) {
        }
        try {
            // test removing an entry
            attrib.getDisplayDescriptions().put(en, "foo");
            Assert.fail();
        } catch (UnsupportedOperationException e) {
        }
    }

    /** Tests that values are properly added and modified. */
    @Test(enabled=false) public void values() {
        StringAttributeValue value1 = new StringAttributeValue("value1");
        StringAttributeValue value2 = new StringAttributeValue("value2");

        IdPAttribute attrib = new IdPAttribute("foo");
        Assert.assertTrue(attrib.getValues().isEmpty());

        attrib.setValues(null);
        Assert.assertTrue(attrib.getValues().isEmpty());

        attrib.setValues(Collections.emptyList());
        Assert.assertTrue(attrib.getValues().isEmpty());
        
        List<IdPAttributeValue> attribValues = new ArrayList<>();
        attrib.setValues(attribValues);
        Assert.assertTrue(attrib.getValues().isEmpty());
        
        attribValues.add(null);
        attrib.setValues(attribValues);
        Assert.assertTrue(attrib.getValues().isEmpty());
        
        Assert.assertTrue(attrib.getValues().add(value1));
        Assert.assertEquals(attrib.getValues().size(), 1);

        // test adding another entry
        Assert.assertTrue(attrib.getValues().add(value2));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 2);
        Assert.assertTrue(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test adding null
        try {
            Assert.assertFalse(attrib.getValues().add(null));
            Assert.fail();
        } catch (NullPointerException e) {
            // THis is OK by the annotation
        }

        // test adding an existing value
        Assert.assertFalse(attrib.getValues().add(value2));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 2);
        Assert.assertTrue(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test removing an entry
        Assert.assertTrue(attrib.getValues().remove(value1));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 1);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test removing the same entry
        Assert.assertFalse(attrib.getValues().remove(value1));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 1);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test removing null
        Assert.assertFalse(attrib.getValues().remove(null));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 1);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));

        // test removing the second entry
        Assert.assertTrue(attrib.getValues().remove(value2));
        Assert.assertTrue(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 0);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertFalse(attrib.getValues().contains(value2));

        // test adding something once the collection has been drained
        Assert.assertTrue(attrib.getValues().add(value1));
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 1);
        Assert.assertTrue(attrib.getValues().contains(value1));
        Assert.assertFalse(attrib.getValues().contains(value2));

        // test replacing all entries
        List<IdPAttributeValue> values = new ArrayList<>();
        values.add(value2);
        attrib.setValues(values);
        Assert.assertFalse(attrib.getValues().isEmpty());
        Assert.assertEquals(attrib.getValues().size(), 1);
        Assert.assertFalse(attrib.getValues().contains(value1));
        Assert.assertTrue(attrib.getValues().contains(value2));
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test public void cloneToString() {
        IdPAttribute attrib = new IdPAttribute("foo");
        IdPAttribute dupl = new IdPAttribute("foo");
        IdPAttribute diff = new IdPAttribute("bar");

        Assert.assertTrue(attrib.equals(attrib));
        Assert.assertTrue(attrib.equals(dupl));
        Assert.assertFalse(attrib.equals(null));
        Assert.assertFalse(attrib.equals(Integer.valueOf(2)));
        
        Assert.assertEquals(attrib.hashCode(), dupl.hashCode());
        Assert.assertNotSame(attrib.hashCode(), diff.hashCode());
        
        Assert.assertTrue(attrib.compareTo(diff) > 0);
        Assert.assertEquals(attrib.compareTo(dupl) , 0);
        
        attrib.setValues(Collections.singletonList(new StringAttributeValue("value1")));
        attrib.setDisplayDescriptions(Collections.singletonMap(new Locale("en"), "Descrption"));
        attrib.setDisplayNames(Collections.singletonMap(new Locale("en"), "Name"));
        attrib.toString();
    }
    
    @Test public void names() {
        assertTrue(IdPAttribute.isDeprecatedId("%"));
        assertTrue(IdPAttribute.isDeprecatedId("elepha{nt"));
        assertTrue(IdPAttribute.isDeprecatedId("IAmtheWalru}"));
        assertTrue(IdPAttribute.isDeprecatedId("JohnHenryBonham%"));
        assertTrue(IdPAttribute.isDeprecatedId("Now\'StheTImeForallgoodmen"));
        assertFalse(IdPAttribute.isDeprecatedId("JamesClarkNaxwell"));
        
        assertTrue(IdPAttribute.isInvalidId("spaces in names"));
        assertTrue(IdPAttribute.isInvalidId("\ttabs\tinnames"));
        assertTrue(IdPAttribute.isInvalidId("Others\rInnames"));
        assertTrue(IdPAttribute.isInvalidId("Others\nInnames"));
        assertTrue(IdPAttribute.isInvalidId("  "));
        assertTrue(IdPAttribute.isInvalidId(""));
        assertTrue(IdPAttribute.isInvalidId(null));
        assertFalse(IdPAttribute.isInvalidId("JohnNapier"));
        
        try {
            new IdPAttribute("names  with S");
            fail("Expected Constraint Violation Exception");
        } catch (final ConstraintViolationException e) {
            //expected
        }
        new IdPAttribute("Check%for{deprecation");
    }

    @Test public void displayValues() {
        final IdPAttributeValue stringVal = StringAttributeValue.valueOf("Stringval");
        final IdPAttributeValue scopedVal = ScopedStringAttributeValue.valueOf("value", "scope");
        final byte array[] = {1,2,3,4};
        final IdPAttributeValue binary = ByteAttributeValue.valueOf(array);
        final IdPAttributeValue nullEmpty = new EmptyAttributeValue(EmptyType.NULL_VALUE);
        final IdPAttributeValue zeroEmpty = new EmptyAttributeValue(EmptyType.ZERO_LENGTH_VALUE);
        final EntityDescriptor entity = (new EntityDescriptorBuilder()).buildObject();
        entity.setEntityID("https://example.org");
        final IdPAttributeValue xmlval = new XMLObjectAttributeValue(entity);

        final List<IdPAttributeValue> inList = List.of(stringVal, scopedVal, binary, nullEmpty, zeroEmpty, xmlval, stringVal);
        final HashSet<IdPAttributeValue> inHash = new HashSet<>(inList);
        final ArrayList<IdPAttributeValue> sorted = new ArrayList<>(inList);
        sorted.sort(null);
        final ArrayList<IdPAttributeValue> sortedDedup = new ArrayList<>(inHash);
        sortedDedup.sort(null);
        assertEquals(inList.size()-1, inHash.size());
        assertEquals(inList.size(), sorted.size());
        assertEquals(sortedDedup.size(), inHash.size());

        for (IdPAttributeValue v:inList) {
            assertTrue(inHash.contains(v));
        }
        for (IdPAttributeValue v:sorted) {
            assertTrue(inHash.contains(v));
        }
        for (IdPAttributeValue v:sortedDedup) {
            assertTrue(inHash.contains(v));
        }
        int offset = 0;
        for (int i = 0; i < sortedDedup.size(); i++) {
            final String s = sorted.get(i+offset).getDisplayValue();
            final String ds = sortedDedup.get(i).getDisplayValue();
            if (!ds.equals(s)) {
                assertEquals(s, sorted.get(i-1).getDisplayValue());
                assertEquals(ds, sorted.get(i+1).getDisplayValue());
                offset = 1;
            }
        }
    }
}
