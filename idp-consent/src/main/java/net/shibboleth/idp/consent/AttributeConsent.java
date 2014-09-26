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

package net.shibboleth.idp.consent;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.joda.time.DateTime;

import com.google.common.base.MoreObjects;

/**
 * Represents consent to release an attribute.
 */
// TODO tests
public class AttributeConsent {

    /** Attribute identifier. */
    @Nullable private String attributeId;

    /** Hash of all attribute values. */
    @Nullable private String valuesHash;
    
    /** When consent expires. */
    @Nullable private DateTime expiration;

    /**
     * Get the attribute identifier.
     * 
     * @return the attribute identifier
     */
    @Nullable public String getAttributeId() {
        return attributeId;
    }

    /**
     * Set the attribute identifier.
     * 
     * @param id The attributeId to set.
     */
    public void setAttributeId(@Nonnull @NotEmpty final String id) {
        attributeId = Constraint.isNotNull(StringSupport.trimOrNull(id), "The attribute id cannot be null or empty");
    }

    /**
     * Get the hash of all attribute values.
     * 
     * @return the hash of all attribute values
     */
    @Nullable public String getValuesHash() {
        return valuesHash;
    }

    /**
     * Set the hash of all attribute values.
     * 
     * @param hash the hash of all attribute values
     */
    public void setValuesHash(@Nonnull @NotEmpty final String hash) {
        valuesHash = Constraint.isNotNull(StringSupport.trimOrNull(hash), "The values hash cannot be null or empty");
    }
    
    /**
     * Get when consent expires.
     * 
     * @return when consent expires
     */
    @Nullable public DateTime getExpiration() {
        return expiration;
    }

    /**
     * Set when consent expires.
     * 
     * @param timestamp when consent expires
     */
    public void setExpiration(@Nonnull final DateTime timestamp) {
        expiration = Constraint.isNotNull(timestamp, "The expiration timestamp cannot be null");
    }
    
    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
    
        if (obj == null) {
            return false;
        }
    
        if (!(obj instanceof AttributeConsent)) {
            return false;
        }
    
        final AttributeConsent other = (AttributeConsent) obj;
    
        return Objects.equals(attributeId, other.getAttributeId()) && Objects.equals(valuesHash, other.getValuesHash())
                && Objects.equals(expiration, other.getExpiration());
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return Objects.hash(attributeId, valuesHash, expiration);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("attributeId", attributeId)
                .add("valuesHash", valuesHash)
                .add("expiration", expiration)
                .toString();
    }
}
