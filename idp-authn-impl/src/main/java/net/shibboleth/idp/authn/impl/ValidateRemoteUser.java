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

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernameContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * An action that checks for a {@link UsernameContext} and directly produces an
 * {@link net.shibboleth.idp.authn.AuthenticationResult} based on that identity.
 * 
 * <p>Various optional properties are supported to control the validation process.</p>
 *  
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#INVALID_CREDENTIALS}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false).getAttemptedFlow() != null</pre>
 * @post If AuthenticationContext.getSubcontext(UsernameContext.class, false).getUsername() != null, then
 * an {@link net.shibboleth.idp.authn.AuthenticationResult} is saved to the {@link AuthenticationContext}.
 */
public class ValidateRemoteUser extends AbstractAuditingValidationAction {

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp.authn.remoteuser";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateRemoteUser.class);
    
    /** Usernames to accept. */
    @Nonnull @NonnullElements private Set<String> allowedUsernames;

    /** Usernames to deny. */
    @Nonnull @NonnullElements private Set<String> deniedUsernames;

    /** A regular expression to apply for acceptance testing. */
    @Nullable private Pattern matchExpression;

    /** Username context identifying identity to validate. */
    @NonnullBeforeExec private UsernameContext usernameContext;
    
    /** Constructor. */
    public ValidateRemoteUser() {
        allowedUsernames = CollectionSupport.emptySet();
        deniedUsernames = CollectionSupport.emptySet();
        setMetricName(DEFAULT_METRIC_NAME);
    }
    
    /**
     * Set the allowed usernames.
     * 
     * @param allowed usernames to allow
     */
    public void setAllowedUsernames(@Nullable @NonnullElements final Collection<String> allowed) {
        checkSetterPreconditions();
        allowedUsernames = CollectionSupport.copyToSet(StringSupport.normalizeStringCollection(allowed));
    }

    /**
     * Set the denied usernames.
     * 
     * @param denied usernames to deny
     */
    public void setDeniedUsernames(@Nullable @NonnullElements final Collection<String> denied) {
        checkSetterPreconditions();
        deniedUsernames = CollectionSupport.copyToSet(StringSupport.normalizeStringCollection(denied));
    }

    /**
     * Set a matching expression to apply for acceptance. 
     * 
     * @param expression a matching expression
     */
    public void setMatchExpression(@Nullable final Pattern expression) {
        checkSetterPreconditions();
        if (expression != null && !expression.pattern().isEmpty()) {
            matchExpression = expression;
        } else {
            matchExpression = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }
        
        usernameContext = authenticationContext.getSubcontext(UsernameContext.class);
        if (usernameContext == null) {
            log.debug("{} No UsernameContext available within authentication context", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, AuthnEventIds.NO_CREDENTIALS,
                    AuthnEventIds.NO_CREDENTIALS);
            return false;
        }

        if (usernameContext.getUsername() == null) {
            log.debug("{} No username available within UsernameContext", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, AuthnEventIds.NO_CREDENTIALS,
                    AuthnEventIds.NO_CREDENTIALS);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final String userName = usernameContext.getUsername();
        assert userName != null;
        if (!isAuthenticated(userName)) {
            log.info("{} User '{}' was not valid", getLogPrefix(), userName);
            handleError(profileRequestContext, authenticationContext, AuthnEventIds.INVALID_CREDENTIALS,
                    AuthnEventIds.INVALID_CREDENTIALS);
            recordFailure(profileRequestContext);
            return;
        }

        log.info("{} Validated user '{}'", getLogPrefix(), userName);
        recordSuccess(profileRequestContext);
        buildAuthenticationResult(profileRequestContext, authenticationContext);
    }
    
    /**
     * Check whitelist, blacklist, and matching expression for acceptance.
     * 
     * @param username  the username to evaluate
     * @return  true iff the username is acceptable
     */
    private boolean isAuthenticated(@Nonnull @NotEmpty final String username) {

        final Pattern mc = matchExpression;
        if (!allowedUsernames.isEmpty() && !allowedUsernames.contains(username)) {
            // Not in allowed set. Only accept if a regexp applies.
            if (mc == null) {
                return false;
            }
            return mc.matcher(username).matches();
        }
        
        // In allowed set (or none). Check deny set, and if necessary a regexp.
        return !deniedUsernames.contains(username)
                && (mc == null || mc.matcher(username).matches());
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject) {
        final UsernameContext usernameContext = this.usernameContext;
        assert usernameContext != null;
        final String userName = usernameContext.getUsername();
        assert userName != null;

        subject.getPrincipals().add(new UsernamePrincipal(userName));
        return subject;
    }
    
}
