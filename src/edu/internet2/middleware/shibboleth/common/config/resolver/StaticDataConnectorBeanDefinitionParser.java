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

package edu.internet2.middleware.shibboleth.common.config.resolver;

import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.internet2.middleware.shibboleth.common.attribute.impl.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.impl.StaticDataConnector;

/**
 * Spring Bean Definition Parser for static data connector.
 */
public class StaticDataConnectorBeanDefinitionParser extends AbstractBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName("urn:mace:shibboleth:2.0:resolver:dc:static", "Static");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return StaticDataConnector.class;
    }

    /** {@inheritDoc} */
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(StaticDataConnectorFactoryBean.class);
        BeanDefinitionBuilder connector = parseConnector(element);
        factory.addPropertyValue("connector", connector.getBeanDefinition());

        NodeList elements = element.getElementsByTagNameNS(StaticDataConnectorNamespaceHandler.NAMESPACE, "Attribute");
        if (elements != null && elements.getLength() > 0) {
            parseAttributes(elements, factory);
        }
        return factory.getBeanDefinition();
    }

    /**
     * Parse connector element.
     * 
     * @param element DOM element
     * @return bean definition builder
     */
    private static BeanDefinitionBuilder parseConnector(Element element) {
        BeanDefinitionBuilder connector = BeanDefinitionBuilder.rootBeanDefinition(StaticDataConnector.class);
        connector.addPropertyValue("id", element.getAttribute("id"));
        return connector;
    }

    /**
     * Parse attribute.
     * 
     * @param elements element to parse
     * @param builder factory
     */
    private static void parseAttributes(NodeList elements, BeanDefinitionBuilder builder) {
        ManagedList attributes = new ManagedList(elements.getLength());

        for (int i = 0; i < elements.getLength(); ++i) {
            Element attrElement = (Element) elements.item(i);
            BeanDefinitionBuilder attr = parseAttribute(attrElement);
            attributes.add(attr.getBeanDefinition());
        }

        builder.addPropertyValue("attributes", attributes);
    }

    /**
     * Parse attribute value elements.
     * 
     * @param elements DOM elements
     * @return set of values
     */
    private static SortedSet<String> parseValues(NodeList elements) {
        SortedSet<String> values = new TreeSet<String>();

        for (int j = 0; j < elements.getLength(); j++) {
            Element valueElement = (Element) elements.item(j);
            String value = DomUtils.getTextValue(valueElement);
            values.add(value);
        }

        return values;
    }

    /**
     * Parse an attribute element.
     * 
     * @param element DOM element
     * @return bean definition builder
     */
    private static BeanDefinitionBuilder parseAttribute(Element element) {
        BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(AttributeFactoryBean.class);
        BeanDefinitionBuilder attribute = BeanDefinitionBuilder.rootBeanDefinition(BaseAttribute.class);
        
        attribute.addPropertyValue("id", element.getAttribute("id"));

        NodeList valueElements = element.getElementsByTagNameNS(StaticDataConnectorNamespaceHandler.NAMESPACE, "Value");
        factory.addPropertyValue("values", parseValues(valueElements));

        factory.addPropertyValue("attribute", attribute.getBeanDefinition());
        return factory;
    }

}