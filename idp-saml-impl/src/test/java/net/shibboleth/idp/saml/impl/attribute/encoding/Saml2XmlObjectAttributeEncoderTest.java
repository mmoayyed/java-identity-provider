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

package net.shibboleth.idp.saml.impl.attribute.encoding;

import java.util.Collection;
import java.util.List;

import net.shibboleth.idp.attribute.AttributeEncodingException;

import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeValue;
import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * {@link Saml1ByteAttributeEncoder} Unit test.
 * 
 * Identical code to the {@link Saml1ByteAttributeEncoder} except that the type of assertion and encoder is changed.
 */
public class Saml2XmlObjectAttributeEncoderTest {

    /** The name we give the test attribute. */
    private final static String ATTR_NAME = "foo";

    /** A test value. */
    private final static String STRING_1 = "Value The First";

    /** A second test value. */
    private final static String STRING_2 = "Second string the value is";

    /**
     * Create an XML object from a string which we can test against later. We use the Saml1StringEncoder to do the work
     * because it is handy in this package.
     * 
     * @param value that we encode
     * @return an XML object
     */
    private static XMLObject ObjectFor(final String value) {
        final Saml1StringAttributeEncoder encoder = new Saml1StringAttributeEncoder();
        final net.shibboleth.idp.attribute.Attribute<Object> inputAttribute;

        inputAttribute = new net.shibboleth.idp.attribute.Attribute(ATTR_NAME);
        inputAttribute.setValues(CollectionSupport.toSet((Object) value));
        try {
            return encoder.encode(inputAttribute);
        } catch (AttributeEncodingException e) {
            return null;
        }
    }

    /**
     * Check that the input XML object is what was expected - something created by {@link ObjectFor} with a value taken
     * from possibles.
     * 
     * @param input the objects in question.
     * @param possibles the strings that they might be encoding.
     */
    private static void CheckValues(final XMLObject input, final String... possibles) {
        Assert.assertTrue(input instanceof org.opensaml.saml1.core.Attribute);
        final org.opensaml.saml1.core.Attribute attribute = (org.opensaml.saml1.core.Attribute) input;

        final List<XMLObject> children = attribute.getOrderedChildren();
        Assert.assertEquals(children.size(), 1, "Data must have but one entry");
        Assert.assertTrue(children.get(0) instanceof XSString, "Data must contain one string");
        final String s = ((XSString) children.get(0)).getValue();

        for (String possible : possibles) {
            if (s.equals(possible)) {
                return;
            }
        }
        Assert.assertTrue(false, "No potential matched matched");
    }

    @BeforeSuite()
    public void initOpenSAML() throws InitializationException {
        InitializationService.initialize();
    }

    @Test
    public void testEmpty() throws Exception {
        final Saml2XmlObjectAttributeEncoder encoder = new Saml2XmlObjectAttributeEncoder();

        final net.shibboleth.idp.attribute.Attribute<Object> inputAttribute;
        inputAttribute = new net.shibboleth.idp.attribute.Attribute(ATTR_NAME);

        final Attribute outputAttribute = encoder.encode(inputAttribute);
        Assert.assertNull(outputAttribute, "Encoding the empty set should yield a null attribute");
    }

    @Test
    public void testInappropriate() throws Exception {
        final Saml2XmlObjectAttributeEncoder encoder = new Saml2XmlObjectAttributeEncoder();
        final int[] intArray = {1, 2, 3, 4};
        final Collection<Object> values = CollectionSupport.toSet(new Integer(3), STRING_1, new Object(), intArray);

        final net.shibboleth.idp.attribute.Attribute<Object> inputAttribute;
        inputAttribute = new net.shibboleth.idp.attribute.Attribute(ATTR_NAME);
        inputAttribute.setValues(values);

        final Attribute outputAttribute = encoder.encode(inputAttribute);
        Assert.assertNull(outputAttribute, "Encoding a series of invalid inputs should yield a null attribute");
    }

    @Test
    public void testSingle() throws Exception {
        final Saml2XmlObjectAttributeEncoder encoder = new Saml2XmlObjectAttributeEncoder();
        final Collection<Object> values = CollectionSupport.toSet(new Object(), ObjectFor(STRING_1));

        final net.shibboleth.idp.attribute.Attribute<Object> inputAttribute;
        inputAttribute = new net.shibboleth.idp.attribute.Attribute(ATTR_NAME);
        inputAttribute.setValues(values);

        final Attribute outputAttribute = encoder.encode(inputAttribute);
        Assert.assertNotNull(outputAttribute);

        final List<XMLObject> children = outputAttribute.getOrderedChildren();
        Assert.assertEquals(children.size(), 1, "Encoding one entry");
        Assert.assertEquals(children.get(0).getElementQName(), AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");
        Assert.assertEquals(children.get(0).getOrderedChildren().size(), 1,
                "Expected exactly one child inside the <AttributeValue/>");

        CheckValues(children.get(0).getOrderedChildren().get(0), STRING_1);
    }

    @Test
    public void testMulti() throws Exception {
        final Saml2XmlObjectAttributeEncoder encoder = new Saml2XmlObjectAttributeEncoder();
        final Collection<Object> values =
                CollectionSupport.toSet(new Object(), ObjectFor(STRING_1), ObjectFor(STRING_2));

        final net.shibboleth.idp.attribute.Attribute<Object> inputAttribute;
        inputAttribute = new net.shibboleth.idp.attribute.Attribute(ATTR_NAME);
        inputAttribute.setValues(values);

        final Attribute outputAttribute = encoder.encode(inputAttribute);
        Assert.assertNotNull(outputAttribute);

        final List<XMLObject> children = outputAttribute.getOrderedChildren();
        Assert.assertEquals(children.size(), 2, "Encoding two entries");

        Assert.assertEquals(children.get(0).getElementQName(), AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");
        Assert.assertEquals(children.get(0).getOrderedChildren().size(), 1,
                "Expected exactly one child inside the <AttributeValue/> for first Attribute");

        Assert.assertEquals(children.get(1).getElementQName(), AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");
        Assert.assertEquals(children.get(1).getOrderedChildren().size(), 1,
                "Expected exactly one child inside the <AttributeValue/> for second Attribute");

        CheckValues(children.get(0).getOrderedChildren().get(0), STRING_1, STRING_2);
        CheckValues(children.get(1).getOrderedChildren().get(0), STRING_1, STRING_2);

    }

}
