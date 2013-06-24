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

package net.shibboleth.idp.profile.impl;

import javax.annotation.Nonnull;

import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.handler.MessageHandler;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.EventContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Action;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/**
 * An {@link Action} implementation which adapts an OpenSAML {@link MessageHandler} for execution 
 * in a Spring WebFlow environment.
 * 
 * <p>
 * The {@link Direction} enum is used to indicate the target message context for the invocation
 * of the handler:
 * <ul>
 * <li>{@link Direction#INBOUND} indicates to execute the handler on the 
 * {@link ProfileRequestContext#getInboundMessageContext()}</li>
 * <li>{@link Direction#OUBTOUND} indicates to execute the handler on the 
 * {@link ProfileRequestContext#getOutboundMessageContext()}</li>
 * </ul>
 * </p>
 * 
 * @param <InboundMessageType> type of inbound message
 * @param <OutboundMessageType> type of outbound message
 */
public class WebFlowMessageHandlerAdaptor<InboundMessageType, OutboundMessageType> 
        extends AbstractIdentifiableInitializableComponent implements Action {
    
    /** Used to indicate the target message context for invocation of the adapted message handler. */
    public enum Direction {
        /** Indicates to invoke the handle on the inbound message context, obtained via 
         * {@link ProfileRequestContext#getInboundMessageContext()}. */
        INBOUND, 
        
        /** Indicates to invoke the handle on the outbound message context, obtained via
         * {@link ProfileRequestContext#getOutboundMessageContext()}. */
        OUTBOUND
        
        };
    
    /** Logger. */
    private Logger log = LoggerFactory.getLogger(WebFlowMessageHandlerAdaptor.class);
    
    /** The message handler being adapted. */
    private MessageHandler handler;
    
    /** The direction of execution for this action instance. */
    private Direction direction;
    
    /** Strategy used to lookup the {@link ProfileRequestContext} from a given WebFlow {@link RequestContext}. */
    private Function<RequestContext, ProfileRequestContext> profileContextLookupStrategy;

    /**
     * Constructor.
     *
     * @param messageHandler the adapted message handler
     * @param executionDirection the direction of execution
     */
    public WebFlowMessageHandlerAdaptor(MessageHandler messageHandler, Direction executionDirection) {
        super();
        handler = Constraint.isNotNull(messageHandler, "MessageHandler may not be null");
        direction = Constraint.isNotNull(executionDirection, "Execution direction may not be null");
        
        profileContextLookupStrategy = new WebflowRequestContextProfileRequestContextLookup();
        
        setId(getClass().getName());
    }

    /** {@inheritDoc} */
    public Event execute(RequestContext springRequestContext) throws Exception {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        final ProfileRequestContext<InboundMessageType, OutboundMessageType> profileRequestContext =
                profileContextLookupStrategy.apply(springRequestContext);
        if (profileRequestContext == null) {
            log.error("Action {}: IdP profile request context is not available", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_PROFILE_CTX);
        }
        
        MessageContext target = null;
        switch (direction) {
            case INBOUND:
                target = profileRequestContext.getInboundMessageContext();
                log.debug("Action {}: Invoking message handler of type '{}' on INBOUND message context", getId(), 
                        handler.getClass().getName());
                break;
            case OUTBOUND:
                target = profileRequestContext.getOutboundMessageContext();
                log.debug("Action {}: Invoking message handler of type '{}' on OUTBOUND message context", getId(), 
                        handler.getClass().getName());
                break;
            default:
                log.warn("Specified direction '{}' was unknown, skipping handler invocation", direction);
                return ActionSupport.buildProceedEvent(handler);
        } 
        
        if (target == null) {
            log.warn("Target message context was null, can not invoke handler");
            //TODO What to do?: 1) return an error event id 2) throw an exception 3) treat as non-fatal
            return null;
        }
        
        log.debug("Action {}: Invoking message handler on message context containing a message of type '{}'", getId(), 
                target.getMessage().getClass().getName());
        
        handler.invoke(target);
        
        // TODO same approach as actions, or different?  For now just copy what Scott did, it may all change anyway.
        return getResult(handler, target);
    }
    
    /**
     * Examines the target message context for an event to return, or signals a "proceed" event if
     * no {@link EventContext} is located; the EventContext will be removed upon completion.
     * 
     * <p>The EventContext must contain a Spring Web Flow {@link Event} or a {@link String}.
     * Any other type of context data will be ignored.</p>
     * 
     * @param messageHandler the action signaling the event
     * @param messageContext the message context to examine
     * @return  an event based on the message context, or "proceed"
     */
    @Nonnull protected Event getResult(@Nonnull final MessageHandler messageHandler, 
            @Nonnull final MessageContext messageContext) {
        
        // Check for an EventContext on output. Do not autocreate it.
        EventContext eventCtx = messageContext.getSubcontext(EventContext.class, false);
        if (eventCtx != null) {
            messageContext.removeSubcontext(eventCtx);
            if (eventCtx.getEvent() instanceof Event) {
                return (Event) eventCtx.getEvent();
            } else if (eventCtx.getEvent() instanceof String) {
                return ActionSupport.buildEvent(messageHandler, (String) eventCtx.getEvent());
            } else {
                return null;
            }
        } else {
            // Assume the result is to proceed.
            return ActionSupport.buildProceedEvent(messageHandler);
        }
    }

}
