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

package net.shibboleth.idp.saml.attribute.decoding;

import java.util.List;

import net.shibboleth.idp.attribute.AbstractAttributeDecoder;
import net.shibboleth.idp.attribute.AttributeDecodingException;

import org.opensaml.common.SAMLObject;
import org.opensaml.util.StringSupport;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.UnmodifiableComponentException;
import org.opensaml.xml.XMLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for decoders that read SAML attributes.
 * 
 * @param <DecodedType> type of object decoded by this decoder
 * @param <ValueType> type of the values of the IdP attribute
 */
public abstract class AbstractSamlAttributeDecoder<DecodedType extends SAMLObject, ValueType> extends
        AbstractAttributeDecoder<DecodedType, ValueType> {

    /** Class logger. */
    private Logger log = LoggerFactory.getLogger(AbstractSamlAttributeDecoder.class);

    /** The name of the attribute. */
    private String name;

    /** The namespace in which the attribute name is interpreted. */
    private String namespace;

    /**
     * Gets the name of the attribute.
     * 
     * @return name of the attribute, never null after initialization
     */
    public final String getName() {
        return name;
    }

    /**
     * Sets the name of the attribute.
     * 
     * @param attributeName name of the attribute
     */
    public final synchronized void setName(final String attributeName) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException(
                    "Attribute name can not be changed after decoder has been initialized");
        }
        name = StringSupport.trimOrNull(attributeName);
    }

    /**
     * Gets the namespace in which the attribute name is interpreted.
     * 
     * @return namespace in which the attribute name is interpreted, never null after initialization
     */
    public final String getNamespace() {
        return namespace;
    }

    /**
     * Sets the namespace in which the attribute name is interpreted.
     * 
     * @param attributeNamespace namespace in which the attribute name is interpreted
     */
    public final synchronized void setNamespace(final String attributeNamespace) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException(
                    "Attribute name format can not be changed after decodser has been initialized");
        }
        namespace = StringSupport.trimOrNull(attributeNamespace);
    }

    /**
     * Checks that the SAML attribute name and namespace/format are not null.
     * 
     * @throws ComponentInitializationException thrown if there is a problem in the SAML attribute name and
     *             namespace/format
     */
    protected void doInitialize() throws ComponentInitializationException {
        if (name == null) {
            throw new ComponentInitializationException("Attribute name can not be null or empty");
        }

        if (namespace == null) {
            throw new ComponentInitializationException("Attribute namespace can not be null or empty");
        }
    }

    /**
     * Decodes each value of the SAML attribute by delegating to {@link #decodeValue(XMLObject)}.
     * 
     * {@inheritDoc}
     */
    protected void doDecode(net.shibboleth.idp.attribute.Attribute<ValueType> attribute, DecodedType data)
            throws AttributeDecodingException {
        final List<XMLObject> attributeValues = data.getOrderedChildren();
        if (attributeValues == null || attributeValues.isEmpty()) {
            log.debug("SAML attribute contained no values, no values decoded in to IdP attribute {}", getId());
            return;
        }

        log.debug("Decoding {} SAML attribute values into IdP attribute {}", attributeValues.size(), getId());
        ValueType decodedValue;
        for (XMLObject attributeValue : attributeValues) {
            if (attributeValue == null) {
                log.trace("Skipping null value");
                continue;
            }

            decodedValue = decodeValue(attributeValue);
            if (decodedValue == null) {
                log.trace("Skipping valude that decoded to null");
                continue;
            }

            attribute.getValues().add(decodedValue);
        }
    };

    /**
     * Decodes a given SAML attribute value.
     * 
     * @param attributeValue the value to be decoded, never null
     * 
     * @return the decoded value
     * 
     * @throws AttributeDecodingException thrown if there is a problem decoding the value
     */
    protected abstract ValueType decodeValue(XMLObject attributeValue) throws AttributeDecodingException;
}