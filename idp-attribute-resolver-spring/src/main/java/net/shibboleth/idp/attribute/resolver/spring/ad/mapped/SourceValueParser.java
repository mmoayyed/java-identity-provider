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

package net.shibboleth.idp.attribute.resolver.spring.ad.mapped;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.impl.ad.mapped.SourceValue;
import net.shibboleth.idp.attribute.resolver.spring.ad.AttributeDefinitionNamespaceHandler;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Bean definition parser for a {@link SourceValue}. */
public class SourceValueParser extends AbstractSingleBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "SourceValue");

    /** log. */
    private Logger log = LoggerFactory.getLogger(SourceValueParser.class);

    /** {@inheritDoc} */
    protected Class<SourceValue> getBeanClass(@Nullable final Element element) {
        return SourceValue.class;
    }

    /** {@inheritDoc} */
    protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
         super.doParse(config, parserContext, builder);

        boolean ignoreCase = false;

        if (config.hasAttributeNS(null, "ignoreCase")) {
            ignoreCase = AttributeSupport.getAttributeValueAsBoolean(config.getAttributeNodeNS(null, "ignoreCase"));
        }

        boolean partialMatch = false;
        if (config.hasAttributeNS(null, "partialMatch")) {
            partialMatch = AttributeSupport.getAttributeValueAsBoolean(config.getAttributeNodeNS(null, "partialMatch"));
        }

        final String value = config.getTextContent();

        log.debug("SourceValue value: {}, ignoreCase: {}, partialMatch: {}", new Object[] {value, ignoreCase,
                partialMatch,});

        builder.addConstructorArgValue(value);
        builder.addConstructorArgValue(ignoreCase);
        builder.addConstructorArgValue(partialMatch);
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}