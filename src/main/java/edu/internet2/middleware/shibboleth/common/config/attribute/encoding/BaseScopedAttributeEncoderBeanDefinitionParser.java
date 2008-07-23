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

package edu.internet2.middleware.shibboleth.common.config.attribute.encoding;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Base class for Spring bean definition parser for Shibboleth scoped attribute encoders.
 */
public abstract class BaseScopedAttributeEncoderBeanDefinitionParser extends BaseAttributeEncoderBeanDefinitionParser {

    /** Local name of scope type attribute. */
    public static final String SCOPE_TYPE_ATTRIBUTE_NAME = "scopeType";

    /** Local name of scope delimiter attribute. */
    public static final String SCOPE_DELIMITER_ATTRIBUTE_NAME = "scopeDelimiter";

    /** Local name of scope attribute attribute. */
    public static final String SCOPE_ATTRIBUTE_ATTRIBUTE_NAME = "scopeAttribute";

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        if (element.hasAttributeNS(null, SCOPE_DELIMITER_ATTRIBUTE_NAME)) {
            builder.addPropertyValue("scopeDelimiter", element.getAttributeNS(null, SCOPE_DELIMITER_ATTRIBUTE_NAME));
        } else {
            builder.addPropertyValue("scopeDelimiter", "@");
        }

        if (element.hasAttributeNS(null, SCOPE_ATTRIBUTE_ATTRIBUTE_NAME)) {
            builder.addPropertyValue("scopeAttribute", element.getAttributeNS(null, SCOPE_ATTRIBUTE_ATTRIBUTE_NAME));
        } else {
            builder.addPropertyValue("scopeAttribute", "Scope");
        }
    }

}
