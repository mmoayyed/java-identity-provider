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

package edu.internet2.middleware.shibboleth.common.attribute.encoding.provider;

import org.opensaml.saml2.core.AttributeValue;
import org.opensaml.saml2.core.impl.AttributeBuilder;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncodingException;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.SAML2AttributeEncoder;

/**
 * Implementation of SAML 2.0 scoped attribute encoder.
 */
public class SAML2ScopedStringAttributeEncoder extends
        AbstractScopedAttributeEncoder<org.opensaml.saml2.core.Attribute> implements SAML2AttributeEncoder {

    /** Attribute factory. */
    private static AttributeBuilder attributeBuilder;

    /** Format of attribute. */
    private String format;

    /** Friendly name of attribute. */
    private String friendlyName;

    /** Constructor. */
    public SAML2ScopedStringAttributeEncoder() {
        super();
        attributeBuilder = new AttributeBuilder();
    }

    /** {@inheritDoc} */
    public String getNameFormat() {
        return format;
    }

    /** {@inheritDoc} */
    public String getFriendlyName() {
        return friendlyName;
    }

    /** {@inheritDoc} */
    public void setNameFormat(String newFormat) {
        format = newFormat;
    }

    /** {@inheritDoc} */
    public void setFriendlyName(String name) {
        friendlyName = name;
    }

    /** {@inheritDoc} */
    public org.opensaml.saml2.core.Attribute encode(BaseAttribute attribute) throws AttributeEncodingException {
        org.opensaml.saml2.core.Attribute samlAttribute;
        samlAttribute = attributeBuilder.buildObject();

        samlAttribute.setName(getAttributeName());
        samlAttribute.setNameFormat(getNameFormat());
        samlAttribute.setFriendlyName(getFriendlyName());
        samlAttribute.getAttributeValues().addAll(encodeAttributeValues(AttributeValue.DEFAULT_ELEMENT_NAME, attribute));

        return samlAttribute;
    }
}