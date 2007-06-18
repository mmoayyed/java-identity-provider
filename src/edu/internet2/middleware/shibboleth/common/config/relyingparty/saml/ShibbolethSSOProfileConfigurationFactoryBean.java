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

import edu.internet2.middleware.shibboleth.common.relyingparty.provider.saml1.ShibbolethSSOConfiguration;

/**
 * Spring factory for Shibboleth SSO profile configurations.
 */
public class ShibbolethSSOProfileConfigurationFactoryBean extends AbstractSAML1ProfileConfigurationFactoryBean {
    
    /** Whether responses to the authentication request should include an attribute statement. */
    private boolean includeAttributeStatement;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return ShibbolethSSOConfiguration.class;
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
        ShibbolethSSOConfiguration configuration = new ShibbolethSSOConfiguration();
        populateBean(configuration);
        configuration.setIncludeAttributeStatement(includeAttributeStatement());

        return configuration;
    }
}