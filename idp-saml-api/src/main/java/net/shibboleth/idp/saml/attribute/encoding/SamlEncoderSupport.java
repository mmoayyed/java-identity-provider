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

package net.shibboleth.idp.saml.attribute.encoding;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.Configuration;
import org.opensaml.util.Base64;
import org.opensaml.util.StringSupport;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.schema.XSAny;
import org.opensaml.xml.schema.XSString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Support class for encoding IdP Attributes and their value. */
public final class SamlEncoderSupport {

    /** Class logger. */
    private static final Logger LOG = LoggerFactory.getLogger(SamlEncoderSupport.class);

    /** Constructor. */
    private SamlEncoderSupport() {

    }

    /**
     * Encodes a String value in to an {@link XSString} SAML attribute value element.
     * 
     * @param attribute attribute to be encoded, never null
     * @param attributeValueElementName the SAML 1 or SAML 1 attribute name
     * @param value value to encoded
     * 
     * @return the attribute value element or null if the given value was null or empty
     */
    public static XMLObject encodeStringValue(Attribute<?> attribute, QName attributeValueElementName, String value) {
        if (StringSupport.isNullOrEmpty(value)) {
            LOG.debug("Skipping empty value for attribute {}", attribute.getId());
            return null;
        }

        XMLObjectBuilder<XSString> stringBuilder = Configuration.getBuilderFactory().getBuilder(XSString.TYPE_NAME);

        LOG.debug("Encoding value {} of attribute {}", value, attribute.getId());
        XSString samlAttributeValue = stringBuilder.buildObject(attributeValueElementName, XSString.TYPE_NAME);
        samlAttributeValue.setValue(value);
        return samlAttributeValue;
    }

    /**
     * Base64 encodes a <code>byte[]</code> in to an {@link XSString} SAML attribute value element.
     * 
     * @param attribute attribute to be encoded, never null
     * @param attributeValueElementName the SAML 1 or SAML 1 attribute name
     * @param value value to encoded
     * 
     * @return the attribute value element or null if the given value was null or empty
     */
    public static XMLObject encodeByteArrayValue(Attribute<?> attribute, QName attributeValueElementName, byte[] value) {
        if (value == null || value.length == 0) {
            LOG.debug("Skipping empty value for attribute {}", attribute.getId());
            return null;
        }

        XMLObjectBuilder<XSString> stringBuilder = Configuration.getBuilderFactory().getBuilder(XSString.TYPE_NAME);

        XSString samlAttributeValue = stringBuilder.buildObject(attributeValueElementName, XSString.TYPE_NAME);

        samlAttributeValue.setValue(Base64.encodeBytes(value));
        return samlAttributeValue;
    }

    /**
     * Encodes an {@link XMLObject} value in to a {@link XSAny} SAML attribute value.
     * 
     * @param attribute attribute to be encoded, never null
     * @param attributeValueElementName the SAML 1 or SAML 1 attribute name
     * @param value value to encoded
     * 
     * @return the attribute value element or null if the given value was null or empty
     */
    public static XMLObject encodeXmlObjectValue(Attribute<?> attribute, QName attributeValueElementName,
            XMLObject value) {
        if (value == null) {
            LOG.debug("Skipping empty value for attribute {}", attribute.getId());
            return null;
        }

        XMLObjectBuilder<XSAny> attributeValueBuilder = Configuration.getBuilderFactory().getBuilder(XSAny.TYPE_NAME);
        XSAny samlAttributeValue = attributeValueBuilder.buildObject(attributeValueElementName);
        samlAttributeValue.getUnknownXMLObjects().add(value);

        return samlAttributeValue;
    }
}