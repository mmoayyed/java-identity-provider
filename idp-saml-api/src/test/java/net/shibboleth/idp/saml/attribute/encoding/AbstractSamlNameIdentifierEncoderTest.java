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

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeEncodingException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link AbstractSamlNameIdentifierEncoder}
 */
public class AbstractSamlNameIdentifierEncoderTest {
    
    private final String FORMAT = "format";
    private final String QUALIFIER = "qualfier";
    private final String PROTOCOL = "protocol";

    @Test public void testAbstractSamlNameIdentifierEncoder() {
        AbstractSamlNameIdentifierEncoder encoder = new mockEncoder();
        
        Assert.assertNull(encoder.getNameFormat());
        Assert.assertNull(encoder.getNameQualifier());
        encoder.setNameFormat(FORMAT);
        encoder.setNameQualifier(QUALIFIER);
        Assert.assertEquals(encoder.getNameFormat(), FORMAT);
        Assert.assertEquals(encoder.getNameQualifier(), QUALIFIER);
    }
    @Test public void testEqualsHash() {

        mockEncoder enc1 = new mockEncoder();
        Assert.assertEquals(enc1, enc1);
        Assert.assertNotSame(enc1, null);
        Assert.assertNotSame(enc1, this);
    
        mockEncoder enc2 = new mockEncoder();
        
        Assert.assertEquals(enc1, enc2);
        Assert.assertEquals(enc1.hashCode(), enc2.hashCode());
        enc1.setNameFormat(FORMAT);
        enc1.setNameQualifier(QUALIFIER);
        enc1.setProtocol(PROTOCOL);
        enc2.setNameFormat(FORMAT);
        enc2.setNameQualifier(QUALIFIER);
        enc2.setProtocol(PROTOCOL);
        Assert.assertEquals(enc1, enc2);
        Assert.assertEquals(enc1.hashCode(), enc2.hashCode());
        enc2.setNameFormat(FORMAT+FORMAT);
        Assert.assertNotSame(enc1,  enc2);
        Assert.assertNotSame(enc1.hashCode(),  enc2.hashCode());
        enc2.setNameFormat(FORMAT);
        enc2.setNameQualifier(QUALIFIER + QUALIFIER);
        Assert.assertNotSame(enc1,  enc2);
        Assert.assertNotSame(enc1.hashCode(),  enc2.hashCode());   
        enc2.setNameQualifier(QUALIFIER);
        enc2.setProtocol(PROTOCOL + PROTOCOL);
        Assert.assertNotSame(enc1,  enc2);
        Assert.assertNotSame(enc1.hashCode(),  enc2.hashCode());   
        
    }
    
    private class mockEncoder extends AbstractSamlNameIdentifierEncoder {

        private String theProtocol;
        protected void setProtocol(String protocol) {
            theProtocol = protocol;
        }
        
        /** {@inheritDoc} */
        public String getProtocol() {
            return theProtocol;
        }

        /** {@inheritDoc} */
        public Object encode(Attribute attribute) throws AttributeEncodingException {
            return null;
        }
        
    }
}
