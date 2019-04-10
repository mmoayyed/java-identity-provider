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
import net.shibboleth.idp.attribute.resolver.spring.enc.impl.SAML2ScopedStringAttributeEncoderParser;
import net.shibboleth.idp.saml.attribute.encoding.impl.SAML2ScopedStringAttributeEncoder;

/**
 * Test for {@link SAML2ScopedStringAttributeEncoderParser}.
 */
public class SAML2ScopedStringAttributeEncoderParserTest extends BaseAttributeDefinitionParserTest {

    @Test public void resolver() {
        final SAML2ScopedStringAttributeEncoder encoder =
                getAttributeEncoder("resolver/saml2Scoped.xml", SAML2ScopedStringAttributeEncoder.class);

        assertEquals(encoder.getName(), "ATTRIBUTE_NAME");
        assertEquals(encoder.getFriendlyName(),"ATTRIBUTE_FRIENDLY_NAME"); 
        assertEquals(encoder.getNameFormat(),"ATTRIBUTE_NAME_FORMAT");
        assertEquals(encoder.getScopeType(),"attribute");
        assertEquals(encoder.getScopeAttributeName(),"scopeAttrib");
        assertEquals(encoder.getScopeDelimiter(),"###");
    }
    
    @Test public void defaultCase() {
        final SAML2ScopedStringAttributeEncoder encoder =
                getAttributeEncoder("resolver/saml2ScopedDefault.xml", SAML2ScopedStringAttributeEncoder.class);

        assertEquals(encoder.getName(), "name");
        assertNull(encoder.getFriendlyName()); 
        assertEquals(encoder.getNameFormat(), Attribute.URI_REFERENCE);
        assertEquals(encoder.getScopeType(),"inline");
        assertEquals(encoder.getScopeDelimiter(),"@");
        assertEquals(encoder.getScopeAttributeName(),"Scope");
    }
    
    @Test(expectedExceptions={BeanDefinitionStoreException.class,})  public void noName() {
        getAttributeEncoder("resolver/saml2ScopedNoName.xml", SAML2ScopedStringAttributeEncoder.class);
    }
}
