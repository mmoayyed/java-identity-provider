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

package edu.internet2.middleware.shibboleth.common.config;

import java.util.List;

import org.apache.log4j.Logger;
import org.opensaml.resource.Resource;
import org.opensaml.resource.ResourceException;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.InputStreamResource;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Utilities to help configure Spring beans.
 */
public final class SpringConfigurationUtils {

    /** Log4j logger. */
    private static Logger log = Logger.getLogger(SpringConfigurationUtils.class);

    /** Private Constructor. */
    private SpringConfigurationUtils() {
    }

    /**
     * Loads a set of spring configuration resources into a given application context.
     * 
     * @param beanRegistry registry of spring beans to be populated with information from the given configurations
     * @param configurationResources list of spring configuration resources
     * 
     * @throws ResourceException thrown if there is a problem reading the spring configuration resources into the
     *             registry
     */
    public static void populateRegistry(BeanDefinitionRegistry beanRegistry, List<Resource> configurationResources)
            throws ResourceException {
        XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(beanRegistry);
        configReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        configReader.setDocumentLoader(new SpringDocumentLoader());

        int numOfResources = configurationResources.size();
        Resource configurationResource;
        org.springframework.core.io.Resource[] configSources = new org.springframework.core.io.Resource[numOfResources];
        for (int i = 0; i < numOfResources; i++) {
            configurationResource = configurationResources.get(i);
            if (configurationResource != null && configurationResource.exists()) {
                configSources[i] = new InputStreamResource(configurationResources.get(i).getInputStream());
            } else {
                log.warn("Configuration resource not loaded because it does not exist: "
                        + configurationResource.getLocation());
            }
        }

        try {
            configReader.loadBeanDefinitions(configSources);
        } catch (BeanDefinitionStoreException e) {
            throw new ResourceException("Unable to load Spring bean registry with configuration resources", e);
        }
    }

    /**
     * xsi:type aware version of BeanDefinitionParserDelegate.parseCustomElement(Element).
     * 
     * @param element element to parse
     * @param parserContext current parser context
     * 
     * @return bean definition
     */
    public static BeanDefinition parseCustomElement(Element element, ParserContext parserContext) {
        BeanDefinitionParserDelegate delegate = parserContext.getDelegate();
        String namespaceUri = element.getNamespaceURI();

        if (XMLHelper.hasXSIType(element)) {
            namespaceUri = XMLHelper.getXSIType(element).getNamespaceURI();
        }

        NamespaceHandler handler = delegate.getReaderContext().getNamespaceHandlerResolver().resolve(namespaceUri);
        if (handler == null) {
            log.error("Unable to locate NamespaceHandler for namespace [" + namespaceUri + "]");
            return null;
        }
        return handler.parse(element, new ParserContext(delegate.getReaderContext(), delegate, null));
    }

    /**
     * Parses a custom element that is a reference to a bean declared elsewhere.
     * 
     * @param element the element that references the bean
     * @param refAttribute the name of the attribute that contains the referenced bean's name
     * @param parserContext current parsing context
     * 
     * @return reference to the bean or null if the element did not contain the reference attribute
     */
    public static RuntimeBeanReference parseCustomElementReference(Element element, String refAttribute,
            ParserContext parserContext) {
        String reference = DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null, refAttribute));
        if (reference != null) {
            return new RuntimeBeanReference(reference);
        }

        return null;
    }

    /**
     * Parse list of elements into bean definitions.
     * 
     * @param elements list of elements to parse
     * @param parserContext current parsing context
     * 
     * @return list of bean references
     */
    public static ManagedList parseCustomElements(NodeList elements, ParserContext parserContext) {
        int numOfElements = elements.getLength();
        ManagedList definitions = new ManagedList(numOfElements);

        Element e;
        for (int i = 0; i < numOfElements; i++) {
            e = (Element) elements.item(i);
            definitions.add(parseCustomElement(e, parserContext));
        }

        return definitions;
    }

    /**
     * Parse list of elements into bean definitions.
     * 
     * @param elements list of elements to parse
     * @param parserContext current parsing context
     * 
     * @return list of bean references
     */
    public static ManagedList parseCustomElements(List<Element> elements, ParserContext parserContext) {
        if (elements == null) {
            return null;
        }

        ManagedList definitions = new ManagedList(elements.size());
        for (Element e : elements) {
            definitions.add(parseCustomElement(e, parserContext));
        }

        return definitions;
    }
}