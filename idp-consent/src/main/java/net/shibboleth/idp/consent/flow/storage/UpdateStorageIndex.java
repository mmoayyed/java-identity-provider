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

package net.shibboleth.idp.consent.flow.storage;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.consent.storage.ConsentResult;
import net.shibboleth.idp.consent.storage.StorageIndex;
import net.shibboleth.idp.consent.storage.StorageIndexSerializer;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorResult;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consent storage action which maintains a secondary index of storage records to facilitate lookup of all storage keys
 * for given storage context and user.
 * 
 * The storage context for the secondary index record is {@link #STORAGE_INDEX_CONTEXT}.
 * 
 * The storage key for the secondary index is configurable but is assumed to be a user identifier.
 * 
 * The storage value for the secondary index is the serialized map of storage index objects keyed by the storage index
 * context.
 * 
 * For every interceptor result in the interceptor context, this action attempts to retrieve the associated storage
 * index from the storage service. If the storage index exists in the storage service, the storage key from the
 * interceptor result is added to the storage index. If the storage index did not exist in storage, a new storage index
 * is created using the storage context and storage key from the interceptor result. If the storage index has changed,
 * either because it was created or a new storage key was added, an interceptor result representing the secondary index
 * is added to the interceptor context to be eventually written to storage.
 * 
 * The storage index record should not index itself, so this action does not index interceptor results from the
 * interceptor context whose storage context is {@link #STORAGE_INDEX_CONTEXT}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 */
public class UpdateStorageIndex extends AbstractConsentStorageAction {

    /** Storage context for the storage index record. */
    @Nonnull @NotEmpty public static final String STORAGE_INDEX_CONTEXT = "_storage_idx";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(UpdateStorageIndex.class);

    /** Constructor. */
    public UpdateStorageIndex() {
        setStorageSerializer(new StorageIndexSerializer());
        setStorageContextLookupStrategy(FunctionSupport.<ProfileRequestContext, String>constant(STORAGE_INDEX_CONTEXT));
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        final String context = getStorageContext();
        final String key = getStorageKey();

        try {
            final StorageRecord storageRecord = getStorageService().read(context, key);
            log.debug("{} Read storage record '{}' with context '{}' and key '{}'", getLogPrefix(), storageRecord,
                    context, key);

            Map<String, StorageIndex> storageIndexes = null;
            if (storageRecord == null) {
                storageIndexes = new LinkedHashMap<>();
            } else {
                storageIndexes =
                        (Map<String, StorageIndex>) storageRecord.getValue(getStorageSerializer(), context, key);
            }

            boolean isIndexChanged = false;

            for (final ProfileInterceptorResult result : interceptorContext.getResults()) {
                // Do not index the storage index record itself.
                if (result.getStorageContext().equals(STORAGE_INDEX_CONTEXT)) {
                    continue;
                }

                // Create a new storage index if it does not exist already in storage.
                StorageIndex storageIndex = storageIndexes.get(result.getStorageContext());
                if (storageIndex == null) {
                    storageIndex = new StorageIndex();
                    storageIndex.setContext(result.getStorageContext());
                    storageIndexes.put(storageIndex.getContext(), storageIndex);
                    isIndexChanged = true;
                }

                // Add the key to the storage index.
                boolean newkey = storageIndex.getKeys().add(result.getStorageKey());
                if (newkey) {
                    isIndexChanged = true;
                }
            }

            if (isIndexChanged) {
                final String serializedStorageIndex = getStorageSerializer().serialize(storageIndexes);
                final ConsentResult result = new ConsentResult(context, key, serializedStorageIndex, null);
                interceptorContext.getResults().add(result);
                log.debug("{} Consent index has changed, adding result '{}' to interceptor context", getLogPrefix(),
                        result);
            } else {
                log.debug("{} Consent index has not changed, nothing to do", getLogPrefix());
            }

        } catch (final IOException e) {
            log.error("{} Unable to read index from storage", getLogPrefix(), e);
        }
    }
}
