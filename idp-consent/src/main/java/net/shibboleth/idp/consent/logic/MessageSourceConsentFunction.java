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

package net.shibboleth.idp.consent.logic;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.flow.ConsentFlowDescriptor;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.NoSuchMessageException;

import com.google.common.base.Function;

/**
 * Function that returns a consent object whose id and value are resolved from a message source.
 */
public class MessageSourceConsentFunction extends AbstractInitializableComponent implements
        Function<ProfileRequestContext, Map<String, Consent>>, MessageSourceAware {

    /** Message code used to resolve the consent id. */
    @Nonnull private String consentIdMessageCode;

    /** Message code used to resolve the consent value. */
    @Nonnull private String consentValueMessageCode;

    /** Consent flow descriptor lookup strategy. */
    @Nonnull private Function<ProfileRequestContext, ConsentFlowDescriptor> consentFlowDescriptorLookupStrategy;

    /** Function used to create a hash of the consent value. */
    @Nonnull private Function<String, String> hashFunction;

    /** Locale lookup strategy. */
    @Nonnull private Function<ProfileRequestContext, Locale> localeLookupStrategy;

    /** MessageSource injected by Spring, typically the parent ApplicationContext itself. */
    @Nonnull private MessageSource messageSource;

    /** Constructor. */
    public MessageSourceConsentFunction() {
        consentFlowDescriptorLookupStrategy =
                new FlowDescriptorLookupFunction<ConsentFlowDescriptor>(ConsentFlowDescriptor.class);
        hashFunction = new HashFunction();
        localeLookupStrategy = new LocaleLookupFunction();
    }

    /** {@inheritDoc} */
    @Override
    public void setMessageSource(MessageSource source) {
        messageSource = source;
    }

    /**
     * Get the consent id message code.
     * 
     * @return consent id message code
     */
    @Nullable public String getConsentIdMessageCode() {
        return consentIdMessageCode;
    }

    /**
     * Set the consent id message code.
     * 
     * @param messageCode consent id message code
     */
    public void setConsentIdMessageCode(@Nonnull @NotEmpty final String messageCode) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        consentIdMessageCode =
                Constraint.isNotNull(StringSupport.trimOrNull(messageCode),
                        "Consent id message code cannot be null nor empty");
    }

    /**
     * Get the consent value message code.
     * 
     * @return consent value message code
     */
    @Nullable public String getConsentValueMessageCode() {
        return consentValueMessageCode;
    }

    /**
     * Set the consent value message code.
     * 
     * @param messageCode The consentValueMessageCode to set.
     */
    public void setConsentValueMessageCode(@Nonnull @NotEmpty final String messageCode) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        consentValueMessageCode =
                Constraint.isNotNull(StringSupport.trimOrNull(messageCode),
                        "Consent value message code cannot be null nor empty");
    }

    /**
     * Set the consent flow descriptor lookup strategy.
     * 
     * @param strategy consent flow descriptor lookup strategy
     */
    public void setConsentFlowDescriptorLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, ConsentFlowDescriptor> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        consentFlowDescriptorLookupStrategy =
                Constraint.isNotNull(strategy, "Consent flow descriptor lookup strategy cannot be null");
    }

    /**
     * Set the hash function.
     * 
     * @param function hash function
     */
    public void setHashFunction(@Nonnull final Function<String, String> function) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        hashFunction = Constraint.isNotNull(function, "Hash function cannot be null");
    }

    /**
     * Set the locale lookup strategy.
     * 
     * @param strategy The localeLookupStrategy to set.
     */
    public void setLocaleLookupStrategy(@Nonnull final Function<ProfileRequestContext, Locale> strategy) {
        localeLookupStrategy = Constraint.isNotNull(strategy, "Locale lookup strategy cannot be null");
    }

    /**
     * Get the consent id.
     * 
     * @param profileRequestContext profile request context
     * @return consent id
     */
    @Nullable protected String getConsentId(@Nonnull final ProfileRequestContext profileRequestContext) {
        try {
            return messageSource.getMessage(consentIdMessageCode, null, getLocale(profileRequestContext));
        } catch (final NoSuchMessageException e) {
            return null;
        }
    }

    /**
     * Get the consent value.
     * 
     * @param profileRequestContext profile request context
     * @return consent value
     */
    @Nullable protected String getConsentValue(@Nonnull final ProfileRequestContext profileRequestContext) {
        try {
            return messageSource.getMessage(consentValueMessageCode, null, getLocale(profileRequestContext));
        } catch (final NoSuchMessageException e) {
            return null;
        }
    }

    /**
     * Get the consent value hash.
     * 
     * @param profileRequestContext profile request context
     * @return consent value hash
     */
    @Nullable protected String getConsentValueHash(@Nonnull final ProfileRequestContext profileRequestContext) {
        return hashFunction.apply(getConsentValue(profileRequestContext));
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
    @Nullable public Map<String, Consent> apply(@Nullable final ProfileRequestContext input) {
        if (input == null) {
            return null;
        }

        final Consent consent = new Consent();
        consent.setId(getConsentId(input));

        if (isCompareValues(input)) {
            consent.setValue(getConsentValueHash(input));
        }

        return Collections.singletonMap(consent.getId(), consent);
    }
}
