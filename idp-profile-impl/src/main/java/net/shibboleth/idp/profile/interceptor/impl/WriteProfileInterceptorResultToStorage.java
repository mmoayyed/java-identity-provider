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

package net.shibboleth.idp.profile.interceptor.impl;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.AbstractProfileInterceptorAction;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorResult;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An action that writes a {@link ProfileInterceptorResult} to a {@link StorageService}.
 * 
 * TODO details
 */
// TODO tests
public class WriteProfileInterceptorResultToStorage extends AbstractProfileInterceptorAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(WriteProfileInterceptorResultToStorage.class);

    /** Flow descriptor. */
    @Nullable private ProfileInterceptorFlowDescriptor flowDescriptor;

    /** Result to be stored. */
    @Nullable private ProfileInterceptorResult result;

    /** Storage service. */
    @Nullable private StorageService storageService;

    /** Storage context. */
    @Nonnull @NotEmpty private String context;

    /** Storage key. */
    @Nonnull @NotEmpty private String key;

    /** Storage value. */
    @Nonnull @NotEmpty private String value;

    /** Storage expiration. */
    @Nullable @Positive @Duration private Long expiration;

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        // TODO build some event when required data is missing ?

        flowDescriptor = interceptorContext.getAttemptedFlow();
        if (flowDescriptor == null) {
            log.debug("{} No flow descriptor within interceptor context", getLogPrefix());
            return false;
        }

        storageService = flowDescriptor.getStorageService();
        if (storageService == null) {
            log.debug("{} No storage service available from interceptor flow descriptor", getLogPrefix());
            return false;
        }

        result = interceptorContext.getResult();
        if (result == null) {
            log.debug("{} No result available from interceptor context, nothing to store", getLogPrefix());
            return false;
        }

        context = result.getStorageContext();
        key = result.getStorageKey();
        value = result.getStorageValue();
        expiration = result.getStorageExpiration();

        return super.doPreExecute(profileRequestContext, interceptorContext);
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        try {
            // Create / update loop until we succeed or exhaust attempts.
            int attempts = 10;
            boolean success = false;
            do {
                success = storageService.create(context, key, value, expiration);
                if (!success) {
                    // The record already exists, so we need to overwrite via an update.
                    success = storageService.update(context, key, value, expiration);
                }
            } while (!success && attempts-- > 0);

            if (!success) {
                log.error("{} Exhausted retry attempts storing result '{}'", getLogPrefix(), result);
            }
        } catch (final IOException e) {
            log.error("{} Unable to write result '{}' to storage", getLogPrefix(), result, e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }
    }
}
