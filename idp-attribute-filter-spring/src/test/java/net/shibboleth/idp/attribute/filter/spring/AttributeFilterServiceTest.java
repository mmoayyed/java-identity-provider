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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.ext.spring.config.IdentifiableBeanPostProcessor;
import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilter;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.spring.impl.AttributeFilterServiceStrategy;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.service.ServiceException;

/** Test the attribute resolver service. */
@SuppressWarnings("javadoc")
public class AttributeFilterServiceTest {

    /** The attributes to be filtered. */
    private Map<String, IdPAttribute> attributesToBeFiltered;

    /** The service configuration dir. */
    private final static String SERVICE_CONFIG_DIR = "net/shibboleth/idp/attribute/filter/spring/";

    private GenericApplicationContext testContext = null;

    @AfterMethod public void tearDownTestContext() {
        if (null == testContext) {
            return;
        }
        testContext.close();
        testContext = null;
    }

    /**
     * Instantiate a new service.
     * 
     * @param name service bean name
     * @param nativeSpring whether native syntax is used
     * 
     * @return the service
     * 
     * @throws ServiceException if an error occurs loading the service
     * @throws ComponentInitializationException ...
     */
    private AttributeFilter getFilter(String name) throws ServiceException,
            ComponentInitializationException {
        final Resource resource = new ClassPathResource(SERVICE_CONFIG_DIR + name);
        if (null != testContext) {
            tearDownTestContext();
        }
        
        final GenericApplicationContext context =
                new ApplicationContextBuilder().setName(name)
                    .setServiceConfiguration(resource)
                    .setBeanPostProcessor(new IdentifiableBeanPostProcessor())
                    .build();

        testContext = context;
        final AttributeFilterServiceStrategy strategy = new AttributeFilterServiceStrategy();
        strategy.setId("ID");
        strategy.initialize();
        return (AttributeFilter) strategy.apply(context);
    }

    @BeforeClass protected void setUp() throws Exception {

        attributesToBeFiltered = new HashMap<>();

        IdPAttribute firstName = new IdPAttribute("firstName");
        firstName.setValues(Collections.singleton(new StringAttributeValue("john")));
        attributesToBeFiltered.put(firstName.getId(), firstName);

        IdPAttribute lastName = new IdPAttribute("lastName");
        lastName.setValues(Collections.singleton(new StringAttributeValue("smith")));
        attributesToBeFiltered.put(lastName.getId(), lastName);

        IdPAttribute email = new IdPAttribute("email");
        email.setValues(Arrays.asList(new StringAttributeValue("jsmith@example.edu"), new StringAttributeValue(
                "john.smith@example.edu")));
        attributesToBeFiltered.put(email.getId(), email);

        IdPAttribute affiliation = new IdPAttribute("affiliation");
        affiliation.setValues(Arrays.asList(new StringAttributeValue("employee"), new StringAttributeValue("staff"),
                new StringAttributeValue("illegalValue")));

        attributesToBeFiltered.put(affiliation.getId(), affiliation);
    }

    @Test public void testPolicy2() throws ServiceException, AttributeFilterException, ComponentInitializationException {
        final AttributeFilter filter = getFilter("policy2.xml");

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(attributesToBeFiltered.values());

        filter.filterAttributes(filterContext);

        Map<String, IdPAttribute> filteredAttributes = filterContext.getFilteredIdPAttributes();

        assertEquals(1, filteredAttributes.size());

        assertNull(filteredAttributes.get("firstName"));

        assertNull(filteredAttributes.get("lastName"));

        assertNull(filteredAttributes.get("email"));

        assertEquals(2, filteredAttributes.get("affiliation").getValues().size(), 2);

        assertTrue(filteredAttributes.get("affiliation").getValues()
                .contains(new StringAttributeValue("employee")));

        assertTrue(filteredAttributes.get("affiliation").getValues().contains(new StringAttributeValue("staff")));

    }

    @Test public void testPolicy3() throws ServiceException, AttributeFilterException, ComponentInitializationException {
        final AttributeFilter filter = getFilter("policy3.xml");

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(attributesToBeFiltered.values());
        filter.filterAttributes(filterContext);

        Map<String, IdPAttribute> filteredAttributes = filterContext.getFilteredIdPAttributes();

        assertEquals(filteredAttributes.size(), 1);

        assertNull(filteredAttributes.get("firstName"));

        assertNull(filteredAttributes.get("lastName"));

        assertEquals(filteredAttributes.get("email").getValues().size(), 2);

        assertTrue(filteredAttributes.get("email").getValues()
                .contains(new StringAttributeValue("jsmith@example.edu")));

        assertTrue(filteredAttributes.get("email").getValues()
                .contains(new StringAttributeValue("john.smith@example.edu")));

        assertNull(filteredAttributes.get("affiliation"));
    }

    @Test public void testPolicy4() throws ServiceException, AttributeFilterException, ComponentInitializationException {

        common45("policy4.xml");
    }

    @Test public void testPolicy5() throws ServiceException, AttributeFilterException, ComponentInitializationException {

        common45("policy5.xml");
    }

    @Test public void testAll() throws ServiceException, AttributeFilterException, ComponentInitializationException {
        final AttributeFilter filter = getFilter("policyAll.xml");

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(attributesToBeFiltered.values());
        filter.filterAttributes(filterContext);

        Map<String, IdPAttribute> filteredAttributes = filterContext.getFilteredIdPAttributes();

        assertEquals(filteredAttributes.size(), 1);

        assertNull(filteredAttributes.get("firstName"));

        assertNull(filteredAttributes.get("lastName"));

        assertEquals(filteredAttributes.get("email").getValues().size(), 2);

        assertTrue(filteredAttributes.get("email").getValues()
                .contains(new StringAttributeValue("jsmith@example.edu")));

        assertTrue(filteredAttributes.get("email").getValues()
                .contains(new StringAttributeValue("john.smith@example.edu")));

        assertNull(filteredAttributes.get("affiliation"));
    }

    private void common45(String file) throws ServiceException, AttributeFilterException,
            ComponentInitializationException {

        final AttributeFilter filter = getFilter(file);

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(attributesToBeFiltered.values());

        filter.filterAttributes(filterContext);

        Map<String, IdPAttribute> filteredAttributes = filterContext.getFilteredIdPAttributes();

        assertEquals(1, filteredAttributes.size());

        assertNull(filteredAttributes.get("firstName"));

        assertNull(filteredAttributes.get("lastName"));

        assertNull(filteredAttributes.get("email"));

        assertEquals(2, filteredAttributes.get("affiliation").getValues().size(), 2);

        assertTrue(filteredAttributes.get("affiliation").getValues()
                .contains(new StringAttributeValue("employee")));

        assertTrue(filteredAttributes.get("affiliation").getValues().contains(new StringAttributeValue("staff")));

    }

    @Test public void deny1() throws ServiceException, AttributeFilterException, ComponentInitializationException {
        denyTest("deny1.xml");
    }

    @Test public void deny2() throws ServiceException, AttributeFilterException, ComponentInitializationException {
        denyTest("deny2.xml");
    }

    private void denyTest(String file) throws ServiceException, AttributeFilterException,
            ComponentInitializationException {
        final AttributeFilter filter = getFilter(file);

        AttributeFilterContext filterContext = new AttributeFilterContext();
        filterContext.setPrefilteredIdPAttributes(attributesToBeFiltered.values());

        filter.filterAttributes(filterContext);

        Map<String, IdPAttribute> filteredAttributes = filterContext.getFilteredIdPAttributes();

        assertEquals(1, filteredAttributes.size());

        assertNull(filteredAttributes.get("firstName"));

        assertNull(filteredAttributes.get("lastName"));

        assertNull(filteredAttributes.get("email"));

        assertEquals(2, filteredAttributes.get("affiliation").getValues().size(), 1);

        assertTrue(filteredAttributes.get("affiliation").getValues()
                .contains(new StringAttributeValue("employee")));

    }

}
