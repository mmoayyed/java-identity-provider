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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Base class for Spring bean definition parser for Shibboleth resolver plug-ins.
 */
public abstract class AbstractResolutionPlugInBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Local name of attribute definition dependency. */
    public static final String ATTRIBUTE_DEFINITION_DEPENDENCY_ELEMENT_LOCAL_NAME = "AttributeDefinitionDependency";

    /** Local name of data connector dependency. */
    public static final String DATA_CONNECTOR_DEPENDENCY_ELEMENT_LOCAL_NAME = "DataConnectorDependency";

    /** NameID format attribute name. */
    public static final String PROPAGATE_ERRORS_ATTRIBUTE_NAME = "propagateErrors";

    /** {@inheritDoc} */
    protected abstract Class getBeanClass(Element element);

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

        // grab attributes off of the plugin element
        builder.addPropertyValue("id", element.getAttribute(ID_ATTRIBUTE));
        builder.addPropertyValue("propagateErrors", element.getAttribute(PROPAGATE_ERRORS_ATTRIBUTE_NAME));

        // parse child elements
        List<Element> elements;

        elements = XMLHelper.getChildElementsByTagNameNS(element, AttributeResolverNamespaceHandler.NAMESPACE,
                ATTRIBUTE_DEFINITION_DEPENDENCY_ELEMENT_LOCAL_NAME);
        if (elements != null && elements.size() > 0) {
            builder.addPropertyValue("attributeDefinitionDependencyIds", parseDependencies(elements));
        }

        elements = XMLHelper.getChildElementsByTagNameNS(element, AttributeResolverNamespaceHandler.NAMESPACE,
                DATA_CONNECTOR_DEPENDENCY_ELEMENT_LOCAL_NAME);
        if (elements != null && elements.size() > 0) {
            builder.addPropertyValue("dataConnectorDependencyIds", parseDependencies(elements));
        }
    }

    /**
     * Parse dependency elements.
     * 
     * @param elements DOM elements of type <code>resolver:PluginDependencyType</code>
     * @return the dependency IDs
     */
    protected Set<String> parseDependencies(List<Element> elements) {
        Set<String> dependencyIds = new HashSet<String>();

        for (Element dependency : elements) {
            dependencyIds.add(dependency.getAttribute("ref"));
        }

        return dependencyIds;
    }

}