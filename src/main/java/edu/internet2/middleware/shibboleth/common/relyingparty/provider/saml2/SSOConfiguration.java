/*
 * Copyright 2007 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.common.relyingparty.provider.saml2;

/** SAML 2 SSO configuration settings. */
public class SSOConfiguration extends AbstractSAML2ProfileConfiguration {

    /** ID for this profile configuration. */
    public static final String PROFILE_ID = "urn:mace:shibboleth:2.0:profiles:saml2:sso";

    /** Whether responses to the authentication request should include an attribute statement. */
    private boolean includeAttributeStatement;

    /** The maximum amount of time, in milliseconds, the service provider should maintain a session for the user. */
    private long maximumSPSessionLifetime;

    /** {@inheritDoc} */
    public String getProfileId() {
        return PROFILE_ID;
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

    /**
     * Gets the maximum amount of time, in milliseconds, the service provider should maintain a session for the user
     * based on the authentication assertion. A value less than or equal to 0 is interpreted as an unlimited lifetime.
     * 
     * @return max lifetime of service provider should maintain a session
     */
    public long getMaximumSPSessionLifetime() {
        return maximumSPSessionLifetime;
    }

    /**
     * Sets the maximum amount of time, in milliseconds, the service provider should maintain a session for the user
     * based on the authentication assertion. A value less than or equal to 0 is interpreted as an unlimited lifetime.
     * 
     * @param lifetime max lifetime of service provider should maintain a session
     */
    public void setMaximumSPSessionLifetime(long lifetime) {
        if (lifetime < 1) {
            maximumSPSessionLifetime = 0;
        } else {
            maximumSPSessionLifetime = lifetime;
        }
    }
}