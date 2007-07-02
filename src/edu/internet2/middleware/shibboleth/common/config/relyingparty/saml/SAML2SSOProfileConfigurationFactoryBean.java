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

import edu.internet2.middleware.shibboleth.common.relyingparty.provider.saml2.SSOConfiguration;

/**
 * Spring factory for SAML 2 SSO profile configurations.
 */
public class SAML2SSOProfileConfigurationFactoryBean extends AbstractSAML2ProfileConfigurationFactoryBean {
    
    /** Override for authentication statement's subject locality address. */
    private String localityAddress;

    /** Override for authentication statement's subject locality DNS name. */
    private String localityDNSName;
    
    /** Whether responses to the authentication request should include an attribute statement. */
    private boolean includeAttributeStatement;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return SSOConfiguration.class;
    }
    
    /**
     * Gets the override for authentication statement's subject locality DNS name.
     * 
     * @return override for authentication statement's subject locality DNS name
     */
    public String getLocalityDNSName() {
        return localityDNSName;
    }

    /**
     * Sets the override for authentication statement's subject locality DNS name.
     * 
     * @param name override for authentication statement's subject locality DNS name
     */
    public void setLocalityDNSName(String name) {
        localityDNSName = name;
    }

    /**
     * Gets the override for authentication statement's subject locality address.
     * 
     * @return override for authentication statement's subject locality address
     */
    public String getLocalityAddress() {
        return localityAddress;
    }

    /**
     * Sets the override for authentication statement's subject locality address.
     * 
     * @param address override for authentication statement's subject locality address.
     */
    public void setLocalityAddress(String address) {
        localityAddress = address;
    }
    
    /**
     * Gets whether responses to the authentication request should include an attribute statement.
     * 
     * @return whether responses to the authentication request should include an attribute statement
     */
    public boolean includeAttributeStatement() {
        return includeAttributeStatement;
    }

    /**
     * Sets whether responses to the authentication request should include an attribute statement.
     * 
     * @param include whether responses to the authentication request should include an attribute statement
     */
    public void setIncludeAttributeStatement(boolean include) {
        includeAttributeStatement = include;
    }
    
    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        SSOConfiguration configuration = new SSOConfiguration();
        populateBean(configuration);
        configuration.setLocalityAddress(localityAddress);
        configuration.setLocalityDNSName(localityDNSName);
        configuration.setIncludeAttributeStatement(includeAttributeStatement());
        
        return configuration;
    }
}