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

package edu.internet2.middleware.shibboleth.common.config.relyingparty.saml;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/**
 * Spring namespace handler for the Shibboleth relying party namespace.
 */
public class SAMLRelyingPartyNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:relying-party:saml";

    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser(SAMLRelyingPartyManagerBeanDefinitionParser.TYPE_NAME,
                new SAMLRelyingPartyManagerBeanDefinitionParser());

        registerBeanDefinitionParser(ShibbolethSSOProfileConfigurationBeanDefinitionParser.TYPE_NAME,
                new ShibbolethSSOProfileConfigurationBeanDefinitionParser());

        registerBeanDefinitionParser(SAML1AttributeQueryProfileConfigurationBeanDefinitionParser.TYPE_NAME,
                new SAML1AttributeQueryProfileConfigurationBeanDefinitionParser());

        registerBeanDefinitionParser(SAML1ArtifactQueryProfileConfigurationBeanDefinitionParser.TYPE_NAME,
                new SAML1ArtifactQueryProfileConfigurationBeanDefinitionParser());

        registerBeanDefinitionParser(SAML2SSOProfileConfigurationBeanDefinitionParser.TYPE_NAME,
                new SAML2SSOProfileConfigurationBeanDefinitionParser());

        registerBeanDefinitionParser(SAML2LogoutRequestProfileConfigurationBeanDefinitionParser.TYPE_NAME,
                new SAML2LogoutRequestProfileConfigurationBeanDefinitionParser());

        registerBeanDefinitionParser(SAML2AttributeQueryProfileConfigurationBeanDefinitionParser.TYPE_NAME,
                new SAML2AttributeQueryProfileConfigurationBeanDefinitionParser());

        registerBeanDefinitionParser(SAML2ArtifactResolutionProfileConfigurationBeanDefinitionParser.TYPE_NAME,
                new SAML2ArtifactResolutionProfileConfigurationBeanDefinitionParser());
    }
}