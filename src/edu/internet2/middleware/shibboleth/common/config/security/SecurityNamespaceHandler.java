/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.config.security;

import org.springframework.beans.factory.xml.BeanDefinitionParser;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/**
 * Spring namespace handler for Shibboleth security objects.
 */
public class SecurityNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Security configuration namespace. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:security";

    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser(FilesystemX509CredentialBeanDefinitionParser.SCHEMA_TYPE,
                new FilesystemX509CredentialBeanDefinitionParser());

        registerBeanDefinitionParser(InlineX509CredentialBeanDefinitionParser.SCHEMA_TYPE,
                new InlineX509CredentialBeanDefinitionParser());

        registerBeanDefinitionParser(FilesystemBasicCredentialBeanDefinitionParser.SCHEMA_TYPE,
                new FilesystemBasicCredentialBeanDefinitionParser());

        registerBeanDefinitionParser(InlineBasicCredentialBeanDefinitionParser.SCHEMA_TYPE,
                new InlineBasicCredentialBeanDefinitionParser());

        registerBeanDefinitionParser(FilesystemPKIXValidationInformationBeanDefinitionParser.SCHEMA_TYPE,
                new FilesystemPKIXValidationInformationBeanDefinitionParser());

        registerBeanDefinitionParser(InlinePKIXValidationInformationBeanDefinitionParser.SCHEMA_TYPE,
                new InlinePKIXValidationInformationBeanDefinitionParser());

        BeanDefinitionParser parser = new ShibbolethSecurityPolicyBeanDefinitionParser();
        registerBeanDefinitionParser(ShibbolethSecurityPolicyBeanDefinitionParser.ELEMENT_NAME, parser);
        registerBeanDefinitionParser(ShibbolethSecurityPolicyBeanDefinitionParser.SCHEMA_TYPE, parser);

        registerBeanDefinitionParser(ChainingTrustEngineBeanDefinitionParser.SCHEMA_TYPE,
                new ChainingTrustEngineBeanDefinitionParser());

        registerBeanDefinitionParser(ChainingSignatureTrustEngineBeanDefinitionParser.SCHEMA_TYPE,
                new ChainingSignatureTrustEngineBeanDefinitionParser());

        registerBeanDefinitionParser(MetadataExplicitKeyTrustEngineBeanDefinitionParser.SCHEMA_TYPE,
                new MetadataExplicitKeyTrustEngineBeanDefinitionParser());

        registerBeanDefinitionParser(MetadataPKIXX509CredentialTrustEngineBeanDefinitionParser.SCHEMA_TYPE,
                new MetadataPKIXX509CredentialTrustEngineBeanDefinitionParser());

        registerBeanDefinitionParser(MetadataExplicitKeySignatureTrustEngineBeanDefinitionParser.SCHEMA_TYPE,
                new MetadataExplicitKeySignatureTrustEngineBeanDefinitionParser());

        registerBeanDefinitionParser(MetadataPKIXSignatureTrustEngineBeanDefinitionParser.SCHEMA_TYPE,
                new MetadataPKIXSignatureTrustEngineBeanDefinitionParser());

        registerBeanDefinitionParser(StaticExplicitKeyTrustEngineBeanDefinitionParser.SCHEMA_TYPE,
                new StaticExplicitKeyTrustEngineBeanDefinitionParser());

        registerBeanDefinitionParser(StaticExplicitKeySignatureTrustEngineBeanDefinitionParser.SCHEMA_TYPE,
                new StaticExplicitKeySignatureTrustEngineBeanDefinitionParser());

        registerBeanDefinitionParser(StaticPKIXX509CredentialTrustEngineBeanDefinitionParser.SCHEMA_TYPE,
                new StaticPKIXX509CredentialTrustEngineBeanDefinitionParser());

        registerBeanDefinitionParser(StaticPKIXSignatureTrustEngineBeanDefinitionParser.SCHEMA_TYPE,
                new StaticPKIXSignatureTrustEngineBeanDefinitionParser());

        registerBeanDefinitionParser(ClientCertAuthRuleBeanDefinitionParser.SCHEMA_TYPE,
                new ClientCertAuthRuleBeanDefinitionParser());

        registerBeanDefinitionParser(MandatoryMessageAuthenticationRuleBeanDefinitionParser.SCHEMA_TYPE,
                new MandatoryMessageAuthenticationRuleBeanDefinitionParser());
    }

}