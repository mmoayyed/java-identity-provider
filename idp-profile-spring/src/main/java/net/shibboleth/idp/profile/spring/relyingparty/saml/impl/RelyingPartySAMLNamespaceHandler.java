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

package net.shibboleth.idp.profile.spring.relyingparty.saml.impl;

import net.shibboleth.ext.spring.util.BaseSpringNamespaceHandler;

/**
 * Namespace handler for the Relying Party SAML configuration. Just Types (since the Elements are
 * handled by the repective parsers).
 */
public class RelyingPartySAMLNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:relying-party:saml";

    /** {@inheritDoc} */
    @Override public void init() {
        // SAML2
        registerBeanDefinitionParser(SAML2ArtifactResolutionProfileParser.TYPE_NAME,
                new SAML2ArtifactResolutionProfileParser());
        registerBeanDefinitionParser(SAML2LogoutRequestProfileParser.TYPE_NAME,
                new SAML2LogoutRequestProfileParser());
        registerBeanDefinitionParser(SAML2AttributeQueryProfileParser.TYPE_NAME,
                new SAML2AttributeQueryProfileParser());
        registerBeanDefinitionParser(SAML2BrowserSSOProfileParser.TYPE_NAME, new SAML2BrowserSSOProfileParser());
        registerBeanDefinitionParser(SAML2ECPProfileParser.TYPE_NAME, new SAML2ECPProfileParser());
        registerBeanDefinitionParser(SAML2SSOSProfileParser.TYPE_NAME, new SAML2SSOSProfileParser());
        // SAML1
        registerBeanDefinitionParser(SAML1ArtifactResolutionProfileParser.TYPE_NAME,
                new SAML1ArtifactResolutionProfileParser());
        registerBeanDefinitionParser(SAML1AttributeQueryProfileParser.TYPE_NAME,
                new SAML1AttributeQueryProfileParser());
        // Propietary
        registerBeanDefinitionParser(ShibbolethSSOProfileParser.TYPE_NAME, new ShibbolethSSOProfileParser());

    }
}