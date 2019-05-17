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

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.cas.attribute.AbstractCASAttributeTranscoder;
import net.shibboleth.idp.cas.attribute.Attribute;

/**
 * {@link net.shibboleth.idp.attribute.AttributeTranscoder} that supports {@link Attribute} and
 * {@link StringAttributeValue} objects.
 */
public class CASStringAttributeTranscoder extends AbstractCASAttributeTranscoder<StringAttributeValue> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CASStringAttributeTranscoder.class);

    /** {@inheritDoc} */
    @Override protected boolean canEncodeValue(@Nonnull final IdPAttribute attribute,
            @Nonnull final IdPAttributeValue value) {
        return value instanceof StringAttributeValue;
    }

    /** {@inheritDoc} */
    @Override @Nullable protected String encodeValue(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final IdPAttribute attribute, @Nonnull final TranscodingRule rule,
            @Nonnull final StringAttributeValue value) throws AttributeEncodingException {
        
        if (value instanceof ScopedStringAttributeValue) {
            log.warn("Attribute '{}': Lossy encoding of attribute value of type {} to SAML1 String Attribute",
                    attribute.getId(), value.getClass().getSimpleName());
        }
        
        return value.getValue();
    }

    /** {@inheritDoc} */
    @Override @Nullable protected IdPAttributeValue<?> decodeValue(
            @Nullable final ProfileRequestContext profileRequestContext, @Nonnull final Attribute attribute,
            @Nonnull final TranscodingRule rule, @Nullable final String value) {
        
        return value != null ? StringAttributeValue.valueOf(value) : null;
    }
    
}