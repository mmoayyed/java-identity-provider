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
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.BaseContext;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

/**
 * Context representing the state of a consent flow.
 */
public class ConsentContext extends BaseContext {

    /** Previous consents read from storage. */
    @Nonnull @NonnullElements private Map<String, Consent> previousConsents;

    /** Current consents extracted from user input. */
    @Nonnull @NonnullElements private Map<String, Consent> currentConsents;
    
    /** Constructor. */
    public ConsentContext() {
        // TODO proper inits
        previousConsents = Collections.EMPTY_MAP;
        currentConsents = Collections.EMPTY_MAP;
    }

    /**
     * Get current consents extracted from user input.
     * 
     * @return consents extracted from user input
     */
    @Nullable @NonnullElements public Map<String, Consent> getCurrentConsents() {
        return currentConsents;
    }

    /**
     * Get previous consents read from storage.
     * 
     * @return consents read from storage
     */
    @Nullable @NonnullElements public Map<String, Consent> getPreviousConsents() {
        return previousConsents;
    }

    /**
     * Set consents extracted from user input.
     * 
     * @param map consents extracted from user input
     */
    public void setCurrentConsents(@Nonnull @NonnullElements final Map<String, Consent> map) {
        Constraint.isNotNull(map, "Current consents cannot be null");

        currentConsents = ConsentContext.setMap(map);
    }

    /**
     * Set previous consents read from storage.
     * 
     * @param map consents read from storage
     */
    public void setPreviousConsents(@Nonnull @NonnullElements final Map<String, Consent> map) {
        Constraint.isNotNull(map, "Previous consents cannot be null");

        previousConsents = ConsentContext.setMap(map);
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
        return MoreObjects.toStringHelper(this).add("previousConsents", previousConsents)
                .add("chosenConsents", currentConsents).toString();
    }

}
