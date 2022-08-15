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

package net.shibboleth.idp.attribute.resolver.spring.ad.impl;

import java.time.format.DateTimeFormatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.resolver.ad.impl.DateTimeAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.ad.BaseAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;

/** Bean definition parser for a {@link DateTimeAttributeDefinition}. */
public class DateTimeAttributeDefinitionParser extends BaseAttributeDefinitionParser {

    /** Schema type name. */
    @Nonnull public static final QName TYPE_NAME_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "DateTime");

    /** {@inheritDoc} */
    @Override protected Class<DateTimeAttributeDefinition> getBeanClass(@Nullable final Element element) {
        return DateTimeAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        if (config.hasAttributeNS(null, "ignoreConversionErrors")) {
            builder.addPropertyValue("ignoreConversionErrors",
                    SpringSupport.getStringValueAsBoolean(config.getAttributeNS(null, "ignoreConversionErrors")));
        }
        
        if (config.hasAttributeNS(null, "epochInSeconds")) {
            builder.addPropertyValue("epochInSeconds",
                    SpringSupport.getStringValueAsBoolean(config.getAttributeNS(null, "epochInSeconds")));
        }
        
        if (config.hasAttributeNS(null, "formattingString")) {
            final BeanDefinitionBuilder formatterBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(DateTimeFormatter.class);
            formatterBuilder.setFactoryMethod("ofPattern");
            formatterBuilder.addConstructorArgValue(config.getAttributeNS(null, "formattingString"));
            builder.addPropertyValue("dateTimeFormatter", formatterBuilder.getBeanDefinition());
        }
    }

}