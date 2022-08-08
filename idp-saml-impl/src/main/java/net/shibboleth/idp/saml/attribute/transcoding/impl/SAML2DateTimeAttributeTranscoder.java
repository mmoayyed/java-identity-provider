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

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSDateTime;
import org.opensaml.core.xml.schema.XSInteger;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.DateTimeAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoder;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.saml.attribute.transcoding.AbstractSAML2AttributeTranscoder;
import net.shibboleth.idp.saml.attribute.transcoding.SAMLEncoderSupport;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.xml.DOMTypeSupport;

/**
 * {@link AttributeTranscoder} that supports {@link Attribute} and {@link DateTimeAttributeValue} objects.
 * 
 * @since 4.3.0
 */
public class SAML2DateTimeAttributeTranscoder extends AbstractSAML2AttributeTranscoder<DateTimeAttributeValue> {

    /** One of "ms" or "s", controlling the unit to use when converting to an epoch. */
    @Nonnull @NotEmpty public static final String PROP_EPOCH_UNITS = "saml2.epochUnits";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SAML2DateTimeAttributeTranscoder.class);
    
    /** {@inheritDoc} */
    @Override protected boolean canEncodeValue(@Nonnull final IdPAttribute attribute,
            @Nonnull final IdPAttributeValue value) {
        return value instanceof DateTimeAttributeValue;
    }

    /** {@inheritDoc} */
    @Override @Nullable protected XMLObject encodeValue(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final IdPAttribute attribute, @Nonnull final TranscodingRule rule,
            @Nonnull final DateTimeAttributeValue value) throws AttributeEncodingException {
        
        final Boolean encodeType = rule.getOrDefault(PROP_ENCODE_TYPE, Boolean.class, Boolean.TRUE);
        
        return SAMLEncoderSupport.encodeDateTimeValue(attribute, AttributeValue.DEFAULT_ELEMENT_NAME, value.getValue(),
                encodeType);
    }

    /** {@inheritDoc} */
    @Override @Nullable protected IdPAttributeValue decodeValue(
            @Nullable final ProfileRequestContext profileRequestContext, @Nonnull final Attribute attribute,
            @Nonnull final TranscodingRule rule, @Nullable final XMLObject value) {
        
        return value != null ? new DateTimeAttributeValue(getDateTimeValue(rule, value)) : null;
    }
    
    /**
     * Function to return an XML object in date/time form.
     * 
     * @param rule transcoding rule
     * @param object object to decode
     * 
     * @return decoded date/time, or null
     */
    @Nullable protected Instant getDateTimeValue(@Nonnull final TranscodingRule rule, @Nonnull final XMLObject object) {
        Instant retVal = null;

        if (object instanceof XSString) {
            
            return getDateTimeValue(rule, ((XSString) object).getValue());
            
        } else if (object instanceof XSInteger) {

            return getDateTimeValue(rule, ((XSInteger) object).getValue().longValue());

        } else if (object instanceof XSDateTime) {

            retVal = ((XSDateTime) object).getValue();

        } else if (object instanceof XSAny) {

            final XSAny wc = (XSAny) object;
            if (wc.getUnknownAttributes().isEmpty() && wc.getUnknownXMLObjects().isEmpty()) {
                return getDateTimeValue(rule, wc.getTextContent());
            }
        }

        if (null == retVal) {
            log.info("Value of type {} could not be converted", object.getClass().getSimpleName());
        }
        return retVal;
    }

    /**
     * Convert a string value into an {@link Instant}.
     * 
     * @param rule transcoding rule
     * @param value input value
     * 
     * @return converted result or null
     */
    @Nullable protected Instant getDateTimeValue(@Nonnull final TranscodingRule rule, @Nullable final String value) {
        try {
            final Long longVal = Long.valueOf(value);
            if (longVal != null) {
                return getDateTimeValue(rule, longVal);
            }
        } catch (final NumberFormatException e) {
            return DOMTypeSupport.stringToInstant(value);
        }
        
        return null;
    }
    
    /**
     * Convert a long value into an {@link Instant}.
     * 
     * @param rule transcoding rule
     * @param value input value
     * 
     * @return converted result
     */
    @Nullable protected Instant getDateTimeValue(@Nonnull final TranscodingRule rule, @Nonnull final Long value) {
        final String units = rule.getOrDefault(PROP_EPOCH_UNITS, String.class, "s");
        if ("s".equals(units)) {
            return Instant.ofEpochSecond(value);
        } else if ("ms".equals(units)) {
            return Instant.ofEpochMilli(value);
        }
        
        log.error("{} rule property {} must be 's' or 'ms'",
                rule.getOrDefault(AttributeTranscoderRegistry.PROP_ID, String.class, "(none)"), PROP_EPOCH_UNITS);
        return null;
    }

}