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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.NameIDType;
import org.slf4j.Logger;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * An action that processes a SAML 2 {@link AuthnRequest} and blocks the use of any "simple"
 * disallowed features.
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link EventIds#ACCESS_DENIED}
 */
public class EnforceDisallowedSSOFeatures extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(EnforceDisallowedSSOFeatures.class);

    /** Strategy used to look up a {@link RelyingPartyContext} for configuration options. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Lookup strategy function for obtaining {@link AuthnRequest}. */
    @Nonnull private Function<ProfileRequestContext,AuthnRequest> authnRequestLookupStrategy;
    
    /** The request message to read from. */
    @NonnullBeforeExec private AuthnRequest authnRequest;
    
    /** Constructor. */
    public EnforceDisallowedSSOFeatures() {
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        final Function<ProfileRequestContext,AuthnRequest> arls = new MessageLookup<>(AuthnRequest.class).compose(new InboundMessageContextLookup());
        assert arls!=null;
        authnRequestLookupStrategy = arls;
    }

    /**
     * Set the strategy used to return the {@link RelyingPartyContext} for configuration options.
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.3.0
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        checkSetterPreconditions();
        
        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link AuthnRequest} to read from.
     * 
     * @param strategy lookup strategy
     */
    public void setAuthnRequestLookupStrategy(@Nonnull final Function<ProfileRequestContext,AuthnRequest> strategy) {
        checkSetterPreconditions();
        
        authnRequestLookupStrategy = Constraint.isNotNull(strategy, "AuthnRequest lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        checkComponentActive();
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        authnRequest = authnRequestLookupStrategy.apply(profileRequestContext);
        if (authnRequest == null) {
            log.debug("{} AuthnRequest message was not returned by lookup strategy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }
        
        return true;
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        final RelyingPartyContext rpContext = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        final ProfileConfiguration pc = rpContext == null ? null : rpContext.getProfileConfig();
        
        if (rpContext == null || pc == null || !(pc instanceof BrowserSSOProfileConfiguration)) {
            log.debug("{} No BrowserSSOProfileConfiguration available, skipping feature enforcement", getLogPrefix());
            return;
        }
        
        @Nonnull final BrowserSSOProfileConfiguration profileConfiguration = (BrowserSSOProfileConfiguration) pc;
        final Boolean forceAuthn = authnRequest.isForceAuthn();
        if (forceAuthn != null && forceAuthn &&
                profileConfiguration.isFeatureDisallowed(profileRequestContext,
                        BrowserSSOProfileConfiguration.FEATURE_FORCEAUTHN)) {
            log.warn("{} Use of ForceAuthn disallowed by profile configuration", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.ACCESS_DENIED);
            return;
        }
        
        final NameIDPolicy nidPolicy = authnRequest.getNameIDPolicy();
        if (nidPolicy != null) {
            if (nidPolicy.getFormat() != null &&
                    profileConfiguration.isFeatureDisallowed(profileRequestContext,
                            BrowserSSOProfileConfiguration.FEATURE_NAMEIDFORMAT)) {
                if (!NameIDType.UNSPECIFIED.equals(nidPolicy.getFormat()) &&
                        !NameIDType.ENCRYPTED.equals(nidPolicy.getFormat())) {
                    log.warn("{} Incoming NameID Format disallowed by profile configuration", getLogPrefix());
                    ActionSupport.buildEvent(profileRequestContext, EventIds.ACCESS_DENIED);
                    return;
                }
            }

            if (nidPolicy.getSPNameQualifier() != null &&
                    profileConfiguration.isFeatureDisallowed(profileRequestContext,
                            BrowserSSOProfileConfiguration.FEATURE_SPNAMEQUALIFIER)) {
                log.warn("{} Incoming SPNameQualifier disallowed by profile configuration", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, EventIds.ACCESS_DENIED);
                return;
            }
        }        
    }
// Checkstyle: CyclomaticComplexity ON
    
}