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

package net.shibboleth.idp.saml.profile.impl;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.config.navigate.ForceAuthnProfileConfigPredicate;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.idp.saml.saml2.profile.config.navigate.ProxyCountLookupFunction;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.IDPEntry;
import org.opensaml.saml.saml2.core.IDPList;
import org.opensaml.saml.saml2.core.Scoping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An action that creates an {@link AuthenticationContext} and attaches it to the current {@link ProfileRequestContext}.
 * 
 * <p>If the incoming message is a SAML 2.0 {@link AuthnRequest}, then basic authentication policy (IsPassive,
 * ForceAuthn, Scoping) is copied into the context from the request.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#ACCESS_DENIED}
 * 
 * @post ProfileRequestContext.getSubcontext(AuthenticationContext.class) != true
 * @post SAML 2.0 AuthnRequest policy flags are (optionally) copied to the {@link AuthenticationContext}
 */
public class InitializeAuthenticationContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InitializeAuthenticationContext.class);

    /** Strategy used to look up a {@link RelyingPartyContext} for configuration options. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Extracts forceAuthn property from profile config. */
    @Nonnull private Predicate<ProfileRequestContext> forceAuthnPredicate;
    
    /** Strategy used to determine proxy count from configuration. */
    @Nullable private Function<ProfileRequestContext,Integer> proxyCountLookupStrategy;
    
    /** Strategy used to locate the {@link AuthnRequest} to operate on, if any. */
    @Nonnull private Function<ProfileRequestContext,AuthnRequest> requestLookupStrategy;
    
    /** Whether to honor various policy in an {@link AuthnRequest}. */
    private boolean honorAuthnRequest;
    
    /** Incoming SAML 2.0 request, if present. */
    @Nullable private AuthnRequest authnRequest;

    /** Constructor. */
    public InitializeAuthenticationContext() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        forceAuthnPredicate = new ForceAuthnProfileConfigPredicate();
        proxyCountLookupStrategy = new ProxyCountLookupFunction();
        requestLookupStrategy = new MessageLookup<>(AuthnRequest.class).compose(new InboundMessageContextLookup());
        honorAuthnRequest = true;
    }
    
    /**
     * Set the strategy used to return the {@link RelyingPartyContext} for configuration options.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.0.0
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }
    
    /**
     * Set the predicate to apply to derive the message-independent forced authn default. 
     * 
     * @param condition condition to set
     * 
     * @since 3.4.0
     */
    public void setForceAuthnPredicate(@Nonnull final Predicate<ProfileRequestContext> condition) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        forceAuthnPredicate = Constraint.isNotNull(condition, "Forced authentication predicate cannot be null");
    }
    
    /**
     * Set the lookup function to apply to derive the proxy count from the configuration.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.0.0
     */
    public void setProxyCountLookupStrategy(@Nonnull final Function<ProfileRequestContext,Integer> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        proxyCountLookupStrategy = Constraint.isNotNull(strategy, "Proxy count lookup strategy cannot be null");
    }
    
    /**
     * Set the strategy used to locate the {@link AuthnRequest} to examine, if any.
     * 
     * @param strategy strategy used to locate the {@link AuthnRequest}
     */
    public void setRequestLookupStrategy(@Nonnull final Function<ProfileRequestContext,AuthnRequest> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        requestLookupStrategy = Constraint.isNotNull(strategy, "AuthnRequest lookup strategy cannot be null");
    }
    
    /**
     * Sets whether to honor various policy in an {@link AuthnRequest} such as IsPassive, ForceAuthn, and Scoping.
     * 
     * <p>Turning this off constitutes a standards violation and is provided for compatibility with garbage SAML
     * implementations and incompetent deployers.</p>
     * 
     * @param flag flag to set
     * 
     * @since 4.0.0
     */
    public void setHonorAuthnRequest(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        honorAuthnRequest = flag;
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        authnRequest = requestLookupStrategy.apply(profileRequestContext);
        
        if (authnRequest != null && !honorAuthnRequest) {
            log.warn("{} Ignoring incoming AuthnRequest policy content in violation of SAML standard",
                    getLogPrefix());
            authnRequest = null;
        }
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final AuthenticationContext authnCtx = new AuthenticationContext();

        if (authnRequest != null) {
            if (!processScoping(profileRequestContext, authnCtx)) {
                return;
            }
            authnCtx.setForceAuthn(authnRequest.isForceAuthn());
            authnCtx.setIsPassive(authnRequest.isPassive());
        }

        if (!authnCtx.isForceAuthn()) {
            authnCtx.setForceAuthn(forceAuthnPredicate.test(profileRequestContext));
        }
        
        // Merge requested and pre-configured proxy count.
        
        final Integer reqCount = authnCtx.getProxyCount();
        Integer configCount = proxyCountLookupStrategy.apply(profileRequestContext);
        if (configCount != null && configCount < 0) {
            configCount = 0;
        }
        
        if (reqCount != null) {
            if (configCount != null) {
                authnCtx.setProxyCount(Integer.min(configCount, reqCount));
                log.debug("{} Combined requested and configured proxy count: {}", getLogPrefix(),
                        authnCtx.getProxyCount());
            }
        } else {
            authnCtx.setProxyCount(configCount);
        }
        
        profileRequestContext.addSubcontext(authnCtx, true);

        log.debug("{} Created authentication context: {}", getLogPrefix(), authnCtx);
    }
    
    /**
     * Check an inbound {@link AuthnRequest} for a {@link Scoping} element.
     * 
     * @param profileRequestContext current profile request context
     * @param authenticationContext the context to populate
     * 
     * @return true iff processing should continue
     */
    private boolean processScoping(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        final Scoping scoping = authnRequest.getScoping();
        if (scoping == null) {
            log.debug("{} AuthnRequest did not contain Scoping, nothing to do", getLogPrefix());
            return true;
        }
        
        // Check if permitted.
        final RelyingPartyContext rpContext = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpContext != null && rpContext.getProfileConfig() != null
                && rpContext.getProfileConfig() instanceof BrowserSSOProfileConfiguration) {
            if (((BrowserSSOProfileConfiguration) rpContext.getProfileConfig()).isFeatureDisallowed(
                    profileRequestContext, BrowserSSOProfileConfiguration.FEATURE_SCOPING)) {
                log.warn("{} Incoming Scoping disallowed by profile configuration", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, EventIds.ACCESS_DENIED);
                return false;
            }
        }
        
        // The IDPList doesn't have mandatory semantics, other than disallowing removal.
        
        final IDPList idpList = scoping.getIDPList();
        if (idpList != null && idpList.getIDPEntrys() != null) {
            final Set<String> requestedAuthorities = idpList.getIDPEntrys()
                    .stream()
                    .map(IDPEntry::getProviderID)
                    .filter(id -> id != null)
                    .collect(Collectors.toUnmodifiableSet());
            authenticationContext.getProxiableAuthorities().addAll(requestedAuthorities);
        }
        
        if (scoping.getProxyCount() != null) {
            authenticationContext.setProxyCount(Integer.max(0, scoping.getProxyCount()));
        }
        return true;
    }
    
}