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

package edu.internet2.middleware.shibboleth.common.config.security;

import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.security.MetadataCredentialResolver;
import org.opensaml.xml.security.trust.ExplicitKeyTrustEngine;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Spring factory bean used to created {@link ExplicitKeyTrustEngine}s based on a metadata provider.
 */
public class MetadataExplicitKeyTrustEngineFactoryBean extends AbstractFactoryBean {
    
    /** Metadata provider used to look up key information for peer entities. */
    private MetadataProvider metadataProvider;

    /**
     * Gets the metadata provider used to look up key information for peer entities.
     * 
     * @return metadata provider used to look up key information for peer entities
     */
    public MetadataProvider getMetadataProvider() {
        return metadataProvider;
    }

    /**
     * Sets the metadata provider used to look up key information for peer entities.
     * 
     * @param provider metadata provider used to look up key information for peer entities
     */
    public void setMetadataProvider(MetadataProvider provider) {
        metadataProvider = provider;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return ExplicitKeyTrustEngine.class;
    }
    
    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        MetadataCredentialResolver credResolver = new MetadataCredentialResolver(getMetadataProvider());        
        return new ExplicitKeyTrustEngine(credResolver);
    }
}