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

import org.opensaml.saml.saml1.core.NameIdentifier;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * test for {@link AbstractSaml1NameIdentifierEncoder}
 */
public class AbstractSaml1NameIdentifierEncoderTest {

    @Test public void testAbstractSaml1NameIdentifierEncoder() {
        
        AbstractSaml1NameIdentifierEncoder encoder = new AbstractSaml1NameIdentifierEncoder() {
            public NameIdentifier encode(Attribute attribute) throws AttributeEncodingException {
                return null;
            }
        };
        // Again, use constants
        Assert.assertEquals(encoder.getProtocol(), "urn:oasis:names:tc:SAML:1.1:protocol");
    }
}
