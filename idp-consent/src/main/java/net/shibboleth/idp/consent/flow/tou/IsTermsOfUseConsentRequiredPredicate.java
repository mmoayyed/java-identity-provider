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
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.consent.flow.ConsentFlowDescriptor;
import net.shibboleth.idp.consent.logic.FlowDescriptorLookup;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * Predicate that returns whether terms of use consent is required.
 */
// TODO tests
public class IsTermsOfUseConsentRequiredPredicate implements Predicate<ProfileRequestContext> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(IsTermsOfUseConsentRequiredPredicate.class);

    /** Consent context lookup strategy. */
    @Nonnull private Function<ProfileRequestContext, ConsentContext> consentContextLookupStrategy;

    /** Consent flow descriptor lookup strategy. */
    @Nonnull private Function<ProfileRequestContext, ConsentFlowDescriptor> consentFlowDescriptorLookupStrategy;

    /** Constructor. */
    public IsTermsOfUseConsentRequiredPredicate() {
        consentContextLookupStrategy = new ChildContextLookup<>(ConsentContext.class);
        consentFlowDescriptorLookupStrategy =
                new FlowDescriptorLookup<ConsentFlowDescriptor>(ConsentFlowDescriptor.class);
    }

    /**
     * Set the consent context lookup strategy.
     * 
     * @param strategy consent context lookup strategy
     */
    public void
            setConsentContextLookupStrategy(@Nonnull final Function<ProfileRequestContext, ConsentContext> strategy) {
        consentContextLookupStrategy = Constraint.isNotNull(strategy, "Consent context lookup strategy cannot be null");
    }

    /**
     * Set the consent flow descriptor lookup strategy.
     * 
     * @param strategy consent flow descriptor lookup strategy
     */
    public void setConsentFlowDescriptorLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, ConsentFlowDescriptor> strategy) {
        consentFlowDescriptorLookupStrategy =
                Constraint.isNotNull(strategy, "Consent flow descriptor lookup strategy cannot be null");
    }

    /**
     * Whether consent equality includes comparing consent values.
     * 
     * @param profileRequestContext profile request context
     * @return true if consent equality includes comparing consent values
     */
    protected boolean isCompareValues(@Nonnull final ProfileRequestContext profileRequestContext) {
        final ConsentFlowDescriptor consentFlowDescriptor =
                consentFlowDescriptorLookupStrategy.apply(profileRequestContext);
        if (consentFlowDescriptor != null) {
            return consentFlowDescriptor.compareValues();
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public boolean apply(@Nullable final ProfileRequestContext input) {
        if (input == null) {
            log.debug("Terms of use consent is not required, no profile request context");
            return false;
        }

        final ConsentContext consentContext = consentContextLookupStrategy.apply(input);
        if (consentContext == null) {
            log.debug("Terms of use consent is not required, no consent context");
            return false;
        }

        final Map<String, Consent> previousConsents = consentContext.getPreviousConsents();
        if (previousConsents.isEmpty()) {
            log.debug("Terms of use consent is required, no previous consents");
            return true;
        }

        final Map<String, Consent> currentConsents = consentContext.getCurrentConsents();
        for (final Consent currentConsent : currentConsents.values()) {
            final Consent previousConsent = previousConsents.get(currentConsent.getId());
            if (previousConsent == null) {
                log.debug("Terms of use consent is required, no previous consent for '{}'", currentConsent);
                return true;
            }
            if (isCompareValues(input) && !Objects.equals(currentConsent.getValue(), previousConsent.getValue())) {
                log.debug(
                        "Terms of use consent is required, previous consent '{}' does not match current consent '{}'",
                        previousConsent, currentConsent);
                return true;
            }
        }

        log.debug("Terms of use consent is not required, previous consents match");
        return false;
    }

}
