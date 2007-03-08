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

import java.util.Set;

import javolution.util.FastSet;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionPlugIn;

/**
 * Base class for Spring bean definition parser for Shibboleth resovler plug-ins.
 */
public abstract class AbstractResolutionPlugInBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Local name of attribute definition dependency. */
    public static final String ATTRIBUTE_DEFINITION_DEPENDENCY_ELEMENT_LOCAL_NAME = "AttributeDefinitionDependency";

    /** Local name of data connector dependency. */
    public static final String DATA_CONNECTOR_DEPENDENCY_ELEMENT_LOCAL_NAME = "DataConnectorDependency";

    /** {@inheritDoc} */
    protected abstract Class<? extends ResolutionPlugIn> getBeanClass(Element element);

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

        // grab attributes off of the plugin element
        builder.addPropertyValue("id", element.getAttribute(ID_ATTRIBUTE));
        builder.addPropertyValue("propagateErrors", element.getAttribute("propagateErrors"));

        // parse child elements
        NodeList elements;

        elements = element.getElementsByTagNameNS(AttributeResolverNamespaceHandler.NAMESPACE,
                ATTRIBUTE_DEFINITION_DEPENDENCY_ELEMENT_LOCAL_NAME);
        if (elements != null && elements.getLength() > 0) {
            builder.addPropertyValue("attributeDefinitionDependencyIds", parseDependencies(elements));
        }

        elements = element.getElementsByTagNameNS(AttributeResolverNamespaceHandler.NAMESPACE,
                DATA_CONNECTOR_DEPENDENCY_ELEMENT_LOCAL_NAME);
        if (elements != null && elements.getLength() > 0) {
            builder.addPropertyValue("dataConnectorDependencyIds", parseDependencies(elements));
        }
    }

    /**
     * Parse dependcy elements.
     * 
     * @param elements DOM elements of type <code>resolver:PluginDependencyType</code>
     * @return the dependency IDs
     */
    protected Set<String> parseDependencies(NodeList elements) {
        Set<String> dependencyIds = new FastSet<String>();

        for (int i = 0; i < elements.getLength(); ++i) {
            Element dependency = (Element) elements.item(i);
            dependencyIds.add(dependency.getAttribute("ref"));
        }

        return dependencyIds;
    }

}