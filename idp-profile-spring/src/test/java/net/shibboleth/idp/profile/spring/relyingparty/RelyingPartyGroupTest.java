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

package net.shibboleth.idp.profile.spring.relyingparty;

import java.util.List;

import net.shibboleth.ext.spring.config.DurationToLongConverter;
import net.shibboleth.ext.spring.config.StringToIPRangeConverter;
import net.shibboleth.idp.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.relyingparty.RelyingPartyConfigurationResolver;
import net.shibboleth.idp.relyingparty.impl.DefaultRelyingPartyConfigurationResolver;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Sets;

/**
 * Test for a complete example RelyingParty file
 */
public class RelyingPartyGroupTest extends OpenSAMLInitBaseTestCase {
    
    private static final String PATH = "/net/shibboleth/idp/profile/spring/relyingparty/";
    
    @Test public void relyingParties() {
        final Resource[] resources = new Resource[2];
        
            resources[0] = new ClassPathResource(PATH + "beans.xml");
            resources[1] = new ClassPathResource(PATH + "relying-party.xml");
        
        final GenericApplicationContext context = new GenericApplicationContext();
        ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        context.setDisplayName("ApplicationContext: " );
        service.setConverters(Sets.newHashSet(new DurationToLongConverter(), new StringToIPRangeConverter()));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());

        final XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(context);

        configReader.setValidating(true);
        
        configReader.loadBeanDefinitions(resources);
        context.refresh();

        DefaultRelyingPartyConfigurationResolver resolver =  (DefaultRelyingPartyConfigurationResolver ) context.getBean(RelyingPartyConfigurationResolver.class);
        
        List<RelyingPartyConfiguration> configs = resolver.getRelyingPartyConfigurations();
        Assert.assertEquals(configs.size(), 2);
           // TODO test that the order is correct    
    }

    
}
