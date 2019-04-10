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

package net.shibboleth.idp.attribute.resolver.spring.ad;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.SAML1NameIdentifierAttributeDefinitionParser;
import net.shibboleth.idp.saml.attribute.resolver.impl.SAML1NameIdentifierAttributeDefinition;

/**
 * Test for {@link SAML1NameIdentifierAttributeDefinitionParser}.
 */
public class SAML1NameIdentifierAttributeDefinitionParserTest extends BaseAttributeDefinitionParserTest {

    @Test public void defaultCase() {
        SAML1NameIdentifierAttributeDefinition attrDef =
                getAttributeDefn("resolver/saml1NameIdDefault.xml", SAML1NameIdentifierAttributeDefinition.class);

        assertEquals(attrDef.getId(), "SAML1NameIdentifier");
        assertEquals(attrDef.getNameIdFormat(), "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
        assertNull(attrDef.getNameIdQualifier());
    }

    @Test public void attributes() {
        SAML1NameIdentifierAttributeDefinition attrDef = getAttributeDefn("resolver/saml1NameIdentifierAttributes.xml",
                SAML1NameIdentifierAttributeDefinition.class);

        assertEquals(attrDef.getId(), "SAML1NameIdentifierAttributes");
        assertEquals(attrDef.getNameIdFormat(), "format");
        assertEquals(attrDef.getNameIdQualifier(), "qualifier");
    }

}
