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

package edu.internet2.middleware.shibboleth.common.config.relyingparty;

import java.util.List;
import java.util.Map;

import org.opensaml.xml.security.credential.Credential;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;

/**
 * Relying party configuration factory bean.
 */
public class RelyingPartyFactoryBean extends AbstractFactoryBean {
    
    /** ID of the relying party. */
    private String relyingPartyId;

    /** ID of the provider to use for this relying party. */
    private String providerId;

    /** The default signing credential for this relying party. */
    private Credential defaultSigningCredential;

    /** Registered profile configurations. */
    private List<ProfileConfiguration> profileConfigurations;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return RelyingPartyConfiguration.class;
    }
    
    /**
     * Gets the ID of the relying party.
     * 
     * @return ID of the provider to use for this relying party
     */
    public String getRelyingPartyId() {
        return relyingPartyId;
    }

    /**
     * Sets the ID of the relying party.
     * 
     * @param id ID of the relying party
     */
    public void setRelyingPartyId(String id) {
        relyingPartyId = id;
    }

    /**
     * Gets the ID of the provider to use for this relying party.
     * 
     * @return ID of the provider to use for this relying party
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * Sets the ID of the provider to use for this relying party.
     * 
     * @param id ID of the provider to use for this relying party
     */
    public void setProviderId(String id) {
        providerId = id;
    }

    /**
     * Gets the default signing credential for this relying party.
     * 
     * @return default signing credential for this relying party
     */
    public Credential getDefaultSigningCredential() {
        return defaultSigningCredential;
    }

    /**
     * Sets the default signing credential for this relying party.
     * 
     * @param credential default signing credential for this relying party
     */
    public void setDefaultSigningCredential(Credential credential) {
        defaultSigningCredential = credential;
    }

    /**
     * Gets the registered profile configurations.
     * 
     * @return registered profile configurations
     */
    public List<ProfileConfiguration> getProfileConfigurations() {
        return profileConfigurations;
    }

    /**
     * Sets the registered profile configurations.
     * 
     * @param configurations registered profile configurations
     */
    public void setProfileConfigurations(List<ProfileConfiguration> configurations) {
        profileConfigurations = configurations;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        RelyingPartyConfiguration configuration = new RelyingPartyConfiguration(relyingPartyId, providerId);
        configuration.setDefaultSigningCredential(defaultSigningCredential);

        if (profileConfigurations != null) {
            Map<String, ProfileConfiguration> registeredProfileConfigs = configuration.getProfileConfigurations();
            for (ProfileConfiguration profileConfig : profileConfigurations) {
                registeredProfileConfigs.put(profileConfig.getProfileId(), profileConfig);
            }
        }

        return configuration;
    }
}
