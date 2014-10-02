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

package net.shibboleth.idp.consent.context;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.flow.ConsentFlowDescriptor;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

/**
 * Context representing the state of a consent flow.
 */
// TODO Just a stub.
public class ConsentContext extends ProfileInterceptorContext {

    /** Consent flow descriptor. */
    @Nullable private ConsentFlowDescriptor consentFlowDescriptor;

    /** Consents read from storage. */
    @Nullable @NonnullElements private Map<String, Consent> storedConsents;

    /** Consent choices to be chosen by user. Key is consent id. Second pair object is serialized form of the first. */
    @Nullable @NonnullElements private Map<String, Pair<Consent, String>> consentChoices;

    /** Consents chosen by user. */
    @Nullable @NonnullElements private Map<String, Consent> chosenConsents;

    /** Constructor. */
    public ConsentContext() {
        // TODO proper inits
        storedConsents = Collections.EMPTY_MAP;
        consentChoices = Collections.EMPTY_MAP;
        chosenConsents = Collections.EMPTY_MAP;
    }

    /**
     * Get the consent flow descriptor.
     * 
     * @return the consent flow descriptor
     */
    public ConsentFlowDescriptor getConsentFlowDescriptor() {
        return consentFlowDescriptor;
    }

    /**
     * Set the consent flow descriptor.
     * 
     * @param descriptor consent flow descriptor
     */
    public void setConsentFlowDescriptor(@Nonnull final ConsentFlowDescriptor descriptor) {
        consentFlowDescriptor = Constraint.isNotNull(descriptor, "Consent flow descriptor cannot be null");
    }

    /**
     * Get consents read from storage.
     * 
     * @return consents read from storage
     */
    @Nullable @NonnullElements public Map<String, Consent> getStoredConsents() {
        return storedConsents;
    }

    /**
     * Set consents read from storage.
     * 
     * @param map consents read from storage
     */
    public void setStoredConsents(@Nonnull @NonnullElements final Map<String, Consent> map) {
        Constraint.isNotNull(map, "Stored consents cannot be null");

        storedConsents = ConsentContext.setMap(map);
    }

    /**
     * Get consent choices to be chosen by user. Key is consent id. Second pair object is serialized form of the first.
     * 
     * @return consent choices to be chosen by user.
     */
    @Nullable @NonnullElements public Map<String, Pair<Consent, String>> getConsentChoices() {
        return consentChoices;
    }

    /**
     * Set consent choices to be chosen by user. Key is consent id. Second pair object is serialized form of the first.
     * 
     * @param choices consent choices to be chosen by user.
     */
    public void setConsentChoices(@Nonnull @NonnullElements Map<String, Pair<Consent, String>> choices) {
        Constraint.isNotNull(choices, "Consent choices cannot be null");

        // TODO non null elements constraint

        consentChoices = choices;
    }

    /**
     * Get consents chosen by user.
     * 
     * @return consents chosen by user
     */
    @Nullable @NonnullElements public Map<String, Consent> getChosenConsents() {
        return chosenConsents;
    }

    /**
     * Set consents chosen by user.
     * 
     * @param map consents chosen by user
     */
    public void setChosenConsents(@Nonnull @NonnullElements final Map<String, Consent> map) {
        Constraint.isNotNull(map, "Chosen consents cannot be null");

        chosenConsents = ConsentContext.setMap(map);
    }

    /**
     * Enforce @NonnullElements.
     * 
     * @param map the source map
     * @return the map with no null elements
     */
    private static Map<String, Consent> setMap(@Nonnull @NonnullElements final Map<String, Consent> map) {

        final Map<String, Consent> newMap = Maps.newHashMapWithExpectedSize(map.size());
        for (final Map.Entry<String, Consent> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                final String trimmed = StringSupport.trimOrNull(entry.getKey());
                if (trimmed != null) {
                    newMap.put(trimmed, entry.getValue());
                }
            }
        }

        return newMap;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return MoreObjects.toStringHelper(this).add("consentFlowDescriptor", consentFlowDescriptor)
                .add("storedConsents", storedConsents).add("consentChoices", consentChoices)
                .add("chosenConsents", chosenConsents).toString();
    }

}
