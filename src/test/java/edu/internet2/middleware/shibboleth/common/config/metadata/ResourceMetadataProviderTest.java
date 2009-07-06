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

package edu.internet2.middleware.shibboleth.common.config.metadata;

import org.opensaml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml2.metadata.provider.ResourceBackedMetadataProvider;
import org.opensaml.saml2.metadata.provider.SchemaValidationFilter;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.config.BaseConfigTestCase;

/**
 * Test that the configuration code for URL metadata providers works correctly.
 */
public class ResourceMetadataProviderTest extends BaseConfigTestCase {

    /**
     * Test configuring an resource metadata provider with Spring.
     * 
     * @throws Exception thrown if there is a problem
     */
    public void testProviderInstantiation() throws Exception {
        ApplicationContext appContext = createSpringContext(new String[] { DATA_PATH + "/config/base-config.xml",
                DATA_PATH + "/config/metadata/HTTPResourceMetadataProvider1.xml", });

        ResourceBackedMetadataProvider provider = 
            (ResourceBackedMetadataProvider) appContext.getBean("ResourceMetadata");
        assertNotNull(provider);
        
        assertEquals(((EntitiesDescriptor) provider.getMetadata()).getName(), "urn:mace:incommon");
    }
    
    /**
     * Test configuring an resource metadata provider with Spring.
     * 
     * @throws Exception thrown if there is a problem
     */
    public void testProviderInstantiationWithFilter() throws Exception {
        ApplicationContext appContext = createSpringContext(new String[] { DATA_PATH + "/config/base-config.xml",
                DATA_PATH + "/config/metadata/HTTPResourceMetadataProvider2.xml", });

        ResourceBackedMetadataProvider provider = 
            (ResourceBackedMetadataProvider) appContext.getBean("ResourceMetadata");
        assertNotNull(provider);
        
        assertNotNull("Missing metadata filter property", provider.getMetadataFilter());
        assertTrue("Wrong metadata filter property", provider.getMetadataFilter() instanceof SchemaValidationFilter);
    }
    
}