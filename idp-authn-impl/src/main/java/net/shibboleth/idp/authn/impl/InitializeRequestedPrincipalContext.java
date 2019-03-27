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
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.config.AuthenticationProfileConfiguration;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An action that creates an {@link RequestedPrincipalContext} and attaches it to the current
 * {@link AuthenticationContext}, if the profile request context contains a {@link RelyingPartyContext}
 * with an {@link AuthenticationProfileConfiguration} containing one or more default authentication
 * methods.
 * 
 * <p>If such a context already exists, it is left in place unless the {@link #replaceExistingContext} property
 * is set.</p>
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CTX}
 * @event {@link IdPEventIds#INVALID_PROFILE_CONFIG}
 * @post See above.
 */
public class InitializeRequestedPrincipalContext extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InitializeRequestedPrincipalContext.class);

    /** Whether to replace an existing subcontext, if any. */
    private boolean replaceExistingContext;
    
    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;
    
    /** Profile configuration source for requested principals. */
    @Nullable private AuthenticationProfileConfiguration authenticationProfileConfig;

    /** Constructor. */
    public InitializeRequestedPrincipalContext() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
    }
    
    /**
     * Whether any existing {@link RequestedPrincipalContext} should be replaced, defaults to "false".
     * 
     * <p>Normally an existing context would indicate requirements that shouldn't be circumvented to comply with
     * expected profile behavior.</p>
     * 
     * @param flag flag to set
     */
    public void setReplaceExistingContext(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        replaceExistingContext = flag;
    }
    
    /**
     * Set the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        } else if (authenticationContext.getSubcontext(RequestedPrincipalContext.class) != null
                && !replaceExistingContext) {
            log.debug("{} Leaving existing RequestedPrincipalContext in place", getLogPrefix());
            return false;
        }
        
        final RelyingPartyContext rpCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpCtx == null) {
            log.debug("{} No relying party context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        }

        final ProfileConfiguration config = rpCtx.getProfileConfig();
        if (config == null) {
            log.debug("{} No profile configuration", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }
        
        if (!(config instanceof AuthenticationProfileConfiguration)) {
            log.debug("{} Not an authentication profile", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }
        
        authenticationProfileConfig = (AuthenticationProfileConfiguration) config;
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final List<Principal> principals =
                authenticationProfileConfig.getDefaultAuthenticationMethods(profileRequestContext);
        if (principals.isEmpty()) {
            log.debug("{} Profile configuration did not supply any default authentication methods", getLogPrefix());
            return;
        }

        final RequestedPrincipalContext principalCtx = new RequestedPrincipalContext();
        principalCtx.setOperator("exact");
        principalCtx.setRequestedPrincipals(principals);
        principalCtx.setPrincipalEvalPredicateFactoryRegistry(
                authenticationContext.getPrincipalEvalPredicateFactoryRegistry());
        authenticationContext.addSubcontext(principalCtx, true);
        
        log.debug("{} Established RequestedPrincipalContext with {} methods", getLogPrefix(),
                principalCtx.getRequestedPrincipals().size());
    }
    
}