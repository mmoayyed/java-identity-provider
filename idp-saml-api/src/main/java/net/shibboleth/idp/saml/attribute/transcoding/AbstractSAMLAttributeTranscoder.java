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

package net.shibboleth.idp.saml.attribute.transcoding;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeDecodingException;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.transcoding.AbstractAttributeTranscoder;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.saml.xmlobject.ScopedValue;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.xml.DOMTypeSupport;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSBase64Binary;
import org.opensaml.core.xml.schema.XSBoolean;
import org.opensaml.core.xml.schema.XSDateTime;
import org.opensaml.core.xml.schema.XSInteger;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.XSURI;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for transcoders that support SAML attributes.
 * 
 * @param <AttributeType> type of object produced
 * @param <EncodedType> the type of data that can be handled by the transcoder
 */
public abstract class AbstractSAMLAttributeTranscoder<AttributeType extends SAMLObject,
        EncodedType extends IdPAttributeValue> extends AbstractAttributeTranscoder<AttributeType> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractSAMLAttributeTranscoder.class);
        
    /** {@inheritDoc} */
    @Override
    @Nullable public AttributeType doEncode(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final IdPAttribute attribute, @Nonnull final Class<? extends AttributeType> to,
            @Nonnull final TranscodingRule rule) throws AttributeEncodingException {

        final String attributeId = attribute.getId();

        log.trace("Beginning to encode attribute {}", attributeId);

        final List<XMLObject> samlAttributeValues = new ArrayList<>();

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
            final XMLObject samlAttributeValue =
                    encodeValue(profileRequestContext, attribute, rule, attributeValue);
            if (samlAttributeValue == null) {
                log.debug("Skipping null value for attribute {}", attributeId);
            } else {
                samlAttributeValues.add(samlAttributeValue);
            }
        }
        
        log.trace("Encoded {} values for attribute {}", samlAttributeValues.size(), attributeId);
        return buildAttribute(profileRequestContext, attribute, to, rule, samlAttributeValues);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public IdPAttribute doDecode(@Nullable final ProfileRequestContext profileRequestContext,
            @Nonnull final AttributeType input, @Nonnull final TranscodingRule rule)
                    throws AttributeDecodingException {

        final String attributeName = getEncodedName(rule);
        
        log.trace("Beginning to decode attribute {}", attributeName);

        final List<IdPAttributeValue> idpAttributeValues = new ArrayList<>();
        final Iterable<XMLObject> samlAttributeValues = getValues(input);

        for (final XMLObject o : samlAttributeValues) {
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
     * Function to return an XML object in string form.
     * 
     * @param object object to decode
     * 
     * @return decoded string, or null
     */
// Checkstyle: CyclomaticComplexity OFF
    @Nullable protected String getStringValue(@Nonnull final XMLObject object) {
        String retVal = null;

        if (object instanceof XSString) {

            retVal = ((XSString) object).getValue();

        } else if (object instanceof XSURI) {

            retVal = ((XSURI) object).getURI();

        } else if (object instanceof XSBoolean) {

            retVal = ((XSBoolean) object).getValue().getValue() ? "1" : "0";

        } else if (object instanceof XSInteger) {

            retVal = ((XSInteger) object).getValue().toString();

        } else if (object instanceof XSDateTime) {

            final Instant dt = ((XSDateTime) object).getValue();
            if (dt != null) {
                retVal = DOMTypeSupport.instantToString(dt);
            } else {
                retVal = null;
            }

        } else if (object instanceof XSBase64Binary) {

            retVal = ((XSBase64Binary) object).getValue();
            
        } else if (object instanceof ScopedValue) {
            
            retVal = ((ScopedValue) object).getValue();

        } else if (object instanceof XSAny) {

            final XSAny wc = (XSAny) object;
            if (wc.getUnknownAttributes().isEmpty() && wc.getUnknownXMLObjects().isEmpty()) {
                retVal = wc.getTextContent();
            } else {
                retVal = null;
            }
        }

        if (null == retVal) {
            log.info("Value of type {} could not be converted", object.getClass().getSimpleName());
        }
        return retVal;
    }
// Checkstyle: CyclomaticComplexity ON

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
     * Builds a SAML attribute element from the given attribute values.
     * 
     * @param profileRequestContext current profile request
     * @param attribute the attribute being encoded
     * @param to target type to create
     * @param rule properties to control encoding
     * @param attributeValues the encoded values for the attribute
     * 
     * @return the SAML attribute object
     * 
     * @throws AttributeEncodingException thrown if there is a problem constructing the SAML attribute
     */
    @Nonnull protected abstract AttributeType buildAttribute(
            @Nullable final ProfileRequestContext profileRequestContext, @Nullable final IdPAttribute attribute,
            @Nonnull final Class<? extends AttributeType> to, @Nonnull final TranscodingRule rule,
            @Nonnull @NonnullElements final List<XMLObject> attributeValues) throws AttributeEncodingException;

    /**
     * Encodes an attribute value into a SAML AttributeValue element.
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
    @Nullable protected abstract XMLObject encodeValue(@Nullable final ProfileRequestContext profileRequestContext,
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
    @Nonnull protected abstract IdPAttribute buildIdPAttribute(
            @Nullable final ProfileRequestContext profileRequestContext, @Nonnull final AttributeType attribute,
            @Nonnull final TranscodingRule rule,
            @Nonnull @NonnullElements final List<IdPAttributeValue> attributeValues)
                    throws AttributeDecodingException;
    
    /**
     * Returns the values to decode from the concrete input object.
     * 
     * @param input input object
     * 
     * @return values to decode
     */
    @Nonnull protected abstract Iterable<XMLObject> getValues(@Nonnull final AttributeType input);
    
    /**
     * Function to decode a single {@link XMLObject} into an {@link IdPAttributeValue}.
     * 
     * @param profileRequestContext current profile request
     * @param attribute the attribute being decoded
     * @param rule properties to control decoding
     * @param value the value to decode
     * 
     * @return the returned final {@link IdPAttributeValue} or null if decoding failed
     */
    @Nullable protected abstract IdPAttributeValue decodeValue(
            @Nullable final ProfileRequestContext profileRequestContext, @Nonnull final AttributeType attribute,
            @Nonnull final TranscodingRule rule, @Nullable final XMLObject value);

}
