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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver;

import java.util.Map;

import org.opensaml.util.resource.ResourceException;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;
import edu.internet2.middleware.shibboleth.common.config.BaseConfigTestCase;
import edu.internet2.middleware.shibboleth.common.profile.provider.BaseSAMLProfileRequestContext;

/**
 * Unit test for database data connector.
 */
public class DBAttributeResolverTest extends BaseConfigTestCase {

    /**
     * Test Handle Request.
     * 
     * @throws ResourceException if unable to access resource
     * @throws AttributeResolutionException if unable to resolve attributes
     */
    public void testResolverInstantiation() throws ResourceException, AttributeResolutionException {
        ApplicationContext ac = createSpringContext(new String[] { DATA_PATH + "/config/base-config.xml",
                DATA_PATH + "/config/attribute/resolver/resolver-db-config.xml", });
        AttributeResolver resolver = (AttributeResolver) ac.getBean("resolver-db");

        BaseSAMLProfileRequestContext context = new BaseSAMLProfileRequestContext();
        context.setPrincipalName("astone");

        try {
            Map<String, BaseAttribute> attributes = resolver.resolveAttributes(context);
            
            BaseAttribute uid = attributes.get("uid");
            assertNotNull(uid);
            assertEquals(1, uid.getValues().size());
            assertTrue(uid.getValues().contains("astone"));
            
            BaseAttribute email = attributes.get("email");
            assertNotNull(email);
            assertEquals(1, email.getValues().size());
            assertTrue(email.getValues().contains("astone@example.edu"));
            
            BaseAttribute firstName = attributes.get("FIRSTNAME");
            assertNotNull(firstName);
            assertEquals(1, firstName.getValues().size());
            assertTrue(firstName.getValues().contains("Alexander"));
            
            BaseAttribute lastName = attributes.get("LASTNAME");
            assertNotNull(lastName);
            assertEquals(1, lastName.getValues().size());
            assertTrue(lastName.getValues().contains("Stone"));
            
            BaseAttribute fullName = attributes.get("fullname");
            assertNotNull(fullName);
            assertEquals(1, fullName.getValues().size());
            assertTrue(fullName.getValues().contains("Alexander Stone"));

        } catch (AttributeResolutionException e) {
            fail(e.getMessage());
        }
    }

}