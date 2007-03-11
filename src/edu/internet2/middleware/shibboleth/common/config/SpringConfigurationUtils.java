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
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
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
        if(elements == null){
            return null;
        }
        
        ManagedList definitions = new ManagedList(elements.size());
        for(Element e: elements){
            definitions.add(parseCustomElement(e, parserContext));
        }

        return definitions;
    }

    /**
     * Parses custom elements which may bean definitions or references to another bean definition.
     * 
     * @param elements list of custom elements to parse
     * @param refAttribute the name of the attribute that contains the referenced bean's name
     * @param parserContext current parsing context
     * 
     * @return list of bean definitions or references
     */
    public static ManagedList parseCustomElements(NodeList elements, String refAttribute, ParserContext parserContext) {
        int numOfElements = elements.getLength();
        ManagedList definitions = new ManagedList();

        Element e;
        for (int i = 0; i < numOfElements; i++) {
            e = (Element) elements.item(0);
            if (e.hasAttributeNS(null, refAttribute)) {
                definitions.add(parseCustomElementReference(e, refAttribute, parserContext));
            } else {
                definitions.add(parseCustomElement(e, parserContext));
            }
        }

        return definitions;
    }
    
    /**
     * Parses custom elements which may bean definitions or references to another bean definition.
     * 
     * @param elements list of custom elements to parse
     * @param refAttribute the name of the attribute that contains the referenced bean's name
     * @param parserContext current parsing context
     * 
     * @return list of bean definitions or references
     */
    public static ManagedList parseCustomElements(List<Element> elements, String refAttribute, ParserContext parserContext) {
        if(elements == null){
            return null;
        }
        
        ManagedList definitions = new ManagedList();
        for (Element e : elements){
            if (e.hasAttributeNS(null, refAttribute)) {
                definitions.add(parseCustomElementReference(e, refAttribute, parserContext));
            } else {
                definitions.add(parseCustomElement(e, parserContext));
            }
        }

        return definitions;
    }
}