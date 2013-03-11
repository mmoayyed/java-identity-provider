/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.resolver.spring;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * TODO Base class for Spring bean definition parser for Shibboleth resolver plug-ins.
 */
public abstract class BaseResolverPluginBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Name of resolution plug-in dependency. */
    public static final QName DEPENDENCY_ELEMENT_NAME = new QName(AttributeResolverNamespaceHandler.NAMESPACE,
            "Dependency");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseResolverPluginBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {

        String id = StringSupport.trimOrNull(config.getAttributeNS(null, "id"));
        log.info("Parsing configuration for {} plugin with ID : {}", config.getLocalName(), id);
        builder.addPropertyValue("id", id);

        // TODO maybe set all attrs besides xsi:type and xmlns ?
        NamedNodeMap attributes = config.getAttributes();
        for (int j = 0; j < attributes.getLength(); j++) {
            log.debug("Attribute '{}' = '{}'", attributes.item(j).getNodeName(), attributes.item(j).getNodeValue());
        }

        // TODO probably incorrect dependency handling
        List<Element> dependencyElements = ElementSupport.getChildElements(config, DEPENDENCY_ELEMENT_NAME);

        Set<ResolverPluginDependency> dependencies = new HashSet<ResolverPluginDependency>();

        for (Element dependencyElement : dependencyElements) {
            String ref = StringSupport.trimOrNull(dependencyElement.getAttributeNS(null, "ref"));
            log.info("Parsing configuration for {} plugin with ref : {}", config.getLocalName(), ref);
            // TODO dependent attr id ?
            ResolverPluginDependency dependency = new ResolverPluginDependency(ref, id);
            dependencies.add(dependency);
        }

        builder.addPropertyValue("dependencies", dependencies);
    }

    /** {@inheritDoc} */
    protected String resolveId(Element config, AbstractBeanDefinition definition, ParserContext parserContext) {
        return config.getAttributeNS(null, "id");
    }
}
