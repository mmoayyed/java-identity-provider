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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.idp.attribute.resolver.ad.impl.SimpleAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;

/** Bean definition parser for a {@link SimpleAttributeDefinition}. */
public class SimpleAttributeDefinitionParser extends AbstractWarningAttributeDefinitionParser {

    /** Schema type names - ad: (legacy). */
    @Nonnull public static final QName TYPE_NAME_AD =
            new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "Simple");

    /** Schema type names - ad: (legacy). */
    @Nonnull public static final QName TYPE_NAME_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "Simple");

    /** {@inheritDoc} */
    @Override protected Class<SimpleAttributeDefinition> getBeanClass(@Nullable final Element element) {
        return SimpleAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);
    }

    /** {@inheritDoc} */
    @Override protected boolean needsAttributeSourceID() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected QName getPreferredName() {
        return TYPE_NAME_RESOLVER;
    }

}