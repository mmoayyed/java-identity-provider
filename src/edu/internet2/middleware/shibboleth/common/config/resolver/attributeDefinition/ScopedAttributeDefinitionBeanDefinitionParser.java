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

package edu.internet2.middleware.shibboleth.common.config.resolver.attributeDefinition;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring Bean Definition Parser for static data connector.
 */
public class ScopedAttributeDefinitionBeanDefinitionParser extends BaseAttributeDefinitionBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName("urn:mace:shibboleth:2.0:resolver:ad:simple", "Scoped");

    /** Scope attribute name. */
    public static final String SCOPE_ATTRIBUTE_NAME = "scope";
    
    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return ScopedAttributeDefinitionFactoryBean.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);
        
        builder.addPropertyValue("scope", element.getAttribute(SCOPE_ATTRIBUTE_NAME));
    }
    
    
}