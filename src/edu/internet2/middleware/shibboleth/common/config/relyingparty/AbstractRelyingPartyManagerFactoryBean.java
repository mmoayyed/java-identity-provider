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

import org.springframework.beans.factory.config.AbstractFactoryBean;

import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;

/**
 * Base relying party manager factory bean.
 */
public abstract class AbstractRelyingPartyManagerFactoryBean extends AbstractFactoryBean {

    /** Anonymous relying party configuration. */
    private RelyingPartyConfiguration anonymousRelyingParty;

    /** Default relying party configuration. */
    private RelyingPartyConfiguration defaultRelyingParty;

    /** Relying party specific configurations. */
    private List<RelyingPartyConfiguration> relyingParties;

    /**
     * Gets the anonymous relying party configuration.
     * 
     * @return anonymous relying party configuration
     */
    public RelyingPartyConfiguration getAnonymousRelyingParty() {
        return anonymousRelyingParty;
    }

    /**
     * Sets the anonymous relying party configuration.
     * 
     * @param configuration Anonymous relying party configuration
     */
    public void setAnonymousRelyingParty(RelyingPartyConfiguration configuration) {
        anonymousRelyingParty = configuration;
    }

    /**
     * Gets the default relying party configuration.
     * 
     * @return default relying party configuration
     */
    public RelyingPartyConfiguration getDefaultRelyingParty() {
        return defaultRelyingParty;
    }

    /**
     * Sets the default relying party configuration.
     * 
     * @param configuration default relying party configuration
     */
    public void setDefaultRelyingParty(RelyingPartyConfiguration configuration) {
        defaultRelyingParty = configuration;
    }

    /**
     * Gets the relying party specific configurations.
     * 
     * @return relying party specific configurations
     */
    public List<RelyingPartyConfiguration> getRelyingParties() {
        return relyingParties;
    }

    /**
     * Sets the relying party specific configurations.
     * 
     * @param parties relying party specific configurations
     */
    public void setRelyingParties(List<RelyingPartyConfiguration> parties) {
        relyingParties = parties;
    }
}