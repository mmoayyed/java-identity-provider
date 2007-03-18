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

package edu.internet2.middleware.shibboleth.common.config.resolver.dataConnector;

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

import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ResolutionPlugIn;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.StaticDataConnector;

/**
 * Spring Bean Definition Parser for static data connector.
 */
public class StaticDataConnectorBeanDefinitionParser extends BaseDataConnectorBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName("urn:mace:shibboleth:2.0:resolver:dc:static", "Static");

    /** Local name of attribute. */
    public static final String ATTRIBUTE_ELEMENT_LOCAL_NAME = "Attribute";

    /** Local name of value. */
    public static final String VALUE_ELEMENT_LOCAL_NAME = "Value";

    /** {@inheritDoc} */
    protected Class<? extends ResolutionPlugIn> getBeanClass(Element element) {
        return StaticDataConnector.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        NodeList elements = element.getElementsByTagNameNS(StaticDataConnectorNamespaceHandler.NAMESPACE,
                ATTRIBUTE_ELEMENT_LOCAL_NAME);
        if (elements != null && elements.getLength() > 0) {
            ManagedList attributes = new ManagedList(elements.getLength());

            for (int i = 0; i < elements.getLength(); ++i) {
                Element attrElement = (Element) elements.item(i);
                BeanDefinition attr = buildAttribute(attrElement);
                attributes.add(attr);
            }

            builder.addPropertyValue("sourceData", attributes);
        }
    }

    /**
     * Parse and build an attribute element.
     * 
     * @param element DOM element
     * @return bean definition builder
     */
    private static BeanDefinition buildAttribute(Element element) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(BasicAttribute.class);

        builder.addPropertyValue("id", element.getAttribute("id"));

        // parse <Value> elements
        NodeList valueElements = element.getElementsByTagNameNS(StaticDataConnectorNamespaceHandler.NAMESPACE,
                VALUE_ELEMENT_LOCAL_NAME);
        if (valueElements != null && valueElements.getLength() > 0) {
            SortedSet<String> values = new TreeSet<String>();

            for (int i = 0; i < valueElements.getLength(); i++) {
                Element valueElement = (Element) valueElements.item(i);
                String value = DomUtils.getTextValue(valueElement);
                values.add(value);
            }

            builder.addPropertyValue("values", values);
        }

        return builder.getBeanDefinition();
    }

}