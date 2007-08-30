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

import java.util.Collection;

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
public class LdapAttributeResolverTest extends BaseConfigTestCase {

    /**
     * Test Handle Request.
     * 
     * @throws ResourceException if unable to access resource
     */
    @SuppressWarnings("unchecked")
    public void testResolverInstantiation() throws ResourceException {
        ApplicationContext ac = createSpringContext(DATA_PATH + "/config/attribute/resolver/service-config.xml");
        AttributeResolver resolver = (AttributeResolver) ac.getBean("resolver-ldap");

        BaseSAMLProfileRequestContext context = new BaseSAMLProfileRequestContext();
        context.setPrincipalName("lajoie");

        try {
            Collection<BaseAttribute> attributes;

            attributes = resolver.resolveAttributes(context).values();

            assertEquals(3, attributes.size());

            for (BaseAttribute attribute : attributes) {
                System.out.println(attribute.getId() + ":" + attribute.getValues());
            }

        } catch (AttributeResolutionException e) {
            fail(e.getMessage());
        }
    }

}