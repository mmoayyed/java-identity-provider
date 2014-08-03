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

package net.shibboleth.idp.attribute.consent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;

import net.shibboleth.idp.attribute.IdPAttribute;

/**
 * Represents the current user.
 */
@NotThreadSafe
public class User {

    /** The users id. */
    private final String id;

    /** Whether the user has given global consent or not. */
    private boolean globalConsent;

    /**
     * A map of attribute releases. The key represents the entityId for which the attribute releases approval was given.
     * The value contains a collection of @see AttributeRelease.
     * */
    private final Map<String, Collection<AttributeRelease>> releases;

    /**
     * Constructs a @see User.
     * 
     * @param userId The id of the user.
     * @param globalConsentGiven If user has given global consent or not.
     */
    public User(final String userId, final boolean globalConsentGiven) {
        id = userId;
        globalConsent = globalConsentGiven;
        releases = new HashMap<String, Collection<AttributeRelease>>();
    }

    /**
     * Gets the global consent flag.
     * 
     * @return Returns the global consent flag.
     */
    public boolean hasGlobalConsent() {
        return globalConsent;
    }

    /**
     * Sets the global consent flag.
     * 
     * @param globalConsentGiven The global consent flag to set.
     */
    public void setGlobalConsent(boolean globalConsentGiven) {
        globalConsent = globalConsentGiven;
    }

    /**
     * Gets the userId.
     * 
     * @return The id of the user.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the attribute releases for a specific relying party.
     * 
     * @param relyingPartyId The id of the relying party for which the attribute releases should be set
     * @param attributeReleases Collection of @see AttributeRelease to set
     */
    public void setAttributeReleases(final String relyingPartyId,
            final Collection<AttributeRelease> attributeReleases) {
        releases.put(relyingPartyId, attributeReleases);
    }

    /**
     * Gets the attribute releases for a specific relying party.
     * 
     * @param relyingPartyId The id of the relying party for which the attribute releases should be returned
     * @return Collection of @see AttributeRelease
     */
    public Collection<AttributeRelease> getAttributeReleases(final String relyingPartyId) {
        if (!releases.containsKey(relyingPartyId)) {
            return Collections.EMPTY_SET;
        }
        return releases.get(relyingPartyId);
    }

    /**
     * Checks if the user has given (in past) attribute release consent to all released attributes.
     * 
     * @param relyingPartyId The id of the relying party for which the attribute releases should be checked
     * @param attributes Collection of @see Attribute, the attributes which are going to be released
     * @return true if the user has given attribute release consent to all attributes.
     */
    public boolean hasApprovedAttributes(final String relyingPartyId, final Collection<IdPAttribute> attributes) {
        Collection<AttributeRelease> attributeReleases = getAttributeReleases(relyingPartyId);

        for (IdPAttribute attribute : attributes) {
            boolean approved = false;
            for (AttributeRelease attributeRelease : attributeReleases) {
                if (attributeRelease.contains(attribute)) {
                    approved = true;
                    break;
                }
            }
            if (!approved) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return id;
    }
}
