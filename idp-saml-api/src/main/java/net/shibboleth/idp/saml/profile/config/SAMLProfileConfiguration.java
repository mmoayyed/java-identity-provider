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

package net.shibboleth.idp.saml.profile.config;

import java.time.Duration;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

/** Common interface for SAML profile configurations. */
public interface SAMLProfileConfiguration extends ProfileConfiguration {

    /**
     * Get the predicate used to determine if generated assertions should be signed.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return predicate used to determine if generated assertions should be signed
     */
    boolean isSignAssertions(@Nullable final ProfileRequestContext profileRequestContext);

    /**
     * Get the predicate used to determine if generated requests should be signed.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return predicate used to determine if generated requests should be signed
     */
    boolean isSignRequests(@Nullable final ProfileRequestContext profileRequestContext);

    /**
     * Get the predicate used to determine if generated responses should be signed.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return predicate used to determine if generated responses should be signed
     */
    boolean isSignResponses(@Nullable final ProfileRequestContext profileRequestContext);

    /**
     * Get the lifetime of generated assertions.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return lifetime of generated assertions
     */
    @Nonnull Duration getAssertionLifetime(@Nullable final ProfileRequestContext profileRequestContext);

    /**
     * Get an unmodifiable set of audiences, in addition to the relying party(ies) to which the IdP is issuing the
     * assertion, with which an assertion may be shared.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return additional audiences to which an assertion may be shared
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable Set<String> getAdditionalAudiencesForAssertion(
            @Nullable final ProfileRequestContext profileRequestContext);
    
    /**
     * Get whether to include a NotBefore attribute in the Conditions of generated assertions.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return  whether to include a NotBefore attribute in the Conditions of generated assertions
     */
    boolean isIncludeConditionsNotBefore(@Nullable final ProfileRequestContext profileRequestContext);
    
}