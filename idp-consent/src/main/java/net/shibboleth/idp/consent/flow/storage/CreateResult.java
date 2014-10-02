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
import net.shibboleth.idp.consent.storage.ConsentResult;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorResult;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The result of a consent flow to be stored in a storage service.
 */
// TODO just a stub
public class CreateResult extends AbstractConsentStorageAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CreateResult.class);

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        if (!super.doPreExecute(profileRequestContext, interceptorContext)) {
            log.debug("TODO");
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        try {
            final Map<String, Consent> chosenAttributeConsents = getConsentContext().getChosenConsents();

            final String value = getConsentSerializer().serialize(chosenAttributeConsents);

            // TODO expiration

            final ProfileInterceptorResult result = new ConsentResult(getContext(), getKey(), value, null);

            interceptorContext.setResult(result);

        } catch (IOException e) {
            log.debug("{} Unable to serialize consent.", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }
    }

}
