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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.MoreObjects;

/**
 * Represents consent to release an attribute.
 * 
 */
public class AttributeConsent {

    /** Attribute identifier. */
    @Nullable private String attributeId;

    /** Hash of all attribute values. */
    @Nullable private String valuesHash;

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

    /** {@inheritDoc} */
    @Override public String toString() {
        return MoreObjects.toStringHelper(this).add("attributeId", attributeId).add("valuesHash", valuesHash)
                .toString();
    }
}
