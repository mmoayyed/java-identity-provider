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

import net.shibboleth.idp.attribute.resolver.ad.impl.IdPAttributePrincipalValueEngine;
import net.shibboleth.idp.attribute.resolver.ad.impl.PrincipalDerivedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.ad.BaseAttributeDefinitionParser;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Spring Bean Definition Parser for attribute definitions derived from the Principal. */
public class PrincipalDerivedAttributeAttributeDefinitionParser extends BaseAttributeDefinitionParser {

    /** Schema type name. */
    @Nonnull public static final QName TYPE_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE,
            "PrincipalDerivedAttribute");

    /** log. */
    private final Logger log = LoggerFactory.getLogger(PrincipalDerivedAttributeAttributeDefinitionParser.class);

    /** {@inheritDoc} */
    @Override protected Class<PrincipalDerivedAttributeDefinition> getBeanClass(final Element element) {
        return PrincipalDerivedAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);
        final String attributeName = StringSupport.trimOrNull(config.getAttributeNS(null, "principalAttributeName"));
        final String engineRef = StringSupport.trimOrNull(config.getAttributeNS(null, "attributeValueEngineRef"));

        if (null != attributeName) {
            if (null != engineRef) {
                log.warn("{} only one of \"principalAttributeName\" or \"attributeValueEngineRef\""
                        + " should be provided. \"attributeValueEngineRef\" ignored", getLogPrefix());
            }
            final BeanDefinitionBuilder engine =
                    BeanDefinitionBuilder.genericBeanDefinition(IdPAttributePrincipalValueEngine.class);
            engine.addConstructorArgValue(attributeName);
            builder.addPropertyValue("attributeValueEngine", engine.getBeanDefinition());
        } else if (null != engineRef) {
            builder.addPropertyReference("attributeValueEngine", engineRef);
        } else {
            log.error("{} one of \"principalAttributeName\" or \"attributeValueEngineRef\" should be supplied."
                        + " should be provided.", getLogPrefix());
            throw new BeanCreationException("Misconfigured PrincipalDerivedAttribute.");
        }
    }

    /** {@inheritDoc}. No input. */
    @Override protected boolean needsAttributeSourceID() {
        return false;
    }

}