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

package edu.internet2.middleware.shibboleth.common.config.attribute.encoding;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/**
 * Spring namespace handler for the Shibboleth encoder namespace.
 */
public class AttributeEncoderNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:attribute:encoder";

    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser(SAML2StringAttributeEncoderBeanDefinitionParser.TYPE_NAME,
                new SAML2StringAttributeEncoderBeanDefinitionParser());

        registerBeanDefinitionParser(SAML2ScopedStringAttributeEncoderBeanDefinitionParser.TYPE_NAME,
                new SAML2ScopedStringAttributeEncoderBeanDefinitionParser());

        registerBeanDefinitionParser(SAML1StringAttributeEncoderBeanDefinitionParser.TYPE_NAME,
                new SAML1StringAttributeEncoderBeanDefinitionParser());

        registerBeanDefinitionParser(SAML1ScopedStringAttributeEncoderBeanDefinitionParser.TYPE_NAME,
                new SAML1ScopedStringAttributeEncoderBeanDefinitionParser());

        registerBeanDefinitionParser(SAML2StringNameIDEncoderBeanDefinitionParser.SCHEMA_TYPE,
                new SAML2StringNameIDEncoderBeanDefinitionParser());
    }

}