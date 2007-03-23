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

package edu.internet2.middleware.shibboleth.common.attribute.provider;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.opensaml.saml2.core.AttributeValue;
import org.opensaml.saml2.core.impl.AttributeBuilder;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.impl.XSStringBuilder;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.AttributeEncodingException;
import edu.internet2.middleware.shibboleth.common.attribute.SAML2AttributeEncoder;

/**
 * Implementation of SAML 2.0 scoped attribute encoder.
 */
public class SAML2ScopedStringAttributeEncoder extends
        AbstractScopedAttributeEncoder<org.opensaml.saml2.core.Attribute> implements SAML2AttributeEncoder {

    /** Attribute factory. */
    private static AttributeBuilder attributeBuilder;

    /** XSString factory. */
    private static XSStringBuilder stringBuilder;

    /** Log4j logger. */
    private static Logger log = Logger.getLogger(SAML2ScopedStringAttributeEncoder.class);

    /** Format of attribute. */
    private String format;

    /** Friendly name of attribute. */
    private String friendlyName;

    /** Constructor. */
    public SAML2ScopedStringAttributeEncoder() {
        attributeBuilder = new AttributeBuilder();
        stringBuilder = new XSStringBuilder();
        setEncoderCategory(SAML2AttributeEncoder.CATEGORY);
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
    public org.opensaml.saml2.core.Attribute encode(Attribute attribute) throws AttributeEncodingException {

        if (!(attribute instanceof ScopedAttribute)) {
            log.error("This attribute encoder (" + getAttributeName() + ") expects a scoped attribute.");
            throw new AttributeEncodingException("This attribute encoder (" + getAttributeName()
                    + ") expects a scoped attribute.");
        }

        org.opensaml.saml2.core.Attribute samlAttribute;
        samlAttribute = attributeBuilder.buildObject();

        samlAttribute.setName(getAttributeName());
        samlAttribute.setNameFormat(getNameFormat());
        samlAttribute.setFriendlyName(getFriendlyName());

        // handle "attribute" scopeType
        if ("attribute".equals(getScopeType())) {
            QName scopeAttribute = new QName(null, getScopeAttribute());
            String scopeValue = ((ScopedAttribute) attribute).getScope();

            samlAttribute.getUnknownAttributes().put(scopeAttribute, scopeValue);
        }

        // get attribute values
        for (Object o : attribute.getValues()) {
            String stringValue = o.toString();

            // handle "inline" scopeType
            if ("inline".equals(getScopeType())) {
                stringValue += getScopeDelimiter() + ((ScopedAttribute) attribute).getScope();
            }

            XSString xsstring = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
            xsstring.setValue(stringValue);
            samlAttribute.getAttributeValues().add(xsstring);
        }

        return samlAttribute;
    }

}