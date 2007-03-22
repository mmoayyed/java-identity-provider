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

package edu.internet2.middleware.shibboleth.common.config.resolver.attributeEncoder;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.provider.SAML1ScopedStringAttributeEncoder;

/**
 * Spring Bean Definition Parser for SAML1 string attribute encoder.
 */
public class SAML1ScopedStringAttributeEncoderBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName("urn:mace:shibboleth:2.0:attribute:encoder", "SAML1ScopedString");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return SAML1ScopedStringAttributeEncoder.class;
    }

}