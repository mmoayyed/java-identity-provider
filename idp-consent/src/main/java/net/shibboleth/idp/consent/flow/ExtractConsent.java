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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.collection.Pair;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consent action which reads serialized consent objects from a HTTP form body.
 */
public class ExtractConsent extends AbstractConsentAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ExtractConsent.class);

    /** Parameters name for consents. */
    @Nonnull @NotEmpty private static final String CONSENT_REQUEST_PARAMETER = "consents";

    /** Consent serializer. */
    private StorageSerializer<Map<String, Consent>> consentSerializer;

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        if (!(super.doPreExecute(profileRequestContext, interceptorContext))) {
            return false;
        }

        consentSerializer = getConsentFlowDescriptor().getConsentSerializer();

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

        final String[] consentParams = request.getParameterValues(CONSENT_REQUEST_PARAMETER);
        log.debug("{} Consent paramter values '{}'", getLogPrefix(), consentParams);
        if (consentParams == null) {
            log.debug("{} No consent choices", getLogPrefix());
            // TODO
            return;
        }

        final Map<String, Consent> userConsents = new LinkedHashMap<>();
        for (final String consentParam : consentParams) {
            try {
                final Map<String, Consent> consents = consentSerializer.deserialize(0, null, null, consentParam, null);
                userConsents.putAll(consents);
            } catch (IOException e) {
                log.debug("{} Unable to serialize consent.", getLogPrefix(), e);
                ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
                return;
            }
        }

        final Map<String, Consent> chosenConsents = new LinkedHashMap<>();

        final Map<String, Pair<Consent, String>> consentChoices = consentContext.getConsentChoices();
        for (final Map.Entry<String, Pair<Consent, String>> entry : consentChoices.entrySet()) {
            final Consent consent = entry.getValue().getFirst();

            if (userConsents.containsKey(consent.getId())) {
                consent.setApproved(Boolean.TRUE);
            } else {
                consent.setApproved(Boolean.FALSE);
            }

            chosenConsents.put(consent.getId(), consent);
        }

        consentContext.setChosenConsents(chosenConsents);

        // TODO Read expiration
        // Long chosenExpiration = null;
        // consentContext.setChosenExpiration(chosenExpiration);

        log.debug("{} Consent context '{}'", getLogPrefix(), consentContext);
    }

}
