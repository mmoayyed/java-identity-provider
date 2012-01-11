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
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.TransformedInputMapBuilder;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.logic.TrimOrNullStringFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

/**
 * Base class for {@link AttributeDecoder} implementations.
 * 
 * @param <DecodedType> type of data decoded
 * @param <ValueType> data type of attribute values
 */
@ThreadSafe
public abstract class AbstractAttributeDecoder<DecodedType, ValueType> extends AbstractInitializableComponent implements
        AttributeDecoder<DecodedType>, UnmodifiableComponent {

    /** Class logger. */
    private Logger log = LoggerFactory.getLogger(AbstractAttributeDecoder.class);

    /** Localized human intelligible attribute names. */
    private Map<Locale, String> displayNames;

    /** Localized human readable descriptions of attribute. */
    private Map<Locale, String> displayDescriptions;

    /**
     * Gets the unmodifiable collection of localized human readable name of the attribute.
     * 
     * @return human readable name of the attribute
     */
    @Nonnull @NonnullElements @Unmodifiable public final Map<Locale, String> getDisplayNames() {
        return displayNames;
    }

    /**
     * Sets he display names for this attribute with the given ones.
     * 
     * @param newNames the new names for this attribute
     */
    public final synchronized void setDisplayNames(@Nullable @NullableElements final Map<Locale, String> newNames) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException(
                    "Attribute display names can not be changed after decoder has been initialized");
        }

        displayNames =
                new TransformedInputMapBuilder<Locale, String>().valuePreprocessor(TrimOrNullStringFunction.INSTANCE)
                        .putAll(newNames).buildImmutableMap();
    }

    /**
     * Gets the localized human readable description of attribute.
     * 
     * @return human readable description of attribute
     */
    @Nonnull @NonnullElements @Unmodifiable public final Map<Locale, String> getDisplayDescriptions() {
        return displayDescriptions;
    }

    /**
     * Sets display descriptions for this attribute with the given ones.
     * 
     * @param newDescriptions the new descriptions for this attribute, may be null
     */
    public final synchronized void setDisplayDescriptions(
            @Nullable @NullableElements final Map<Locale, String> newDescriptions) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException(
                    "Attribute display descriptions can not be changed after decoder has been initialized");
        }

        displayDescriptions =
                new TransformedInputMapBuilder<Locale, String>().valuePreprocessor(TrimOrNullStringFunction.INSTANCE)
                        .putAll(newDescriptions).buildImmutableMap();
    }

    /**
     * Checks if this decoder has been initialized and if the given data is null, then creates the IdP attribute by
     * calling {@link #createAttribute(Object)}, sets its display names and descriptions, then delegates to
     * {@link #doDecode(Object)}.
     * 
     * {@inheritDoc}
     */
    @Nullable public final Attribute decode(@Nullable final DecodedType data) throws AttributeDecodingException {
        if (!isInitialized()) {
            throw new AttributeDecodingException("Decoder has not been initialized and so can not be used");
        }

        if (data == null) {
            return null;
        }

        log.debug("Decoding IdP attribute {}", getDecodedAttributeId());
        final Attribute<ValueType> attribute = createAttribute(data);
        assert attribute != null : "createAttribute() returned a null Attribute";
        attribute.setDisplayNames(displayNames);
        attribute.setDisplayDescriptions(displayDescriptions);

        doDecode(attribute, data);

        return attribute;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return Objects.hashCode(getProtocol(), getDecodedAttributeId());
    }

    /** {@inheritDoc} */
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof AttributeDecoder) {
            AttributeDecoder other = (AttributeDecoder) obj;
            return Objects.equal(getProtocol(), other.getProtocol())
                    && Objects.equal(getDecodedAttributeId(), other.getDecodedAttributeId());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String toString() {
        return Objects.toStringHelper(this).add("protocol", getProtocol())
                .add("decodedAttributeId", getDecodedAttributeId()).add("displayNames", displayNames)
                .add("displayDescriptions", displayDescriptions).toString();
    }

    /**
     * Initializes the display name and descriptors to empty collections if they are null.
     * 
     * {@inheritDoc}
     */
    protected void doInitialize() throws ComponentInitializationException {
        if (displayNames == null || displayNames.isEmpty()) {
            displayNames = Collections.emptyMap();
        }

        if (displayDescriptions == null || displayDescriptions.isEmpty()) {
            displayDescriptions = Collections.emptyMap();
        }
    }

    /**
     * Creates the attribute that will receive the decoded values. The created attributed is a {@link Attribute} whose
     * ID, display name, and display descriptions are set to {@link #getDecodedAttributeId()},
     * {@link #getDisplayNames()} and {@link #getDisplayDescriptions()}, respectively.
     * 
     * @param data SAML attribute to be decoded
     * 
     * @return the attribute that will receive the decoded values
     * 
     * @throws AttributeDecodingException thrown if there is a problem creating the IdP attribute from the given SAML
     *             attribute
     */
    @Nonnull protected Attribute createAttribute(@Nonnull final DecodedType data) throws AttributeDecodingException {
        return new Attribute<ValueType>(getDecodedAttributeId());
    }

    /**
     * Decodes the data in to the constructed IdP attribute. At this point the encoder is guaranteed to be initialized
     * and the IdP attribute contains its ID, display names, and display descriptions.
     * 
     * @param attribute the IdP attribute that will receive the decoded data
     * @param data the data to decode
     * 
     * @throws AttributeDecodingException thrown if there is a problem decoding the data in to the IdP attribute
     */
    protected abstract void doDecode(@Nonnull final Attribute<ValueType> attribute, @Nonnull final DecodedType data)
            throws AttributeDecodingException;
}