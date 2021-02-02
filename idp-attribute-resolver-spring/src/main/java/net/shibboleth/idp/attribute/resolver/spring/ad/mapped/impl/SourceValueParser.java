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

package net.shibboleth.idp.attribute.resolver.spring.ad.mapped.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.ext.spring.util.AbstractCustomBeanDefinitionParser;
import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.resolver.ad.mapped.impl.SourceValue;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** Bean definition parser for a {@link SourceValue}. */
public class SourceValueParser extends AbstractCustomBeanDefinitionParser {

    /** Schema type name. */
    @Nonnull public static final QName TYPE_NAME_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "SourceValue");

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(SourceValueParser.class);

    /** {@inheritDoc} */
    @Override protected Class<SourceValue> getBeanClass(@Nullable final Element element) {
        return SourceValue.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        builder.setInitMethodName("initialize");
        builder.setDestroyMethodName("destroy");
        
        final String value = config.getTextContent();
        builder.addPropertyValue("value", value);

        String caseSensitive = null;
        if (config.hasAttributeNS(null, "caseSensitive")) {
            caseSensitive = StringSupport.trimOrNull(config.getAttributeNS(null, "caseSensitive"));
            builder.addPropertyValue("caseSensitive", SpringSupport.getStringValueAsBoolean(caseSensitive));
            if (config.hasAttributeNS(null, "ignoreCase")) {
                log.warn("{}: Both \"caseSensitive\" and \"ignoreCase\" specified, only the former will be used",
                        parserContext.getReaderContext().getResource().getDescription());
            }
        } else if (config.hasAttributeNS(null, "ignoreCase")) {
            DeprecationSupport.warnOnce(ObjectType.ELEMENT,
                    "ignoreCase",
                    parserContext.getReaderContext().getResource().getDescription(),
                    "caseSensitive");
            builder.addPropertyValue("ignoreCase", SpringSupport.getStringValueAsBoolean(
                    config.getAttributeNS(null, "ignoreCase")));
        }

        String partialMatch = null;
        if (config.hasAttributeNS(null, "partialMatch")) {
            partialMatch = StringSupport.trimOrNull(config.getAttributeNS(null, "partialMatch"));
            builder.addPropertyValue("partialMatch", SpringSupport.getStringValueAsBoolean(partialMatch));
        }

        log.debug("SourceValue value: {}, caseSensitive: {}, partialMatch: {}", value, caseSensitive, partialMatch);

    }

    /** {@inheritDoc} */
    @Override protected boolean shouldGenerateId() {
        return true;
    }

}