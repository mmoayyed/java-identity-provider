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

import java.util.Map;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.service.ServiceException;
import net.shibboleth.idp.spring.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** A work in progress to test the attribute resolver service. */
// TODO incomplete
public class TestAttributeResolverService {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(TestAttributeResolverService.class);

    // stub test
    @Test public void testOne() throws ComponentInitializationException, ServiceException, ResolutionException {

        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + TestAttributeResolverService.class);

        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions("net/shibboleth/test/service.xml");

        AttributeResolverService attributeResolverService =
                (AttributeResolverService) context.getBean("shibboleth.AttributeResolver");
        Assert.assertNotNull(attributeResolverService);

        // not sure
        attributeResolverService.initialize();
        attributeResolverService.start();

        // try to resolve some attributes
        AttributeResolutionContext resolutionContext = new AttributeResolutionContext();
        attributeResolverService.resolveAttributes(resolutionContext);
        Map<String, Attribute> resolvedAttributes = resolutionContext.getResolvedAttributes();
        log.debug("resolved attributes '{}'", resolvedAttributes);

        // just one attribute for now
        Assert.assertEquals(resolvedAttributes.size(), 1);
        Assert.assertNotNull(resolvedAttributes.get("eduPersonAffiliation"));
        Assert.assertEquals(resolvedAttributes.get("eduPersonAffiliation").getValues().size(), 1);
        Assert.assertTrue(resolvedAttributes.get("eduPersonAffiliation").getValues()
                .contains(new StringAttributeValue("member")));
    }
}
