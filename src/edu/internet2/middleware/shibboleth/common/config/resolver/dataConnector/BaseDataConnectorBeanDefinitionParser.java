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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.internet2.middleware.shibboleth.common.config.resolver.AbstractResolutionPlugInBeanDefinitionParser;
import edu.internet2.middleware.shibboleth.common.config.resolver.AttributeResolverNamespaceHandler;

/**
 * Base spring {@link BeanDefinitionParser} for data connectors. DataConnector implementations should provide a custom
 * BeanDefinitionParser by extending this class and overriding the doParse() method to parse any additional attributes
 * or elements it requires. Standard attributes and elements defined by the ResolutionPlugIn and DataConnector schemas
 * will automatically attempt to be parsed.
 */
public abstract class BaseDataConnectorBeanDefinitionParser extends AbstractResolutionPlugInBeanDefinitionParser {

    /** Failover data connector attribute name. */
    public static final String FAILOVER_DATA_CONNECTOR_ELEMENT_LOCAL_NAME = "failoverDataConnector";

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        // parse failover connector
        NodeList elements = element.getElementsByTagNameNS(AttributeResolverNamespaceHandler.NAMESPACE,
                FAILOVER_DATA_CONNECTOR_ELEMENT_LOCAL_NAME);
        if (elements != null && elements.getLength() > 0) {
            builder.addPropertyValue("attributeDefinitionDependencyIds", parseDependencies(elements));
        }
    }

}