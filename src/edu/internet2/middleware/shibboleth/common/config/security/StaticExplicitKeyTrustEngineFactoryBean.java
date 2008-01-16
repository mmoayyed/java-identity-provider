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

import java.util.List;

import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.StaticCredentialResolver;
import org.opensaml.xml.security.trust.ExplicitKeyTrustEngine;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Spring factory bean used to created {@link ExplicitKeyTrustEngine}s based on a static credential resolver.
 */
public class StaticExplicitKeyTrustEngineFactoryBean extends AbstractFactoryBean {
    
    /** List of trusted credentials. */
    private List<Credential> credentials;

    /**
     * Gets the list of trusted credentials.
     * 
     * @return the list of trusted credentials
     */
    public List<Credential> getCredentials() {
        return credentials;
    }

    /**
     * Sets the list of trusted credentials.
     * 
     * @param newCredentials the new list of trusted credentials
     */
    public void setMetadataProvider(List<Credential> newCredentials) {
        credentials = newCredentials;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return ExplicitKeyTrustEngine.class;
    }
    
    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        StaticCredentialResolver credResolver = new StaticCredentialResolver(getCredentials());
        
        return new ExplicitKeyTrustEngine(credResolver);
    }
}