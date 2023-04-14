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

package net.shibboleth.idp.consent.flow.storage.impl;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;

import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * Consent action which maintains a storage record whose value is the current time in milliseconds. The storage record
 * version may be used to track the number of times this action, or a flow, has been executed.
 */
public class UpdateCounter extends AbstractConsentStorageAction {

    /** Storage context for the storage index record. */
    @Nonnull @NotEmpty public static final String COUNTER_KEY = "_counter";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(UpdateCounter.class);

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {
        try {
            final String value = Long.toString(System.currentTimeMillis());

            final String context = getStorageContext();
            final String key = getStorageKey();
            final StorageService service = getStorageService();
            assert context!=null && key!=null && service!=null && value != null;

            final StorageRecord<?> storageRecord = service.read(context, key);
            log.debug("{} Read storage record '{}' with context '{}' and key '{}'", getLogPrefix(), storageRecord,
                    context, key);

            if (storageRecord == null) {
                log.debug("{} Creating counter with value '{}'", getLogPrefix(), value);
                service.create(context, key, value, null);
            } else {
                log.debug("{} Updating counter with value '{}'", getLogPrefix(), value);
                service.update(context, key, value, null);
            }

        } catch (final IOException e) {
            log.debug("{} Unable to update counter", getLogPrefix(), e);
        }
    }

}
