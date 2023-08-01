/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.consent.storage.impl;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;

import net.shibboleth.idp.profile.interceptor.AbstractProfileInterceptorResult;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * The result of a consent flow, suitable for storage.
 */
public class ConsentResult extends AbstractProfileInterceptorResult {

    /**
     * Constructor.
     *
     * @param context storage context
     * @param key storage key
     * @param value storage value
     * @param expiration storage expiration
     */
    public ConsentResult(
            @Nonnull @NotEmpty final String context,
            @Nonnull @NotEmpty final String key,
            @Nonnull @NotEmpty final String value,
            @Nullable final Instant expiration) {
        super(context, key, value, expiration);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", getId())
                .add("context", getStorageContext())
                .add("key", getStorageKey())
                .add("value", getStorageValue())
                .add("expiration", getStorageExpiration())
                .toString();
    }

}