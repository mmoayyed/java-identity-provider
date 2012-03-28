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

import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeValue;
import org.opensaml.xml.schema.XSString;
import org.owasp.esapi.codecs.Base64;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/**
 * {@link Saml1ByteAttributeEncoder} Unit test.
 * 
 * Identical code to the {@link Saml1ByteAttributeEncoder} except that the type of assertion and encoder is changed.
 */
public class Saml2ByteAttributeEncoderTest {

    /** The name we give the test attribute. */
    private final static String ATTR_NAME = "foo";

    /** A test value. */
    private final static byte[] BYTE_ARRAY_1 = {1, 2, 3, 4, 5};

    /** A second test value. */
    private final static byte[] BYTE_ARRAY_2 = {4, 3, 2, 1};

    @BeforeSuite() public void initOpenSAML() throws InitializationException {
        InitializationService.initialize();
    }

    @Test public void testEmpty() throws Exception {
        final Saml2ByteAttributeEncoder encoder = new Saml2ByteAttributeEncoder();
        final net.shibboleth.idp.attribute.Attribute inputAttribute;

        inputAttribute = new net.shibboleth.idp.attribute.Attribute(ATTR_NAME);

        final Attribute outputAttribute = encoder.encode(inputAttribute);

        Assert.assertNull(outputAttribute, "Encoding the empty set should yield a null attribute");
    }

    @Test public void testInappropriate() throws Exception {
        final Saml2ByteAttributeEncoder encoder = new Saml2ByteAttributeEncoder();
        final int[] intArray = {1, 2, 3, 4};
        final Collection<Object> values = Lists.newArrayList(new Integer(3), "1234", new Object(), intArray);
        final net.shibboleth.idp.attribute.Attribute inputAttribute;

        inputAttribute = new net.shibboleth.idp.attribute.Attribute(ATTR_NAME);
        inputAttribute.setValues(values);

        final Attribute outputAttribute = encoder.encode(inputAttribute);

        Assert.assertNull(outputAttribute, "Encoding a series of invalid inputs should yield a null attribute");
    }

    @Test public void testSingle() throws Exception {
        final Saml2ByteAttributeEncoder encoder = new Saml2ByteAttributeEncoder();
        final Collection<Object> values = Lists.newArrayList(new Object(), BYTE_ARRAY_1);
        final net.shibboleth.idp.attribute.Attribute inputAttribute;

        inputAttribute = new net.shibboleth.idp.attribute.Attribute(ATTR_NAME);
        inputAttribute.setValues(values);

        final Attribute outputAttribute = encoder.encode(inputAttribute);

        Assert.assertNotNull(outputAttribute);

        List<XMLObject> children = outputAttribute.getOrderedChildren();

        Assert.assertEquals(children.size(), 1, "Encoding one entry");

        XMLObject child = children.get(0);
        Assert.assertEquals(child.getElementQName(), AttributeValue.DEFAULT_ELEMENT_NAME);

        Assert.assertTrue(child instanceof XSString, "Child of result attribute shoulld be a string");

        XSString childAsString = (XSString) child;

        byte childAsBa[] = Base64.decode(childAsString.getValue());

        Assert.assertEquals(childAsBa, BYTE_ARRAY_1, "Input equals output");
    }

    @Test public void testMulti() throws Exception {
        final Saml2ByteAttributeEncoder encoder = new Saml2ByteAttributeEncoder();
        final Collection<Object> values = Lists.newArrayList((Object) BYTE_ARRAY_1, BYTE_ARRAY_2);

        final net.shibboleth.idp.attribute.Attribute inputAttribute;
        inputAttribute = new net.shibboleth.idp.attribute.Attribute(ATTR_NAME);
        inputAttribute.setValues(values);

        final Attribute outputAttribute = encoder.encode(inputAttribute);

        Assert.assertNotNull(outputAttribute);

        final List<XMLObject> children = outputAttribute.getOrderedChildren();
        Assert.assertEquals(children.size(), 2, "Encoding two entries");

        XMLObject child = children.get(0);
        Assert.assertEquals(child.getElementQName(), AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");
        Assert.assertTrue(child instanceof XSString, "Child of result attribute shoulld be a string");

        XSString childAsString = (XSString) child;
        Assert.assertEquals(child.getElementQName(), AttributeValue.DEFAULT_ELEMENT_NAME,
                "Attribute Value not inside <AttributeValue/>");
        final byte[] res0 = Base64.decode(childAsString.getValue());

        child = children.get(1);
        Assert.assertTrue(child instanceof XSString, "Child of result attribute shoulld be a string");

        childAsString = (XSString) child;
        final byte[] res1 = Base64.decode(childAsString.getValue());

        //
        // order of results is not guaranteed so sense the result from the length
        //
        if (BYTE_ARRAY_1.length == res0.length) {
            Assert.assertEquals(BYTE_ARRAY_1, res0, "Input matches output");
            Assert.assertEquals(BYTE_ARRAY_2, res1, "Input matches output");
        } else if (BYTE_ARRAY_1.length == res1.length) {
            Assert.assertEquals(BYTE_ARRAY_1, res1, "Input matches output");
            Assert.assertEquals(BYTE_ARRAY_2, res0, "Input matches output");
        } else {
            Assert.assertTrue(BYTE_ARRAY_1.length == res1.length || BYTE_ARRAY_2.length == res1.length,
                    "One of the output's size should match an input size");
        }
    }

}
