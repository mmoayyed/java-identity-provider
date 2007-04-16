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

package edu.internet2.middleware.shibboleth.common.config.relyingparty.saml;

import org.opensaml.saml2.metadata.provider.MetadataProvider;

import edu.internet2.middleware.shibboleth.common.config.relyingparty.AbstractRelyingPartyManagerFactoryBean;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;
import edu.internet2.middleware.shibboleth.common.relyingparty.provider.SAMLMDRelyingPartyConfigurationManager;

/**
 * Spring factory bean for creating {@link SAMLMDRelyingPartyConfigurationManager}s.
 */
public class SAMLRelyingPartyManagerFactoryBean extends AbstractRelyingPartyManagerFactoryBean {

    /** Metadata provider used by relying party manager. */
    private MetadataProvider metadataProvider;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return SAMLMDRelyingPartyConfigurationManager.class;
    }

    /**
     * Gets the metadata provider used by relying party manager.
     * 
     * @return metadata provider used by relying party manager
     */
    public MetadataProvider getMetadataProvider() {
        return metadataProvider;
    }

    /**
     * Sets the metadata provider used by relying party manager.
     * 
     * @param provider metadata provider used by relying party manager
     */
    public void setMetadataProvider(MetadataProvider provider) {
        metadataProvider = provider;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        SAMLMDRelyingPartyConfigurationManager manager = new SAMLMDRelyingPartyConfigurationManager(metadataProvider);
        manager.setAnonymousRelyingConfiguration(getAnonymousRelyingParty());
        manager.setDefaultRelyingPartyConfiguration(getDefaultRelyingParty());
        
        if(getRelyingParties() != null){
            for(RelyingPartyConfiguration configuration : getRelyingParties()){
                manager.getRelyingPartyConfigurations().put(configuration.getRelyingPartyId(), configuration);
            }
        }
        
        return manager;
    }
}