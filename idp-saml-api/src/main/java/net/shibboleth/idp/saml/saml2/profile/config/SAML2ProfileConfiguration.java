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

package net.shibboleth.idp.saml.saml2.profile.config;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

import org.opensaml.profile.context.ProfileRequestContext;

/**
 * Base interface for SAML 2 profile configurations. 
 */
public interface SAML2ProfileConfiguration {

    /**
     * Gets the maximum number of times an assertion may be proxied.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return maximum number of times an assertion may be proxied
     */
    @NonNegative long getProxyCount(@Nullable final ProfileRequestContext profileRequestContext);

    /**
     * Gets the unmodifiable collection of audiences for a proxied assertion.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return audiences for a proxied assertion
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable Collection<String> getProxyAudiences(
            @Nullable final ProfileRequestContext profileRequestContext);
    
    /**
     * Gets whether to bypass verification of request signatures.
     * 
     * <p>This is typically of use to deal with broken services or to allow a
     * signer's key to be bypassed in the event that it is managed improperly.</p>
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return true iff request signatures should be ignored
     * 
     * @since 4.0.0
     */
    boolean isIgnoreRequestSignatures(@Nonnull final ProfileRequestContext profileRequestContext);

    /**
     * Gets whether to ignore an inability to encrypt due to external factors.
     * 
     *  <p>This allows a deployer to signal that encryption is "best effort" and
     *  can be omitted if a relying party doesn't possess a key, support a compatible
     *  algorithm, etc.</p>
     *  
     *  <p>Defaults to false.</p>
     *  
     * @param profileRequestContext current profile request context
     * 
     * @return true iff encryption should be treated as optional
     */
    boolean isEncryptionOptional(@Nullable final ProfileRequestContext profileRequestContext);
    
    /**
     * Gets the predicate used to determine if assertions should be encrypted.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return predicate used to determine if assertions should be encrypted
     */
    boolean isEncryptAssertions(@Nullable final ProfileRequestContext profileRequestContext);

    /**
     * Gets the predicate used to determine if name identifiers should be encrypted.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return predicate used to determine if name identifiers should be encrypted
     */
    boolean isEncryptNameIDs(@Nullable final ProfileRequestContext profileRequestContext);

    /**
     * Gets the predicate used to determine if attributes should be encrypted.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return predicate used to determine if attributes should be encrypted
     */
    boolean isEncryptAttributes(@Nullable final ProfileRequestContext profileRequestContext);
    
}