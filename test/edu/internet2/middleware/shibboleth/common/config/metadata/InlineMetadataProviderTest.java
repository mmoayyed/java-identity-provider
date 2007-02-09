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

import junit.framework.TestCase;

import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

/**
 * Test that the configuration code for inline metadata providers works correctly.
 */
public class InlineMetadataProviderTest extends TestCase {

    private GenericApplicationContext springCtx;
    
    private String providerId;
    
    private String expectedName;
    
    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();
        
        DefaultBootstrap.bootstrap();
        
        providerId = "InlineMetadata";
        expectedName = "urn:mace:incommon";
        
        String springConfigFile = "/data/edu/internet2/middleware/shibboleth/common/config/metadata/InlineMetadataProvider.xml";
        springCtx = new GenericApplicationContext();
        XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(InlineMetadataProviderTest.this.springCtx);
        configReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
        configReader.setNamespaceAware(true);
        configReader.loadBeanDefinitions(new ClassPathResource(springConfigFile));
    }
    
    public void testProviderInstantiation() throws Exception{
        assertEquals(1, springCtx.getBeanDefinitionCount());
        
        assertTrue("Configured metadata provider no present in Spring context", springCtx.containsBean(providerId));
        
        MetadataProvider provider = (MetadataProvider) springCtx.getBean(providerId);
        assertNotNull(provider);
        assertEquals(((EntitiesDescriptor)provider.getMetadata()).getName(), expectedName);
    }
}