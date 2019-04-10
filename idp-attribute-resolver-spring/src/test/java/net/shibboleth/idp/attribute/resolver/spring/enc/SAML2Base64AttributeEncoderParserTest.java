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

package net.shibboleth.idp.attribute.resolver.spring.enc;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.opensaml.saml.saml2.core.Attribute;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.idp.attribute.resolver.spring.enc.impl.SAML2Base64AttributeEncoderParser;
import net.shibboleth.idp.saml.attribute.encoding.impl.SAML2ByteAttributeEncoder;

/**
 * Test for {@link SAML2Base64AttributeEncoderParser}.
 */
public class SAML2Base64AttributeEncoderParserTest extends BaseAttributeDefinitionParserTest {

    @Test public void resolver() {
        final SAML2ByteAttributeEncoder encoder =
                getAttributeEncoder("resolver/saml2Base64.xml", SAML2ByteAttributeEncoder.class);

        assertEquals(encoder.getName(), "Saml2Base64_ATTRIBUTE_NAME");
        assertEquals(encoder.getFriendlyName(),"Saml2Base64_ATTRIBUTE_FRIENDLY_NAME"); 
        assertEquals(encoder.getNameFormat(),"Saml2Base64_ATTRIBUTE_NAME_FORMAT");
    }
    
    @Test public void defaultCase() {
        final SAML2ByteAttributeEncoder encoder =
                getAttributeEncoder("resolver/saml2Base64Default.xml", SAML2ByteAttributeEncoder.class);

        assertEquals(encoder.getName(), "Base64Name");
        assertNull(encoder.getFriendlyName()); 
        assertEquals(encoder.getNameFormat(), Attribute.URI_REFERENCE);
    }
    
    @Test(expectedExceptions={BeanDefinitionStoreException.class,})  public void noName() {
        getAttributeEncoder("resolver/saml2Base64NoName.xml", SAML2ByteAttributeEncoder.class);
    }
}
