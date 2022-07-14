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

package net.shibboleth.idp.saml.messaging.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.InOutOperationContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.RecursiveTypedParentContextLookup;
import org.opensaml.messaging.handler.AbstractMessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.saml.profile.impl.SAMLRelyingPartyIdLookupStrategy;
import net.shibboleth.idp.saml.profile.impl.SAMLVerificationLookupStrategy;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Message handler that adds a {@link RelyingPartyContext} to the current {@link InOutOperationContext} tree
 * via a creation function. The context is populated via a lookup strategy to locate a {@link SAMLPeerEntityContext},
 * by default as a direct child of the parent {@link InOutOperationContext}.
 * 
 * @post InOutOperationContext.getSubcontext(RelyingPartyContext.class) != null and populated as above.
 */
public class InitializeRelyingPartyContextFromSAMLPeer extends AbstractMessageHandler {

    /** The relying party ID lookup function to inject. */
    @Nonnull private static final Function<RelyingPartyContext,String> RPID_LOOKUP
        = new SAMLRelyingPartyIdLookupStrategy();

    /** The verification lookup function to inject. */
    @Nonnull private static final Function<RelyingPartyContext,Boolean> VERIFY_LOOKUP
        = new SAMLVerificationLookupStrategy();
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(InitializeRelyingPartyContextFromSAMLPeer.class);

    /** Strategy that will return or create a {@link RelyingPartyContext}. */
    @Nonnull private Function<MessageContext,RelyingPartyContext> relyingPartyContextCreationStrategy;
    
    /** Strategy used to look up the {@link SAMLPeerEntityContext} to draw from. */
    @Nonnull private Function<MessageContext,SAMLPeerEntityContext> peerEntityContextLookupStrategy;

    /** SAML peer entity context to populate from. */
    @Nullable private SAMLPeerEntityContext peerEntityCtx;
    
    /** Constructor. */
    public InitializeRelyingPartyContextFromSAMLPeer() {
        relyingPartyContextCreationStrategy =
                new ChildContextLookup<>(RelyingPartyContext.class, true).compose(
                        new RecursiveTypedParentContextLookup<>(InOutOperationContext.class));
        peerEntityContextLookupStrategy =
                new ChildContextLookup<>(SAMLPeerEntityContext.class).compose(
                        new RecursiveTypedParentContextLookup<>(InOutOperationContext.class));
    }

    /**
     * Set the strategy used to return or create the {@link RelyingPartyContext}.
     * 
     * @param strategy creation strategy
     */
    public void setRelyingPartyContextCreationStrategy(
            @Nonnull final Function<MessageContext,RelyingPartyContext> strategy) {
        throwSetterPreconditionExceptions();
        relyingPartyContextCreationStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext creation strategy cannot be null");
    }
    
    /**
     * Set the strategy used to look up the {@link SAMLPeerEntityContext} to draw from.
     * 
     * @param strategy strategy used to look up the {@link SAMLPeerEntityContext}
     */
    public void setPeerEntityContextLookupStrategy(
            @Nonnull final Function<MessageContext,SAMLPeerEntityContext> strategy) {
        throwSetterPreconditionExceptions();
        peerEntityContextLookupStrategy =
                Constraint.isNotNull(strategy, "SAMLPeerEntityContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreInvoke(@Nonnull final MessageContext messageContext) throws MessageHandlerException {
        
        peerEntityCtx = peerEntityContextLookupStrategy.apply(messageContext);
        if (peerEntityCtx == null) {
            log.debug("{} Unable to locate SAMLPeerEntityContext", getLogPrefix());
            throw new MessageHandlerException("Unable to locate SAMLPeerEntityContext");
        }
        
        return super.doPreInvoke(messageContext);
    }
        
    /** {@inheritDoc} */
    @Override
    protected void doInvoke(@Nonnull final MessageContext messageContext) throws MessageHandlerException {

        final RelyingPartyContext rpContext = relyingPartyContextCreationStrategy.apply(messageContext);
        if (rpContext == null) {
            log.debug("{} Unable to locate or create RelyingPartyContext", getLogPrefix());
            throw new MessageHandlerException("Unable to locate or create RelyingPartyContext");
        }
        
        log.debug("{} Attaching RelyingPartyContext based on SAML peer {}", getLogPrefix(),
                peerEntityCtx.getEntityId());
        rpContext.setRelyingPartyIdContextTree(peerEntityCtx);
        rpContext.setRelyingPartyIdLookupStrategy(RPID_LOOKUP);
        rpContext.setVerificationLookupStrategy(VERIFY_LOOKUP);
    }
    
}