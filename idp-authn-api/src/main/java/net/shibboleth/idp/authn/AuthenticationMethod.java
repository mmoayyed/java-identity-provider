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

import java.security.Principal;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;

/**
 * Context describing a particular authentication method by which a user may be authenticated. An authentication method
 * generally correspond to a broad class of concrete authentication mechanisms. For example a username/password method
 * rather than a username/password authenticated an LDAP directory over TLS.
 */
@ThreadSafe
public class AuthenticationMethod {

    /** Indicates the requirement level for a particular authentication method. */
    public enum MethodRequirement {
        /**
         * Indicates that the authentication method is not required in order, nor sufficient, to complete the
         * authentication process.
         */
        OPTIONAL,

        /**
         * Indicates that the authentication method is not required, but is sufficient, to complete the authentication
         * process.
         */
        SUFFICIENT,

        /**
         * Indicates that the authentication method is required and sufficient to complete the authentication process.
         */
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

    /** Unique identifier for this method. */
    private final String id;

    /** Instant the authentication completed given in millisecond since the epoch. */
    private long authenticationInstant;

    /** The requirement level for this method. */
    private final MethodRequirement requirement;

    /** The current state of this method. */
    private MethodState state;

    /** The principal that has been authenticated. */
    private Principal authenticatedPrinicpal;

    /** The credential for the principal that has been authenticated. */
    private PrincipalCredential principalCredential;

    /** The error that prevented the principal from being authenticated. */
    private AuthenticationException authenticationError;
    
    //TODO duration

    /**
     * Constructor. Also sets the current state to {@link MethodState#NOT_STARTED}.
     * 
     * @param methodId unique identifier for this method, can not be null or empty
     * @param requirementLevel requirement level for this method, {@link MethodRequirement#OPTIONAL} used if this value
     *            is null
     */
    public AuthenticationMethod(final String methodId, final MethodRequirement requirementLevel) {
        super();
        
        id = StringSupport.trimOrNull(methodId);
        Assert.isNotNull(id, "Authentication method identifier can not be null or empty");

        if (requirementLevel == null) {
            requirement = MethodRequirement.OPTIONAL;
        } else {
            requirement = requirementLevel;
        }

        state = MethodState.NOT_STARTED;
        authenticationInstant = -1;
    }

    /**
     * Gets the unique identifier for this method.
     * 
     * @return unique identifier for this method, never null
     */
    public String getMethodId() {
        return id;
    }

    /**
     * Gets the instant the authentication completed given in millisecond since the epoch.
     * 
     * @return Instant the authentication completed given in millisecond since the epoch or -1 if this method has not
     *         yet completed
     */
    public long getAuthenticationInstant() {
        return authenticationInstant;
    }

    /**
     * Gets the requirement "level" for this authentication method.
     * 
     * @return requirement level for this method, never null
     */
    public MethodRequirement getRequirement() {
        return requirement;
    }

    /**
     * Gets the current state of this authentication method.
     * 
     * @return current state of this authentication method, never null
     */
    public MethodState getCurrentState() {
        return state;
    }

    /**
     * Transitions this method to the state {@link MethodState#IN_PROGRESS}. This removes any previously set
     * authentication principal, principal credential, and authentication error state.
     */
    public void transitionStateToInProgress() {
        state = MethodState.IN_PROGRESS;
        authenticatedPrinicpal = null;
        principalCredential = null;
        authenticationError = null;
        authenticationInstant = -1;
    }

    /**
     * Transitions this method to the state {@link MethodState#SUCCESS}. This sets the authenticated principal and
     * authentication instant for this method and wipes out any authentication error state that may have existed.
     * 
     * @param principal principal that was authenticated
     */
    public void transitionStateToSuccess(final Principal principal) {
        Assert.isNotNull(principal, "Authenticated principal can not be null");
        state = MethodState.SUCCESS;
        authenticatedPrinicpal = principal;
        authenticationError = null;
        authenticationInstant = System.currentTimeMillis();
    }

    /**
     * Transitions this method to the state {@link MethodState#ERROR}. This sets the authentication error and
     * authentication instant for this method and wipes out any existing authenticated principal state.
     * 
     * @param error the error that occured during authentication
     */
    public void transitionStateToError(final AuthenticationException error) {
        Assert.isNotNull(error, "Authenticated error can not be null");
        state = MethodState.ERROR;
        authenticationError = error;
        authenticatedPrinicpal = null;
        authenticationInstant = System.currentTimeMillis();
    }

    /**
     * Gets the principal that was authenticated by this method.
     * 
     * @return principal that was authenticated by this method, may be null
     */
    public Principal getAuthenticatedPrincipal() {
        return authenticatedPrinicpal;
    }

    /**
     * Gets the credential, used by this method, to authenticate the user.
     * 
     * @return credential used to authenticate the user, may be null
     */
    public PrincipalCredential getPrincipalCredential() {
        return principalCredential;
    }

    /**
     * Sets the credential, used by this method, to authenticate the user.
     * 
     * @param newCredential credential, used by this method, to authenticate the user, may be null
     */
    public void setPrincipalCredential(final PrincipalCredential newCredential) {
        principalCredential = newCredential;
    }

    /**
     * Gets the error that caused this authentication method to fail.
     * 
     * @return error that caused this authentication method to fail, will be null if the authentication method completed
     *         successfully
     */
    public AuthenticationException getAuthenticationError() {
        return authenticationError;
    }
}