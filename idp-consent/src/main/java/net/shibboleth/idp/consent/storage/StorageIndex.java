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

package net.shibboleth.idp.consent.storage;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.MoreObjects;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

/**
 * Holds a storage context and associated keys to serve as a secondary index for lookup of records from storage.
 */
// TODO tests
public class StorageIndex {

    /** Storage context. */
    @Nullable private String context;

    /** Storage keys associated with the storage context. */
    @Nonnull @NonnullElements @Live private Set<String> keys;

    /** Constructor. */
    public StorageIndex() {
        keys = new LinkedHashSet<>();
    }

    /**
     * Get the storage context.
     * 
     * @return storage context
     */
    @Nullable public String getContext() {
        return context;
    }

    /**
     * Set the storage context.
     * 
     * @param storageContext the storage context
     */
    public void setContext(@Nonnull @NotEmpty final String storageContext) {
        context = Constraint.isNotNull(StringSupport.trimOrNull(storageContext), "Context cannot be null nor empty");
    }

    /**
     * Get the storage keys associated with the storage context.
     * 
     * @return storage keys associated with the storage context
     */
    @Nonnull @NonnullElements @Live public Set<String> getKeys() {
        return keys;
    }

    /**
     * Set the storage keys associated with the storage context.
     * 
     * @param storageKeys the storage keys associated with the storage context
     */
    public void setKeys(@Nonnull @NonnullElements final Set<String> storageKeys) {
        Constraint.isNotNull(storageKeys, "Storage keys cannot be null");

        keys = new LinkedHashSet<>(Collections2.filter(storageKeys, Predicates.notNull()));
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof StorageIndex)) {
            return false;
        }

        final StorageIndex other = (StorageIndex) obj;

        return Objects.equals(getContext(), other.getContext()) && Objects.equals(getKeys(), other.getKeys());
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return Objects.hash(getContext(), getKeys());
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return MoreObjects.toStringHelper(this).add("context", getContext()).add("keys", getKeys()).toString();
    }

}
