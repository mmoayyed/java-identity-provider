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

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

// TODO incomplete port from v2
/**
 * Spring configuration parser for
 * {@link edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver} beans.
 */
public class AttributeResolverBeanDefinitionParser implements BeanDefinitionParser {

    private final Logger log = LoggerFactory.getLogger(AttributeResolverBeanDefinitionParser.class);

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(AttributeResolverNamespaceHandler.NAMESPACE,
            "AttributeResolverType");

    /** Element name. */
    public static final QName ELEMENT_NAME =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "AttributeResolver");

    /** {@inheritDoc} */
    public BeanDefinition parse(Element config, ParserContext context) {

        log.info("parse");

        // Map<QName, List<Element>> configChildren = XMLHelper.getChildElements(config);
        Map<QName, List<Element>> configChildren = ElementSupport.getIndexedChildElements(config);
        List<Element> children;

        children = configChildren.get(new QName(AttributeResolverNamespaceHandler.NAMESPACE, "PrincipalConnector"));
        // SpringConfigurationUtils.parseCustomElements(children, context);

        children = configChildren.get(new QName(AttributeResolverNamespaceHandler.NAMESPACE, "DataConnector"));
        // SpringConfigurationUtils.parseCustomElements(children, context);

        children = configChildren.get(new QName(AttributeResolverNamespaceHandler.NAMESPACE, "AttributeDefinition"));
        // SpringConfigurationUtils.parseCustomElements(children, context);
        SpringSupport.parseCustomElements(children, context);

        return null;
    }
}