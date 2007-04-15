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

import java.util.Map;

/**
 * Locates the configuration for a given relying party.
 */
public interface RelyingPartyManager {

    /**
     * Gets the configuration for the given relying party.
     * 
     * @param relyingPartyEntityID the entity of the relying part to get the configuration for
     * 
     * @return configuration for the given relying party
     */
    public RelyingPartyConfiguration getRelyingPartyConfiguration(String relyingPartyEntityID);

    /**
     * Gets the registered relying party configurations indexed by relying party ID.
     * 
     * @return the registered relying party configurations
     */
    public Map<String, RelyingPartyConfiguration> getRelyingPartyConfigurations();

    /**
     * Gets the default relying party configuration.
     * 
     * @return the default relying party configuration
     */
    public RelyingPartyConfiguration getDefaultRelyingPartyConfiguration();

    /**
     * Gets the relying party configuration to use for anonymous parties.
     * 
     * @return the relying party configuration to use for anonymous parties
     */
    public RelyingPartyConfiguration getAnonymousRelyingConfiguration();
}