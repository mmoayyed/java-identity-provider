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

package net.shibboleth.idp.ui.saml;

import java.util.Arrays;

import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.idp.ui.context.SAMLRelyingPartyUIInformation;
import net.shibboleth.utilities.java.support.codec.HTMLEncoder;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SAMLRelyingPartyUIInformationTest extends AbstractUIComponentTest {

    @Test public void spring() throws ResolverException {
        GenericApplicationContext context = new GenericApplicationContext();
        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions("net/shibboleth/idp/ui/beans.xml");
        
        context.refresh();
        
        final SAMLRelyingPartyUIInformation rpui = context.getBean(SAMLRelyingPartyUIInformation.class);
        
        final EntityDescriptor entity = get("https://sp.example.org");
        
        rpui.setBrowserLanguages(Arrays.asList("fr", "de"));
        rpui.setRelyingParty(entity);
        
        RelyingPartyUIContext ctx = new RelyingPartyUIContext();
        rpui.populateContext(ctx);
        
        Assert.assertEquals(ctx.getServiceDescription(), HTMLEncoder.encodeForHTML("TEST SP (en francais dans la texte)"));
        Assert.assertEquals(ctx.getServiceName(), HTMLEncoder.encodeForHTML("sp.example.org"));
        
    }
}
