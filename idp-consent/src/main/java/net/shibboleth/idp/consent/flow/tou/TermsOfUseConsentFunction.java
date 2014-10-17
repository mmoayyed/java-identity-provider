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

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.flow.ConsentFlowDescriptor;
import net.shibboleth.idp.consent.logic.FlowDescriptorLookupStrategy;
import net.shibboleth.idp.consent.logic.HashFunction;
import net.shibboleth.idp.consent.logic.LocaleLookupStrategy;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.NoSuchMessageException;

import com.google.common.base.Function;

/**
 * Function that returns a consent object representing consent to a terms of use. The id of the terms of use consent
 * object is resolved from a message source. The value of the consent object is the hash of the text of the terms of use
 * resolved from a message source.
 */
public class TermsOfUseConsentFunction implements Function<ProfileRequestContext, Map<String, Consent>>,
        MessageSourceAware {

    /** Message code used to resolve the terms of use id. */
    @Nonnull public static final String TERMS_OF_USE_ID_CODE = "idp.tou.id";

    /** Message code used to resolve the terms of use text. */
    @Nonnull public static final String TERMS_OF_USE_TEXT_CODE = "idp.tou.text";

    /** Consent flow descriptor lookup strategy. */
    @Nonnull private Function<ProfileRequestContext, ConsentFlowDescriptor> consentFlowDescriptorLookupStrategy;

    /** Function used to create a hash of the terms of use text. */
    @Nullable @NonnullAfterInit private Function<String, String> hashFunction;

    /** Locale lookup strategy. */
    @Nonnull private Function<ProfileRequestContext, Locale> localeLookupStrategy;

    /** MessageSource injected by Spring, typically the parent ApplicationContext itself. */
    @Nonnull private MessageSource messageSource;

    /** Constructor. */
    public TermsOfUseConsentFunction() {
        consentFlowDescriptorLookupStrategy = new FlowDescriptorLookupStrategy<ConsentFlowDescriptor>();
        hashFunction = new HashFunction();
        localeLookupStrategy = new LocaleLookupStrategy();
    }

    /** {@inheritDoc} */
    @Override public void setMessageSource(MessageSource source) {
        messageSource = source;
    }

    /**
     * Get the terms of use id.
     * 
     * @param profileRequestContext profile request context
     * @return terms of use id
     */
    @Nullable protected String getConsentId(@Nonnull final ProfileRequestContext profileRequestContext) {
        try {
            return messageSource.getMessage(TERMS_OF_USE_ID_CODE, null, getLocale(profileRequestContext));
        } catch (final NoSuchMessageException e) {
            return null;
        }
    }

    /**
     * Get the hash of the terms of use text.
     * 
     * @param profileRequestContext profile request context
     * @return hash of the terms of use text
     */
    @Nullable protected String getConsentValue(@Nonnull final ProfileRequestContext profileRequestContext) {
        return hashFunction.apply(getTermsOfUseText(profileRequestContext));
    }

    /**
     * Get the locale.
     * 
     * @param profileRequestContext profile request context
     * @return locale
     */
    @Nullable protected Locale getLocale(@Nonnull final ProfileRequestContext profileRequestContext) {
        return localeLookupStrategy.apply(profileRequestContext);
    }

    /**
     * Get the terms of use text.
     * 
     * @param profileRequestContext profile request context
     * @return terms of use text
     */
    @Nullable protected String getTermsOfUseText(@Nonnull final ProfileRequestContext profileRequestContext) {
        try {
            return messageSource.getMessage(TERMS_OF_USE_TEXT_CODE, null, getLocale(profileRequestContext));
        } catch (final NoSuchMessageException e) {
            return null;
        }
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
    @Nullable public Map<String, Consent> apply(@Nullable final ProfileRequestContext input) {
        if (input == null) {
            return null;
        }
    
        final Consent consent = new Consent();
        consent.setId(getConsentId(input));
    
        if (isCompareValues(input)) {
            consent.setValue(getConsentValue(input));
        }
    
        return Collections.singletonMap(consent.getId(), consent);
    }
}
