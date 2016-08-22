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

import net.shibboleth.ext.spring.util.BaseSpringNamespaceHandler;
import net.shibboleth.idp.attribute.resolver.spring.ad.mapped.impl.MappedAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.mapped.impl.SourceValueParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.mapped.impl.ValueMapParser;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * Namespace handler for {@link net.shibboleth.idp.attribute.resolver.AttributeDefinition}s.
 */
public class AttributeDefinitionNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    @Nonnull @NotEmpty public static final String NAMESPACE = "urn:mace:shibboleth:2.0:resolver:ad";

    /** {@inheritDoc} */
    @Override public void init() {
        registerBeanDefinitionParser(CryptoTransientIdAttributeDefinitionParser.TYPE_NAME_AD,
                new CryptoTransientIdAttributeDefinitionParser());
        registerBeanDefinitionParser(PrescopedAttributeDefinitionParser.TYPE_NAME_AD,
                new PrescopedAttributeDefinitionParser());
        registerBeanDefinitionParser(PrincipalAuthenticationMethodAttributeDefinitionParser.TYPE_NAME_AD,
                new PrincipalAuthenticationMethodAttributeDefinitionParser());
        registerBeanDefinitionParser(PrincipalNameAttributeDefinitionParser.TYPE_NAME_AD,
                new PrincipalNameAttributeDefinitionParser());
        registerBeanDefinitionParser(SubjectDerivedAttributeDefinitionParser.TYPE_NAME_AD,
                new SubjectDerivedAttributeDefinitionParser());
        registerBeanDefinitionParser(ContextDerivedAttributeDefinitionParser.TYPE_NAME_AD,
                new ContextDerivedAttributeDefinitionParser());
        registerBeanDefinitionParser(RegexSplitAttributeDefinitionParser.TYPE_NAME_AD,
                new RegexSplitAttributeDefinitionParser());
        registerBeanDefinitionParser(SAML1NameIdentifierAttributeDefinitionParser.TYPE_NAME_AD,
                new SAML1NameIdentifierAttributeDefinitionParser());
        registerBeanDefinitionParser(SAML2NameIDAttributeDefinitionParser.TYPE_NAME_AD,
                new SAML2NameIDAttributeDefinitionParser());
        registerBeanDefinitionParser(ScopedAttributeDefinitionParser.TYPE_NAME_AD,
                new ScopedAttributeDefinitionParser());
        registerBeanDefinitionParser(ScriptedAttributeDefinitionParser.TYPE_NAME_AD,
                new ScriptedAttributeDefinitionParser());
        registerBeanDefinitionParser(SimpleAttributeDefinitionParser.TYPE_NAME_AD,
                new SimpleAttributeDefinitionParser());
        registerBeanDefinitionParser(TemplateAttributeDefinitionParser.TYPE_NAME_AD,
                new TemplateAttributeDefinitionParser());
        registerBeanDefinitionParser(TransientIdAttributeDefinitionParser.TYPE_NAME_AD,
                new TransientIdAttributeDefinitionParser());
        registerBeanDefinitionParser(SourceValueParser.TYPE_NAME_AD, new SourceValueParser());
        registerBeanDefinitionParser(ValueMapParser.TYPE_NAME_AD, new ValueMapParser());
        registerBeanDefinitionParser(MappedAttributeDefinitionParser.TYPE_NAME_AD,
                new MappedAttributeDefinitionParser());
    }

}