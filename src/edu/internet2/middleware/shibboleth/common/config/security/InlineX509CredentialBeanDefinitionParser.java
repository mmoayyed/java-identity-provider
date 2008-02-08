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

import javax.xml.namespace.QName;

/**
 * Spring bean definition parser for inline credential configuration elements.
 */
public class InlineX509CredentialBeanDefinitionParser extends AbstractX509CredentialBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(SecurityNamespaceHandler.NAMESPACE, "X509Inline");

    /** {@inheritDoc} */
    protected byte[] getEncodedCRL(String certCRLContent) {
        return certCRLContent.getBytes();
    }

    /** {@inheritDoc} */
    protected byte[] getEncodedCertificate(String certConfigContent) {
        return certConfigContent.getBytes();
    }

    /** {@inheritDoc} */
    protected byte[] getEncodedPrivateKey(String keyConfigContent) {
        return keyConfigContent.getBytes();
    }
}