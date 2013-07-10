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

package net.shibboleth.idp.attribute.filter.spring;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.spring.AttributeFilterService;
import net.shibboleth.idp.service.ServiceException;
import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.resource.ClasspathResource;
import net.shibboleth.utilities.java.support.resource.Resource;

import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** A work in progress to test the attribute resolver service. */
// TODO incomplete
public class AttributeFilterServiceTest {

    /** The service configuration file. */
    private final static String SERVICE_CONFIG_FILE = "net/shibboleth/idp/attribute/filter/spring/service.xml";

    /** The attributes to be filtered. */
    private Map<String, Attribute> attributesToBeFiltered;

    /**
     * Instantiate a new service.
     * 
     * @param name service bean name
     * @param resources configuration resources
     * @return the service
     * @throws ServiceException if an error occurs loading the service
     */
    public static AttributeFilterService newService(String name, Resource... resources) throws ServiceException {
        GenericApplicationContext context = SpringSupport.newContext(name, Arrays.asList(resources), null);
        AttributeFilterService service = context.getBean(name, AttributeFilterService.class);
        service.start();
        return service;
    }

    @BeforeClass protected void setUp() throws Exception {

        attributesToBeFiltered = new HashMap<String, Attribute>();

        Attribute firstName = new Attribute("firstName");
        firstName.getValues().add(new StringAttributeValue("john"));
        attributesToBeFiltered.put(firstName.getId(), firstName);

        Attribute lastName = new Attribute("lastName");
        lastName.getValues().add(new StringAttributeValue("smith"));
        attributesToBeFiltered.put(lastName.getId(), lastName);

        Attribute email = new Attribute("email");
        email.getValues().add(new StringAttributeValue("jsmith@example.edu"));
        email.getValues().add(new StringAttributeValue("john.smith@example.edu"));
        attributesToBeFiltered.put(email.getId(), email);

        Attribute affiliation = new Attribute("affiliation");
        affiliation.getValues().add(new StringAttributeValue("employee"));
        affiliation.getValues().add(new StringAttributeValue("staff"));
        affiliation.getValues().add(new StringAttributeValue("illegalValue"));
        attributesToBeFiltered.put(affiliation.getId(), affiliation);
    }

    @Test public void testPolicy1() {
        // TODO
    }
// TODO
    @Test public void testPolicy2() throws ServiceException, AttributeFilterException {

        AttributeFilterService service =
                AttributeFilterServiceTest.newService("policy2", new ClasspathResource(SERVICE_CONFIG_FILE));

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredAttributes(attributesToBeFiltered.values());

        service.filterAttributes(filterContext);

        Map<String, Attribute> filteredAttributes = filterContext.getFilteredAttributes();

        Assert.assertEquals(1, filteredAttributes.size());

        Assert.assertNull(filteredAttributes.get("firstName"));

        Assert.assertNull(filteredAttributes.get("lastName"));

        Assert.assertNull(filteredAttributes.get("email"));

        Assert.assertEquals(2, filteredAttributes.get("affiliation").getValues().size(), 2);

        Assert.assertTrue(filteredAttributes.get("affiliation").getValues()
                .contains(new StringAttributeValue("employee")));

        Assert.assertTrue(filteredAttributes.get("affiliation").getValues().contains(new StringAttributeValue("staff")));

    }

    @Test public void testPolicy3() throws ServiceException, AttributeFilterException {

        AttributeFilterService service =
                AttributeFilterServiceTest.newService("policy3", new ClasspathResource(SERVICE_CONFIG_FILE));

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredAttributes(attributesToBeFiltered.values());
        service.filterAttributes(filterContext);

        Map<String, Attribute> filteredAttributes = filterContext.getFilteredAttributes();

        Assert.assertEquals(filteredAttributes.size(), 1);

        Assert.assertNull(filteredAttributes.get("firstName"));

        Assert.assertNull(filteredAttributes.get("lastName"));

        Assert.assertEquals(filteredAttributes.get("email").getValues().size(), 2);

        Assert.assertTrue(filteredAttributes.get("email").getValues()
                .contains(new StringAttributeValue("jsmith@example.edu")));

        Assert.assertTrue(filteredAttributes.get("email").getValues()
                .contains(new StringAttributeValue("john.smith@example.edu")));

        Assert.assertNull(filteredAttributes.get("affiliation"));
    }
    
    @Test public void testPolicy4() throws ServiceException, AttributeFilterException {


        AttributeFilterService service =
                AttributeFilterServiceTest.newService("policy4", new ClasspathResource(SERVICE_CONFIG_FILE));

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredAttributes(attributesToBeFiltered.values());

        service.filterAttributes(filterContext);

        Map<String, Attribute> filteredAttributes = filterContext.getFilteredAttributes();

        Assert.assertEquals(1, filteredAttributes.size());

        Assert.assertNull(filteredAttributes.get("firstName"));

        Assert.assertNull(filteredAttributes.get("lastName"));

        Assert.assertNull(filteredAttributes.get("email"));

        Assert.assertEquals(2, filteredAttributes.get("affiliation").getValues().size(), 2);

        Assert.assertTrue(filteredAttributes.get("affiliation").getValues()
                .contains(new StringAttributeValue("employee")));

        Assert.assertTrue(filteredAttributes.get("affiliation").getValues().contains(new StringAttributeValue("staff")));

    }

}
