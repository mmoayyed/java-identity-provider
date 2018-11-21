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

import net.shibboleth.idp.attribute.resolver.ad.impl.PrincipalAuthenticationMethodAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Spring Bean Definition Parser for PrincipalAuthenticationMethod. */
public class PrincipalAuthenticationMethodAttributeDefinitionParser extends AbstractWarningAttributeDefinitionParser {

    /** Schema type name ad: (legacy). */
    @Nonnull public static final QName TYPE_NAME_AD =
            new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "PrincipalAuthenticationMethod");

    /** Schema type name resolver:. */
    @Nonnull public static final QName TYPE_NAME_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "PrincipalAuthenticationMethod");

    /** {@inheritDoc} */
    @Override protected Class<PrincipalAuthenticationMethodAttributeDefinition> getBeanClass(final Element element) {
        return PrincipalAuthenticationMethodAttributeDefinition.class;
    }
    
    /** {@inheritDoc} */
    @Override @Nonnull protected QName getPreferredName() {
        return TYPE_NAME_RESOLVER;
    }

    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        DeprecationSupport.warnOnce(ObjectType.ELEMENT, "PrincipalAuthenticationMethod",
                parserContext.getReaderContext().getResource().getDescription(), null);
        super.doParse(config, parserContext, builder);
    }

}