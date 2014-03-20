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

package net.shibboleth.idp.profile.spring.relyingparty.metadata;

import net.shibboleth.ext.spring.config.DurationToLongConverter;
import net.shibboleth.ext.spring.config.StringToIPRangeConverter;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.google.common.collect.Sets;

/**
 * Base class for testing metadata providers.
 */
public class AbstractMetadataParserTest extends OpenSAMLInitBaseTestCase {
    
    private static final String PATH = "/net/shibboleth/idp/profile/spring/relyingparty/metadata/";
    
    protected static final String SP_ID = "https://sp.example.org/sp/shibboleth"; 
    protected static final String IDP_ID = "https://idp.example.org/idp/shibboleth"; 
    
    static protected <T extends MetadataResolver> T getBean(Class<T> claz,  boolean validating, String... files){
        final Resource[] resources = new Resource[files.length];
       
        for (int i = 0; i < files.length; i++) {
            resources[i] = new ClassPathResource(PATH + files[i]);
        }
        
        final GenericApplicationContext context = new GenericApplicationContext();
        ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        context.setDisplayName("ApplicationContext: " + claz);
        service.setConverters(Sets.newHashSet(new DurationToLongConverter(), new StringToIPRangeConverter()));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());

        final XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(context);

        configReader.setValidating(validating);
        
        configReader.loadBeanDefinitions(resources);
        context.refresh();

        return context.getBean(claz);
    }
    
    static protected CriteriaSet criteriaFor(String entityId) {
        EntityIdCriterion criterion = new EntityIdCriterion(entityId);
        return  new CriteriaSet(criterion);
    }
 }
