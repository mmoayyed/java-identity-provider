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

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.flow.AbstractConsentAction;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageSerializer;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Base for class for consent actions which interact with a {@link StorageService}.
 * 
 * TODO details
 */
// TODO this class might go away
public abstract class AbstractConsentStorageAction extends AbstractConsentAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractConsentStorageAction.class);

    /** Flow descriptor. */
    @Nullable private ProfileInterceptorFlowDescriptor flowDescriptor;

    /** Storage service. */
    @Nullable private StorageService storageService;

    /** Strategy used to determine the storage context. */
    @Nullable private Function<ProfileRequestContext, String> storageContextLookupStrategy;

    /** Strategy used to determine the storage key. */
    @Nullable private Function<ProfileRequestContext, String> storageKeyLookupStrategy;

    /** Storage context. */
    @Nullable private String context;

    /** Storage key. */
    @Nullable private String key;

    /** Attribute consent serializer. */
    @Nonnull private StorageSerializer<Map<String, Consent>> consentSerializer;

    /**
     * Get the profile interceptor flow descriptor.
     * 
     * @return the profile interceptor flow descriptor
     */
    @Nullable public ProfileInterceptorFlowDescriptor getFlowDescriptor() {
        return flowDescriptor;
    }

    /**
     * Get the storage service.
     * 
     * @return the storage service
     */
    @Nullable public StorageService getStorageService() {
        return storageService;
    }

    /**
     * Get the storage context lookup strategy.
     * 
     * @return the storage context lookup strategy
     */
    @Nullable public Function<ProfileRequestContext, String> getStorageContextLookupStrategy() {
        return storageContextLookupStrategy;
    }

    /**
     * Get the storage key lookup strategy.
     * 
     * @return the storage key lookup strategy
     */
    @Nullable public Function<ProfileRequestContext, String> getStorageKeyLookupStrategy() {
        return storageKeyLookupStrategy;
    }

    /**
     * Get the consent serializer.
     * 
     * @return the consent serializer
     */
    public StorageSerializer<Map<String, Consent>> getConsentSerializer() {
        return consentSerializer;
    }

    /**
     * Get the storage context.
     * 
     * @return the storage context
     */
    @Nullable public String getContext() {
        return context;
    }

    /**
     * Get the storage key.
     * 
     * @return the storage key
     */
    @Nullable public String getKey() {
        return key;
    }

// Checkstyle: ReturnCount OFF
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        // TODO build some event when required data is missing ?

        if (!super.doPreExecute(profileRequestContext, interceptorContext)) {
            // TODO log
            return false;
        }

        flowDescriptor = interceptorContext.getAttemptedFlow();
        log.trace("{} Flow descriptor '{}'", getLogPrefix(), flowDescriptor);
        if (flowDescriptor == null) {
            log.debug("{} No flow descriptor available from interceptor context", getLogPrefix());
            return false;
        }

        storageService = flowDescriptor.getStorageService();
        log.trace("{} Storage service '{}'", getLogPrefix(), storageService);
        if (storageService == null) {
            log.debug("{} No storage service available from interceptor context", getLogPrefix());
            return false;
        }

        storageContextLookupStrategy = flowDescriptor.getStorageContextLookupStrategy();
        log.trace("{} Storage context lookup strategy '{}'", getLogPrefix(), storageContextLookupStrategy);
        if (storageContextLookupStrategy == null) {
            log.debug("{} No storage context lookup strategy available from flow descriptor", getLogPrefix());
            return false;
        }

        storageKeyLookupStrategy = flowDescriptor.getStorageKeyLookupStrategy();
        log.trace("{} Storage key lookup strategy '{}'", getLogPrefix(), storageKeyLookupStrategy);
        if (storageKeyLookupStrategy == null) {
            log.debug("{} No storage key lookup strategy available from flow descriptor", getLogPrefix());
            return false;
        }

        context = storageContextLookupStrategy.apply(profileRequestContext);
        log.trace("{} Storage context '{}'", getLogPrefix(), context);
        if (context == null) {
            log.debug("{} No storage context", getLogPrefix());
            return false;
        }

        key = storageKeyLookupStrategy.apply(profileRequestContext);
        log.trace("{} Storage key '{}'", getLogPrefix(), key);
        if (key == null) {
            log.debug("{} No storage key", getLogPrefix());
            return false;
        }

        consentSerializer = getConsentFlowDescriptor().getConsentSerializer();
        log.trace("{} Consent serializer '{}'", getLogPrefix(), consentSerializer);
        if (consentSerializer == null) {
            log.debug("{} No consent serializer available from consent flow descriptor", getLogPrefix());
            return false;
        }

        return true;
    }
// Checkstyle: ReturnCount ON
}
