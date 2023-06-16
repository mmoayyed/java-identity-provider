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

package net.shibboleth.idp.cas.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import com.google.common.base.Strings;

import net.shibboleth.idp.attribute.AttributeDecodingException;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.transcoding.AbstractAttributeTranscoder;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Base class for transcoders that support CAS attributes.
 *
 * @param <EncodedType> the type of data that can be handled by the transcoder
 */
public abstract class AbstractCASAttributeTranscoder<EncodedType extends IdPAttributeValue>
        extends AbstractAttributeTranscoder<Attribute> implements CASAttributeTranscoder {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractCASAttributeTranscoder.class);
        
    /** {@inheritDoc} */
    @Nonnull public Class<Attribute> getEncodedType() {
        return Attribute.class;
    }
    
    /** {@inheritDoc} */
    @Nullable public String getEncodedName(@Nonnull final TranscodingRule rule) {
        
        // CAS naming should be based on only what needs to be available from the properties alone.
        final String name = rule.getOrDefault(PROP_NAME, String.class,
                rule.get(AttributeTranscoderRegistry.PROP_ID, String.class));
        if (name != null) {
            return new NamingFunction().apply(new Attribute(name));
        }
        return null;
    }
    
    /** {@inheritDoc} */
    @Override
    @Nullable public Attribute doEncode(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final IdPAttribute attribute, @Nonnull final Class<? extends Attribute> to,
            @Nonnull final TranscodingRule rule) throws AttributeEncodingException {

        final String attributeId = attribute.getId();

        log.trace("Beginning to encode attribute {}", attributeId);

        final String name = rule.getOrDefault(PROP_NAME, String.class, attributeId);
        // by construction if attribute id is nonnnul then the so if name.
        assert name != null;
        final Attribute casAttribute = new Attribute(name);
        
        for (final IdPAttributeValue o : attribute.getValues()) {
            if (o == null) {
                // filtered out upstream leave in test for sanity
                log.debug("Skipping null value of attribute {}", attributeId);
                continue;
            }

            if (!canEncodeValue(attribute, o)) {
                log.warn("Skipping value of attribute '{}'; Type {} cannot be encoded by this encoder ({}).",
                        attributeId, o.getClass().getSimpleName(), this.getClass().getSimpleName());
                continue;
            }

            final EncodedType attributeValue = (EncodedType) o;
            final String casAttributeValue = encodeValue(profileRequestContext, attribute, rule, attributeValue);
            if (casAttributeValue == null) {
                log.debug("Skipping null value for attribute {}", attributeId);
            } else {
                casAttribute.getValues().add(casAttributeValue);
            }
        }
        
        if (!attribute.getValues().isEmpty() && casAttribute.getValues().isEmpty()) {
            throw new AttributeEncodingException("Failed to encode any values for attribute " + attribute.getId());
        }
        
        log.trace("Encoded {} values for attribute {}", casAttribute.getValues().size(), attributeId);
        return casAttribute;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public IdPAttribute doDecode(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final Attribute input, @Nonnull final TranscodingRule rule) throws AttributeDecodingException {

        final String attributeName = getEncodedName(rule);
        
        log.trace("Beginning to decode attribute {}", attributeName);

        final List<IdPAttributeValue> idpAttributeValues = new ArrayList<>();

        for (final String o : input.getValues()) {
            if (o == null) {
                // filtered out upstream leave in test for sanity
                log.debug("Skipping null value of attribute {}", attributeName);
                continue;
            }

            final IdPAttributeValue idpAttributeValue = decodeValue(profileRequestContext, input, rule, o);
            if (idpAttributeValue == null) {
                log.debug("Unable to decode value of attribute {}", attributeName);
            } else {
                idpAttributeValues.add(idpAttributeValue);
            }
        }

        if (!idpAttributeValues.isEmpty()) {
            log.trace("Decoded {} values for attribute {}", idpAttributeValues.size(), attributeName);
        }
        return buildIdPAttribute(profileRequestContext, input, rule, idpAttributeValues);
    }

    /**
     * Checks if the given value can be handled by the transcoder.
     * 
     * <p>In many cases this is simply a check to see if the given object is of the right type.</p>
     * 
     * @param idpAttribute the attribute being encoded, never null
     * @param value the value to check, never null
     * 
     * @return true if the transcoder can encode this value, false if not
     */
    protected abstract boolean canEncodeValue(@Nonnull final IdPAttribute idpAttribute,
            @Nonnull final IdPAttributeValue value);
    
    /**
     * Encodes an attribute value into a string.
     * 
     * @param profileRequestContext current profile request
     * @param attribute the attribute being encoded
     * @param rule properties to control encoding
     * @param value the value to encode
     * 
     * @return the attribute value or null if the resulting attribute value would be empty
     * 
     * @throws AttributeEncodingException thrown if there is a problem encoding the attribute value
     */
    @Nullable protected abstract String encodeValue(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final IdPAttribute attribute, @Nonnull final TranscodingRule rule,
            @Nonnull final EncodedType value) throws AttributeEncodingException;

    /**
     * Builds an {@link IdPAttribute} from the given values.
     * 
     * @param profileRequestContext current profile request
     * @param attribute the attribute being decoded
     * @param rule properties to control decoding
     * @param attributeValues the decoded values for the attribute
     * 
     * @return the IdPAttribute object
     * 
     * @throws AttributeDecodingException thrown if there is a problem constructing the IdPAttribute
     */
    @Nonnull protected IdPAttribute buildIdPAttribute(
            @Nullable final ProfileRequestContext profileRequestContext, @Nonnull final Attribute attribute,
            @Nonnull final TranscodingRule rule,
            @Nonnull final List<IdPAttributeValue> attributeValues)
                    throws AttributeDecodingException {
        
        final String id = rule.get(AttributeTranscoderRegistry.PROP_ID, String.class);
        if (Strings.isNullOrEmpty(id)) {
            throw new AttributeDecodingException("Required transcoder property 'id' not found");
        }
        assert id != null;
        
        if (!attribute.getValues().isEmpty() && attributeValues.isEmpty()) {
            throw new AttributeDecodingException("Failed to decode any values for attribute " + attribute.getName());
        }
        
        final IdPAttribute idpAttribute = new IdPAttribute(id);
        idpAttribute.setValues(attributeValues);
        return idpAttribute;
    }
    
    /**
     * Function to decode a single string value into an {@link IdPAttributeValue}.
     * 
     * @param profileRequestContext current profile request
     * @param attribute the attribute being decoded
     * @param rule properties to control decoding
     * @param value the value to decode
     * 
     * @return the returned final {@link IdPAttributeValue} or null if decoding failed
     */
    @Nullable protected abstract IdPAttributeValue decodeValue(
            @Nullable final ProfileRequestContext profileRequestContext, @Nonnull final Attribute attribute,
            @Nonnull final TranscodingRule rule, @Nullable final String value);

    /** A function to produce a "canonical" name for a CAS {@link Attribute} for transcoding rules. */
    public static class NamingFunction implements Function<Attribute,String> {

        /** {@inheritDoc} */
        @Nullable public String apply(@Nullable final Attribute input) {
            
            if (input == null || input.getName() == null) {
                return null;
            }
            
            return "CAS:" + input.getName();
        }

    }

}
