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
import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consent action which reads consents from storage and adds them to the consent context as previous consents.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 */
public class ReadConsentFromStorage extends AbstractConsentStorageAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ReadConsentFromStorage.class);

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        try {
            final StorageRecord storageRecord = getStorageService().read(getStorageContext(), getStorageKey());
            log.debug("{} Read storage record '{}'", getLogPrefix(), storageRecord);

            if (storageRecord == null) {
                // TODO
                return;
            }

            final Map<String, Consent> consents =
                    (Map<String, Consent>) storageRecord.getValue(getStorageSerializer(), getStorageContext(),
                            getStorageKey());

            getConsentContext().getPreviousConsents().putAll(consents);

        } catch (final IOException e) {
            log.error("{} Unable to read consent from storage", getLogPrefix(), e);
        }
    }
    
}