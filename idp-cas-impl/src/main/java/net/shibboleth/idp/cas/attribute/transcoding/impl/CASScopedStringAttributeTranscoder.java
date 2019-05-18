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

package net.shibboleth.idp.cas.attribute.transcoding.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoder;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.cas.attribute.AbstractCASAttributeTranscoder;
import net.shibboleth.idp.cas.attribute.Attribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AttributeTranscoder} that supports {@link Attribute} and
 * {@link ScopedStringAttributeValue} objects.
 */
public class CASScopedStringAttributeTranscoder extends AbstractCASAttributeTranscoder<ScopedStringAttributeValue> {

    /** Scope delimiter. */
    @Nonnull @NotEmpty public static final String PROP_SCOPE_DELIMITER = "scopeDelimiter";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CASScopedStringAttributeTranscoder.class);

    /** {@inheritDoc} */
    @Override protected boolean canEncodeValue(@Nonnull final IdPAttribute attribute,
            @Nonnull final IdPAttributeValue value) {
        return value instanceof ScopedStringAttributeValue;
    }

    /** {@inheritDoc} */
    @Override @Nullable protected String encodeValue(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final IdPAttribute attribute, @Nonnull final TranscodingRule rule,
            @Nonnull final ScopedStringAttributeValue value) throws AttributeEncodingException {
        
        final String scopeDelimiter = rule.getOrDefault(PROP_SCOPE_DELIMITER, String.class, "@");
        return value.getValue() + scopeDelimiter + value.getScope();
    }

    /** {@inheritDoc} */
    @Override @Nullable protected IdPAttributeValue decodeValue(
            @Nullable final ProfileRequestContext profileRequestContext, @Nonnull final Attribute attribute,
            @Nonnull final TranscodingRule rule, @Nullable final String value) {
        
        if (value == null) {
            return null;
        }
        
        final String scopeDelimiter = rule.getOrDefault(PROP_SCOPE_DELIMITER, String.class, "@");
        final int offset = value.indexOf(scopeDelimiter);
        if (offset < 0) {
            log.warn("Ignoring value with no scope delimiter ({})", scopeDelimiter);
            return null;
        }

        return ScopedStringAttributeValue.valueOf(value.substring(0, offset),
                value.substring(offset + scopeDelimiter.length()));
    }
    
}
