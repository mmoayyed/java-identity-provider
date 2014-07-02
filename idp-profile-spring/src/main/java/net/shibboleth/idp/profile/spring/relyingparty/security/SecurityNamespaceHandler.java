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

package net.shibboleth.idp.profile.spring.relyingparty.security;

import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.util.BaseSpringNamespaceHandler;
import net.shibboleth.idp.profile.spring.relyingparty.security.credential.BasicFilesystemCredentialParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.credential.BasicInlineCredentialParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.credential.ResourceCredentialParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.credential.X509FilesystemCredentialParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.credential.X509InlineCredentialParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.trustengine.PKIXResourceBackedValidationInfoParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.trustengine.SignatureChainingParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.trustengine.StaticExplicitKeySignatureParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.trustengine.StaticPKIXSignatureParser;
import net.shibboleth.idp.profile.spring.relyingparty.security.trustengine.UnsupportedTrustEngineParser;

/** Namespace handler <em>{@value NAMESPACE}</em>. */
public class SecurityNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:security";

    /** Credential element name. */
    public static final QName CREDENTIAL_ELEMENT_NAME = new QName(NAMESPACE, "Credential");

    /** TrustEngine element name. */
    public static final QName TRUST_ENGINE_ELEMENT_NAME = new QName(NAMESPACE, "TrustEngine");

    /** SecurityPolicy element name. */
    public static final QName SECRURITY_POLICY_NAME = new QName(NAMESPACE, "SecurityPolicy");

    /** {@inheritDoc} */
    @Override public void init() {
        registerBeanDefinitionParser(X509FilesystemCredentialParser.ELEMENT_NAME, new X509FilesystemCredentialParser());
        registerBeanDefinitionParser(X509InlineCredentialParser.ELEMENT_NAME, new X509InlineCredentialParser());
        registerBeanDefinitionParser(BasicInlineCredentialParser.ELEMENT_NAME, new BasicInlineCredentialParser());
        registerBeanDefinitionParser(BasicFilesystemCredentialParser.ELEMENT_NAME,
                new BasicFilesystemCredentialParser());

        registerBeanDefinitionParser(StaticExplicitKeySignatureParser.ELEMENT_NAME,
                new StaticExplicitKeySignatureParser());
        registerBeanDefinitionParser(StaticPKIXSignatureParser.ELEMENT_NAME, new StaticPKIXSignatureParser());
        registerBeanDefinitionParser(SignatureChainingParser.ELEMENT_NAME, new SignatureChainingParser());

        // Metadata based unsupported
        registerBeanDefinitionParser(UnsupportedTrustEngineParser.METADATA_EXPLICIT_KEY,
                new UnsupportedTrustEngineParser());
        registerBeanDefinitionParser(UnsupportedTrustEngineParser.METADATA_EXPLICIT_KEY_SIGNATURE,
                new UnsupportedTrustEngineParser());
        registerBeanDefinitionParser(UnsupportedTrustEngineParser.METADATA_PKIX_CREDENTIAL,
                new UnsupportedTrustEngineParser());
        registerBeanDefinitionParser(UnsupportedTrustEngineParser.METADATA_PKIX_SIGNATURE,
                new UnsupportedTrustEngineParser());

        // Credential unsupported
        registerBeanDefinitionParser(UnsupportedTrustEngineParser.CHAINING, new UnsupportedTrustEngineParser());
        registerBeanDefinitionParser(UnsupportedTrustEngineParser.PKIX_CREDENTIAL, new UnsupportedTrustEngineParser());

        // Resource backed anything
        registerBeanDefinitionParser(ResourceCredentialParser.X509_RESOURCE_ELEMENT_NAME,
                new ResourceCredentialParser());
        registerBeanDefinitionParser(ResourceCredentialParser.BASIC_RESOURCE_ELEMENT_NAME,
                new ResourceCredentialParser());
        registerBeanDefinitionParser(PKIXResourceBackedValidationInfoParser.ELEMENT_NAME,
                new PKIXResourceBackedValidationInfoParser());
        
    }
}