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
import net.shibboleth.idp.saml.impl.attribute.encoding.Saml2ScopedStringAttributeEncoder;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for {@link Saml2ScopedStringAttributeEncoderParser}.
 */
public class Saml2ScopedStringAttributeEncoderParserTest extends BaseAttributeDefinitionParserTest {

    @Test public void specified() {
        Saml2ScopedStringAttributeEncoder encoder =
                getAttributeEncoder("saml2Scoped.xml", Saml2ScopedStringAttributeEncoder.class);

        Assert.assertEquals(encoder.getName(), "ATTRIBUTE_NAME");
        Assert.assertEquals(encoder.getFriendlyName(),"ATTRIBUTE_FRIENDLY_NAME"); 
        Assert.assertEquals(encoder.getNamespace(),"ATTRIBUTE_NAME_FORMAT");
        Assert.assertEquals(encoder.getScopeType(),"attribute");
        Assert.assertEquals(encoder.getScopeAttributeName(),"scopeAttrib");
        Assert.assertEquals(encoder.getScopeDelimiter(),"###");
    }
    
    @Test public void defaultCase() {
        Saml2ScopedStringAttributeEncoder encoder =
                getAttributeEncoder("saml2ScopedDefault.xml", Saml2ScopedStringAttributeEncoder.class);

        Assert.assertEquals(encoder.getName(), "name");
        Assert.assertNull(encoder.getFriendlyName()); 
        Assert.assertEquals(encoder.getNamespace(),"urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
        Assert.assertEquals(encoder.getScopeType(),"inline");
        Assert.assertEquals(encoder.getScopeDelimiter(),"@");
        Assert.assertEquals(encoder.getScopeAttributeName(),"Scope");
    }
    
    @Test(expectedExceptions={BeanDefinitionStoreException.class,})  public void noName() {
        getAttributeEncoder("saml2ScopedNoName.xml", Saml2ScopedStringAttributeEncoder.class);
    }
}