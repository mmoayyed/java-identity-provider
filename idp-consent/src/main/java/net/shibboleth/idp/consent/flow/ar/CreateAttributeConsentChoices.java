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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.utilities.java.support.collection.Pair;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageSerializer;
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

    /** Consent serializer. */
    @Nonnull private StorageSerializer<Map<String, Consent>> consentSerializer;

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        if (!super.doPreExecute(profileRequestContext, interceptorContext)) {
            log.debug("{} Storage is not properly configured", getLogPrefix());
            return false;
        }

        attributeValuesHashFunction = getAttributeConsentFlowDescriptor().getAttributeValuesHashFunction();

        consentSerializer = getConsentFlowDescriptor().getConsentSerializer();

        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        final Map<String, IdPAttribute> attributes = getAttributeContext().getIdPAttributes();
        if (attributes.isEmpty()) {
            log.debug("{} No attributes available from attribute context, nothing to do", getLogPrefix());
            return;
        }

        final Map<String, Consent> storedConsents = getConsentContext().getStoredConsents();
        log.debug("{} Stored consents '{}'", getLogPrefix(), storedConsents);

        final Map<String, Pair<Consent, String>> consentChoices = new LinkedHashMap<>();

        try {
            for (IdPAttribute attribute : attributes.values()) {
                final Consent consent = storedConsents.get(attribute.getId());
                if (consent == null) {
                    final Pair<Consent, String> pair = createConsentPair(attribute);
                    consentChoices.put(pair.getFirst().getId(), pair);
                } else {
                    final String hash = attributeValuesHashFunction.apply(attribute.getValues());
                    if (!hash.equals(consent.getValue())) {
                        final Pair<Consent, String> pair = createConsentPair(attribute);
                        consentChoices.put(pair.getFirst().getId(), pair);
                    }
                }
            }
        } catch (final IOException e) {
            log.debug("{} Unable to serialize consent.", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }

        getConsentContext().setConsentChoices(consentChoices);
    }

    /**
     * Create a pair consisting of a consent object and its serialized form.
     * 
     * @param attribute idp attribute to be consented to
     * @return pair consisting of a consent object and its serialized form
     * @throws IOException if the consent cannot be serialized
     */
    @Nonnull private Pair<Consent, String> createConsentPair(@Nonnull final IdPAttribute attribute) throws IOException {

        final Consent consent = new Consent();
        consent.setId(attribute.getId());
        consent.setValue(attributeValuesHashFunction.apply(attribute.getValues()));

        final String serializedConsent =
                consentSerializer.serialize(Collections.singletonMap(consent.getId(), consent));

        return new Pair(consent, serializedConsent);
    }

}
