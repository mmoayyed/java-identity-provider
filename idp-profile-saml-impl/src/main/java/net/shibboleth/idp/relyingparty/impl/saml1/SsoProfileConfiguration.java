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

package net.shibboleth.idp.relyingparty.impl.saml1;

/** Configuration for SAML 1 SSO requests. */
public class SsoProfileConfiguration extends AbstractSAML1ProfileConfiguration {

    /** ID for this profile configuration. */
    public static final String PROFILE_ID = "urn:mace:shibboleth:2.0:profiles:saml1:sso";

    /** Whether responses to the authentication request should include an attribute statement. */
    private boolean includeAttributeStatement;

    /** Constructor. */
    public SsoProfileConfiguration() {
        super(PROFILE_ID);
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
}