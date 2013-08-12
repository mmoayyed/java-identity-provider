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

import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.AbstractValidationAction;
import net.shibboleth.idp.authn.AuthenticationException;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.UsernamePrincipal;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernameContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;

/**
 * An action that checks for a {@link UsernameContext} and directly produces an {@link AuthenticationResult}
 * based on that identity.
 * 
 * <p>Various optional properties are supported to control the validation process.</p>
 *  
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#INVALID_AUTHN_CTX}
 * @event {@link AuthnEventIds#INVALID_CREDENTIALS}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false).getAttemptedFlow() != null</pre>
 * @post If AuthenticationContext.getSubcontext(UsernameContext.class, false).getUsername() != null, then
 * an {@link AuthenticationResult} is saved to the {@link AuthenticationContext}.
 */
public class ValidateRemoteUser extends AbstractValidationAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ValidateRemoteUser.class);

    /** Username context identifying identity to validate. */
    @Nullable private UsernameContext usernameContext;
    
    /** A whitelist of usernames to accept. */
    @Nonnull @NonnullElements @Unmodifiable private Set<String> whitelistedUsernames;

    /** A blacklist of usernames to deny. */
    @Nonnull @NonnullElements @Unmodifiable private Set<String> blacklistedUsernames;

    /** A regular expression to apply for acceptance testing. */
    @Nullable private Pattern matchExpression;
    
    /**
     * Get the whitelisted usernames.
     * 
     * @return the whitelisted usernames
     */
    @Nonnull @NonnullElements @Unmodifiable public Set<String> getWhitelistedUsernames() {
        return whitelistedUsernames;
    }

    /**
     * Set the whitelisted usernames.
     * 
     * @param whitelist whitelist to set
     */
    public void setWhitelistedUsernames(@Nonnull @NonnullElements final Collection<String> whitelist) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        whitelistedUsernames = ImmutableSet.copyOf(Collections2.filter(whitelist, Predicates.notNull()));
    }

    /**
     * Get the blacklisted usernames.
     * 
     * @return the blacklisted usernames
     */
    @Nonnull @NonnullElements @Unmodifiable public Set<String> getBlacklistedUsernames() {
        return blacklistedUsernames;
    }

    /**
     * Set the blacklisted usernames.
     * 
     * @param blacklist blacklist to set
     */
    public void setBlacklistedUsernames(@Nonnull @NonnullElements final Collection<String> blacklist) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        blacklistedUsernames = ImmutableSet.copyOf(Collections2.filter(blacklist, Predicates.notNull()));
    }

    /**
     * Get a matching expression to apply for acceptance.
     * 
     * @return a matching expression
     */
    @Nullable public Pattern getMatchExpression() {
        return matchExpression;
    }

    /**
     * Set a matching expression to apply for acceptance. 
     * 
     * @param expression a matching expression
     */
    public void setMatchExpression(@Nullable final Pattern expression) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        matchExpression = expression;
    }

    /** {@inheritDoc} */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {
        if (authenticationContext.getAttemptedFlow() == null) {
            log.debug("{} no attempted flow within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_AUTHN_CTX);
            return false;
        }
        
        usernameContext = authenticationContext.getSubcontext(UsernameContext.class, false);
        if (usernameContext == null) {
            log.debug("{} no UsernameContext available within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return false;
        }

        if (usernameContext.getUsername() == null) {
            log.debug("{} no username available within UsernameContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) throws AuthenticationException {
                
        if (!isAuthenticated(usernameContext.getUsername())) {
            log.debug("{} user '{}' was not valid", getLogPrefix(), usernameContext.getUsername());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_CREDENTIALS);
            return;
        }

        log.debug("{} validated user '{}'", getLogPrefix(), usernameContext.getUsername());
        
        buildAuthenticationResult(profileRequestContext, authenticationContext);
    }
    
    /**
     * Check whitelist, blacklist, and matching expression for acceptance.
     * 
     * @param username  the username to evaluate
     * @return  true iff the username is acceptable
     */
    private boolean isAuthenticated(@Nonnull @NotEmpty final String username) {
        
        if (!getWhitelistedUsernames().contains(username)) {
            // Not in whitelist. Only accept if a regexp applies.
            if (getMatchExpression() == null) {
                return false;
            } else {
                return getMatchExpression().matcher(username).matches();
            }
        } else {
            // In whitelist. Check blacklist, and if necessary a regexp.
            return !getBlacklistedUsernames().contains(username)
                    && (getMatchExpression() == null || getMatchExpression().matcher(username).matches());
        }
    }

    /** {@inheritDoc} */
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject) throws AuthenticationException {
        subject.getPrincipals().add(new UsernamePrincipal(usernameContext.getUsername()));
        return subject;
    }
}