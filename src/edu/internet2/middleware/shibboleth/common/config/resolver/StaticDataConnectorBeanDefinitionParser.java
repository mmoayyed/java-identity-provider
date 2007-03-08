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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.internet2.middleware.shibboleth.common.attribute.impl.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionPlugIn;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.impl.StaticDataConnector;

/**
 * Spring Bean Definition Parser for static data connector.
 */
public class StaticDataConnectorBeanDefinitionParser extends BaseDataConnectorBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName("urn:mace:shibboleth:2.0:resolver:dc:static", "Static");

    /** {@inheritDoc} */
    protected Class getFactoryBeanClass(Element element) {
        return StaticDataConnectorFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected Class<? extends ResolutionPlugIn> getInternalBeanClass(Element element) {
        return StaticDataConnector.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder factory,
            BeanDefinitionBuilder pluginBuilder) {
        super.doParse(element, parserContext, factory, pluginBuilder);
        
        NodeList elements = element.getElementsByTagNameNS(StaticDataConnectorNamespaceHandler.NAMESPACE, "Attribute");
        if (elements != null && elements.getLength() > 0) {
            ManagedList attributes = new ManagedList(elements.getLength());

            for (int i = 0; i < elements.getLength(); ++i) {
                Element attrElement = (Element) elements.item(i);
                BeanDefinition attr = buildAttribute(attrElement);
                attributes.add(attr);
            }

            factory.addPropertyValue("attributes", attributes);
        }
    }

    /**
     * Parse and build an attribute element.
     * 
     * @param element DOM element
     * @return bean definition builder
     */
    private static BeanDefinition buildAttribute(Element element) {
        BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(AttributeFactoryBean.class);
        BeanDefinitionBuilder attribute = BeanDefinitionBuilder.rootBeanDefinition(BaseAttribute.class);

        attribute.addPropertyValue("id", element.getAttribute("id"));
        factory.addPropertyValue("attribute", attribute.getBeanDefinition());

        // parse <Value> elements
        NodeList valueElements = element.getElementsByTagNameNS(StaticDataConnectorNamespaceHandler.NAMESPACE, "Value");
        if (valueElements != null && valueElements.getLength() > 0) {
            SortedSet<String> values = new TreeSet<String>();

            for (int i = 0; i < valueElements.getLength(); i++) {
                Element valueElement = (Element) valueElements.item(i);
                String value = DomUtils.getTextValue(valueElement);
                values.add(value);
            }

            factory.addPropertyValue("values", values);
        }

        return factory.getBeanDefinition();
    }

}