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

package net.shibboleth.idp.attribute;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.opensaml.util.StringSupport;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.InitializableComponent;
import org.opensaml.util.component.UnmodifiableComponent;
import org.opensaml.util.component.UnmodifiableComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link AttributeDecoder} implementations.
 * 
 * @param <DecodedType> type of data decoded
 * @param <ValueType> data type of attribute values
 */
public abstract class AbstractAttributeDecoder<DecodedType, ValueType> implements AttributeDecoder<DecodedType>,
        UnmodifiableComponent, InitializableComponent {

    /** Class logger. */
    private Logger log = LoggerFactory.getLogger(AbstractAttributeDecoder.class);

    /** Whether this encoder has been initialized. */
    private boolean initialized;

    /** The ID of the IdP attribute. */
    private String id;

    /** Localized human intelligible attribute names. */
    private Map<Locale, String> displayNames;

    /** Localized human readable descriptions of attribute. */
    private Map<Locale, String> displayDescriptions;

    /**
     * Gets the ID of the generated {@link Attribute}.
     * 
     * @return the ID of the generated IdP attribute
     */
    public final String getId() {
        return id;
    }

    /**
     * Sets the ID of the generated {@link Attribute}.
     * 
     * @param attributeId the ID of the generated IdP attribute
     */
    public final synchronized void setId(final String attributeId) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException(
                    "Attribute ID can not be changed after decoder has been initialized");
        }
        id = StringSupport.trimOrNull(attributeId);
    }

    /**
     * Gets the unmodifiable collection of localized human readable name of the attribute. This collection never
     * contains entries whose key or value are null.
     * 
     * @return human readable name of the attribute, never null
     */
    public final Map<Locale, String> getDisplayNames() {
        return displayNames;
    }

    /**
     * Sets he display names for this attribute with the given ones.
     * 
     * @param newNames the new names for this attribute, may be null
     */
    public final synchronized void setDisplayNames(final Map<Locale, String> newNames) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException(
                    "Attribute display names can not be changed after decoder has been initialized");
        }

        if (newNames == null || newNames.isEmpty()) {
            return;
        }

        HashMap<Locale, String> checkedNames = new HashMap<Locale, String>();
        for (Entry<Locale, String> entry : newNames.entrySet()) {
            Locale locale = entry.getKey();
            if (locale == null) {
                continue;
            }

            final String trimmedName = StringSupport.trimOrNull(entry.getValue());
            if (trimmedName == null) {
                continue;
            }

            checkedNames.put(locale, trimmedName);
        }

        if (checkedNames == null || checkedNames.isEmpty()) {
            displayNames = Collections.emptyMap();
        } else {
            displayNames = Collections.unmodifiableMap(checkedNames);
        }
    }

    /**
     * Gets the unmodifiable localized human readable description of attribute. This collection never contains entries
     * whose key or value are null.
     * 
     * @return human readable description of attribute, never null
     */
    public final Map<Locale, String> getDisplayDescriptions() {
        return displayDescriptions;
    }

    /**
     * Sets display descriptions for this attribute with the given ones.
     * 
     * @param newDescriptions the new descriptions for this attribute, may be null
     */
    public final synchronized void setDisplayDescriptions(final Map<Locale, String> newDescriptions) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException(
                    "Attribute display descriptions can not be changed after decoder has been initialized");
        }

        if (newDescriptions == null || newDescriptions.isEmpty()) {
            return;
        }

        HashMap<Locale, String> checkedDescriptions = new HashMap<Locale, String>();
        for (Entry<Locale, String> entry : newDescriptions.entrySet()) {
            Locale locale = entry.getKey();
            if (locale == null) {
                continue;
            }

            final String trimmedDescription = StringSupport.trimOrNull(entry.getValue());
            if (trimmedDescription == null) {
                continue;
            }

            checkedDescriptions.put(locale, trimmedDescription);
        }

        if (checkedDescriptions == null || checkedDescriptions.isEmpty()) {
            displayDescriptions = Collections.emptyMap();
        } else {
            displayDescriptions = Collections.unmodifiableMap(checkedDescriptions);
        }
    }

    /** {@inheritDoc} */
    public final boolean isInitialized() {
        return initialized;
    }

    /** {@inheritDoc} */
    public final synchronized void initialize() throws ComponentInitializationException {
        if (id == null) {
            throw new ComponentInitializationException("Attribute ID can not be null or empty");
        }

        if (displayNames == null || displayNames.isEmpty()) {
            displayNames = Collections.emptyMap();
        }

        if (displayDescriptions == null || displayDescriptions.isEmpty()) {
            displayDescriptions = Collections.emptyMap();
        }

        doInitialize();

        initialized = true;
    }

    /**
     * Checks if this decoder has been initialized and if the given data is null, then creates the IdP attribute by
     * calling {@link #createAttribute(Object)}, sets its display names and descriptions, then delegates to
     * {@link #doDecode(Object)}.
     * 
     * {@inheritDoc}
     */
    public final Attribute decode(final DecodedType data) throws AttributeDecodingException {
        if (!isInitialized()) {
            throw new AttributeDecodingException("Decoder has not been initialized and so can not be used");
        }

        if (data == null) {
            return null;
        }

        log.debug("Decoding IdP attribute {}", getId());
        final Attribute<ValueType> attribute = createAttribute(data);
        attribute.setDisplayNames(getDisplayNames());
        attribute.setDisplayDescriptions(displayDescriptions);

        doDecode(attribute, data);

        return attribute;
    };

    /**
     * Performs additional component initialization. This method is called after the checks ensuring the attribute ID,
     * name and namespace are populated.
     * 
     * Default implementation of this method is a no-op
     * 
     * @throws ComponentInitializationException thrown if there is a problem initializing this encoder
     */
    protected void doInitialize() throws ComponentInitializationException {

    }

    /**
     * Creates the attribute that will receive the decoded values. The created attributed is a {@link Attribute} whose
     * ID, display name, and display descriptions are set to {@link #getId()}, {@link #getDisplayNames()} and
     * {@link #getDisplayDescriptions()}, respectively.
     * 
     * @param data SAML attribute to be decoded, never null
     * 
     * @return the attribute that will receive the decoded values, never null
     * 
     * @throws AttributeDecodingException thrown if there is a problem creating the IdP attribute from the given SAML
     *             attribute
     */
    protected Attribute createAttribute(final DecodedType data) throws AttributeDecodingException {
        return new Attribute<ValueType>(getId());
    }

    /**
     * Decodes the data in to the constructed IdP attribute. At this point the encoder is guaranteed to be initialized
     * and the IdP attribute contains its ID, display names, and display descriptions.
     * 
     * @param attribute the IdP attribute that will receive the decoded data, never null
     * @param data the data to decode, never null
     * 
     * @throws AttributeDecodingException thrown if there is a problem decoding the data in to the IdP attribute
     */
    protected abstract void doDecode(final Attribute<ValueType> attribute, final DecodedType data)
            throws AttributeDecodingException;
}