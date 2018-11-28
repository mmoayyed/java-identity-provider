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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.idp.attribute.resolver.ad.impl.ContextDerivedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ad.impl.IdPAttributePrincipalValuesFunction;
import net.shibboleth.idp.attribute.resolver.ad.impl.SubjectDerivedAttributeValuesFunction;
import net.shibboleth.idp.attribute.resolver.spring.ad.BaseAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** Spring Bean Definition Parser for attribute definitions derived from the Principal. */
public class SubjectDerivedAttributeDefinitionParser extends BaseAttributeDefinitionParser {

    /** Schema type name. */
    @Nonnull public static final QName TYPE_NAME_RESOLVER =
            new QName(AttributeResolverNamespaceHandler.NAMESPACE, "SubjectDerivedAttribute");

    /** log. */
    private final Logger log = LoggerFactory.getLogger(SubjectDerivedAttributeDefinitionParser.class);

    /** {@inheritDoc} */
    @Override protected Class<ContextDerivedAttributeDefinition> getBeanClass(final Element element) {
        return ContextDerivedAttributeDefinition.class;
    }

    /**
     * {@inheritDoc}.
     * 
     * We inject an inferred {@link SubjectDerivedAttributeValuesFunction}.<br/>
     * If 'principalAttributeName' we also inject an inferred {@link IdPAttributePrincipalValuesFunction} If
     * 'attributeValueFunctionRef' the user has provided the function The
     * {@link ContextDerivedAttributeDefinitionParser} does the case when the user injects the top level function
     */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);
        final String attributeName = StringSupport.trimOrNull(config.getAttributeNS(null, "principalAttributeName"));
        final String functionRef = StringSupport.trimOrNull(config.getAttributeNS(null, "attributeValuesFunctionRef"));

        final BeanDefinitionBuilder contextFunctionBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(SubjectDerivedAttributeValuesFunction.class);
        contextFunctionBuilder.addPropertyValue("id", getDefinitionId());

        if (null != attributeName) {
            if (null != functionRef) {
                log.warn("{} only one of \"principalAttributeName\" or \"attributeValuesFunctionRef\""
                        + " should be provided. \"attributeValuesFunctionRef\" ignored", getLogPrefix());
            }
            final BeanDefinitionBuilder principalValuesFunctionBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(IdPAttributePrincipalValuesFunction.class);
            principalValuesFunctionBuilder.addPropertyValue("attributeName", attributeName);
            contextFunctionBuilder.addPropertyValue("attributeValuesFunction",
                    principalValuesFunctionBuilder.getBeanDefinition());
        } else if (null != functionRef) {
            contextFunctionBuilder.addPropertyReference("attributeValuesFunction", functionRef);
        } else {
            log.error("{} one of \"principalAttributeName\" or \"attributeValuesFunctionRef\" should be supplied."
                    + " should be provided.", getLogPrefix());
            throw new BeanCreationException("Misconfigured PrincipalDerivedAttribute.");
        }
        builder.addPropertyValue("attributeValuesFunction", contextFunctionBuilder.getBeanDefinition());
    }

    /** {@inheritDoc} */
    @Override protected boolean failOnDependencies() {
        return true;
    }
}