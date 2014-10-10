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
import net.shibboleth.idp.consent.logic.FlowIdLookupFunction;
import net.shibboleth.idp.consent.storage.ConsentSerializer;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

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

    /** Strategy used to determine the storage context. */
    @Nullable private Function<ProfileRequestContext, String> storageContextLookupStrategy;

    /** Strategy used to determine the storage key. */
    @Nullable private Function<ProfileRequestContext, String> storageKeyLookupStrategy;

    /** Storage serializer for map of consent objects keyed by consent id. */
    @Nonnull private StorageSerializer<Map<String, Consent>> consentSerializer;

    /** Storage service from the {@link ProfileInterceptorFlowDescriptor}. */
    @Nullable private StorageService storageService;

    /** Storage context resulting from lookup strategy. */
    @Nullable private String context;

    /** Storage key resulting from lookup strategy. */
    @Nullable private String key;

    /** Constructor. */
    public AbstractConsentStorageAction() {
        setStorageContextLookupStrategy(new FlowIdLookupFunction());
        setConsentSerializer(new ConsentSerializer());
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
     * Set the consent serializer.
     * 
     * @param serializer consent serializer
     */
    public void setConsentSerializer(@Nonnull final StorageSerializer<Map<String, Consent>> serializer) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        consentSerializer = Constraint.isNotNull(serializer, "Consent serializer cannot be null");
    }

    /**
     * Set the storage context lookup strategy.
     * 
     * @param strategy the storage context lookup strategy
     */
    public void setStorageContextLookupStrategy(@Nonnull final Function<ProfileRequestContext, String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        storageContextLookupStrategy = Constraint.isNotNull(strategy, "Storage context lookup strategy cannot be null");
    }

    /**
     * Set the storage key lookup strategy.
     * 
     * @param strategy the storage key lookup strategy
     */
    public void setStorageKeyLookupStrategy(@Nonnull final Function<ProfileRequestContext, String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        storageKeyLookupStrategy = Constraint.isNotNull(strategy, "Storage key lookup strategy cannot be null");
    }

    /**
     * Get the storage service from the {@link ProfileInterceptorFlowDescriptor}.
     * 
     * @return the storage service
     */
    @Nullable public StorageService getStorageService() {
        return storageService;
    }

    /**
     * Get the storage context resulting from lookup strategy.
     * 
     * @return the storage context
     */
    @Nullable public String getContext() {
        return context;
    }

    /**
     * Get the storage key resulting from lookup strategy.
     * 
     * @return the storage key
     */
    @Nullable public String getKey() {
        return key;
    }

    // Checkstyle: ReturnCount OFF
    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        // TODO build some event when required data is missing ?

        if (!super.doPreExecute(profileRequestContext, interceptorContext)) {
            return false;
        }

        final ProfileInterceptorFlowDescriptor flowDescriptor = interceptorContext.getAttemptedFlow();
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

        log.trace("{} Consent serializer '{}'", getLogPrefix(), consentSerializer);
        if (consentSerializer == null) {
            log.debug("{} No consent serializer available from consent flow descriptor", getLogPrefix());
            return false;
        }

        log.trace("{} Storage context lookup strategy '{}'", getLogPrefix(), storageContextLookupStrategy);
        if (storageContextLookupStrategy == null) {
            log.debug("{} No storage context lookup strategy available from flow descriptor", getLogPrefix());
            return false;
        }

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

        return true;
    }
    // Checkstyle: ReturnCount ON
}
