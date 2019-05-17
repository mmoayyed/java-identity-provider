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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.XMLObjectAttributeValue;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.saml.attribute.transcoding.AbstractSAML1AttributeTranscoder;
import net.shibboleth.idp.saml.attribute.transcoding.SAMLEncoderSupport;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml1.core.AttributeDesignator;
import org.opensaml.saml.saml1.core.AttributeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link net.shibboleth.idp.attribute.AttributeTranscoder} that supports {@link AttributeDesignator} and
 * {@link XMLObjectAttributeValue} objects.
 */
public class SAML1XMLObjectAttributeTranscoder extends AbstractSAML1AttributeTranscoder<XMLObjectAttributeValue> {

    /** Property indicating whether to decode the AttributeValue element itself, or its child element. */
    @Nonnull @NotEmpty public static final String PROP_INCLUDE_ATTR_VALUE = "includeAttributeValue";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SAML1XMLObjectAttributeTranscoder.class);

    /** {@inheritDoc} */
    @Override protected boolean canEncodeValue(@Nonnull final IdPAttribute attribute,
            @Nonnull final IdPAttributeValue value) {
        return value instanceof XMLObjectAttributeValue;
    }

    /** {@inheritDoc} */
    @Override @Nullable protected XMLObject encodeValue(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final IdPAttribute attribute, @Nonnull final TranscodingRule rule,
            @Nonnull final XMLObjectAttributeValue value) throws AttributeEncodingException {

        return SAMLEncoderSupport.encodeXMLObjectValue(attribute, AttributeValue.DEFAULT_ELEMENT_NAME,
                value.getValue());
    }

    /** {@inheritDoc} */
    @Override @Nullable protected IdPAttributeValue decodeValue(
            @Nullable final ProfileRequestContext profileRequestContext, @Nonnull final AttributeDesignator attribute,
            @Nonnull final TranscodingRule rule, @Nullable final XMLObject value) {

        if (value == null) {
            return null;
        }
        
        final Boolean includeAttributeValue = rule.getOrDefault(PROP_INCLUDE_ATTR_VALUE, Boolean.class, Boolean.FALSE);
        if (includeAttributeValue) {
            return new XMLObjectAttributeValue(value);
        }
        
        final List<XMLObject> children = value.getOrderedChildren();
        
        if (null == children || children.isEmpty()) {
            log.debug("Ignoring XMLObject with no child elements");
            return null;
        }
        if (children.size() > 1) {
            log.debug("XMLObject has more than one child, returning first element only");
        }
        
        final XMLObject child = children.get(0);
        log.debug("Returning value of type {}", child.getClass().getSimpleName());
        return new XMLObjectAttributeValue(child);
    }
    
}
