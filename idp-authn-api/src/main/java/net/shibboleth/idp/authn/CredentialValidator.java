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

package net.shibboleth.idp.authn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.security.auth.Subject;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.IdentifiableComponent;

/**
 * High-level API for validating credentials and producing a Java Subject as a result.
 * 
 * <p>This is more or less what JAAS does but with a simpler interface adapted better
 * to the IdP's needs. Predominantly for password validation scenarios but the interface
 * is not specific to that use case.</p>
 * 
 * <p>Instances of this interface must be stateless.</p>
 * 
 * @since 4.0.0
 */
@ThreadSafe
public interface CredentialValidator extends IdentifiableComponent {

    /**
     * Validate any credentials found in a supported form within the input context tree
     * and produce a {@link Subject} as the outcome.
     * 
     * <p>A null result is used to signal that validation was not attempted.</p>
     * 
     * @param profileRequestContext profile request context
     * @param authenticationContext authentication context
     * @param warningHandler optional warning handler interface
     * @param errorHandler optional error handler interface
     * 
     * @return result of a successful validation, or null
     * 
     * @throws Exception when validation is unsuccessful due to a failed attempt
     */
    @Nullable Subject validate(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nullable final WarningHandler warningHandler,
            @Nullable final ErrorHandler errorHandler) throws Exception;
    
    /**
     * Interface to use to report warnings to the caller.
     */
    @ThreadSafe
    public interface WarningHandler {
        
        /**
         * Reports a warning state to the caller.
         * 
         * <p>Warnings are an indication that authentication may have succeeded but with some information
         * worth capturing.</p>
         * 
         * @param profileRequestContext the current profile request context
         * @param authenticationContext the current authentication context
         * @param message to report
         * @param eventId a default webflow event to report as the result of the calling action
         */
        void handleWarning(@Nonnull final ProfileRequestContext profileRequestContext,
                @Nonnull final AuthenticationContext authenticationContext, @Nullable final String message,
                @Nonnull @NotEmpty final String eventId);
    }
    
    /**
     * Interface to use to report errors to the caller.
     */
    @ThreadSafe
    public interface ErrorHandler {
        
        /**
         * Reports an error state to the caller.
         * 
         * <p>Errors should never be reported as part of a successful login.</p>
         * 
         * @param profileRequestContext the current profile request context
         * @param authenticationContext the current authentication context
         * @param e exception to report
         * @param eventId a default webflow event to report as the result of the calling action
         */
        void handleError(@Nonnull final ProfileRequestContext profileRequestContext,
                @Nonnull final AuthenticationContext authenticationContext, @Nonnull final Exception e,
                @Nonnull @NotEmpty final String eventId);
        
        /**
         * Reports an error state to the caller.
         * 
         * <p>Errors should never be reported as part of a successful login.</p>
         * 
         * @param profileRequestContext the current profile request context
         * @param authenticationContext the current authentication context
         * @param message to report
         * @param eventId a default webflow event to report as the result of the calling action
         */
        void handleError(@Nonnull final ProfileRequestContext profileRequestContext,
                @Nonnull final AuthenticationContext authenticationContext, @Nullable final String message,
                @Nonnull @NotEmpty final String eventId);
    }

}