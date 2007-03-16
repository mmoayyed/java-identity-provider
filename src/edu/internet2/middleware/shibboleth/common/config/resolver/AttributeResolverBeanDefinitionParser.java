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

import javax.xml.namespace.QName;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.impl.AttributeResolverImpl;
import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;

/**
 * Spring Bean Definition Parser for Shibboleth Attribute Resolver.
 */
public class AttributeResolverBeanDefinitionParser extends AbstractBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName("urn:mace:shibboleth:2.0:resolver", "AttributeResolver");

    /** Local name of attribute definition. */
    public static final String ATTRIBUTE_DEFINITION_ELEMENT_LOCAL_NAME = "AttributeDefinition";

    /** Local name of data connector. */
    public static final String DATA_CONNECTOR_ELEMENT_LOCAL_NAME = "DataConnector";

    /** Local name of principal connector. */
    public static final String PRINCIPAL_CONNECTOR_ELEMENT_LOCAL_NAME = "PrincipalConnector";

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }

    /** {@inheritDoc} */
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(AttributeResolverFactoryBean.class);
        BeanDefinitionBuilder resolver = BeanDefinitionBuilder.rootBeanDefinition(AttributeResolverImpl.class);
        factory.addPropertyValue("resolver", resolver.getBeanDefinition());

        NodeList elements;

        // parse AttributeDefinition plug-ins
        elements = element.getElementsByTagNameNS(AttributeResolverNamespaceHandler.NAMESPACE,
                ATTRIBUTE_DEFINITION_ELEMENT_LOCAL_NAME);
        if (elements != null && elements.getLength() > 0) {
            ManagedList definitions = SpringConfigurationUtils.parseCustomElements(elements, parserContext);
            factory.addPropertyValue("attributeDefinitions", definitions);
        }

        // parse DataConnector plug-ins
        elements = element
                .getElementsByTagNameNS(AttributeResolverNamespaceHandler.NAMESPACE, DATA_CONNECTOR_ELEMENT_LOCAL_NAME);
        if (elements != null && elements.getLength() > 0) {
            ManagedList connectors = SpringConfigurationUtils.parseCustomElements(elements, parserContext);
            factory.addPropertyValue("dataConnectors", connectors);
        }

        // parse PrincipalConnector plug-ins
        elements = element.getElementsByTagNameNS(AttributeResolverNamespaceHandler.NAMESPACE,
                PRINCIPAL_CONNECTOR_ELEMENT_LOCAL_NAME);
        if (elements != null && elements.getLength() > 0) {
            ManagedList connectors = SpringConfigurationUtils.parseCustomElements(elements, parserContext);
            factory.addPropertyValue("principalConnectors", connectors);
        }

        return factory.getBeanDefinition();
    }
}