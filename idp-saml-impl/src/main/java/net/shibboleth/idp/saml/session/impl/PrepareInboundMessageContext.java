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

package net.shibboleth.idp.saml.session.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.saml.session.SAML2SPSession;
import net.shibboleth.idp.session.context.LogoutPropagationContext;
import net.shibboleth.shared.logic.Constraint;

/**
 * Action that adds an inbound {@link MessageContext} and a {@link SAMLPeerEntityContext} to the
 * {@link ProfileRequestContext} based on the identity of a relying party, by default from a
 * {@link SAML2SPSession} found in a {@link LogoutPropagationContext}.  
 * 
 * <p>This action primarily mocks up a minimal amount of machinery on the inbound message side to drive a
 * SAML 2 Logout Propagation flow, which needs to issue a logout request message for the {@link SAML2SPSession}
 * it's given.</p>
 * 
 * <p>It has some generic capability to allow it to be used for some other outbound messaging cases, such as
 * SAML 2 SSO proxying.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 */
public class PrepareInboundMessageContext extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PrepareInboundMessageContext.class);
    
    /** Logout propagation context lookup strategy. */
    @Nonnull private Function<ProfileRequestContext,LogoutPropagationContext> logoutPropContextLookupStrategy;

    /** Optional circumvention of usual method to identify the relying party name. */
    @Nullable private Function<ProfileRequestContext,String> relyingPartyLookupStrategy;
    
    /** The relying party name to base the inbound context on. */
    @Nullable private String relyingPartyId;

    /** Constructor. */
    public PrepareInboundMessageContext() {
        logoutPropContextLookupStrategy = new ChildContextLookup<>(LogoutPropagationContext.class);
    }

    /**
     * Set the logout propagation context lookup strategy.
     * 
     * @param strategy lookup strategy
     */
    public void setLogoutPropagationContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,LogoutPropagationContext> strategy) {
        checkSetterPreconditions();
        logoutPropContextLookupStrategy =
                Constraint.isNotNull(strategy, "LogoutPropagationContext lookup strategy cannot be null");
    }
    
    /**
     * Set an optional lookup strategy to identify the relying party name, as a substitute for the session/logout
     * assumptions made by the action otherwise.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.0.0
     */
    public void setRelyingPartyLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        relyingPartyLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        if (relyingPartyLookupStrategy != null) {
            relyingPartyId = relyingPartyLookupStrategy.apply(profileRequestContext);
            if (relyingPartyId != null) {
                return true;
            }
            
            log.warn("{} No relying party ID returned from lookup function", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        final LogoutPropagationContext logoutPropCtx = logoutPropContextLookupStrategy.apply(profileRequestContext);
        if (logoutPropCtx == null) {
            log.debug("{} No logout propagation context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        } else if (logoutPropCtx.getSession() == null || !(logoutPropCtx.getSession() instanceof SAML2SPSession)) {
            log.debug("{} Logout propagation context did not contain a SAML2SPSession", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        relyingPartyId = ((SAML2SPSession) logoutPropCtx.getSession()).getId();
        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final MessageContext msgCtx = new MessageContext();
        profileRequestContext.setInboundMessageContext(msgCtx);

        final SAMLPeerEntityContext peerContext = msgCtx.getSubcontext(SAMLPeerEntityContext.class, true);
        peerContext.setEntityId(relyingPartyId);

        log.debug("{} Initialized inbound context for message to {}", getLogPrefix(), relyingPartyId);
    }
    
}