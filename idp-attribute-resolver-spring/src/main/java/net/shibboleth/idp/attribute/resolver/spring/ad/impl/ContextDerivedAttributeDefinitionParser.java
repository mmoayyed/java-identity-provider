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
import javax.xml.namespace.QName;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.idp.attribute.resolver.ad.impl.ContextDerivedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** Spring Bean Definition Parser for attribute definitions derived from the Principal. */
public class ContextDerivedAttributeDefinitionParser extends AbstractWarningAttributeDefinitionParser {

    /** Schema type name - ad: (legacy). */
    @Nonnull public static final QName TYPE_NAME_AD =
            new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "ContextDerivedAttribute");

    /** Schema type name - resolver: . */
    @Nonnull public static final QName TYPE_NAME_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "ContextDerivedAttribute");

    /** {@inheritDoc} */
    @Override protected Class<ContextDerivedAttributeDefinition> getBeanClass(final Element element) {
        return ContextDerivedAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);
        final String functionRef = StringSupport.trimOrNull(config.getAttributeNS(null, "attributeValuesFunctionRef"));

        if (null == functionRef) {
            throw new BeanCreationException(getLogPrefix() + "requires 'attributeValuesFunctionRef'");
        } else {
            builder.addPropertyReference("attributeValuesFunction", functionRef);
        }
    }

    /** {@inheritDoc}. No input. */
    @Override protected boolean needsAttributeSourceID() {
        return false;
    }

    /** {@inheritDoc} */
    @Override protected boolean failOnDependencies() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected QName getPreferredName() {
        return TYPE_NAME_RESOLVER;
    }

}