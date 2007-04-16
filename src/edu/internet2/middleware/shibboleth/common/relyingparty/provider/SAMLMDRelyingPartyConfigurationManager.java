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

package edu.internet2.middleware.shibboleth.common.relyingparty.provider;

import java.util.HashMap;
import java.util.Map;

import org.opensaml.saml2.metadata.provider.MetadataProvider;

import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfigurationManager;

/**
 * A relying party manager that uses SAML metadata to lookup information about requested entities. Relying party
 * configuration information is looked up as follows:
 * 
 * If the given entity ID is null, empty, or contains only whitespace the anonymous relying party configuration is
 * returned. Otherwise, the given relying party entity ID is looked for in the list of registered
 * {@link RelyingPartyConfiguration}s and if found is returned. If no configuration is registered for the specific
 * entity ID the entity descriptor for the relying party is located using the {@link MetadataProvider}. The name of
 * ancestral entities descriptors are then looked up, in ascending order (i.e. the parent entities descriptor, then the
 * grandparent, great-grandparent, etc.), with the first configuration found being returned. If no configuration is
 * found once the top of the tree is reached the default configuration is returned.
 */
public class SAMLMDRelyingPartyConfigurationManager implements RelyingPartyConfigurationManager {

    /** Metadata provider used to lookup information about entities. */
    private MetadataProvider metadata;

    /** Relying party config used for anonymous parties. */
    private RelyingPartyConfiguration anonymousRPConfig;

    /** Relying party config used as the default config. */
    private RelyingPartyConfiguration defaultRPConfig;

    /** Regisered relying party configurations. */
    private HashMap<String, RelyingPartyConfiguration> rpConfigs;

    /**
     * Constructor.
     * 
     * @param provider metadata provider used to lookup information about entities
     * 
     */
    public SAMLMDRelyingPartyConfigurationManager(MetadataProvider provider) {
        metadata = provider;
        rpConfigs = new HashMap<String, RelyingPartyConfiguration>();
    }

    /** {@inheritDoc} */
    public RelyingPartyConfiguration getAnonymousRelyingConfiguration() {
        return anonymousRPConfig;
    }

    /**
     * Sets the anonymous relying party configuration.
     * 
     * @param configuration anonymous relying party configuration
     */
    public void setAnonymousRelyingConfiguration(RelyingPartyConfiguration configuration) {
        anonymousRPConfig = configuration;
    }

    /** {@inheritDoc} */
    public RelyingPartyConfiguration getDefaultRelyingPartyConfiguration() {
        return defaultRPConfig;
    }

    /**
     * Sets the default relying party configuration.
     * 
     * @param configuration default relying party configuration
     */
    public void setDefaultRelyingPartyConfiguration(RelyingPartyConfiguration configuration) {
        defaultRPConfig = configuration;
    }

    /**
     * Gets the metadata provider used to lookup information about entities.
     * 
     * @return metadata provider used to lookup information about entities
     */
    public MetadataProvider getMetadataProvider() {
        return metadata;
    }

    /** {@inheritDoc} */
    public RelyingPartyConfiguration getRelyingPartyConfiguration(String relyingPartyEntityID) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public Map<String, RelyingPartyConfiguration> getRelyingPartyConfigurations() {
        return rpConfigs;
    }
}