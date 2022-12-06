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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.authn.AccountLockoutManager;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnAuditFields;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.CredentialValidator;
import net.shibboleth.idp.authn.CredentialValidator.ErrorHandler;
import net.shibboleth.idp.authn.CredentialValidator.WarningHandler;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * An action that processes a list of {@link CredentialValidator} objects to produce an {@link AuthenticationResult}.
 *  
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event others on error
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class).getAttemptedFlow() != null</pre>
 * 
 * @since 4.0.0
 */
public class ValidateCredentials extends AbstractAuditingValidationAction implements WarningHandler, ErrorHandler {

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp.authn";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateCredentials.class);
    
    /** Ordered list of validators. */
    @Nonnull @NonnullElements private List<CredentialValidator> credentialValidators;
    
    /** Whether all validators must succeed. */
    private boolean requireAll;

    /** Optional lockout management interface. */
    @Nullable private AccountLockoutManager lockoutManager;
    
    /** Results from successful validators. */
    @Nonnull @NonnullElements private Collection<Subject> results;
    
    /** Currently executing validator. */
    @Nullable private CredentialValidator currentValidator;

    /** Tracks whether a warning event was signaled. */
    private boolean warningSignaled;

    /** Tracks whether an error event was signaled. */
    private boolean errorSignaled;
    
    /** Constructor. */
    public ValidateCredentials() {
        setMetricName(DEFAULT_METRIC_NAME);
        credentialValidators = Collections.emptyList();
        results = new ArrayList<>(1);
    }
    
    /**
     * Set an account lockout management component.
     * 
     * @param manager lockout manager
     */
    public void setLockoutManager(@Nullable final AccountLockoutManager manager) {
        checkSetterPreconditions();
        lockoutManager = manager;
    }
    
    /**
     * Set the list of validators to use.
     * 
     * @param validators validators to use
     */
    public void setValidators(@Nullable @NonnullElements final List<CredentialValidator> validators) {
        checkSetterPreconditions();
        if (validators != null) {
            credentialValidators = List.copyOf(validators);
        } else {
            credentialValidators = Collections.emptyList();
        }
    }
    
    /**
     * Set whether to execute and require success from all configured validators,
     * or stop at the first successful result.
     * 
     * @param flag flag to set
     */
    public void setRequireAll(final boolean flag) {
        checkSetterPreconditions();
        requireAll = flag;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty public String getMetricName() {
        return super.getMetricName() + '.' + currentValidator.getId();
    }
       
    /** {@inheritDoc} */
    @Override
    public void handleWarning(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext, @Nullable final String message,
            @Nonnull @NotEmpty final String eventId) {
        warningSignaled = true;
        super.handleWarning(profileRequestContext, authenticationContext, message, eventId);
    }

    /** {@inheritDoc} */
    @Override
    public void handleError(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext, @Nullable final String message,
            @Nonnull @NotEmpty final String eventId) {
        errorSignaled = true;
        super.handleError(profileRequestContext, authenticationContext, message, eventId);
    }
    
    /** {@inheritDoc} */
    @Override
    public void handleError(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext, @Nonnull final Exception e,
            @Nonnull @NotEmpty final String eventId) {
        errorSignaled = true;
        super.handleError(profileRequestContext, authenticationContext, e, eventId);
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (lockoutManager != null && lockoutManager.check(profileRequestContext)) {
            log.info("{} Account locked out, aborting authentication", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, AuthnEventIds.ACCOUNT_LOCKED,
                    AuthnEventIds.ACCOUNT_LOCKED);
            return;
        }
        
        for (final CredentialValidator validator : credentialValidators) {
            log.trace("{} Attempting credential validation via {}", getLogPrefix(), validator.getId());
            
            currentValidator = validator;
            
            try {
                final Subject subject =
                        currentValidator.validate(profileRequestContext, authenticationContext, this, this);
                if (subject == null) {
                    // Ignored, so try next one.
                    continue;
                }
                
                // Add the result to the list and record it.
                results.add(subject);
                
                if (!requireAll) {
                    recordSuccess(profileRequestContext);
                    buildAuthenticationResult(profileRequestContext, authenticationContext);
                    if (!warningSignaled) {
                        ActionSupport.buildProceedEvent(profileRequestContext);
                    }
                    return;
                }
            } catch (final Exception e) {
                if (requireAll || !errorSignaled) {
                    super.handleError(profileRequestContext, authenticationContext, e, AuthnEventIds.AUTHN_EXCEPTION);
                    errorSignaled = true;
                }
                
                recordFailure(profileRequestContext);
                
                if (requireAll) {
                    break;
                }
            }
        }
        
        // If all must pass, and all passed, and at least one did something, then that's also success.
        if (requireAll && !errorSignaled && !results.isEmpty()) {
            recordSuccess(profileRequestContext);
            buildAuthenticationResult(profileRequestContext, authenticationContext);
            if (!warningSignaled) {
                ActionSupport.buildProceedEvent(profileRequestContext);
            }
            return;
        }

        // If failure, then we may need to bump a lockout count if one of them outright
        // failed. Failure could also just mean nothing was attempted.
        
        if (errorSignaled) {
            if (lockoutManager != null) {
                lockoutManager.increment(profileRequestContext);
            }
        } else {
            log.warn("{} No validators were available or usable", getLogPrefix());
            handleError(profileRequestContext, authenticationContext, AuthnEventIds.REQUEST_UNSUPPORTED,
                    AuthnEventIds.REQUEST_UNSUPPORTED);
        }
    }
// Checkstyle: CyclomaticComplexity ON
    
    /** {@inheritDoc} */
    @Override
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject) {
        
        for (final Subject s : results) {
            subject.getPrincipals().addAll(s.getPrincipals());
            subject.getPublicCredentials().addAll(s.getPublicCredentials());
            subject.getPrivateCredentials().addAll(s.getPrivateCredentials());
        }
        
        return subject;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Also optionally clears account lockout state.</p>
     */
    @Override
    protected void recordSuccess(@Nonnull final ProfileRequestContext profileRequestContext) {
        // Need to do this first because the superclass's method will call the cleanup hook.
        if (lockoutManager != null) {
            if (!lockoutManager.clear(profileRequestContext)) {
                log.warn("{} Failed to clear lockout state", getLogPrefix());
            }
        }
        
        super.recordSuccess(profileRequestContext);
    }
    
    /** {@inheritDoc} */
    @Override
    @Nullable @NonnullElements protected Map<String,String> getAuditFields(
            @Nonnull final ProfileRequestContext profileRequestContext) {
        return Map.of(AuthnAuditFields.CREDENTIAL_VALIDATOR, currentValidator.getId());
    }
    
    /**
     * A default cleanup hook that removes the {@link UsernamePasswordContext} from the tree.
     * 
     * It also "clears" the password field, but this won't be useful until we get off the String type.
     * 
     * @since 4.1.0
     */
    public static class UsernamePasswordCleanupHook implements Consumer<ProfileRequestContext> {

        /** {@inheritDoc} */
        public void accept(@Nullable final ProfileRequestContext input) {
            if (input != null) {
                final AuthenticationContext authnCtx = input.getSubcontext(AuthenticationContext.class);
                if (authnCtx != null) {
                    final UsernamePasswordContext upCtx = authnCtx.getSubcontext(UsernamePasswordContext.class);
                    if (upCtx != null) {
                        upCtx.setPassword(null);
                        authnCtx.removeSubcontext(upCtx);
                    }
                }
            }
        }
    }

}
