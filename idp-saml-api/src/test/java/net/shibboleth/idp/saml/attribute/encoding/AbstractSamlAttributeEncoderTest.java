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

package net.shibboleth.idp.saml.attribute.encoding;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.impl.XSStringBuilder;
import org.opensaml.saml.common.SAMLObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/**
 *
 */
public class AbstractSamlAttributeEncoderTest extends OpenSAMLInitBaseTestCase {
    
    private XSStringBuilder theBuilder;
    private QName theQName = new QName("LocalQNAME");
    private final String MY_NAME = "myName";
    private final String MY_NAMESPACE = "myNameSpace";
    private final String ATTRIBUTE_ID = "attrID";
    private final String ATTRIBUTE_VALUE_1 = "attrValOne";
    private final String ATTRIBUTE_VALUE_2 = "attrValeTwo";
    
    @BeforeClass public void initTest() throws ComponentInitializationException {
        theBuilder = new XSStringBuilder();
    }
    
    @Test public void testInitializeAndSetters() throws AttributeEncodingException, ComponentInitializationException {
        AbstractSamlAttributeEncoder encoder = new mockEncoder(theBuilder, theQName);
        
        Assert.assertNull(encoder.getName());
        Assert.assertNull(encoder.getNamespace());
        
        encoder.setName("");
        Assert.assertNull(encoder.getName());
        encoder.setNamespace(MY_NAMESPACE);
        Assert.assertEquals(encoder.getNamespace(), MY_NAMESPACE);
        
        try {
            encoder.initialize();
            Assert.fail();
        } catch (ComponentInitializationException ex) {
            //OK
        }
        encoder = new mockEncoder(theBuilder, theQName);
        encoder.setNamespace("");
        Assert.assertNull(encoder.getNamespace());
        encoder.setName(MY_NAME);
        Assert.assertEquals(encoder.getName(), MY_NAME);

        try {
            encoder.initialize();
            Assert.fail();
        } catch (ComponentInitializationException ex) {
            //OK
        }
        encoder = new mockEncoder(theBuilder, theQName);
        encoder.setNamespace("");
        encoder.setNamespace(MY_NAMESPACE);
        encoder.setName(MY_NAME);
        
        try {
            encoder.encode(new Attribute(ATTRIBUTE_ID));
            Assert.fail();
        } catch (UninitializedComponentException ex) {
            // OK
        }
        
        encoder.initialize();
        try {
            encoder.setName(" ");
            Assert.fail();
        } catch (UnmodifiableComponentException ex) {
            //
        }
        
        try {
            encoder.setNamespace(" ");
            Assert.fail();
        } catch (UnmodifiableComponentException ex) {
            //
        }

    }

    @Test public void testEncode() throws AttributeEncodingException, ComponentInitializationException {
        AbstractSamlAttributeEncoder encoder = new mockEncoder(theBuilder, theQName);
        encoder.setNamespace(MY_NAMESPACE);
        encoder.setName(MY_NAME);
        encoder.initialize();
        Attribute attr = new Attribute(ATTRIBUTE_ID);
        
        try {
            encoder.encode(attr);
        } catch (AttributeEncodingException e) {
            // OK
        }
        
        final int[] intArray = {1, 2, 3, 4};
        final Collection<AttributeValue> values =
                Lists.newArrayList((AttributeValue) new ByteAttributeValue(new byte[] {1, 2, 3,}),
                        null,
                        new AttributeValue() {
                            public Object getValue() {
                                return intArray;
                            }
                        }
                        );
        attr.setValues(values);
        try {
            encoder.encode(attr);
        } catch (AttributeEncodingException e) {
            // OK
        }
        values.add(new StringAttributeValue(ATTRIBUTE_VALUE_1));
        values.add(new StringAttributeValue(ATTRIBUTE_VALUE_2));
        attr.setValues(values);
        
        List<XMLObject> result = ((org.opensaml.saml.saml1.core.Attribute) encoder.encode(attr)).getAttributeValues();
        
        Assert.assertEquals(result.size(), 2);
        Set<String> resultSet = new HashSet<String>(2); 
        for (XMLObject o: result) {
            Assert.assertTrue(o instanceof XSString);
            resultSet.add(((XSString) o).getValue());
        }
        Assert.assertTrue(resultSet.contains(ATTRIBUTE_VALUE_1));
        Assert.assertTrue(resultSet.contains(ATTRIBUTE_VALUE_2));

    }
    
    @Test public void testEqualsHash() {
        mockEncoder enc1 = new mockEncoder(theBuilder, theQName);
        Assert.assertEquals(enc1, enc1);
        Assert.assertNotSame(enc1, null);
        Assert.assertNotSame(enc1, this);

        mockEncoder enc2 = new mockEncoder(theBuilder, theQName);
        
        Assert.assertEquals(enc1, enc2);
        Assert.assertEquals(enc1.hashCode(), enc2.hashCode());
        enc1.setName(MY_NAME);
        enc1.setNamespace(MY_NAMESPACE);
        enc2.setName(MY_NAME);
        enc2.setNamespace(MY_NAMESPACE);
        Assert.assertEquals(enc1, enc2);
        Assert.assertEquals(enc1.hashCode(), enc2.hashCode());
        enc2.setName(MY_NAME + MY_NAME);
        Assert.assertNotSame(enc1,  enc2);
        Assert.assertNotSame(enc1.hashCode(),  enc2.hashCode());
        enc2.setName(MY_NAME);
        enc2.setNamespace(MY_NAME);
        Assert.assertNotSame(enc1,  enc2);
        Assert.assertNotSame(enc1.hashCode(),  enc2.hashCode());        
        
        AbstractSamlAttributeEncoder enc3 = new AbstractSamlAttributeEncoder<SAMLObject, AttributeValue>() {

            public String getProtocol() {
                return "Random Stuff";
            }

            protected boolean canEncodeValue(Attribute attribute, AttributeValue value) {
                return false;
            }

            protected XMLObject encodeValue(Attribute attribute, AttributeValue value)
                    throws AttributeEncodingException {
                return null;
            }

            protected SAMLObject buildAttribute(Attribute attribute, List<XMLObject> attributeValues)
                    throws AttributeEncodingException {
                return null;
            }};
            enc3.setName(MY_NAME);
            enc3.setNamespace(MY_NAMESPACE);
            Assert.assertNotSame(enc1,  enc3);
            Assert.assertNotSame(enc1.hashCode(),  enc3.hashCode());        
    }
 
    protected static class mockEncoder extends AbstractSaml1AttributeEncoder {
        
        private final XSStringBuilder builder;
        private final QName myQName;
        
        public mockEncoder(final XSStringBuilder theBuilder, final QName theQName) {
            builder = theBuilder;
            myQName = theQName;
        }

        /** {@inheritDoc} */
        protected boolean canEncodeValue(Attribute attribute, AttributeValue value) {
            return ! (value instanceof ByteAttributeValue);
        }

        /** {@inheritDoc} */
        protected XMLObject encodeValue(Attribute attribute, AttributeValue value) throws AttributeEncodingException {
            if (!(value instanceof StringAttributeValue)) {
                return null;
            }
            XSString result = builder.buildObject(myQName);
            result.setValue((String) value.getValue());
            return result;
        }
        
    }
}
