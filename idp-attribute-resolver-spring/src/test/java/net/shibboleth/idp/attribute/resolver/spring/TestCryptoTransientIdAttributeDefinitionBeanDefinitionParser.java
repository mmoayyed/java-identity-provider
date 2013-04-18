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

package net.shibboleth.idp.attribute.resolver.spring;

import net.shibboleth.idp.attribute.resolver.impl.ad.CryptoTransientIdAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.impl.ad.DataSealer;
import net.shibboleth.idp.attribute.resolver.spring.ad.CryptoTransientIdAttributeDefinitionBeanDefinitionParser;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * test for {@link CryptoTransientIdAttributeDefinitionBeanDefinitionParser}
 */
public class TestCryptoTransientIdAttributeDefinitionBeanDefinitionParser extends BaseTestAttributeDefinitionBeanParser {

    private CryptoTransientIdAttributeDefinition getDefinition(String fileName) {

        return getAttributeDefn(fileName, "sealer.xml", CryptoTransientIdAttributeDefinition.class);
    }
    
    @Test
    public void testWithTime() throws ComponentInitializationException {

        CryptoTransientIdAttributeDefinition defn = getDefinition("cryptoWithTime.xml");
        
        DataSealer sealer = defn.getDataSealer();
        Assert.assertEquals(sealer.getMacKeyAlias(), "secret");
        defn.initialize();
        
        Assert.assertEquals(defn.getIdLifetime(), 3 * 60 * 1000);
    }
    
    
    @Test
    public void testNoTime() throws ComponentInitializationException {

        CryptoTransientIdAttributeDefinition defn = getDefinition("cryptoNoTime.xml");
        defn.initialize();
        
        DataSealer sealer = defn.getDataSealer();
        Assert.assertEquals(sealer.getMacKeyAlias(), "secret");
        
        Assert.assertEquals(defn.getIdLifetime(), 4 * 3600 * 1000);
    }

}
