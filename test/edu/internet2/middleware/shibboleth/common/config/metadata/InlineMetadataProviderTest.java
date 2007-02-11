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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.xml.parse.ClasspathResolver;
import org.opensaml.xml.parse.LoggingErrorHandler;
import org.springframework.beans.factory.xml.DocumentLoader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;


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
        
        XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(springCtx);
        configReader.setDocumentLoader(new ValidatingDocumentLoader());
        //configReader.setEntityResolver(new ClasspathEntityResolver());
        //configReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
        //configReader.setNamespaceAware(true);
        configReader.loadBeanDefinitions(new ClassPathResource(springConfigFile));
    }
    
    public void testProviderInstantiation() throws Exception{
        assertEquals(1, springCtx.getBeanDefinitionCount());
        
        assertTrue("Configured metadata provider no present in Spring context", springCtx.containsBean(providerId));
        
        MetadataProvider provider = (MetadataProvider) springCtx.getBean(providerId);
        assertNotNull(provider);
        assertEquals(((EntitiesDescriptor)provider.getMetadata()).getName(), expectedName);
    }
    
    public class ValidatingDocumentLoader implements DocumentLoader{

        Logger log = Logger.getLogger(ValidatingDocumentLoader.class);
        
        /** {@inheritDoc} */
        public Document loadDocument(InputSource inputSource, EntityResolver entityResolver, ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
    "http://www.w3.org/2001/XMLSchema");
            factory.setCoalescing(true);
            factory.setIgnoringComments(true);
            factory.setNamespaceAware(true);
            factory.setValidating(true);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new LoggingErrorHandler(log));
            builder.setEntityResolver(new ClasspathResolver());
            return builder.parse(inputSource);
        }
    }
}