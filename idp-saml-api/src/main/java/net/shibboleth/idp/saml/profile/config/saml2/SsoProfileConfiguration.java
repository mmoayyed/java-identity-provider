/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.saml.profile.config.saml2;

/** Configuration for SAML 2 SSO requests. */
public class SsoProfileConfiguration extends AbstractSAML2ProfileConfiguration {

    /** ID for this profile configuration. */
    public static final String PROFILE_ID = "http://shibboleth.net/ns/profiles/saml2/sso";

    /** Whether responses to the authentication request should include an attribute statement. Default value: true */
    private boolean includeAttributeStatement;

    /**
     * The maximum amount of time, in milliseconds, the service provider should maintain a session for the user. A value
     * of 0 or less indicates no cap is put on the SP's session lifetime. Default value: 0
     */
    private long maximumSPSessionLifetime;

    /** Whether produced assertions may be delegated. Default value: false */
    private boolean allowingDelegation;

    /** Constructor. */
    public SsoProfileConfiguration() {
        this(PROFILE_ID);
    }

    /**
     * Constructor.
     * 
     * @param profileId unique ID for this profile
     */
    protected SsoProfileConfiguration(String profileId) {
        super(profileId);
        includeAttributeStatement = true;
        maximumSPSessionLifetime = 0;
        allowingDelegation = false;
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

    /**
     * Gets whether produced assertions may be delegated.
     * 
     * @return whether produced assertions may be delegated
     */
    public boolean isAllowingDelegation() {
        return allowingDelegation;
    }

    /**
     * Sets whether produced assertions may be delegated.
     * 
     * @param isAllowed whether produced assertions may be delegated
     */
    public void setAllowingDelegation(boolean isAllowed) {
        allowingDelegation = isAllowed;
    }
}