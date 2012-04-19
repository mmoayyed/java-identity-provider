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

import java.util.List;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.impl.NameIDBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test form {@link SamlEncoderSupport}.
 */
public class SamlEncoderSupportTest  extends OpenSAMLInitBaseTestCase {
    
    /** Test values. */
    private final static String QNAME_LOCALPART = "myQName";
    private final static QName QNAME = new QName(QNAME_LOCALPART);
    private final static String STRING_VALUE = "TestValue";
    private final static Attribute ATTR = new Attribute("attr");
    private final static byte[] BYTE_ARRAY_VALUE = {1, 2, 3, 4, 5};
    
    @Test public void testEncodeStringValue() {
        
        try {
            SamlEncoderSupport.encodeStringValue(null, QNAME, STRING_VALUE );
            Assert.fail("Missed contraint");
        } catch (ConstraintViolationException ex) {
            //OK
        }
        
        try {
            SamlEncoderSupport.encodeStringValue(ATTR, null, STRING_VALUE );
            Assert.fail("Missed contraint");
        } catch (ConstraintViolationException ex) {
            //OK
        }
        
        Assert.assertNull(SamlEncoderSupport.encodeStringValue(ATTR, QNAME, ""));
        Assert.assertNull(SamlEncoderSupport.encodeStringValue(ATTR, QNAME, null));
        
        XMLObject obj = SamlEncoderSupport.encodeStringValue(ATTR, QNAME, STRING_VALUE);
        Assert.assertEquals(obj.getElementQName().getLocalPart(), QNAME_LOCALPART);
        Assert.assertTrue(obj instanceof XSString);
        XSString str = (XSString) obj;
        
        Assert.assertEquals(str.getValue(), STRING_VALUE);
    }
    
    @Test public void testEncodeByteArrayValue() {
        
        try {
            SamlEncoderSupport.encodeByteArrayValue(null, QNAME, BYTE_ARRAY_VALUE);
            Assert.fail("Missed contraint");
        } catch (ConstraintViolationException ex) {
            //OK
        }
        
        try {
            SamlEncoderSupport.encodeByteArrayValue(ATTR, null, BYTE_ARRAY_VALUE);
            Assert.fail("Missed contraint");
        } catch (ConstraintViolationException ex) {
            //OK
        }
        
        Assert.assertNull(SamlEncoderSupport.encodeByteArrayValue(ATTR, QNAME, null));
        Assert.assertNull(SamlEncoderSupport.encodeByteArrayValue(ATTR, QNAME, new byte[] {}));
        
        XMLObject obj = SamlEncoderSupport.encodeByteArrayValue(ATTR, QNAME, BYTE_ARRAY_VALUE);
        Assert.assertEquals(obj.getElementQName().getLocalPart(), QNAME_LOCALPART);
        Assert.assertTrue(obj instanceof XSString);
        XSString str = (XSString) obj;
        
        Assert.assertEquals(Base64Support.decode(str.getValue()), BYTE_ARRAY_VALUE);
    }

    @Test public void testEncodeXmlObjectValue() {
        
        final NameID objToEncode= new NameIDBuilder().buildObject();
        objToEncode.setValue(STRING_VALUE);
        
        try {
            SamlEncoderSupport.encodeXmlObjectValue(null, QNAME, objToEncode);
            Assert.fail("Missed contraint");
        } catch (ConstraintViolationException ex) {
            //OK
        }
        
        try {
            SamlEncoderSupport.encodeXmlObjectValue(ATTR, null, objToEncode);
            Assert.fail("Missed contraint");
        } catch (ConstraintViolationException ex) {
            //OK
        }
        
        Assert.assertNull(SamlEncoderSupport.encodeXmlObjectValue(ATTR, QNAME, null));
        
        XMLObject obj = SamlEncoderSupport.encodeXmlObjectValue(ATTR, QNAME, objToEncode);
        Assert.assertEquals(obj.getElementQName().getLocalPart(), QNAME_LOCALPART);
        Assert.assertTrue(obj instanceof XSAny);
        XSAny any = (XSAny) obj;
        List<XMLObject> what = any.getUnknownXMLObjects();
       
        Assert.assertEquals(what.size(),1);
        Assert.assertTrue(what.get(0) instanceof NameID);
        
        NameID other = (NameID) what.get(0);
        Assert.assertEquals(other.getValue(), STRING_VALUE);
    }

}
