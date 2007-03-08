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
import org.springframework.util.Assert;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionPlugIn;

/**
 * Base class for Spring bean definition parser for Shibboleth resovler plug-ins.
 */
public abstract class AbstractResolutionPlugInBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /**
     * Determine the class of the factory bean corresponding to the supplied {@link Element}.
     * 
     * @param element the <code>Element</code> that is being parsed
     * @return the {@link Class} of the bean that is being defined via parsing the supplied <code>Element</code>
     */
    protected abstract Class getFactoryBeanClass(Element element);

    /**
     * Determine the bean class corresponding to the supplied {@link Element}.
     * 
     * @param element the <code>Element</code> that is being parsed
     * @return the {@link Class} of the bean that is being defined via parsing the supplied <code>Element</code>
     */
    protected abstract Class<? extends ResolutionPlugIn> getInternalBeanClass(Element element); 

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return getFactoryBeanClass(element);
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder factory) {
        Class internalBeanClass = getInternalBeanClass(element);
        Assert.state(internalBeanClass != null, "Class returned from getInternalBeanClass(Element) must not be null");
        BeanDefinitionBuilder pluginBuilder = BeanDefinitionBuilder.rootBeanDefinition(internalBeanClass);

        doParse(element, parserContext, factory, pluginBuilder);
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

    /**
     * TODO
     * 
     * @param element
     * @param parserContext
     * @param factory
     * @param pluginBuilder
     */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder factory,
            BeanDefinitionBuilder pluginBuilder) {
        
        // grab attributes off of the plugin element
        pluginBuilder.addPropertyValue("id", element.getAttribute("id"));
        pluginBuilder.addPropertyValue("propagateErrors", element.getAttribute("propagateErrors"));

        // parse child elements
        NodeList elements;

        elements = element.getElementsByTagNameNS(ResolverNamespaceHandler.NAMESPACE, "AttributeDefinitionDependency");
        if (elements != null && elements.getLength() > 0) {
            factory.addPropertyValue("attributeDefinitionDependencyIds", parseDependencies(elements));
        }

        elements = element.getElementsByTagNameNS(ResolverNamespaceHandler.NAMESPACE, "DataConnectorDependency");
        if (elements != null && elements.getLength() > 0) {
            factory.addPropertyValue("dataConnectorDependencyIds", parseDependencies(elements));
        }

        factory.addPropertyValue("plugin", pluginBuilder.getBeanDefinition());
    }
}