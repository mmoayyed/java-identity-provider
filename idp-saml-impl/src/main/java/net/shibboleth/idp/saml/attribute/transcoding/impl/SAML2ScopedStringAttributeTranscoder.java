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

package net.shibboleth.idp.saml.attribute.transcoding.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.saml.attribute.transcoding.AbstractSAML2AttributeTranscoder;
import net.shibboleth.idp.saml.attribute.transcoding.SAMLEncoderSupport;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import org.opensaml.core.xml.AttributeExtensibleXMLObject;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link net.shibboleth.idp.attribute.AttributeTranscoder} that supports {@link Attribute} and
 * {@link ScopedStringAttributeValue} objects.
 */
public class SAML2ScopedStringAttributeTranscoder extends AbstractSAML2AttributeTranscoder<ScopedStringAttributeValue> {

    /** One of "inline" or "attribute", controlling the style of XML encoding. */
    @Nonnull @NotEmpty public static final String PROP_SCOPE_TYPE = "scopeType";

    /** Name of XML attribute when scopeType property is "attribute". */
    @Nonnull @NotEmpty public static final String PROP_SCOPE_ATTR_NAME = "scopeAttributeName";

    /** Scope delimiter when scopeType property is "inline". */
    @Nonnull @NotEmpty public static final String PROP_SCOPE_DELIMITER = "scopeDelimiter";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SAML2ScopedStringAttributeTranscoder.class);

    /** {@inheritDoc} */
    @Override protected boolean canEncodeValue(@Nonnull final IdPAttribute attribute,
            @Nonnull final IdPAttributeValue value) {
        return value instanceof ScopedStringAttributeValue;
    }

    /** {@inheritDoc} */
    @Override @Nullable protected XMLObject encodeValue(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final IdPAttribute attribute, @Nonnull final TranscodingRule rule,
            @Nonnull final ScopedStringAttributeValue value) throws AttributeEncodingException {
                
        final Boolean encodeType = rule.getOrDefault(PROP_ENCODE_TYPE, Boolean.class, Boolean.TRUE);

        final String scopeType = rule.getOrDefault(PROP_SCOPE_TYPE, String.class, "inline");
        
        if ("attribute".equals(scopeType)) {
            final String scopeAttributeName = rule.getOrDefault(PROP_SCOPE_ATTR_NAME, String.class, "Scope");
            return SAMLEncoderSupport.encodeScopedStringValueAttribute(attribute,
                    AttributeValue.DEFAULT_ELEMENT_NAME, value, scopeAttributeName, encodeType);
        } else if ("inline".equals(scopeType)) {
            final String scopeDelimiter = rule.getOrDefault(PROP_SCOPE_DELIMITER, String.class, "@");
            return SAMLEncoderSupport.encodeScopedStringValueInline(
                    attribute, AttributeValue.DEFAULT_ELEMENT_NAME, value, scopeDelimiter, encodeType);
        } else {
            throw new AttributeEncodingException("Invalid scopeType property (must be inline or attribute)");
        }
    }

    /** {@inheritDoc} */
    @Override @Nullable protected IdPAttributeValue decodeValue(
            @Nullable final ProfileRequestContext profileRequestContext, @Nonnull final Attribute attribute,
            @Nonnull final TranscodingRule rule, @Nullable final XMLObject value) {
        
        if (value == null) {
            return null;
        }
        
        final String stringValue = getStringValue(value);
        if (null == stringValue) {
            return null;
        }

        final String scopeType = rule.getOrDefault(PROP_SCOPE_TYPE, String.class, "inline");
        if ("attribute".equals(scopeType)) {
            
            if (value instanceof AttributeExtensibleXMLObject) {
                final String scopeValue = ((AttributeExtensibleXMLObject) value).getUnknownAttributes().get(
                        new QName(null, rule.getOrDefault(PROP_SCOPE_ATTR_NAME, String.class, "Scope")));
                if (scopeValue == null) {
                    log.warn("Scope not found in designated XML attribute");
                    return null;
                }
                
                return ScopedStringAttributeValue.valueOf(stringValue, scopeValue);
                
            } else {
                log.warn("Object does not support required interface to access the scope via XML attribute");
                return null;
            }
        } else if ("inline".equals(scopeType)) {
            final String scopeDelimiter = rule.getOrDefault(PROP_SCOPE_DELIMITER, String.class, "@");
            final int offset = stringValue.indexOf(scopeDelimiter);
            if (offset < 0) {
                log.warn("Ignoring value with no scope delimiter ({})", scopeDelimiter);
                return null;
            }

            return ScopedStringAttributeValue.valueOf(stringValue.substring(0, offset), stringValue.substring(offset
                    + scopeDelimiter.length()));
            
        } else {
            log.error("Invalid scopeType property (must be inline or attribute)");
            return null;
        }
    }
        
}
