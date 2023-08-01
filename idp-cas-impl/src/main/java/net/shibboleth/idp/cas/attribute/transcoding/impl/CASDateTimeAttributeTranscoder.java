/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.cas.attribute.transcoding.impl;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.DateTimeAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoder;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.cas.attribute.AbstractCASAttributeTranscoder;
import net.shibboleth.idp.cas.attribute.Attribute;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.xml.DOMTypeSupport;

/**
 * {@link AttributeTranscoder} that supports {@link Attribute} and
 * {@link DateTimeAttributeValue} objects.
 */
public class CASDateTimeAttributeTranscoder extends AbstractCASAttributeTranscoder<DateTimeAttributeValue> {

    /** One of "ms" or "s", controlling the unit to use when converting to an epoch. */
    @Nonnull @NotEmpty public static final String PROP_EPOCH_UNITS = "cas.epochUnits";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CASDateTimeAttributeTranscoder.class);

    /** {@inheritDoc} */
    @Override protected boolean canEncodeValue(@Nonnull final IdPAttribute attribute,
            @Nonnull final IdPAttributeValue value) {
        return value instanceof DateTimeAttributeValue;
    }

    /** {@inheritDoc} */
    @Override @Nullable protected String encodeValue(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final IdPAttribute attribute, @Nonnull final TranscodingRule rule,
            @Nonnull final DateTimeAttributeValue value) throws AttributeEncodingException {
        
        return DOMTypeSupport.instantToString(value.getValue());
    }

    /** {@inheritDoc} */
    @Override @Nullable protected IdPAttributeValue decodeValue(
            @Nullable final ProfileRequestContext profileRequestContext, @Nonnull final Attribute attribute,
            @Nonnull final TranscodingRule rule, @Nullable final String value) {
        
        if (value != null) {
            final Instant retVal = getDateTimeValue(rule, value);
            if (retVal != null) {
                return new DateTimeAttributeValue(retVal);
            }
        }
        
        return null;
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
        if (value == null) {
            return null;
        }
        
        try {
            final Long longVal = Long.valueOf(value);
            if (longVal != null) {
                return getDateTimeValue(rule, longVal);
            }
        } catch (final NumberFormatException e) {
        }

        try {
            return DOMTypeSupport.stringToInstant(value);
        } catch (final IllegalArgumentException e) {
        }

        log.warn("{} rule unable to process string value as numeric or ISO format",
                rule.getOrDefault(AttributeTranscoderRegistry.PROP_ID, String.class, "(none)"));
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