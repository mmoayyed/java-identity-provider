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

package net.shibboleth.idp.attribute.resolver.spring.ad.mapped;

import net.shibboleth.idp.attribute.resolver.impl.ad.mapped.ValueMap;
import net.shibboleth.idp.attribute.resolver.spring.ad.BaseTestAttributeDefinitionBeanParser;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for {@link ValueMapBeanDefinitionParser}.
 */
public class TestValueMapBeanDefinitionParser extends BaseTestAttributeDefinitionBeanParser {

    private ValueMap getValueMap(String fileName) {

        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + TestValueMapBeanDefinitionParser.class);

        return getBean("mapped/" + fileName, ValueMap.class, context);
    }

    @Test public void testValueMap() {
        
        ValueMap value = getValueMap("valueMap.xml");
        Assert.assertEquals(value.getReturnValue(), "return");
        Assert.assertEquals(value.getSourceValues().size(), 1);
        Assert.assertEquals(value.getSourceValues().iterator().next().getValue(), "source");
    }
    
    @Test public void testNoSourceValues() {
        
        try {
            getValueMap("valueMapNoSourceValue.xml");
            Assert.fail();
        } catch (BeanDefinitionStoreException e) {
            // OK
        }
    }
    
    @Test public void testNoValues() {
        
        try {
            getValueMap("valueMapNoValues.xml");
            Assert.fail();
        } catch (BeanDefinitionStoreException e) {
            // OK
        }
    }
 }
