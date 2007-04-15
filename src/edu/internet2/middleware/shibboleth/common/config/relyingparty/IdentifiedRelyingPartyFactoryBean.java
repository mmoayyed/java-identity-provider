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

import java.util.Map;

import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;

/**
 * A spring factory bean that creates {@link RelyingPartyConfiguration}..
 */
public class IdentifiedRelyingPartyFactoryBean extends UnidentifiedRelyingPartyFactoryBean {

    /** ID of the relying party. */
    private String relyingPartyId;

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

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        RelyingPartyConfiguration configuration = new RelyingPartyConfiguration(relyingPartyId, getProviderId());
        configuration.setDefaultSigningCredential(getDefaultSigningCredential());

        if (getProfileConfigurations() != null) {
            Map<String, ProfileConfiguration> registeredProfileConfigs = configuration.getProfileConfigurations();
            for (ProfileConfiguration profileConfig : getProfileConfigurations()) {
                registeredProfileConfigs.put(profileConfig.getProfileId(), profileConfig);
            }
        }

        return configuration;
    }
}