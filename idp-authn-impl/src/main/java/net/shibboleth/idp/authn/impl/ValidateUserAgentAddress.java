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

package net.shibboleth.idp.authn.impl;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.UsernamePrincipal;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UserAgentContext;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.net.IPRange;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * An action that ensures that a user-agent address found within a {@link UserAgentContext}
 * is within a given range and generates an {@link AuthenticationResult}.
 *  
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#INVALID_AUTHN_CTX}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @event {@link AuthnEventIds#INVALID_CREDENTIALS}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false).getAttemptedFlow() != null</pre>
 * @post If AuthenticationContext.getSubcontext(UserAgentContext.class, false) != null, and the
 * content of getAddress() satisfies a configured address range, an {@link AuthenticationResult} is
 * saved to the {@link AuthenticationContext}.
 */
public class ValidateUserAgentAddress extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateUserAgentAddress.class);

    /** The ID of the subject to treat as authenticated by this action. */
    @NonnullAfterInit @NotEmpty private String principalName;

    /** List of designated IP ranges. */
    @Nonnull @NonnullElements private Collection<IPRange> designatedRanges;

    /** User Agent context containing address to evaluate. */
    @Nullable private UserAgentContext uaContext;
    
    /** Constructor. */
    public ValidateUserAgentAddress() {
        designatedRanges = Collections.emptyList();
    }
    
    /**
     * Get the name of the subject to use.
     * 
     * @return the name of the subject to use
     */
    @NonnullAfterInit @NotEmpty public String getPrincipalName() {
        return principalName;
    }
    
    /**
     * Set the name to use to identify a successfully evaluated address, by means of a simple username,
     * by attaching a {@link UsernamePrincipal}.
     * 
     * @param name  the principal name to use
     */
    public void setPrincipalName(@Nonnull @NotEmpty final String name) {
        principalName = Constraint.isNotNull(StringSupport.trimOrNull(name), "Principal name cannot be null or empty");
    }
    
    /**
     * Get the IP range(s) to authenticate.
     * 
     * @return  the IP range(s) to authenticate
     */
    @Nonnull @NonnullElements @Unmodifiable public Collection<IPRange> getDesignatedRanges() {
        return designatedRanges;
    }
    
    /**
     * Set the IP range(s) to authenticate.
     * 
     * @param ranges the IP range(s) to authenticate
     */
    public void setDesignatedRanges(@Nonnull @NonnullElements Collection<IPRange> ranges) {
        designatedRanges = ImmutableList.copyOf(Iterables.filter(ranges, Predicates.notNull()));
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        if (principalName == null) {
            throw new ComponentInitializationException("Principal name cannot be null"); 
        }
    }

    /** {@inheritDoc} */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {
        if (authenticationContext.getAttemptedFlow() == null) {
            log.debug("{} no attempted flow within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_AUTHN_CTX);
            return false;
        }
        
        uaContext = authenticationContext.getSubcontext(UserAgentContext.class, false);
        if (uaContext == null) {
            log.debug("{} no UserAgentContext available within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return false;
        }

        if (uaContext.getAddress() == null) {
            log.debug("{} no address available within UserAgentContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {

        if (!isAuthenticated(uaContext.getAddress())) {
            log.debug("{} user agent with address {} was not authenticated", getLogPrefix(), uaContext.getAddress());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_CREDENTIALS);
            return;
        }
        
        log.debug("{} authenticated user agent", getLogPrefix());
        AuthenticationResult result = new AuthenticationResult(authenticationContext.getAttemptedFlow().getId(),
                new UsernamePrincipal(getPrincipalName()));
        authenticationContext.setAuthenticationResult(result);
    }

    /**
     * Checks whether the given IP address meets this stage's IP range requirements.
     * 
     * @param address the IP address to check
     * 
     * @return true if the given IP address meets this stage's IP range requirements, false otherwise
     */
    private boolean isAuthenticated(@Nonnull final InetAddress address) {
        byte[] resolvedAddress = address.getAddress();

        for (IPRange range : designatedRanges) {
            if (range.contains(resolvedAddress)) {
                return true;
            }
        }
        
        return false;
    }

}