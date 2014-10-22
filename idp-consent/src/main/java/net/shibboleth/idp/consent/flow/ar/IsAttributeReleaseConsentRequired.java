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

package net.shibboleth.idp.consent.flow.ar;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.ConsentEventIds;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Attribute consent action which determines if consent is required and returns the appropriate event id.
 * 
 * TODO details
 */
public class IsAttributeReleaseConsentRequired extends AbstractAttributeReleaseAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(IsAttributeReleaseConsentRequired.class);

    /** Function to create hash of all attribute values. */
    @Nonnull private Function<Collection<IdPAttributeValue<?>>, String> attributeValuesHashFunction;

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        if (!super.doPreExecute(profileRequestContext, interceptorContext)) {
            return false;
        }
        
        attributeValuesHashFunction = getAttributeReleaseFlowDescriptor().getAttributeValuesHashFunction();

        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        // TODO general consent

        // TODO consent always required

        // TODO wildcard

        // TODO match attributes
        if (isAttributeConsentRequired(getConsentContext())) {
            // TODO proceed
        } else {
            ActionSupport.buildEvent(profileRequestContext, ConsentEventIds.ConsentNotRequired.toString());
        }

    }

    /**
     * Whether consent is required by matching stored consents to attributes.
     * 
     * @param consentContext consent context
     * 
     * @return true if consent is required
     */
    private boolean isAttributeConsentRequired(@Nonnull final ConsentContext consentContext) {

        final Map<String, IdPAttribute> attributes = getAttributeReleaseContext().getConsentableAttributes();
        if (attributes.isEmpty()) {
            log.debug("{} Consent is not required because there are no attributes to consent to.", getLogPrefix());
            return false;
        }

        final Map<String, Consent> previousConsents = consentContext.getPreviousConsents();
        if (previousConsents == null || previousConsents.isEmpty()) {
            log.debug("{} Consent is required because there is no previous consent.", getLogPrefix());
            return true;
        }

        for (IdPAttribute attribute : attributes.values()) {
            final Consent consent = previousConsents.get(attribute.getId());
            if (consent == null) {
                log.debug("{} Consent is required because there is a new attribute to consent to.", getLogPrefix());
                return true;
            }
            
            // TODO compare values
            
            final String hash = attributeValuesHashFunction.apply(attribute.getValues());
            if (!hash.equals(consent.getValue())) {
                log.debug("{} Consent is required because an attribute has changed since the previous consent.",
                        getLogPrefix());
                return true;
            }
        }

        log.debug("{} Consent not required, all attributes match stored consents.", getLogPrefix());
        return false;
    }
}
