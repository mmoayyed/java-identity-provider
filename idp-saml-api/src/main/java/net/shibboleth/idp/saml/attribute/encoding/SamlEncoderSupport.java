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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

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
    @Nullable public static XMLObject encodeStringValue(@Nonnull final Attribute attribute,
            @Nonnull final QName attributeValueElementName, @Nullable final String value) {

        Constraint.isNotNull(attribute, "Attribute can not be null");
        Constraint.isNotNull(attributeValueElementName, "Attribute Element Name resolution context can not be null");

        if (Strings.isNullOrEmpty(value)) {
            LOG.debug("Skipping empty value for attribute {}", attribute.getId());
            return null;
        }

        final XMLObjectBuilder<XSString> stringBuilder =
                XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(XSString.TYPE_NAME);

        LOG.debug("Encoding value {} of attribute {}", value, attribute.getId());
        final XSString samlAttributeValue = stringBuilder.buildObject(attributeValueElementName, XSString.TYPE_NAME);
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
    @Nullable public static XMLObject encodeByteArrayValue(@Nonnull final Attribute attribute,
            @Nonnull final QName attributeValueElementName, @Nullable final byte[] value) {
        if (value == null || value.length == 0) {
            LOG.debug("Skipping empty value for attribute {}", attribute.getId());
            return null;
        }
        Constraint.isNotNull(attribute, "Attribute can not be null");
        Constraint.isNotNull(attributeValueElementName, "Attribute Element Name resolution context can not be null");

        final XMLObjectBuilder<XSString> stringBuilder =
                XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(XSString.TYPE_NAME);

        final XSString samlAttributeValue = stringBuilder.buildObject(attributeValueElementName, XSString.TYPE_NAME);

        samlAttributeValue.setValue(Base64Support.encode(value, Base64Support.UNCHUNKED));
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
    @Nullable public static XMLObject encodeXmlObjectValue(@Nonnull final Attribute attribute,
            @Nonnull final QName attributeValueElementName, @Nullable final XMLObject value) {
        if (value == null) {
            LOG.debug("Skipping empty value for attribute {}", attribute.getId());
            return null;
        }
        Constraint.isNotNull(attribute, "Attribute can not be null");
        Constraint.isNotNull(attributeValueElementName, "Attribute Element Name resolution context can not be null");

        final XMLObjectBuilder<XSAny> attributeValueBuilder =
                XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(XSAny.TYPE_NAME);
        final XSAny samlAttributeValue = attributeValueBuilder.buildObject(attributeValueElementName);
        samlAttributeValue.getUnknownXMLObjects().add(value);

        return samlAttributeValue;
    }
}