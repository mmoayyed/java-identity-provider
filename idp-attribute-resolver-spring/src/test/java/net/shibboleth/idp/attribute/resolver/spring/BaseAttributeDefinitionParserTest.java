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

import net.shibboleth.idp.attribute.AttributeEncoder;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.spring.ad.BaseAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.SimpleAttributeDefinitionParser;
import net.shibboleth.idp.saml.impl.attribute.principalconnector.PrincipalConnector;
import net.shibboleth.idp.spring.SchemaTypeAwareXMLBeanDefinitionReader;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;

/**
 * Test for {@link SimpleAttributeDefinitionParser} and by extension {@link BaseAttributeDefinitionParser}.
 */
public abstract class BaseAttributeDefinitionParserTest extends OpenSAMLInitBaseTestCase {
    
    public static final String BEAN_FILE_PATH = "net/shibboleth/idp/attribute/resolver/spring/";

    public static final String ATTRIBUTE_FILE_PATH = BEAN_FILE_PATH + "ad/";

    public static final String DATACONNECTOR_FILE_PATH = BEAN_FILE_PATH + "dc/";

    public static final String ENCODER_FILE_PATH = BEAN_FILE_PATH + "enc/";

    public static final String PRINCIPALCONNECTOR_FILE_PATH = BEAN_FILE_PATH + "pc/";

    protected <Type> Type getBean(String fileName, Class<Type> claz, GenericApplicationContext context,
            boolean supressValid) {

        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        if (supressValid) {
            beanDefinitionReader.setValidating(false);
        }
        beanDefinitionReader.loadBeanDefinitions(fileName);

        context.refresh();
        
        Collection<Type> beans = context.getBeansOfType(claz).values();
        Assert.assertEquals(beans.size(), 1);

        return beans.iterator().next();
    }

    protected <Type> Type getBean(String fileName, Class<Type> claz, GenericApplicationContext context) {
        return getBean(fileName, claz, context, false);
    }

    protected <Type extends AttributeDefinition> Type getAttributeDefn(String fileName, Class<Type> claz,
            GenericApplicationContext context) {

        return getBean(ATTRIBUTE_FILE_PATH + fileName, claz, context);
    }

    protected <Type extends AttributeDefinition> Type getAttributeDefn(String fileName, Class<Type> claz,
            GenericApplicationContext context, boolean supressValidation) {

        return getBean(ATTRIBUTE_FILE_PATH + fileName, claz, context, supressValidation);
    }

    protected <Type extends AttributeDefinition> Type getAttributeDefn(String fileName, String beanFileName,
            Class<Type> claz) {
        return getAttributeDefn(fileName, beanFileName, claz, false);

    }

    protected <Type extends AttributeDefinition> Type getAttributeDefn(String fileName, String beanFileName,
                Class<Type> claz, boolean supressValidation) {

        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + claz);
        XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(context);

        configReader.loadBeanDefinitions(BEAN_FILE_PATH + beanFileName);

        return getAttributeDefn(fileName, claz, context, supressValidation);
    }

    protected <Type extends AttributeDefinition> Type getAttributeDefn(String fileName, Class<Type> claz) {
        return getAttributeDefn(fileName, claz, false);
        
    }
        protected <Type extends AttributeDefinition> Type getAttributeDefn(String fileName, Class<Type> claz, boolean supressValid) {

        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + claz);

        return getAttributeDefn(fileName, claz, context, supressValid);
    }

    protected <Type extends DataConnector> Type getDataConnector(String fileName, Class<Type> claz) {
        return getDataConnector(fileName, claz, false);
    }

    protected <Type extends DataConnector> Type getDataConnector(String fileName, Class<Type> claz, boolean supressValid) {

        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + claz);

        return getBean(DATACONNECTOR_FILE_PATH + fileName, claz, context, supressValid);
    }

    protected <Type extends AttributeEncoder> Type getAttributeEncoder(String fileName, Class<Type> claz) {

        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + claz);

        return getBean(ENCODER_FILE_PATH + fileName, claz, context);

    }
    
    protected PrincipalConnector getPrincipalConnector(String fileName) {

        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + PrincipalConnector.class);

        return getBean(PRINCIPALCONNECTOR_FILE_PATH + fileName, PrincipalConnector.class, context);
    }
    
    protected PrincipalConnector getPrincipalConnector(String fileName, String beanFileName) {

        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + PrincipalConnector.class);
        XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(context);

        configReader.loadBeanDefinitions(BEAN_FILE_PATH + beanFileName);

        return getBean(PRINCIPALCONNECTOR_FILE_PATH + fileName, PrincipalConnector.class, context);
    }


}
