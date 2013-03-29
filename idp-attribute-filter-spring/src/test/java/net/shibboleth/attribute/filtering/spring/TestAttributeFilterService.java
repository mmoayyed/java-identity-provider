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

package net.shibboleth.attribute.filtering.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.service.ServiceException;
import net.shibboleth.idp.spring.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/** A work in progress to test the attribute resolver service. */
// TODO incomplete
public class TestAttributeFilterService {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(TestAttributeFilterService.class);

    // stub test
    @Test public void testOne() throws ComponentInitializationException, ServiceException, AttributeFilteringException {

        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + TestAttributeFilterService.class);

        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions("net/shibboleth/test/service.xml");

        AttributeFilterService service = (AttributeFilterService) context.getBean("shibboleth.AttributeFilterEngine");
        Assert.assertNotNull(service);

        // not sure
        service.initialize();
        service.start();

        // try to resolve some attributes
        AttributeFilterContext filterContext = new AttributeFilterContext();

        Attribute foo = new Attribute("foo");
        AttributeValue v1 = new StringAttributeValue("fooValue1");
        AttributeValue v2 = new StringAttributeValue("fooValue2");
        foo.setValues(Lists.newArrayList(v1,v2));
        
        Attribute transientId = new Attribute("transientId");
        AttributeValue transientIdValue1 = new StringAttributeValue("transientIdValue1");
        AttributeValue transientIdValue2 = new StringAttributeValue("transientIdValue2");
        transientId.setValues(Lists.newArrayList(transientIdValue1,transientIdValue2));
        
        Collection<Attribute> preFilteredAttributes = new ArrayList<Attribute>();
        preFilteredAttributes.add(foo);
        preFilteredAttributes.add(transientId);        
        filterContext.setPrefilteredAttributes(preFilteredAttributes);
        
        service.filterAttributes(filterContext);
        Map<String, Attribute> filteredAttributes = filterContext.getFilteredAttributes();
        log.debug("filtered attributes '{}'", filteredAttributes);
                
        // just one attribute for now
        // Assert.assertEquals(filteredAttributes.size(), 1);
        // Assert.assertNotNull(resolvedAttributes.get("eduPersonAffiliation"));
        // Assert.assertEquals(resolvedAttributes.get("eduPersonAffiliation").getValues().size(), 1);
        // Assert.assertTrue(resolvedAttributes.get("eduPersonAffiliation").getValues()
        // .contains(new StringAttributeValue("member")));
    }
}
