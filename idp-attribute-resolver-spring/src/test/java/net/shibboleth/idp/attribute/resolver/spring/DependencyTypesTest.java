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

import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverAttributeDefinitionDependency;
import net.shibboleth.idp.attribute.resolver.ResolverDataConnectorDependency;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 */
public class DependencyTypesTest extends BaseAttributeDefinitionParserTest {

    @Test public void springList() {
        final ResolverDataConnectorDependency re = getBean(BEAN_FILE_PATH + "inputDataConnector1.xml", ResolverDataConnectorDependency.class, new GenericApplicationContext());
        
        Assert.assertEquals(re.getDependencyPluginId(), "DC1");
        Assert.assertFalse(re.isAllAttributes());
        Assert.assertEquals(re.getAttributeNames().size(), 3);
        Assert.assertTrue(re.getAttributeNames().contains("1"));
        Assert.assertTrue(re.getAttributeNames().contains("2"));
        Assert.assertTrue(re.getAttributeNames().contains("3"));
    }
    
    @Test public void allAttributeDataConnector() {
        final ResolverDataConnectorDependency re = getBean(BEAN_FILE_PATH + "inputDataConnector2.xml", ResolverDataConnectorDependency.class, new GenericApplicationContext());
        
        Assert.assertEquals(re.getDependencyPluginId(), "DC2");
        Assert.assertTrue(re.isAllAttributes());
        Assert.assertTrue(re.getAttributeNames().isEmpty());
    }
    
    @Test(expectedExceptions={BeanDefinitionStoreException.class,}) public void bothAttributeDataConnector() {
        getBean(BEAN_FILE_PATH + "inputDataConnector3.xml", ResolverDataConnectorDependency.class, new GenericApplicationContext());
    }

    @Test(expectedExceptions={BeanDefinitionStoreException.class,}) public void neitherAttributeDataConnector() {
        getBean(BEAN_FILE_PATH + "inputDataConnector4.xml", ResolverDataConnectorDependency.class, new GenericApplicationContext());
    }
    
    @Test public void attributeInput() {
        final ResolverAttributeDefinitionDependency re = getBean(BEAN_FILE_PATH + "inputAttributeDefinition1.xml", ResolverAttributeDefinitionDependency.class, new GenericApplicationContext());
        
        Assert.assertEquals(re.getDependencyPluginId(), "AD1");
    }
    
    @Test(dependsOnMethods={"attributeInput", "allAttributeDataConnector"}) public void simple() {
        final AttributeDefinition attr =  getBean(BEAN_FILE_PATH + "simpleDependencies.xml", AttributeDefinition.class, new GenericApplicationContext());
        
        Assert.assertEquals(attr.getDependencies().size(), 3);
    }

}
