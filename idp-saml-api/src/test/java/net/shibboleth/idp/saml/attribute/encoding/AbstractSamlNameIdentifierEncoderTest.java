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

    @Test public void testAbstractSamlNameIdentifierEncoder() {
        AbstractSamlNameIdentifierEncoder encoder = new mockEncoder();
        
        Assert.assertNull(encoder.getNameFormat());
        Assert.assertNull(encoder.getNameQualifier());
        encoder.setNameFormat(FORMAT);
        encoder.setNameQualifier(QUALIFIER);
        Assert.assertEquals(encoder.getNameFormat(), FORMAT);
        Assert.assertEquals(encoder.getNameQualifier(), QUALIFIER);
    }
    
    private class mockEncoder extends AbstractSamlNameIdentifierEncoder {

        /** {@inheritDoc} */
        public String getProtocol() {
            return null;
        }

        /** {@inheritDoc} */
        public Object encode(Attribute attribute) throws AttributeEncodingException {
            return null;
        }
        
    }
}
