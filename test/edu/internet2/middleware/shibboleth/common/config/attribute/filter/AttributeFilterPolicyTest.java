/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.config.attribute.filter;

import java.util.HashMap;
import java.util.Map;

import org.opensaml.resource.ResourceException;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringException;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.ShibbolethAttributeFilteringEngine;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.config.BaseConfigTestCase;

/**
 * Tests parsing an attribute filter policy configuration.
 */
public class AttributeFilterPolicyTest extends BaseConfigTestCase {

    private Map<String, Attribute> attributes;

    private ShibbolethAttributeRequestContext requestContext;
    
    private ApplicationContext appContext;

    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();

        attributes = new HashMap<String, Attribute>();

        BasicAttribute<String> firstName = new BasicAttribute<String>("firstName");
        firstName.getValues().add("john");
        attributes.put(firstName.getId(), firstName);

        BasicAttribute<String> lastName = new BasicAttribute<String>("lastName");
        lastName.getValues().add("smith");
        attributes.put(lastName.getId(), lastName);

        BasicAttribute<String> email = new BasicAttribute<String>("email");
        email.getValues().add("jsmith@example.edu");
        email.getValues().add("john.smith@example.edu");
        attributes.put(email.getId(), email);

        BasicAttribute<String> affiliation = new BasicAttribute<String>("affiliation");
        affiliation.getValues().add("employee");
        affiliation.getValues().add("staff");
        affiliation.getValues().add("illegalValue");
        attributes.put(affiliation.getId(), affiliation);

        requestContext = new ShibbolethAttributeRequestContext();
        requestContext.setPrincipalName("jsmith");
        requestContext.setRequestedAttributes(attributes.keySet());
        
        String[] configs = { DATA_PATH + "/config/attribute/filter/filter-engine-config.xml" , };
        appContext = createSpringContext(configs);
    }
    
    public void testEngineA() throws ResourceException, AttributeFilteringException {
        ShibbolethAttributeFilteringEngine filterEngine = (ShibbolethAttributeFilteringEngine) appContext
                .getBean("engineA");
        Map<String, Attribute> filteredAttributes = filterEngine.filterAttributes(attributes, requestContext);

        assertEquals(4, filteredAttributes.size());

        Attribute attribute;
        attribute = filteredAttributes.get("firstName");
        assertEquals(0, attribute.getValues().size());
        
        attribute = filteredAttributes.get("lastName");
        assertEquals(0, attribute.getValues().size());
        
        attribute = filteredAttributes.get("email");
        assertEquals(2, attribute.getValues().size());
        
        attribute = filteredAttributes.get("affiliation");
        assertEquals(0, attribute.getValues().size());
    }
    
    public void testEngineB() throws ResourceException, AttributeFilteringException {
        ShibbolethAttributeFilteringEngine filterEngine = (ShibbolethAttributeFilteringEngine) appContext
                .getBean("engineB");
        Map<String, Attribute> filteredAttributes = filterEngine.filterAttributes(attributes, requestContext);

        assertEquals(4, filteredAttributes.size());

        Attribute attribute;
        attribute = filteredAttributes.get("firstName");
        assertEquals(0, attribute.getValues().size());
        
        attribute = filteredAttributes.get("lastName");
        assertEquals(0, attribute.getValues().size());
        
        attribute = filteredAttributes.get("email");
        assertEquals(0, attribute.getValues().size());
        
        attribute = filteredAttributes.get("affiliation");
        assertEquals(2, attribute.getValues().size());
    }
}