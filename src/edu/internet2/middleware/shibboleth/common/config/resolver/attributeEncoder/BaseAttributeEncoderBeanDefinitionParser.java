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

package edu.internet2.middleware.shibboleth.common.config.resolver.attributeEncoder;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Base class for Spring bean definition parser for Shibboleth attribute encoders.
 */
public abstract class BaseAttributeEncoderBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {


    /** Local name of attribute name attribute. */
    public static final String ATTRIBUTE_NAME_ATTRIBUTE_NAME = "name";
    
    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        builder.addPropertyValue("attributeName", element.getAttribute(ATTRIBUTE_NAME_ATTRIBUTE_NAME));
    }
    
    /** {@inheritDoc} */
    public boolean shouldGenerateId() {
        return true;
    }
}
