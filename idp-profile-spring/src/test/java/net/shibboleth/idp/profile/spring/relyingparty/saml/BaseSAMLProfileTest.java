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

package net.shibboleth.idp.profile.spring.relyingparty.saml;

import net.shibboleth.ext.spring.config.DurationToLongConverter;
import net.shibboleth.ext.spring.config.StringToIPRangeConverter;
import net.shibboleth.idp.saml.profile.config.AbstractSAMLProfileConfiguration;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.Assert;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

public class BaseSAMLProfileTest  extends OpenSAMLInitBaseTestCase {
    
    private static final String PATH = "/net/shibboleth/idp/profile/spring/relyingparty/";
    
    protected <T extends AbstractSAMLProfileConfiguration> T getBean(Class<T> claz,  boolean validating, String... files){
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
    
    protected static void assertTruePredicate(Predicate<ProfileRequestContext> predicate) {
        Assert.assertTrue(predicate.apply(null));
    }
    
    protected static void assertFalsePredicate(Predicate<ProfileRequestContext> predicate) {
        Assert.assertFalse(predicate.apply(null));
    }
    
    protected static void assertConditionalPredicate(Predicate<ProfileRequestContext> predicate) {
        // a conditional predicate will look at the parameter and fail immediately
        try {
            predicate.apply(null);
            Assert.fail("Was not a conditional Predicate");
        } catch (ConstraintViolationException ex) {
            // expected   
        }
    }

}
