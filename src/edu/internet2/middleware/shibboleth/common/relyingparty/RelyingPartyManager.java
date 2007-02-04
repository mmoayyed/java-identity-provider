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

import java.util.List;

import org.opensaml.saml2.metadata.provider.MetadataProvider;

/**
 * Locates the configuration for a given relying party.
 */
public interface RelyingPartyManager {

    /**
     * Gets the configuration for the given relying party.
     * 
     * If the given entity ID is null, empty, or contains only whitespace the anonymous relying party configuration is
     * returned. Otherwise, the given relying party entity ID is looked for in the list of registered
     * {@link RelyingPartyConfiguration}s and if found is returned. If no configuration is registered for the specific
     * entity ID the entity descriptor for the relying party is located using the {@link MetadataProvider}. The name of
     * ancestral entities descriptors are then looked up, in ascending order (i.e. the parent entities descriptor, then
     * the grandparent, great-grandparent, etc.), with the first configuration found being returned. If no configuration
     * is found once the top of the tree is reached the default configuration is returned.
     * 
     * @param relyingPartyEntityID the entity of the relying part to get the configuration for
     * 
     * @return configuration for the given relying party
     */
    public RelyingPartyConfiguration getRelyingPartyConfiguration(String relyingPartyEntityID);

    /**
     * Gets the metadata provider used to lookup information about relying parties.
     * 
     * @return metadata provider used to lookup information about relying parties
     */
    public MetadataProvider getMetadataProvider();

    /**
     * Sets the metadata provider used to lookup information about relying parties.
     * 
     * @param provider metadata provider used to lookup information about relying parties
     */
    public void setMetadataProvider(MetadataProvider provider);

    /**
     * Gets the registered relying party configurations.
     * 
     * @return the registered relying party configurations
     */
    public List<RelyingPartyConfiguration> getRelyingPartyConfigurations();

    /**
     * Sets the registered relying party configurations.
     * 
     * @param relyingParties the registered relying party configurations
     */
    public void setRelyingPartyConfigurations(List<RelyingPartyConfiguration> relyingParties);

    /**
     * Gets the default relying party configuration.
     * 
     * @return the default relying party configuration
     */
    public RelyingPartyConfiguration getDefaultRelyingPartyConfiguration();

    /**
     * Sets the default relying party configuration.
     * 
     * @param defaultConfiguration the default relying party configuration
     */
    public void setDefaultRelyingPartyConfiguration(RelyingPartyConfiguration defaultConfiguration);

    /**
     * Gets the relying party configuration to use for anonymous parties.
     * 
     * @return the relying party configuration to use for anonymous parties
     */
    public RelyingPartyConfiguration getAnonymousRelyingConfiguration();

    /**
     * Sets the relying party configuration to use for anonymous parties.
     * 
     * @param anonymousConfiguration the relying party configuration to use for anonymous parties
     */
    public void setAnonymousRelyingPartyConfiguration(RelyingPartyConfiguration anonymousConfiguration);
}