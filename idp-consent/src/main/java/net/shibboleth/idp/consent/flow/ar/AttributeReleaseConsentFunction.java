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
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.context.AttributeReleaseContext;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.consent.flow.ConsentFlowDescriptor;
import net.shibboleth.idp.consent.logic.AttributeValuesHashFunction;
import net.shibboleth.idp.consent.logic.FlowDescriptorLookup;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Function;

/**
 * Function that returns a map of consent objects representing consent to attribute release. Each consent object
 * represents consent to an attribute. The id of each consent object is an attribute id, and the value of each consent
 * object is a hash of the attribute's values.
 */
public class AttributeReleaseConsentFunction implements Function<ProfileRequestContext, Map<String, Consent>> {

    /** Strategy used to find the {@link ConsentContext} from the {@link ProfileRequestContext}. */
    @Nonnull private Function<ProfileRequestContext, ConsentContext> consentContextLookupStrategy;

    /** Consent flow descriptor lookup strategy. */
    @Nonnull private Function<ProfileRequestContext, ConsentFlowDescriptor> consentFlowDescriptorLookupStrategy;

    /** Strategy used to find the {@link AttributeReleaseContext} from the {@link ProfileRequestContext}. */
    @Nonnull private Function<ProfileRequestContext, AttributeReleaseContext> attributeReleaseContextLookupStrategy;

    /** Function used to create a hash of attribute values. */
    @Nonnull private Function<Collection<IdPAttributeValue<?>>, String> hashFunction;

    /** Constructor. */
    public AttributeReleaseConsentFunction() {
        consentContextLookupStrategy = new ChildContextLookup<>(ConsentContext.class, false);
        consentFlowDescriptorLookupStrategy =
                new FlowDescriptorLookup<ConsentFlowDescriptor>(ConsentFlowDescriptor.class);
        attributeReleaseContextLookupStrategy = new ChildContextLookup<>(AttributeReleaseContext.class, false);
        hashFunction = new AttributeValuesHashFunction();
    }

    /** {@inheritDoc} */
    @Nullable public Map<String, Consent> apply(@Nullable final ProfileRequestContext input) {
        if (input == null) {
            return null;
        }

        final ConsentFlowDescriptor consentFlowDescriptor = consentFlowDescriptorLookupStrategy.apply(input);
        if (consentFlowDescriptor == null) {
            return null;
        }

        final ConsentContext consentContext = consentContextLookupStrategy.apply(input);
        if (consentContext == null) {
            return null;
        }

        final AttributeReleaseContext attributeReleaseContext = attributeReleaseContextLookupStrategy.apply(input);
        if (attributeReleaseContext == null) {
            return null;
        }

        final Map<String, Consent> currentConsents = new LinkedHashMap<>();

        final Map<String, IdPAttribute> consentableAttributes = attributeReleaseContext.getConsentableAttributes();
        for (final IdPAttribute attribute : consentableAttributes.values()) {

            final Consent consent = new Consent();
            consent.setId(attribute.getId());

            if (consentFlowDescriptor.compareValues()) {
                consent.setValue(hashFunction.apply(attribute.getValues()));
            }

            // Remember previous choice.
            final Consent previousConsent = consentContext.getPreviousConsents().get(consent.getId());
            if (previousConsent != null && Objects.equals(consent.getValue(), previousConsent.getValue())) {
                consent.setApproved(previousConsent.isApproved());
            }

            currentConsents.put(consent.getId(), consent);
        }

        return currentConsents;
    }

}
