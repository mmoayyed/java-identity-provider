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

import org.opensaml.util.resource.ResourceException;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringException;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.ShibbolethAttributeFilteringEngine;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.config.BaseConfigTestCase;
import edu.internet2.middleware.shibboleth.common.profile.provider.BaseSAMLProfileRequestContext;

/**
 * Tests parsing an attribute filter policy configuration.
 */
public class AttributeFilterPolicyTest extends BaseConfigTestCase {

    private Map<String, BaseAttribute> attributes;

    private BaseSAMLProfileRequestContext requestContext;

    private ApplicationContext appContext;

    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();

        attributes = new HashMap<String, BaseAttribute>();

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

        requestContext = new BaseSAMLProfileRequestContext();
        requestContext.setPrincipalName("jsmith");
        requestContext.setReleaseAttributes(attributes.keySet());

        String[] configs = { DATA_PATH + "/config/attribute/filter/service-config.xml", };
        appContext = createSpringContext(configs);
    }

    public void testEngineA() throws ResourceException, AttributeFilteringException {
        ShibbolethAttributeFilteringEngine filterEngine = (ShibbolethAttributeFilteringEngine) appContext
                .getBean("engineA");
        Map<String, BaseAttribute> filteredAttributes = filterEngine.filterAttributes(attributes, requestContext);

        assertEquals(1, filteredAttributes.size());

        BaseAttribute attribute;
        attribute = filteredAttributes.get("firstName");
        assertNull(attribute);

        attribute = filteredAttributes.get("lastName");
        assertNull(attribute);

        attribute = filteredAttributes.get("email");
        assertEquals(2, attribute.getValues().size());

        attribute = filteredAttributes.get("affiliation");
        assertNull(attribute);
    }

    public void testEngineB() throws ResourceException, AttributeFilteringException {
        ShibbolethAttributeFilteringEngine filterEngine = (ShibbolethAttributeFilteringEngine) appContext
                .getBean("engineB");
        Map<String, BaseAttribute> filteredAttributes = filterEngine.filterAttributes(attributes, requestContext);

        assertEquals(1, filteredAttributes.size());

        BaseAttribute attribute;
        attribute = filteredAttributes.get("firstName");
        assertNull(attribute);

        attribute = filteredAttributes.get("lastName");
        assertNull(attribute);

        attribute = filteredAttributes.get("email");
        assertNull(attribute);

        attribute = filteredAttributes.get("affiliation");
        assertEquals(2, attribute.getValues().size());
    }
}