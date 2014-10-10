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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Attribute consent action to create the consents to be chosen by the user.
 */
public class CreateAttributeConsentChoices extends AbstractAttributeConsentAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CreateAttributeConsentChoices.class);

    /** Function to create hash of all attribute values. */
    @Nonnull private Function<Collection<IdPAttributeValue<?>>, String> attributeValuesHashFunction;

    /** Attributes to be consented to. */
    @Nonnull @NonnullElements private Map<String, IdPAttribute> consentableAttributes;

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        if (!super.doPreExecute(profileRequestContext, interceptorContext)) {
            return false;
        }

        attributeValuesHashFunction = getAttributeConsentFlowDescriptor().getAttributeValuesHashFunction();

        consentableAttributes = getAttributeConsentContext().getConsentableAttributes();
        if (consentableAttributes.isEmpty()) {
            log.debug("{} No attributes available from attribute consent context, nothing to do", getLogPrefix());
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        final Map<String, Consent> currentConsents = new LinkedHashMap<>();

        for (final IdPAttribute attribute : consentableAttributes.values()) {
            final String hash = attributeValuesHashFunction.apply(attribute.getValues());

            final Consent consent = new Consent();
            consent.setId(attribute.getId());
            consent.setValue(hash);

            // Remember previous choice.
            final Consent previousConsent = getConsentContext().getPreviousConsents().get(consent.getId());
            if (previousConsent != null && Objects.equals(consent.getValue(), previousConsent.getValue())) {
                consent.setApproved(previousConsent.isApproved());
            }

            currentConsents.put(consent.getId(), consent);
        }

        getConsentContext().setCurrentConsents(currentConsents);
    }

}
