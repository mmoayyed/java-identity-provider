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

import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.idp.saml.impl.attribute.encoding.Saml2StringSubjectNameIDEncoder;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for {@link Saml2StringNameIDAttributeEncoderParser}.
 */
public class Saml2StringNameIDAttributeEncoderParserTest extends BaseAttributeDefinitionParserTest {

    @Test public void specified() {
        Saml2StringSubjectNameIDEncoder encoder =
                getAttributeEncoder("saml2StringNameID.xml",  Saml2StringSubjectNameIDEncoder.class);

        Assert.assertEquals(encoder.getNameFormat(), "S2_NAMEID_FORMAT");
        Assert.assertEquals(encoder.getNameQualifier(),"S2_NAMEID_QUALIFIER");
    }
    
    @Test public void defaultCase() {
        Saml2StringSubjectNameIDEncoder encoder =
                getAttributeEncoder("saml2StringNameIDDefault.xml", Saml2StringSubjectNameIDEncoder.class);

        Assert.assertEquals(encoder.getNameFormat(), "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
        Assert.assertNull(encoder.getNameQualifier());;
    }
    
}