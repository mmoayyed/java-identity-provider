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

import org.apache.log4j.Logger;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.config.BeanDefinition;
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
     * Parse list of elements into bean definitions.
     * 
     * @param elements list of elements to parse
     * @param parserContext current parsing context
     * 
     * @return list of bean references
     */
    public static ManagedList parseCustomElements(NodeList elements, ParserContext parserContext) {
        ManagedList definitions = new ManagedList(elements.getLength());

        Element e;
        for (int i = 0; i < elements.getLength(); i++) {
            e = (Element) elements.item(i);
            definitions.add(parseCustomElement(e, parserContext));
        }

        return definitions;
    }
}