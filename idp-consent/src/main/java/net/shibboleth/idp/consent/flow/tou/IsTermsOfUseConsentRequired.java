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

package net.shibboleth.idp.consent.flow.tou;

import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.ConsentEventIds;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action to determine whether terms of use consent is required.
 * 
 * TODO details
 */
public class IsTermsOfUseConsentRequired extends AbstractTermsOfUseAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(IsTermsOfUseConsentRequired.class);

    /** Terms of use. */
    @Nullable private TermsOfUse termsOfUse;

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        if (!super.doPreExecute(profileRequestContext, interceptorContext)) {
            return false;
        }

        termsOfUse = getTermsOfUseContext().getTermsOfUse();
        if (termsOfUse == null) {
            log.debug("{} Terms of use cannot be null", getLogPrefix());
            // TODO event ?
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        final Map<String, Consent> storedConsents = getConsentContext().getPreviousConsents();
        if (storedConsents == null || storedConsents.isEmpty()) {
            log.debug("{} Terms of use consent is required because there is no previous consent.", getLogPrefix());
            return;
        }

        for (final Consent consent : storedConsents.values()) {
            if (Objects.equals(consent.getId(), termsOfUse.getId())) {
                if (getConsentFlowDescriptor().compareValues()) {
                    final String hash = getTermsOfUseFlowDescriptor().getTermsOfUseHashFunction().apply(termsOfUse);
                    if (Objects.equals(consent.getValue(), hash)) {
                        log.debug("{} Terms of use consent is not required due to previous consent.", getLogPrefix());
                        ActionSupport.buildEvent(profileRequestContext, ConsentEventIds.ConsentNotRequired.toString());
                        return;
                    }
                } else {
                    log.debug("{} Terms of use consent is not required due to previous consent.", getLogPrefix());
                    ActionSupport.buildEvent(profileRequestContext, ConsentEventIds.ConsentNotRequired.toString());
                    return;
                }
            }
        }

        // TODO log additional info ?
        // TODO audit ?
        log.debug("{} Terms of use consent is required, no previous consents match.", getLogPrefix());
    }
}
