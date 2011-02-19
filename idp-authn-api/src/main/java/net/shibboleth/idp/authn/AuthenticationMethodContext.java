/*
 * Copyright 2011 University Corporation for Advanced Internet Development, Inc.
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

package net.shibboleth.idp.authn;

import java.security.Principal;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.messaging.context.SubcontextContainer;

/**
 * Context describing a particular authentication method by which a user may be authenticated. An authentication method
 * generally correspond to a broad class of concrete authentication mechanisms. For example a username/password method
 * rather than a username/password authenticated an LDAP directory over TLS. If additional information about the actual
 * authentication mechanism needs to be carried with this method descriptor it should be added as a typed
 * {@link org.opensaml.messaging.context.Subcontext}.
 */
@ThreadSafe
public interface AuthenticationMethodContext extends Comparable<AuthenticationMethodContext>, SubcontextContainer {

    /** Indicates the requirement level for a particular authentication method. */
    public enum MethodRequirement {
        /** Indicates that the authentication method is not required in order to complete the authentication process. */
        OPTIONAL,

        /** Indicates that the authentication method is sufficient to complete the authentication process. */
        SUFFICIENT,

        /** Indicates that the authentication method is required to complete the authentication process. */
        REQUIRED
    };

    /** Indicates the current process state of a particular authentication method. */
    public enum MethodState {
        /** Indicates that no work has yet been done with the authentication method. */
        NOT_STARTED,

        /**
         * Indicates that work has begun on the authentication method but has not yet finished, perhaps because more
         * user input is needed.
         */
        IN_PROGRESS,

        /**
         * Indicates that the authentication method has completed successfully. This means that
         * {@link AuthenticationMethodContext#getAuthenticatedPrincipal()} will return the authenticated principal and
         * {@link AuthenticationMethodContext#getAuthenticationError()} will return null.
         */
        SUCCESS,

        /**
         * Indicates that the authentication method has completed, but failed. This means that
         * {@link AuthenticationMethodContext#getAuthenticatedPrincipal()} will return null and
         * {@link AuthenticationMethodContext#getAuthenticationError()} will return error that caused the method to
         * fail.
         */
        ERROR
    };

    /**
     * Gets the requirement "level" for this authentication method.
     * 
     * @return requirement level for this method, never null
     */
    public MethodRequirement getRequirement();

    /**
     * Gets the current state of this authentication method.
     * 
     * @return current state of this authentication method, never null
     */
    public MethodState getCurrentState();

    /**
     * Whether forced authentication is required or if information from a recent, previous, authentication may be used.
     * 
     * @return whether forced authentication is required
     */
    public boolean forceAuthn();

    /**
     * Gets the principal that was authenticated by this method.
     * 
     * @return principal that was authenticated by this method, may be null if the method has not completed or errored
     *         out
     */
    public Principal getAuthenticatedPrincipal();

    /**
     * Gets the credential, used by this method, to authenticate the user.
     * 
     * @return credential used to authenticate the user, may be null if the method has not completed or error'ed out
     */
    public PrincipalCredential getPrincipalCredential();

    /**
     * Gets the error that caused this authentication method to fail.
     * 
     * @return error that caused this authentication method to fail, will be null if the authentication method completed
     *         sucessfully
     */
    public AuthenticationException getAuthenticationError();
}