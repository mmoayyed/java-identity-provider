/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.security;

import org.opensaml.xml.security.Criteria;
import org.opensaml.xml.util.DatatypeHelper;

/**
 * Criteria that represents the communication profile in use. Example profiles are SAML 1 Attribute Query or SAML 2
 * Authentication request.
 */
public final class CommunicationProfileCriteria implements Criteria {

    /** Unique ID of the profile. */
    private String profile;

    /**
     * Constructor.
     * 
     * @param profileId unique ID of the profile
     */
    public CommunicationProfileCriteria(String profileId) {
        setCommunicationProfile(profileId);
    }

    /**
     * Gets the unique ID of the profile.
     * 
     * @return unique ID of the profile
     */
    public String getCommunicationProfile() {
        return profile;
    }

    /**
     * Sets the unique ID of the profile.
     * 
     * @param profileId unique ID of the profile
     */
    public void setCommunicationProfile(String profileId) {
        if (DatatypeHelper.isEmpty(profileId)) {
            throw new IllegalArgumentException("Profile ID may not be null or empty");
        }

        profile = DatatypeHelper.safeTrim(profileId);
    }
}
