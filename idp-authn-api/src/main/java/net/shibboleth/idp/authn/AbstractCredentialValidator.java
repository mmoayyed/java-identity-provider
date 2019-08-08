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

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
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
        implements CredentialValidator {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractCredentialValidator.class);

    /** Activation condition. */
    @Nonnull private Predicate<ProfileRequestContext> activationCondition;
    
    /** Cached log prefix. */
    @Nullable private String logPrefix;
    
    /** Constructor. */
    public AbstractCredentialValidator() {
        activationCondition = Predicates.alwaysTrue();
    }
    
    /** {@inheritDoc} */
    @Override
    public void setId(final String id) {
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
    public Subject validate(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext,
            @Nullable final WarningHandler warningHandler,
            @Nullable final ErrorHandler errorHandler) throws Exception {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        if (!activationCondition.test(profileRequestContext)) {
            log.debug("{} Activation condition was false, ignoring request", getLogPrefix());
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
    
}