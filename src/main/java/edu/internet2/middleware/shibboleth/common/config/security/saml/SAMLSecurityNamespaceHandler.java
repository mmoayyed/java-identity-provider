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

package edu.internet2.middleware.shibboleth.common.config.security.saml;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/**
 * Spring namespace handler for SAML security objects.
 */
public class SAMLSecurityNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for SAML security elements. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:security:saml";

    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser(SAMLProtocolMessageXMLSignatureSecurityPolicyBeanDefinitionParser.SCHEMA_TYPE,
                new SAMLProtocolMessageXMLSignatureSecurityPolicyBeanDefinitionParser());

        registerBeanDefinitionParser(IssueInstantRuleBeanDefinitionParser.SCHEMA_TYPE,
                new IssueInstantRuleBeanDefinitionParser());

        registerBeanDefinitionParser(MessageReplayRuleBeanDefinitionParser.SCHEMA_TYPE,
                new MessageReplayRuleBeanDefinitionParser());

        registerBeanDefinitionParser(MandatoryIssuerRuleBeanDefinitionParser.SCHEMA_TYPE,
                new MandatoryIssuerRuleBeanDefinitionParser());

        registerBeanDefinitionParser(SAML2HTTPPostSimpleSignRuleBeanDefinitionParser.SCHEMA_TYPE,
                new SAML2HTTPPostSimpleSignRuleBeanDefinitionParser());

        registerBeanDefinitionParser(SAML2HTTPRedirectDeflateSignatureRuleBeanDefinitionParser.SCHEMA_TYPE,
                new SAML2HTTPRedirectDeflateSignatureRuleBeanDefinitionParser());
    }
}