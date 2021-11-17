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

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.ext.saml2aslo.Asynchronous;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.metadata.SSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Profile action that performs initial analysis of a {@link LogoutRequest} or {@link LogoutResponse} to
 * dispatch it for subsequent processing.
 * 
 * <p>This action is essentially three decision states in one, using some custom events.</p>
 * 
 * <p>If the inbound message is a {@link LogoutResponse} then the event "IsLogoutResponse" is signaled.</p>
 * 
 * <p>If the inbound message is a {@link LogoutRequest}, then one of two events is signaled. If the request
 * contains the {@link Asynchronous} extension, then the "IsLogoutRequestAsync" event is signaled. This also
 * occurs, provided a particular option is enabled, if the request does not contain the extension but there
 * is no SAML metadata available for the requester or the metadata contains no {@link SingleLogoutService}
 * endpoints.</p>
 * 
 * <p>Finally, {@link EventIds#PROCEED_EVENT_ID} is the result if neither of the above applies.</p>
 * 
 * <p>Various standard events may occur if the message is missing or isn't an appropriate type.</p>
 * 
 * @event {@link #IS_LOGOUT_RESPONSE}
 * @event {@link #IS_LOGOUT_REQUEST_ASYNC}
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#INVALID_MESSAGE}
 * 
 * @since 4.2.0
 */
public class PreProcessLogoutMessage extends AbstractProfileAction {

    /** Event to signal for a logout response. */
    @Nonnull public static final String IS_LOGOUT_RESPONSE = "IsLogoutResponse";

    /** Event to signal for a logout response. */
    @Nonnull public static final String IS_LOGOUT_REQUEST_ASYNC = "IsLogoutRequestAsync";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PreProcessLogoutMessage.class);

    /** Assume asynchronous in absence of metadata. */
    private boolean assumeAsync;
    
    /** Lookup strategy for metadata context. */
    @Nonnull private Function<ProfileRequestContext,SAMLMetadataContext> metadataContextLookupStrategy;
    
    /** Constructor. */
    public PreProcessLogoutMessage() {
        metadataContextLookupStrategy = new InboundMessageContextLookup().andThen(
                new ChildContextLookup<>(SAMLPeerEntityContext.class).andThen(
                        new ChildContextLookup<>(SAMLMetadataContext.class)));
    }

    /**
     * Sets whether to treat logout requests as asynchronous (not requiring a response) if no
     * metadata is available or lacks endpoints.
     * 
     * <p>Defaults to false.</p>
     * 
     * @param flag what to set
     */
    public void setAssumeAsynchronousLogout(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        assumeAsync = flag;
    }
    
    /**
     * Set the lookup strategy for the {@link SAMLMetadataContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setMetadataContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SAMLMetadataContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        metadataContextLookupStrategy =
                Constraint.isNotNull(strategy, "SAMLMetadataContext lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        if (profileRequestContext.getInboundMessageContext() == null) {
            log.warn("{} No inbound message context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        } else if (!(profileRequestContext.getInboundMessageContext().getMessage() instanceof SAMLObject)) {
            log.warn("{} No inbound SAML message", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MESSAGE);
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        final SAMLObject msg = (SAMLObject) profileRequestContext.getInboundMessageContext().getMessage();
        if (msg instanceof LogoutResponse) {
            ActionSupport.buildEvent(profileRequestContext, IS_LOGOUT_RESPONSE);
            return;
        } else if (!(msg instanceof LogoutRequest)) {
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MESSAGE);
            return;
        }
        
        final Extensions exts = ((LogoutRequest) msg).getExtensions();
        if (exts != null && !exts.getUnknownXMLObjects(Asynchronous.DEFAULT_ELEMENT_NAME).isEmpty()) {
            log.debug("{} LogoutRequest contained Asynchronous extension", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IS_LOGOUT_REQUEST_ASYNC);
            return;
        }
        
        if (assumeAsync) {
            final SAMLMetadataContext mdCtx = metadataContextLookupStrategy.apply(profileRequestContext);
            if (mdCtx == null || !(mdCtx.getRoleDescriptor() instanceof SSODescriptor) ||
                    ((SSODescriptor) mdCtx.getRoleDescriptor()).getSingleLogoutServices().isEmpty()) {
                log.debug("{} LogoutRequest treated as Asynchronous due to lack of metadata", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, IS_LOGOUT_REQUEST_ASYNC);
            }
        }
    }

}