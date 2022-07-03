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

import java.security.Principal;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.authn.AbstractValidationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

/**
 * An action that executes a deployer-supplied function and produces an
 * {@link net.shibboleth.idp.authn.AuthenticationResult} based on the function result.
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#INVALID_CREDENTIALS}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class).getAttemptedFlow() != null</pre>
 * @post If the function returns a String, Principal, or Subject, an
 * {@link net.shibboleth.idp.authn.AuthenticationResult} is saved to the {@link AuthenticationContext}.
 * 
 * @since 3.4.0
 */
public class ValidateFunctionResult extends AbstractValidationAction {

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp.authn.function";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateFunctionResult.class);
    
    /** Function to evaluate. */
    @NonnullAfterInit private Function<ProfileRequestContext,?> resultLookupStrategy;
    
    /** Authentication result. */
    @Nullable private Object result;
    
    /** Constructor. */
    public ValidateFunctionResult() {
        setMetricName(DEFAULT_METRIC_NAME);
    }
    
    /**
     * Set the function to execute to produce the authentication result.
     * 
     * <p>The function can return a {@link String}, a {@link Principal}, or a {@link Subject}.</p>
     * 
     * @param strategy result strategy
     */
    public void setResultLookupStrategy(@Nonnull final Function<ProfileRequestContext,?> strategy) {
        throwSetterPreconditionExceptions();
        resultLookupStrategy = Constraint.isNotNull(strategy, "Result lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (resultLookupStrategy == null) {
            throw new ComponentInitializationException("Result lookup strategy cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        result = resultLookupStrategy.apply(profileRequestContext);

        if (result == null) {
            log.info("{} Authentication by function failed", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            recordFailure(profileRequestContext);
        } else if (result instanceof String) {
            log.info("{} Validated user via name '{}'", getLogPrefix(), result);
            recordSuccess(profileRequestContext);
            buildAuthenticationResult(profileRequestContext, authenticationContext);
        } else if (result instanceof Principal) {
            log.info("{} Validated user via Principal '{}'", getLogPrefix(), result);
            recordSuccess(profileRequestContext);
            buildAuthenticationResult(profileRequestContext, authenticationContext);
        } else if (result instanceof Subject) {
            log.info("{} Validated user via Subject", getLogPrefix());
            recordSuccess(profileRequestContext);
            buildAuthenticationResult(profileRequestContext, authenticationContext);
        } else {
            log.info("{} Authentication by function failed, result type was invalid", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_CREDENTIALS);
            recordFailure(profileRequestContext);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject) {
        
        if (result instanceof String) {
            subject.getPrincipals().add(new UsernamePrincipal((String) result));
            return subject;
        } else if (result instanceof Principal) {
            subject.getPrincipals().add((Principal) result);
            return subject;
        } else if (result instanceof Subject) {
            // Override supplied Subject with our own, after transferring over any custom Principals.
            ((Subject) result).getPrincipals().addAll(subject.getPrincipals());
            return (Subject) result;
        }
        
        // Save my walrus!
        throw new ConstraintViolationException("Result type was unexpected");
    }
    
}