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

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.idp.attribute.resolver.spring.enc.impl.SAML1ScopedStringAttributeEncoderParser;
import net.shibboleth.idp.saml.attribute.encoding.impl.SAML1ScopedStringAttributeEncoder;
import net.shibboleth.idp.saml.xml.SAMLConstants;

/**
 * Test for {@link SAML1ScopedStringAttributeEncoderParser}.
 */
public class SAML1ScopedStringAttributeEncoderParserTest extends BaseAttributeDefinitionParserTest {

    @Test public void resolver() {
        final SAML1ScopedStringAttributeEncoder encoder =
                getAttributeEncoder("resolver/saml1Scoped.xml", SAML1ScopedStringAttributeEncoder.class);

        assertEquals(encoder.getName(), "SAML1_SCOPED_ATTRIBUTE_NAME");
        assertEquals(encoder.getNamespace(),"SAML1_SCOPED_ATTRIBUTE_NAME_FORMAT");
        assertEquals(encoder.getScopeType(),"attribute");
        assertEquals(encoder.getScopeAttributeName(),"saml1ScopeAttrib");
        assertEquals(encoder.getScopeDelimiter(),"#@#");
    }

    
    @Test public void defaultCase() {
        final SAML1ScopedStringAttributeEncoder encoder =
                getAttributeEncoder("resolver/saml1ScopedDefault.xml", SAML1ScopedStringAttributeEncoder.class);

        assertEquals(encoder.getName(), "saml1_scoped_name");
        assertEquals(encoder.getNamespace(), SAMLConstants.SAML1_ATTR_NAMESPACE_URI);
        assertEquals(encoder.getScopeType(),"attribute");
        assertEquals(encoder.getScopeDelimiter(),"@");
        assertEquals(encoder.getScopeAttributeName(),"Scope");
    }
    
    @Test(expectedExceptions={BeanDefinitionStoreException.class,})  public void noName() {
        getAttributeEncoder("resolver/saml1ScopedNoName.xml", SAML1ScopedStringAttributeEncoder.class);
    }
}
