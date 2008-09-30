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

import org.opensaml.saml2.metadata.provider.ChainingMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.config.BaseConfigTestCase;

/**
 * Test that the configuration code for Chaining metadata providers works correctly.
 */
public class ChainingMetadataProviderTest extends BaseConfigTestCase {
    
    private String entityId1 = "urn:mace:incommon:washington.edu";
    private String entityId2 = "urn:mace:switch.ch:SWITCHaai:ethz.ch";

    /**
     * Test configuring an chaining metadata provider with Spring.
     * 
     * @throws Exception thrown if there is a problem
     */
    public void testProviderInstantiation() throws Exception {
        ApplicationContext appContext = createSpringContext(new String[] { DATA_PATH + "/config/base-config.xml",
                DATA_PATH + "/config/metadata/ChainingMetadataProvider1.xml", });

        ChainingMetadataProvider provider = (ChainingMetadataProvider) appContext.getBean("ChainingMetadata");
        assertNotNull(provider);
        
        assertNotNull("Did not find expected entity ID " + entityId1, provider.getEntityDescriptor(entityId1));
        assertNotNull("Did not find expected entity ID " + entityId2, provider.getEntityDescriptor(entityId2));
    }
    
    /**
     * Test configuring an chaining metadata provider with Spring, with a disallowed metadata filter on the chain. 
     * 
     * @throws Exception thrown if there is a problem
     */
    public void testProviderInstantiationWithFilter() throws Exception {
        try {
            ApplicationContext appContext = createSpringContext(new String[] { DATA_PATH + "/config/base-config.xml",
                    DATA_PATH + "/config/metadata/ChainingMetadataProvider2.xml", });
            fail("Chaining metadata provider instantiation should have failed, due to disallowed metadata filter");
        } catch (Exception e) {
            //do nothing, expected
        }
    }
    
    /**
     * Test configuring an chaining metadata provider with Spring, where the child members have filters.
     * 
     * @throws Exception thrown if there is a problem
     */
    public void testProviderInstantiationWithFiltersOnMembers() throws Exception {
        ApplicationContext appContext = createSpringContext(new String[] { DATA_PATH + "/config/base-config.xml",
                DATA_PATH + "/config/metadata/ChainingMetadataProvider3.xml", });

        ChainingMetadataProvider provider = (ChainingMetadataProvider) appContext.getBean("ChainingMetadata");
        assertNotNull(provider);
        assertNull("Chaining provider had disallowed metadata filter", provider.getMetadataFilter());
        
        MetadataProvider member0 = provider.getProviders().get(0);
        assertNotNull("Chain member 0 had no filter present", member0.getMetadataFilter());
        
        MetadataProvider member1 = provider.getProviders().get(1);
        assertNotNull("Chain member 1 had no filter present", member1.getMetadataFilter());
        
        assertFalse("Chain members had the same filter", member0.getMetadataFilter() == member1.getMetadataFilter());
    }
    
    
}