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

package net.shibboleth.idp.spring.resource;

import javax.xml.namespace.QName;

import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.resource.ClasspathResource;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

//TODO incomplete port from v2
/** Bean definition parser for {@link ClasspathResource}s. */
public class ClasspathResourceBeanDefinitionParser extends AbstractResourceBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(ResourceNamespaceHandler.NAMESPACE, "ClasspathResource");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return ClasspathResource.class;
    }

    /** {@inheritDoc} */
    protected String
            resolveId(Element configElement, AbstractBeanDefinition beanDefinition, ParserContext parserContext) {
        return ClasspathResource.class.getName() + ":"
                + StringSupport.trimOrNull(configElement.getAttributeNS(null, "file"));

        // TODO remove later
        // return ClasspathResource.class.getName() + ":"
        // + DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null, "file"));
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

        builder.addConstructorArgValue(StringSupport.trimOrNull(element.getAttributeNS(null, "file")));

        // TODO remove later
        // builder.addConstructorArgValue(DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null, "file")));

        // TODO filtering
        // addResourceFilter(element, parserContext, builder);
    }
}