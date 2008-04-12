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

package edu.internet2.middleware.shibboleth.common.relyingparty;

import java.util.HashMap;
import java.util.Map;

import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.util.DatatypeHelper;

/**
 * A set of configuration options for a relying party.
 */
public class RelyingPartyConfiguration {

    /** Entity ID of the relying party. */
    private String relyingPartyId;

    /** Entity ID of the responder when communicating with the relying party. */
    private String providerId;

    /** Authentication method to use if none is specified within a request. */
    private String defaultAuthenticationMethod;

    /** Default signing credential. */
    private Credential signingCredential;

    /** Various profile configurations. */
    private HashMap<String, ProfileConfiguration> profiles;

    /**
     * Constructor.
     * 
     * @param provider entity ID of the responder when communicating with the relying party
     */
    public RelyingPartyConfiguration(String provider) {
        setProviderId(provider);
        profiles = new HashMap<String, ProfileConfiguration>();
    }

    /**
     * Constructor.
     * 
     * @param relyingParty ID of the relying party this configuration is for
     * @param provider entity ID of the responder when communicating with the relying party
     */
    public RelyingPartyConfiguration(String relyingParty, String provider) {
        setRelyingPartyId(relyingParty);
        setProviderId(provider);
        profiles = new HashMap<String, ProfileConfiguration>();
    }

    /**
     * Gets the entity ID of the relying party this configuration is for.
     * 
     * @return the entity ID of the relying party this configuration is for
     */
    public String getRelyingPartyId() {
        return relyingPartyId;
    }

    /**
     * Sets the entity ID of the relying party this configuration is for.
     * 
     * @param id entity ID of the relying party this configuration is for
     */
    protected void setRelyingPartyId(String id) {
        relyingPartyId = DatatypeHelper.safeTrimOrNullString(id);
    }

    /**
     * Gets the entity ID of the responder when communicating with the relying party.
     * 
     * @return entity ID of the responder when communicating with the relying party
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * Sets the entity ID of the responder when communicating with the relying party.
     * 
     * @param id entity ID of the responder when communicating with the relying party
     */
    protected void setProviderId(String id) {
        providerId = DatatypeHelper.safeTrimOrNullString(id);
    }

    /**
     * Gets the authentication method to use if one is not specified within a request.
     * 
     * @return authentication method to use if one is not specified within a request
     */
    public String getDefaultAuthenticationMethod() {
        return defaultAuthenticationMethod;
    }

    /**
     * Sets the authentication method to use if one is not specified within a request.
     * 
     * @param method authentication method to use if one is not specified within a request
     */
    public void setDefaultAuthenticationMethod(String method) {
        defaultAuthenticationMethod = method;
    }

    /**
     * Gets the default signing credential for the relying party. This is provided as a convenience method so that this
     * credential need not be defined on every signing supporting profile configuration. If a profile configuration has
     * a defined signing credential it must be used in place of the credential retrieved here.
     * 
     * @return default signing credential for the relying party
     */
    public Credential getDefaultSigningCredential() {
        return signingCredential;
    }

    /**
     * Sets the default signing credential for the relying party.
     * 
     * @param credential default signing credential for the relying party
     */
    public void setDefaultSigningCredential(Credential credential) {
        signingCredential = credential;
    }

    /**
     * Gets whether assertions should be encrypted.
     * 
     * @return configuration for specific communication profiles used by the system indexed by profile ID
     */
    public Map<String, ProfileConfiguration> getProfileConfigurations() {
        return profiles;
    }

    /**
     * Convenience method for retrieving a given profile configuration from the {@link Map} returned by
     * {@link #getProfileConfigurations()}.
     * 
     * @param profileId unique Id of the profile
     * 
     * @return the profile configuration or null
     */
    public ProfileConfiguration getProfileConfiguration(String profileId) {
        if (profiles != null) {
            return profiles.get(profileId);
        }

        return null;
    }
}