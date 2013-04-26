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

import java.util.Collection;

import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.ad.BaseAttributeDefinitionBeanDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.SimpleAttributeDefinitionBeanDefinitionParser;
import net.shibboleth.idp.spring.SchemaTypeAwareXMLBeanDefinitionReader;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;

/**
 * Test for {@link SimpleAttributeDefinitionBeanDefinitionParser} and by extension
 * {@link BaseAttributeDefinitionBeanDefinitionParser}.
 */
public abstract class BaseTestAttributeDefinitionBeanParser extends OpenSAMLInitBaseTestCase  {
    
    public static final String ATTRIBUTE_FILE_PATH = "net/shibboleth/idp/attribute/resolver/spring/ad/"; 
    public static final String DATACONNECTOR_FILE_PATH = "net/shibboleth/idp/attribute/resolver/spring/dc/"; 

    protected <Type> Type getBean(String fileName, Class<Type> claz, GenericApplicationContext context) {

        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions(fileName);

        Collection<Type> beans = context.getBeansOfType(claz).values();
        Assert.assertEquals(beans.size(), 1);

        return (Type) beans.iterator().next();
    }

    
    protected <Type extends BaseAttributeDefinition> Type getAttributeDefn(String fileName, Class<Type> claz, GenericApplicationContext context) {

        return getBean(ATTRIBUTE_FILE_PATH + fileName, claz, context);
    }
    
    protected <Type extends BaseAttributeDefinition> Type getAttributeDefn(String fileName, String beanFileName, Class<Type> claz) {

    
        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + claz);
        XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(context);
        
        configReader.loadBeanDefinitions(ATTRIBUTE_FILE_PATH + beanFileName);
    
        return getAttributeDefn(fileName, claz, context);
    }

    
    protected <Type extends BaseAttributeDefinition> Type getAttributeDefn(String fileName, Class<Type> claz) {

        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + claz);

        return getAttributeDefn(fileName, claz, context);
    }
    
    protected <Type extends BaseDataConnector> Type getDataConnector(String fileName, Class<Type> claz) {
        
        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + claz);
        
        return getBean(DATACONNECTOR_FILE_PATH + fileName, claz, context);

    }

}
