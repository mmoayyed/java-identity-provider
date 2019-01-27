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

package net.shibboleth.idp.attribute.resolver.spring.impl;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.xml.BeanDefinitionParser;

import net.shibboleth.ext.spring.util.BaseSpringNamespaceHandler;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.ContextDerivedAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.CryptoTransientIdAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.PrescopedAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.PrincipalAuthenticationMethodAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.PrincipalNameAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.RegexSplitAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.SAML1NameIdentifierAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.SAML2NameIDAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.ScopedAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.ScriptedAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.SimpleAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.SubjectDerivedAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.TemplateAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.TransientIdAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.mapped.impl.MappedAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.mapped.impl.SourceValueParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.mapped.impl.ValueMapParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.http.impl.HTTPDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.ComputedIDDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.ScriptDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.StaticDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.StoredIDDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.ldap.impl.LDAPDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.dc.rdbms.impl.RDBMSDataConnectorParser;
import net.shibboleth.idp.attribute.resolver.spring.enc.impl.SAML1Base64AttributeEncoderParser;
import net.shibboleth.idp.attribute.resolver.spring.enc.impl.SAML1ScopedStringAttributeEncoderParser;
import net.shibboleth.idp.attribute.resolver.spring.enc.impl.SAML1StringAttributeEncoderParser;
import net.shibboleth.idp.attribute.resolver.spring.enc.impl.SAML1StringNameIdentifierEncoderParser;
import net.shibboleth.idp.attribute.resolver.spring.enc.impl.SAML1XMLObjectAttributeEncoderParser;
import net.shibboleth.idp.attribute.resolver.spring.enc.impl.SAML2Base64AttributeEncoderParser;
import net.shibboleth.idp.attribute.resolver.spring.enc.impl.SAML2ScopedStringAttributeEncoderParser;
import net.shibboleth.idp.attribute.resolver.spring.enc.impl.SAML2StringAttributeEncoderParser;
import net.shibboleth.idp.attribute.resolver.spring.enc.impl.SAML2StringNameIDEncoderParser;
import net.shibboleth.idp.attribute.resolver.spring.enc.impl.SAML2XMLObjectAttributeEncoderParser;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/** Namespace handler for the attribute resolver. */
@SuppressWarnings("deprecation")
public class AttributeResolverNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    @Nonnull @NotEmpty public static final String NAMESPACE = "urn:mace:shibboleth:2.0:resolver";

    /** {@inheritDoc} */
    // Checkstyle: MethodLength OFF
    @Override public void init() {
        final BeanDefinitionParser parser = new AttributeResolverParser();

        registerBeanDefinitionParser(AttributeResolverParser.SCHEMA_TYPE, parser);
        registerBeanDefinitionParser(AttributeResolverParser.ELEMENT_NAME, parser);
        registerBeanDefinitionParser(InputDataConnectorParser.ELEMENT_NAME, new InputDataConnectorParser());
        registerBeanDefinitionParser(InputAttributeDefinitionParser.ELEMENT_NAME, new InputAttributeDefinitionParser());

        // Attribute Resolvers
        registerBeanDefinitionParser(CryptoTransientIdAttributeDefinitionParser.TYPE_NAME_RESOLVER,
                new CryptoTransientIdAttributeDefinitionParser());
        registerBeanDefinitionParser(PrescopedAttributeDefinitionParser.TYPE_NAME_RESOLVER,
                new PrescopedAttributeDefinitionParser());
        registerBeanDefinitionParser(PrincipalAuthenticationMethodAttributeDefinitionParser.TYPE_NAME_RESOLVER,
                new PrincipalAuthenticationMethodAttributeDefinitionParser());
        registerBeanDefinitionParser(PrincipalNameAttributeDefinitionParser.TYPE_NAME_RESOLVER,
                new PrincipalNameAttributeDefinitionParser());
        registerBeanDefinitionParser(RegexSplitAttributeDefinitionParser.TYPE_NAME_RESOLVER,
                new RegexSplitAttributeDefinitionParser());

        registerBeanDefinitionParser(SubjectDerivedAttributeDefinitionParser.TYPE_NAME_RESOLVER,
                new SubjectDerivedAttributeDefinitionParser());
        registerBeanDefinitionParser(ContextDerivedAttributeDefinitionParser.TYPE_NAME_RESOLVER,
                new ContextDerivedAttributeDefinitionParser());
        registerBeanDefinitionParser(SAML1NameIdentifierAttributeDefinitionParser.TYPE_NAME_RESOLVER,
                new SAML1NameIdentifierAttributeDefinitionParser());
        registerBeanDefinitionParser(SAML2NameIDAttributeDefinitionParser.TYPE_NAME_RESOLVER,
                new SAML2NameIDAttributeDefinitionParser());
        registerBeanDefinitionParser(ScopedAttributeDefinitionParser.TYPE_NAME_RESOLVER,
                new ScopedAttributeDefinitionParser());
        registerBeanDefinitionParser(ScriptedAttributeDefinitionParser.TYPE_NAME_RESOLVER,
                new ScriptedAttributeDefinitionParser());
        registerBeanDefinitionParser(SimpleAttributeDefinitionParser.TYPE_NAME_RESOLVER,
                new SimpleAttributeDefinitionParser());
        registerBeanDefinitionParser(TemplateAttributeDefinitionParser.TYPE_NAME_RESOLVER,
                new TemplateAttributeDefinitionParser());
        registerBeanDefinitionParser(TransientIdAttributeDefinitionParser.TYPE_NAME_RESOLVER,
                new TransientIdAttributeDefinitionParser());
        registerBeanDefinitionParser(SourceValueParser.TYPE_NAME_RESOLVER, new SourceValueParser());
        registerBeanDefinitionParser(ValueMapParser.TYPE_NAME_RESOLVER, new ValueMapParser());
        registerBeanDefinitionParser(MappedAttributeDefinitionParser.TYPE_NAME_RESOLVER,
                new MappedAttributeDefinitionParser());
        
        // Data Connectors
        registerBeanDefinitionParser(ComputedIDDataConnectorParser.TYPE_NAME_RESOLVER, 
                new ComputedIDDataConnectorParser());
        registerBeanDefinitionParser(RDBMSDataConnectorParser.TYPE_NAME_RESOLVER, new RDBMSDataConnectorParser());
        registerBeanDefinitionParser(LDAPDataConnectorParser.TYPE_NAME_RESOLVER, new LDAPDataConnectorParser());
        registerBeanDefinitionParser(HTTPDataConnectorParser.TYPE_NAME, new HTTPDataConnectorParser());
        registerBeanDefinitionParser(ScriptDataConnectorParser.TYPE_NAME_RESOLVER, new ScriptDataConnectorParser());
        registerBeanDefinitionParser(StaticDataConnectorParser.TYPE_NAME_RESOLVER, new StaticDataConnectorParser());
        registerBeanDefinitionParser(StoredIDDataConnectorParser.TYPE_NAME_RESOLVER, new StoredIDDataConnectorParser());


        // Encoders
        registerBeanDefinitionParser(SAML1StringAttributeEncoderParser.TYPE_NAME_RESOLVER,
                new SAML1StringAttributeEncoderParser());
        registerBeanDefinitionParser(SAML1Base64AttributeEncoderParser.TYPE_NAME_RESOLVER,
                new SAML1Base64AttributeEncoderParser());

        registerBeanDefinitionParser(SAML1ScopedStringAttributeEncoderParser.TYPE_NAME_RESOLVER,
                new SAML1ScopedStringAttributeEncoderParser());

        registerBeanDefinitionParser(SAML1StringNameIdentifierEncoderParser.TYPE_NAME_RESOLVER,
                new SAML1StringNameIdentifierEncoderParser());

        registerBeanDefinitionParser(SAML1XMLObjectAttributeEncoderParser.TYPE_NAME_RESOLVER,
                new SAML1XMLObjectAttributeEncoderParser());

        registerBeanDefinitionParser(SAML2Base64AttributeEncoderParser.TYPE_NAME_RESOLVER,
                new SAML2Base64AttributeEncoderParser());

        registerBeanDefinitionParser(SAML2ScopedStringAttributeEncoderParser.TYPE_NAME_RESOLVER,
                new SAML2ScopedStringAttributeEncoderParser());

        registerBeanDefinitionParser(SAML2StringAttributeEncoderParser.TYPE_NAME_RESOLVER,
                new SAML2StringAttributeEncoderParser());

        registerBeanDefinitionParser(SAML2StringNameIDEncoderParser.TYPE_NAME_RESOLVER,
                new SAML2StringNameIDEncoderParser());

        registerBeanDefinitionParser(SAML2XMLObjectAttributeEncoderParser.TYPE_NAME_RESOLVER,
                new SAML2XMLObjectAttributeEncoderParser());
    }
    // Checkstyle: MethodLength ON

}