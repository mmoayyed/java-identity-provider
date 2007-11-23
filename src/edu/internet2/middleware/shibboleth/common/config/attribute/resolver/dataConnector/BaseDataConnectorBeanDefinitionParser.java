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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.dataConnector;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.AbstractResolutionPlugInBeanDefinitionParser;
import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.AttributeResolverNamespaceHandler;

/**
 * Base spring bean definition parser for data connectors. DataConnector implementations should provide a custom
 * BeanDefinitionParser by extending this class and overriding the
 * {@link #doParse(String, Element, Map, BeanDefinitionBuilder, ParserContext)} method to parse any additional
 * attributes or elements it requires. Standard attributes and elements defined by the ResolutionPlugIn and
 * DataConnector schemas will automatically attempt to be parsed.
 */
public abstract class BaseDataConnectorBeanDefinitionParser extends AbstractResolutionPlugInBeanDefinitionParser {

    /** Failover data connector attribute name. */
    public static final QName FAILOVER_DATA_CONNECTOR_ELEMENT_NAME = new QName(
            AttributeResolverNamespaceHandler.NAMESPACE, "FailoverDataConnector");

    /** Log4j logger. */
    private final Logger log = LoggerFactory.getLogger(BaseDataConnectorBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected void doParse(String pluginId, Element pluginConfig, Map<QName, List<Element>> pluginConfigChildren,
            BeanDefinitionBuilder pluginBuilder, ParserContext parserContext) {

        List<Element> failoverConnector = pluginConfigChildren.get(FAILOVER_DATA_CONNECTOR_ELEMENT_NAME);
        if (failoverConnector != null && !failoverConnector.isEmpty()) {
            String connectorId = DatatypeHelper.safeTrimOrNullString(failoverConnector.get(0).getAttributeNS(null,
                    "ref"));
            log.debug("Setting the following failover data connector dependencies for plugin {}: {}", pluginId,
                    connectorId);
            pluginBuilder.addPropertyValue("failoverDataConnectorId", connectorId);
        }
    }
}