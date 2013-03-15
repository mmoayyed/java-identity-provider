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

package net.shibboleth.idp.spring.service;

import javax.xml.namespace.QName;

import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

// TODO incomplete port from v2
/** Bean definition parser for services root element. */
public class ServicesBeanDefinitionParser implements BeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(ServiceNamespaceHandler.NAMESPACE, "Services");

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(ServiceNamespaceHandler.NAMESPACE, "ServicesType");

    /** {@inheritDoc} */
    public BeanDefinition parse(Element config, ParserContext context) {
        SpringSupport.parseCustomElements(
                ElementSupport.getChildElements(config, AbstractServiceBeanDefinitionParser.ELEMENT_NAME), context);
        return null;
    }
}