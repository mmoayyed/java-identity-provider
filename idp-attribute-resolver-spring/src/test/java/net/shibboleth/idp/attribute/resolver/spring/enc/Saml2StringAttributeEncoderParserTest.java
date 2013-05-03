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
import net.shibboleth.idp.saml.impl.attribute.encoding.Saml2StringAttributeEncoder;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for {@link Saml2StringAttributeEncoderParserTest}.
 */
public class Saml2StringAttributeEncoderParserTest extends BaseAttributeDefinitionParserTest {

    @Test public void specified() {
        Saml2StringAttributeEncoder encoder =
                getAttributeEncoder("saml2String.xml", Saml2StringAttributeEncoder.class);

        Assert.assertEquals(encoder.getName(), "Saml2String_ATTRIBUTE_NAME");
        Assert.assertEquals(encoder.getFriendlyName(),"Saml2String_ATTRIBUTE_FRIENDLY_NAME"); 
        Assert.assertEquals(encoder.getNamespace(),"Saml2String_ATTRIBUTE_NAME_FORMAT");
    }
    
    @Test public void defaultCase() {
        Saml2StringAttributeEncoder encoder =
                getAttributeEncoder("saml2StringDefault.xml", Saml2StringAttributeEncoder.class);

        Assert.assertEquals(encoder.getName(), "Saml2StringName");
        Assert.assertNull(encoder.getFriendlyName()); 
        Assert.assertEquals(encoder.getNamespace(),"urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
    }
    
    @Test(expectedExceptions={BeanDefinitionStoreException.class,})  public void noName() {
        getAttributeEncoder("saml2StringNoName.xml", Saml2StringAttributeEncoder.class);
    }
}