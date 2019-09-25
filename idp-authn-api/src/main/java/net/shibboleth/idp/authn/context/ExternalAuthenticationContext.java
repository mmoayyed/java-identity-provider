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

package net.shibboleth.idp.authn.context;

import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.messaging.context.BaseContext;

import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A context representing the state of an externalized authentication attempt,
 * a case where authentication happens outside of a web flow.
 * 
 * @parent {@link AuthenticationContext}
 * @added Before dispatching control to an external login flow
 */
public final class ExternalAuthenticationContext extends BaseContext {
    
    /** Implementation object. */
    @Nonnull private final ExternalAuthentication externalAuthentication; 
    
    /** Value of flowExecutionUrl on branching from flow. */
    @Nullable private String flowExecutionUrl;

    /** A {@link Principal} that was authenticated. */
    @Nullable private Principal principal;

    /** Name of a principal that was authenticated. */
    @Nullable private String principalName;

    /** Name of a {@link Subject} that was authenticated. */
    @Nullable private Subject subject;

    /** Time of authentication. */
    @Nullable private Instant authnInstant;
    
    /** Proxied authenticating sources. */
    @Nonnull @NonnullElements private Collection<String> authenticatingAuthorities;
    
    /** Error message. */
    @Nullable private String authnError;
    
    /** Exception. */
    @Nullable private Exception authnException;
    
    /** Flag preventing caching of result for SSO. */
    private boolean doNotCache;
    
    /** Flag indicating this "new" result is really "old". */
    private boolean previousResult;
    
    /**
     * Constructor.
     * 
     * @param authentication implementation object
     */
    public ExternalAuthenticationContext(@Nonnull final ExternalAuthentication authentication) {
        externalAuthentication = Constraint.isNotNull(authentication, "ExternalAuthentication cannot be null");
        authenticatingAuthorities = new ArrayList<>();
    }
    
    /**
     * Get the {@link ExternalAuthentication} object installed in the context.
     * 
     * @return the external authentication implementation
     * 
     * @since 4.0.0
     */
    @Nonnull public ExternalAuthentication getExternalAuthentication() {
        return externalAuthentication;
    }
    
    /**
     * Get the flow execution URL to return control to.
     * 
     * @return return location
     */
    @Nullable public String getFlowExecutionUrl() {
        return flowExecutionUrl;
    }
    
    /**
     * 
     * Set the flow execution URL to return control to.
     * 
     * @param url   return location
     * 
     * @return this context
     */
    @Nonnull public ExternalAuthenticationContext setFlowExecutionUrl(@Nullable final String url) {
        flowExecutionUrl = url;
        
        return this;
    }

    /**
     * Get a {@link Principal} that was authenticated.
     * 
     * @return the principal
     */
    @Nullable public Principal getPrincipal() {
        return principal;
    }

    /**
     * Set a {@link Principal} that was authenticated.
     * 
     * @param prin principal to set
     * 
     * @return this context
     */
    @Nonnull public ExternalAuthenticationContext setPrincipal(@Nullable final Principal prin) {
        principal = prin;
        
        return this;
    }

    /**
     * Get the name of a principal that was authenticated.
     * 
     * @return name of a principal
     */
    @Nullable public String getPrincipalName() {
        return principalName;
    }

    /**
     * Set the name of a principal that was authenticated.
     * 
     * @param name name of principal to set
     * 
     * @return this context
     */
    @Nonnull public ExternalAuthenticationContext setPrincipalName(@Nullable final String name) {
        principalName = name;
        
        return this;
    }

    /**
     * Get a {@link Subject} that was authenticated.
     * 
     * @return subject that was authenticated
     */
    @Nullable public Subject getSubject() {
        return subject;
    }

    /**
     * Set a {@link Subject} that was authenticated.
     * 
     * @param sub The subject to set
     * 
     * @return this context
     */
    @Nonnull public ExternalAuthenticationContext setSubject(@Nullable final Subject sub) {
        subject = sub;
        
        return this;
    }

    /**
     * Get the time of authentication.
     * 
     * @return time of authentication
     */
    @Nullable public Instant getAuthnInstant() {
        return authnInstant;
    }

    /**
     * Set the time of authentication.
     * 
     * @param instant time of authentication to set
     * 
     * @return this context
     */
    @Nonnull public ExternalAuthenticationContext setAuthnInstant(@Nullable final Instant instant) {
        authnInstant = instant;
        
        return this;
    }
    
    /**
     * Get a mutable, ordered list of proxied authentication sources.
     * 
     * @return proxied authentication sources
     * 
     * @since 3.4.0
     */
    @Nonnull @NonnullElements @Live public Collection<String> getAuthenticatingAuthorities() {
        return authenticatingAuthorities;
    }

    /**
     * Get an error message from the authentication process.
     * 
     * @return an error message
     */
    @Nullable public String getAuthnError() {
        return authnError;
    }

    /**
     * Set an error message from the authentication process.
     * 
     * @param message message to set
     * 
     * @return this context
     */
    @Nonnull public ExternalAuthenticationContext setAuthnError(@Nullable final String message) {
        authnError = message;
        
        return this;
    }

    /**
     * Get an exception from the authentication process.
     * 
     * @return an exception
     */
    @Nullable public Exception getAuthnException() {
        return authnException;
    }

    /**
     * Set an exception from the authentication process.
     * 
     * @param exception exception to set
     * 
     * @return this context
     */
    @Nonnull public ExternalAuthenticationContext setAuthnException(@Nullable final Exception exception) {
        authnException = exception;
        
        return this;
    }
    
    /**
     * Get the "do not cache" flag.
     * 
     * @return true iff the result of the authentication should not be cached
     */
    public boolean doNotCache() {
        return doNotCache;
    }
    
    /**
     * Set the "do not cache" flag.
     * 
     * @param flag flag to set
     * 
     * @return this context
     */
    @Nonnull public ExternalAuthenticationContext setDoNotCache(final boolean flag) {
        doNotCache = flag;
        
        return this;
    }
    
    /**
     * Get whether this result is the product of an external SSO event and not
     * a new act of authentication.
     * 
     * @return true iff this result was produced as part of an earlier request
     * 
     * @since 3.3.0
     */
    public boolean isPreviousResult() {
        return previousResult;
    }
    
    /**
     * Set whether this result is the product of an external SSO event and not
     * a new act of authentication.
     * 
     * @param flag flag to set
     * 
     * @return this context
     * 
     * @since 3.3.0
     */
    @Nonnull public ExternalAuthenticationContext setPreviousResult(final boolean flag) {
        previousResult = flag;
        
        return this;
    }
}