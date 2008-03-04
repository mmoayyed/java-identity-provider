/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
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
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.encoding.provider.SAML1NameIDAttributeEncoder;

/**
 * Spring Bean Definition Parser for SAML2 string attribute encoder.
 */
public class SAML1NameIDAttributeEncoderBeanDefinitionParser extends BaseAttributeEncoderBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeEncoderNamespaceHandler.NAMESPACE, "SAML1NameIDAttribute");

    /** Local name of name format attribute. */
    public static final String NAME_FORMAT_ATTRIBUTE_NAME = "nameFormat";

    /** Local name of friendly name attribute. */
    public static final String FRIENDLY_NAME_ATTRIBUTE_NAME = "friendlyName";

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return SAML1NameIDAttributeEncoder.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        builder.addPropertyValue("nameFormat", element.getAttribute(NAME_FORMAT_ATTRIBUTE_NAME));
        builder.addPropertyValue("friendlyName", element.getAttribute(FRIENDLY_NAME_ATTRIBUTE_NAME));

        String attributeName = DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null, "name"));
        if (attributeName == null) {
            throw new BeanCreationException("SAML 2 attribute encoders must contain a name");
        }

        builder.addPropertyValue("nameIdFormat", element.getAttributeNS(null, "nameIdFormat"));
        builder.addPropertyValue("nameIdQualifier", element.getAttributeNS(null, "nameIdQualifier"));
    }
}