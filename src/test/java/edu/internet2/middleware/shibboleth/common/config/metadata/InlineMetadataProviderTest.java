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
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.SchemaValidationFilter;
import org.opensaml.util.resource.ResourceException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import edu.internet2.middleware.shibboleth.common.config.BaseConfigTestCase;


/**
 * Test that the configuration code for inline metadata providers works correctly.
 */
public class InlineMetadataProviderTest extends BaseConfigTestCase {
    
    /**
     * Tests that an Inline provider is properly created when given an {@link EntitiesDescriptor}.
     * 
     * @throws Exception thrown if there is an error using the configuration
     */
    public void testProviderInstantiationEntitiesDescriptor() throws Exception{
        ApplicationContext appContext = createSpringContext(DATA_PATH + "/config/metadata/InlineMetadataProvider1.xml");
        
        MetadataProvider provider = (MetadataProvider) appContext.getBean("InlineMetadata");
        assertNotNull(provider);
        assertEquals(((EntitiesDescriptor)provider.getMetadata()).getName(), "urn:mace:incommon");
    }
    
    /**
     * Tests that an Inline provider is properly created when given an {@link EntityDescriptor}.
     * 
     * @throws Exception thrown if there is an error using the configuration
     */
    public void testProviderInstantiationEntityDescriptor() throws Exception{
        ApplicationContext appContext = createSpringContext(DATA_PATH + "/config/metadata/InlineMetadataProvider2.xml");
        
        MetadataProvider provider = (MetadataProvider) appContext.getBean("InlineMetadata");
        assertNotNull(provider);
        assertEquals(((EntityDescriptor)provider.getMetadata()).getEntityID(), "urn:mace:incommon:internet2.edu");
    }
    
    /**
     * Tests that an Inline provider is properly created when given an {@link EntitiesDescriptor}
     * with a MetadataFilter.
     * 
     * @throws Exception thrown if there is an error using the configuration
     */
    public void testProviderInstantiationWithFilter() throws Exception{
        ApplicationContext appContext = createSpringContext(DATA_PATH + "/config/metadata/InlineMetadataProvider4.xml");
        
        MetadataProvider provider = (MetadataProvider) appContext.getBean("InlineMetadata");
        assertNotNull(provider);
        assertEquals(((EntitiesDescriptor)provider.getMetadata()).getName(), "urn:mace:incommon");
        
        assertNotNull("Missing metadata filter property", provider.getMetadataFilter());
        assertTrue("Wrong metadata filter property", provider.getMetadataFilter() instanceof SchemaValidationFilter);
    }
    
    /**
     * Tests that an Inline provider is properly created when given an {@link EntitiesDescriptor}.
     * 
     * @throws Exception thrown if there is an error using the configuration
     */
    public void testFailedProviderInstantiation() throws Exception{
        try{
            createSpringContext(DATA_PATH + "/config/metadata/InlineMetadataProvider3.xml");
            fail("Loaded invalid configuration file.");
        }catch(ResourceException e){
            // expected
        }
    }
}