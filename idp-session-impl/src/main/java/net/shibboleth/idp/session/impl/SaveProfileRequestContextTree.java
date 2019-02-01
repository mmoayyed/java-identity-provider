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

package net.shibboleth.idp.session.impl;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.context.AuditContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.session.context.LogoutContext;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.InOutOperationContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import javax.annotation.Nonnull;

/**
 * Saves off specific portions of the context tree in use during logout processing to enable
 * reuse of logout propagation subflows during back channel logout.
 * 
 * <p>Some propagation flows make use of substantial portions of the tree, including the
 * inbound/outbound contexts, audit context, relying party context, etc., so this amounts to
 * a "push/pop" to preserve the state of the "outer" logout operation.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * 
 * @post LogoutContext.getSubcontext(InOutOperationContext.class) != null
 */
public class SaveProfileRequestContextTree extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SaveProfileRequestContextTree.class);

    /** Looks up a LogoutContext from PRC. */
    @Nonnull private Function<ProfileRequestContext,LogoutContext> logoutContextLookupStrategy;
            
    /** Constructor. */
    public SaveProfileRequestContextTree() {
        logoutContextLookupStrategy = new ChildContextLookup<>(LogoutContext.class);
    }
    
    /**
     * Set the lookup strategy for the {@link LogoutContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setLogoutContextLookupStrategy(@Nonnull final Function<ProfileRequestContext,LogoutContext> strategy) {
        logoutContextLookupStrategy = Constraint.isNotNull(strategy, "Lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final LogoutContext logoutContext = logoutContextLookupStrategy.apply(profileRequestContext);
        if (logoutContext == null) {
            log.debug("{} LogoutContext not found in ProfileRequestContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return;
        }
        
        final MessageContext inbound = profileRequestContext.getInboundMessageContext();
        final MessageContext outbound = profileRequestContext.getOutboundMessageContext();
        
        final InOutOperationContext placeholder = new InOutOperationContext(inbound, outbound);
        logoutContext.addSubcontext(placeholder);
        profileRequestContext.setInboundMessageContext(null);
        profileRequestContext.setOutboundMessageContext(null);
        
        final RelyingPartyContext relyingPartyCtx = profileRequestContext.getSubcontext(RelyingPartyContext.class);
        if (relyingPartyCtx != null) {
            placeholder.addSubcontext(relyingPartyCtx);
        }
        
        final AuditContext auditCtx = profileRequestContext.getSubcontext(AuditContext.class);
        if (auditCtx != null) {
            placeholder.addSubcontext(auditCtx);
        }
        
        log.debug("{} Saved off LogoutRequest processing state during logout propagation", getLogPrefix());
    }
    
}