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
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicate;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.principal.PrincipalSupportingComponent;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiedInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;

/**
 * An abstract {@link CredentialValidator} that handles some common behavior.
 * 
 * @since 4.0.0
 */
public abstract class AbstractCredentialValidator extends AbstractIdentifiedInitializableComponent
        implements CredentialValidator, PrincipalSupportingComponent {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractCredentialValidator.class);

    /** Activation condition. */
    @Nonnull private Predicate<ProfileRequestContext> activationCondition;
    
    /** Cached log prefix. */
    @Nullable private String logPrefix;
    
    /** Container that carries additional {@link Principal} objects. */
    @Nullable private Subject customPrincipals;
    
    /** Constructor. */
    public AbstractCredentialValidator() {
        activationCondition = Predicates.alwaysTrue();
    }
    
    /** {@inheritDoc} */
    @Override
    public synchronized void setId(final String id) {
        super.setId(id);
    }
    
    /**
     * Set the activation condition controlling use of validator.
     * 
     * @param condition condition to use
     */
    public void setActivationCondition(@Nonnull final Predicate<ProfileRequestContext> condition) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        activationCondition = Constraint.isNotNull(condition, "Activation condition cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements @Unmodifiable @NotLive public <T extends Principal> Set<T> getSupportedPrincipals(
            @Nonnull final Class<T> c) {
        return customPrincipals != null ? customPrincipals.getPrincipals(c) : Collections.emptySet();
    }
    
    /**
     * Set supported non-user-specific principals that the validator will include in the subjects
     * it generates.
     * 
     * @param principals supported principals to include
     */
    public void setSupportedPrincipals(@Nullable @NonnullElements final Collection<Principal> principals) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        if (principals != null) {
            final Collection<Principal> copy = Set.copyOf(principals);
            if (!copy.isEmpty()) {
                customPrincipals = new Subject();
                customPrincipals.getPrincipals().addAll(copy);
            } else {
                customPrincipals = null;
            }
        } else {
            customPrincipals = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Subject validate(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nullable final WarningHandler warningHandler,
            @Nullable final ErrorHandler errorHandler) throws Exception {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        if (!activationCondition.test(profileRequestContext)) {
            log.debug("{} Activation condition was false, ignoring request", getLogPrefix());
            return null;
        } else if (!isAcceptable(authenticationContext.getSubcontext(RequestedPrincipalContext.class),
                customPrincipals, getId())) {
            return null;
        }
        
        return doValidate(profileRequestContext, authenticationContext, warningHandler, errorHandler);
    }

    /**
     * Override method for subclasses to use to perform the actual validation.
     * 
     * @param profileRequestContext profile request context
     * @param authenticationContext authentication context
     * @param warningHandler optional warning handler interface
     * @param errorHandler optional error handler interface
     * 
     * @return the validated result, or null if inapplicable
     * 
     * @throws Exception if an error occurs
     */
    @Nullable protected abstract Subject doValidate(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nullable final WarningHandler warningHandler,
            @Nullable final ErrorHandler errorHandler) throws Exception;

    /**
     * Decorate the subject with custom principals if needed.
     * 
     * @param subject the subject being returned
     * 
     * @return the decorated subject
     */
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject) {
        if (customPrincipals != null) {
            subject.getPrincipals().addAll(customPrincipals.getPrincipals());
        }
        return subject;
    }
    
    /**
     * Return a prefix for logging messages for this component.
     * 
     * @return a string for insertion at the beginning of any log messages
     */
    @Nonnull @NotEmpty protected String getLogPrefix() {
        if (logPrefix == null) {
            logPrefix = "Credential Validator " + (getId() != null ? getId() : "(unknown)") + ":";
        }
        return logPrefix;
    }
    
    /**
     * Checks a particular request and principal collection for suitability.
     * 
     * @param requestedPrincipalCtx the relevant context
     * @param subject collection of custom principals to check, embedded in a subject
     * @param configName name for logging
     * 
     * @return true iff the request does not specify requirements or the principal collection is empty
     *  or the combination is acceptable
     */
    protected boolean isAcceptable(@Nullable final RequestedPrincipalContext requestedPrincipalCtx,
            @Nullable final Subject subject, @Nonnull @NotEmpty final String configName) {
        
        if (subject != null && requestedPrincipalCtx != null && requestedPrincipalCtx.getOperator() != null) {
            log.debug("{} Request contains principal requirements, checking validator '{}' for compatibility",
                    getLogPrefix(), configName);
            for (final Principal p : requestedPrincipalCtx.getRequestedPrincipals()) {
                final PrincipalEvalPredicateFactory factory =
                        requestedPrincipalCtx.getPrincipalEvalPredicateFactoryRegistry().lookup(
                                p.getClass(), requestedPrincipalCtx.getOperator());
                if (factory != null) {
                    final PrincipalEvalPredicate predicate = factory.getPredicate(p);
                    final PrincipalSupportingComponent wrapper = new PrincipalSupportingComponent() {
                        public <T extends Principal> Set<T> getSupportedPrincipals(final Class<T> c) {
                            return subject.getPrincipals(c);
                        }
                    };
                    if (predicate.test(wrapper)) {
                        log.debug("{} Validator '{}' compatible with principal type '{}' and operator '{}'",
                                getLogPrefix(), configName, p.getClass(), requestedPrincipalCtx.getOperator());
                        requestedPrincipalCtx.setMatchingPrincipal(predicate.getMatchingPrincipal());
                        return true;
                    }
                    log.debug("{} Validator '{}' not compatible with principal type '{}' and operator '{}'",
                            getLogPrefix(), configName, p.getClass(), requestedPrincipalCtx.getOperator());
                } else {
                    log.debug("{} No comparison logic registered for principal type '{}' and operator '{}'",
                            getLogPrefix(), p.getClass(), requestedPrincipalCtx.getOperator());
                }
            }
            
            log.debug("{} Skipping validator '{}', not compatible with request's principal requirements",
                    getLogPrefix(), configName);
            return false;
        }
        
        return true;
    }

}