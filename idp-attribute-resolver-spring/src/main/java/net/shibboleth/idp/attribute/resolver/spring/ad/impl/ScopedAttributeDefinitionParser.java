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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.idp.attribute.resolver.ad.impl.ScopedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.ad.BaseAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Spring Bean Definition Parser for scoped attribute definitions.
 */
public class ScopedAttributeDefinitionParser extends BaseAttributeDefinitionParser {

    /** Schema type name. */
    @Nonnull public static final QName TYPE_NAME_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "Scoped");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ScopedAttributeDefinitionParser.class);

    /** {@inheritDoc} */
    @Override protected Class<ScopedAttributeDefinition> getBeanClass(@Nullable final Element element) {
        return ScopedAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        final String scope = StringSupport.trimOrNull(config.getAttributeNS(null, "scope"));
        if (scope != null) {
            log.debug("{} Setting scope to '{}'.", getLogPrefix(), scope);
            builder.addPropertyValue("scope", scope);
        }
        
        final String scopeSource = StringSupport.trimOrNull(config.getAttributeNS(null, "scopeFromDependency"));
        if (scopeSource != null) {
            log.debug("{} Setting scope source to '{}'.", getLogPrefix(), scopeSource);
            builder.addPropertyValue("scopeSource", scopeSource);
        }
    }
}