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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Base class for Spring bean definition parser for Shibboleth resolver plug-ins.
 */
public abstract class AbstractResolutionPlugInBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Name of attribute definition dependency. */
    public static final QName ATTRIBUTE_DEFINITION_DEPENDENCY_ELEMENT_NAME = new QName(
            AttributeResolverNamespaceHandler.NAMESPACE, "AttributeDefinitionDependency");

    /** Name of data connector dependency. */
    public static final QName DATA_CONNECTOR_DEPENDENCY_ELEMENT_NAME = new QName(
            AttributeResolverNamespaceHandler.NAMESPACE, "DataConnectorDependency");

    /** Class logger. */
    private static Logger log = Logger.getLogger(AbstractResolutionPlugInBeanDefinitionParser.class);

    /**
     * Parses the plugins ID and attribute definition and data connector dependencies.
     * 
     * {@inheritDoc}
     */
    protected final void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        String pluginId = DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null, "id"));
        if (log.isInfoEnabled()) {
            log.info("Parsing configuration for " + element.getLocalName() + " plugin with ID: " + pluginId);
        }
        builder.addPropertyValue("pluginId", pluginId);

        Map<QName, List<Element>> children = XMLHelper.getChildElements(element);

        Set<String> adIds = parseDependencies(children.get(ATTRIBUTE_DEFINITION_DEPENDENCY_ELEMENT_NAME));
        if (log.isDebugEnabled()) {
            log.debug("Setting the following attribute definition dependencies for " + element.getLocalName()
                    + " plugin " + pluginId + ": " + adIds);
        }
        builder.addPropertyValue("attributeDefinitionDependencyIds", adIds);

        Set<String> dcIds = parseDependencies(children.get(DATA_CONNECTOR_DEPENDENCY_ELEMENT_NAME));
        if (log.isDebugEnabled()) {
            log.debug("Setting the following data connector dependencies for " + element.getLocalName() + " plugin "
                    + pluginId + ": " + dcIds);
        }
        builder.addPropertyValue("dataConnectorDependencyIds", dcIds);

        doParse(pluginId, element, children, builder, parserContext);
    }

    /**
     * Parses the plugin configuration.
     * 
     * @param pluginId unique ID of the plugin
     * @param pluginConfig root plugin configuration element
     * @param pluginConfigChildren immediate children of the root configuration element (provided to save from having to
     *            reparse them)
     * @param pluginBuilder bean definition builder for the plugin
     * @param parserContext current parsing context
     */
    protected abstract void doParse(String pluginId, Element pluginConfig,
            Map<QName, List<Element>> pluginConfigChildren, BeanDefinitionBuilder pluginBuilder,
            ParserContext parserContext);

    /** {@inheritDoc} */
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        return element.getAttributeNS(null, "id");
    }

    /**
     * Parse dependency elements.
     * 
     * @param elements DOM elements of type <code>resolver:PluginDependencyType</code>
     * 
     * @return the dependency IDs
     */
    protected Set<String> parseDependencies(List<Element> elements) {
        if (elements == null || elements.size() == 0) {
            return null;
        }

        Set<String> dependencyIds = new HashSet<String>();
        for (Element dependency : elements) {
            dependencyIds.add(DatatypeHelper.safeTrimOrNullString(dependency.getAttributeNS(null,"ref")));
        }

        return dependencyIds;
    }
}