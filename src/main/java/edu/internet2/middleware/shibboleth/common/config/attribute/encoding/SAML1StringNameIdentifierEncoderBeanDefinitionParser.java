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

import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.encoding.provider.SAML1StringNameIdentifierEncoder;

/**
 * Spring bean definition parser for {@link SAML1StringNameIdentifierEncoder}s.
 */
public class SAML1StringNameIdentifierEncoderBeanDefinitionParser extends BaseAttributeEncoderBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(AttributeEncoderNamespaceHandler.NAMESPACE,
            "SAML1StringNameIdentifier");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return SAML1StringNameIdentifierEncoder.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        String namespace = "urn:oasis:names:tc:SAML:1.0:nameid-format:unspecified";
        if (element.hasAttributeNS(null, "nameFormat")) {
            namespace = DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null, "nameFormat"));
        }
        builder.addPropertyValue("nameFormat", namespace);

        builder.addPropertyValue("nameQualifier", element.getAttributeNS(null, "nameQualifier"));
    }
}