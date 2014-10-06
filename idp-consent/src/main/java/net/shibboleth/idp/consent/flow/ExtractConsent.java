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

package net.shibboleth.idp.consent.flow;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Consent action which reads serialized consent objects from an HTTP form body.
 */
public class ExtractConsent extends AbstractConsentAction {

    /** Parameter name for consent IDs. */
    @Nonnull @NotEmpty private static final String CONSENT_IDS_REQUEST_PARAMETER = "consentIds";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ExtractConsent.class);

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        if (!(super.doPreExecute(profileRequestContext, interceptorContext))) {
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        final ConsentContext consentContext = getConsentContext();

        final HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            log.debug("{} Profile action does not contain an HttpServletRequest", getLogPrefix());
            // TODO ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return;
        }

        final String[] consentParams = request.getParameterValues(CONSENT_IDS_REQUEST_PARAMETER);
        log.debug("{} Consent paramter values '{}'", getLogPrefix(), consentParams);
        if (consentParams == null) {
            log.debug("{} No consent choices", getLogPrefix());
            // TODO
            return;
        }

        final Collection<String> consentIds = StringSupport.normalizeStringCollection(Sets.newHashSet(consentParams));

        final Map<String, Consent> currentConsents = getConsentContext().getCurrentConsents();
        for (final Consent consent : currentConsents.values()) {
            if (consentIds.contains(consent.getId())) {
                consent.setApproved(Boolean.TRUE);
            } else {
                consent.setApproved(Boolean.FALSE);
            }
        }

        // TODO Read expiration
        // Long chosenExpiration = null;
        // consentContext.setChosenExpiration(chosenExpiration);

        log.debug("{} Consent context '{}'", getLogPrefix(), consentContext);
    }

}
