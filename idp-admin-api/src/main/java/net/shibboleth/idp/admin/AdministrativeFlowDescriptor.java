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

package net.shibboleth.idp.admin;

import javax.annotation.Nullable;

import org.opensaml.saml.ext.saml2mdui.UIInfo;

import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;

/**
 * A descriptor for an administrative flow.
 * 
 * <p>Administrative flows are essentially any feature intrinsic to the IdP itself and generally
 * not exposed to external systems using security mechanisms that would involve the more traditional
 * "relying party" machinery and security models. Examples include status reporting and service
 * management features, or user self-service features.
 * </p>
 * 
 * @since 3.3.0
 */
public interface AdministrativeFlowDescriptor extends AuthenticationProfileConfiguration {
    
    /**
     * Get a logging ID to use when auditing this profile.
     * 
     * @return logging ID
     */
    @Nullable String getLoggingId();

    /**
     * Get whether this flow supports non-browser clients (default is true).
     * 
     * @return whether this flow supports non-browser clients
     */
    boolean isNonBrowserSupported();
    
    /**
     * Get whether user authentication is required (default is false).
     * 
     * @return whether user authentication is required
     */
    boolean isAuthenticated();
    
    /**
     * Get the user interface details for this profile.
     * 
     * @return user interface details
     */
    @Nullable UIInfo getUIInfo();
    
    /**
     * Get the access control policy for this flow.
     * 
     * @return name of access control policy
     */
    @Nullable String getPolicyName();

    /**
     * Get whether to resolve attributes during the profile (default is false).
     * 
     * @return whether to resolve attributes during the profile
     */
    boolean resolveAttributes();

}