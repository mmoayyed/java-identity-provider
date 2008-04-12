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

import java.util.ArrayList;
import java.util.List;

import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.StaticCredentialResolver;
import org.opensaml.xml.security.keyinfo.BasicProviderKeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.KeyInfoProvider;
import org.opensaml.xml.security.keyinfo.provider.DSAKeyValueProvider;
import org.opensaml.xml.security.keyinfo.provider.InlineX509DataProvider;
import org.opensaml.xml.security.keyinfo.provider.RSAKeyValueProvider;
import org.opensaml.xml.signature.impl.ExplicitKeySignatureTrustEngine;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Spring factory bean used to created {@link ExplicitKeySignatureTrustEngine}s based on a static credential resolver.
 */
public class StaticExplicitKeySignatureTrustEngineFactoryBean extends AbstractFactoryBean {
    
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
    public void setCredentials(List<Credential> newCredentials) {
        credentials = newCredentials;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return ExplicitKeySignatureTrustEngine.class;
    }
    
    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        StaticCredentialResolver credResolver = new StaticCredentialResolver(getCredentials());
        
        List<KeyInfoProvider> keyInfoProviders = new ArrayList<KeyInfoProvider>();
        keyInfoProviders.add(new DSAKeyValueProvider());
        keyInfoProviders.add(new RSAKeyValueProvider());
        keyInfoProviders.add(new InlineX509DataProvider());
        KeyInfoCredentialResolver keyInfoCredResolver = new BasicProviderKeyInfoCredentialResolver(keyInfoProviders);
        
        return new ExplicitKeySignatureTrustEngine(credResolver, keyInfoCredResolver);
    }
}