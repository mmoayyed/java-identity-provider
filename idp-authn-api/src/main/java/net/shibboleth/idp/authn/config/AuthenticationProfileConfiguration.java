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

package net.shibboleth.idp.authn.config;

import java.security.Principal;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

/** Configuration of profiles for authentication. */
public interface AuthenticationProfileConfiguration extends ProfileConfiguration {
    
    /**
     * Get the default authentication methods to use, expressed as custom principals.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return  default authentication methods to use
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable List<Principal> getDefaultAuthenticationMethods(
            @Nullable final ProfileRequestContext profileRequestContext);
        
    /**
     * Get the allowable authentication flows for this profile.
     * 
     * <p>The flow IDs returned MUST NOT contain the
     * {@link net.shibboleth.idp.authn.AuthenticationFlowDescriptor#FLOW_ID_PREFIX}
     * prefix common to all interceptor flows.</p>
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return  a set of authentication flow IDs to allow 
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable Set<String> getAuthenticationFlows(
            @Nullable final ProfileRequestContext profileRequestContext);

    /**
     * Get an ordered list of post-authentication interceptor flows to run for this profile.
     * 
     * <p>The flow IDs returned MUST NOT contain the
     * {@link net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor#FLOW_ID_PREFIX}
     * prefix common to all interceptor flows.</p>
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return  a set of interceptor flow IDs to enable
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable List<String> getPostAuthenticationFlows(
            @Nullable final ProfileRequestContext profileRequestContext);

    /**
     * Get the name identifier formats to use with this relying party, in order of preference.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return  name identifier formats to use
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable List<String> getNameIDFormatPrecedence(
            @Nullable final ProfileRequestContext profileRequestContext);

    /**
     * Get whether the authentication process should include a proof of user presence.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return true iff authentication should require user presence
     * 
     * @since 4.0.0
     */
    boolean isForceAuthn(@Nullable final ProfileRequestContext profileRequestContext);
    
    /**
     * Gets the maximum number of times an assertion may be proxied outbound and/or
     * the maximum number of hops between the relying party and a proxied authentication
     * authority inbound.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return maximum number of times an assertion or authentication may be proxied
     * 
     * @since 4.0.0
     */
    @NonNegative @Nullable Integer getProxyCount(@Nullable final ProfileRequestContext profileRequestContext);
    
    /**
     * Get whether this profile is for functionality local to the IdP.
     * 
     * <p>Most authentication profiles are non-local, designed to issue security tokens to other
     * systems, so this is generally false.</p>
     * 
     * @return true iff the use of the associated profile is local to the IdP
     */
    default boolean isLocal() {
        return false;
    }
}