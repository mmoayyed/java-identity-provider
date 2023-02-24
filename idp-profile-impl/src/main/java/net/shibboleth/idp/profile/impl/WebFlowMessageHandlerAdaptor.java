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

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.handler.MessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.MetricContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * An {@link AbstractProfileAction} subclass that adapts an OpenSAML {@link MessageHandler} for execution 
 * in a Spring WebFlow environment.
 * 
 * <p>The handler to run may be injected directly, or supplied via a lookup function.</p>
 * 
 * <p>
 * The {@link Direction} enum is used to indicate the target message context for the invocation
 * of the handler:
 * </p>
 * <ul>
 * <li>{@link Direction#INBOUND} indicates to execute the handler on the 
 * {@link ProfileRequestContext#getInboundMessageContext()}</li>
 * <li>{@link Direction#OUTBOUND} indicates to execute the handler on the 
 * {@link ProfileRequestContext#getOutboundMessageContext()}</li>
 * </ul>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event any, as set
 */
public class WebFlowMessageHandlerAdaptor extends AbstractProfileAction {
    
    /** Used to indicate the target message context for invocation of the adapted message handler. */
    public enum Direction {
        /** Indicates to invoke the handle on the inbound message context, obtained via 
         * {@link ProfileRequestContext#getInboundMessageContext()}. */
        INBOUND, 
        
        /** Indicates to invoke the handle on the outbound message context, obtained via
         * {@link ProfileRequestContext#getOutboundMessageContext()}. */
        OUTBOUND,
        };
    
    /** Logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(WebFlowMessageHandlerAdaptor.class);    

    /** Lookup strategy for handler to run if not set directly. */
    @Nullable private Function<ProfileRequestContext,MessageHandler> handlerLookupStrategy;
    
    /** The message handler being adapted. */
    @Nullable private MessageHandler handler;
    
    /** The direction of execution for this action instance. */
    private final Direction direction;
    
    /** An event to signal in the event of a handler exception. */
    @Nullable private String errorEvent;

    /**
     * Constructor.
     *
     * @param executionDirection the direction of execution
     */
    private WebFlowMessageHandlerAdaptor(
            @Nonnull @ParameterName(name="executionDirection") final Direction executionDirection) {
        direction = Constraint.isNotNull(executionDirection, "Execution direction cannot be null");
    }
    
    /**
     * Constructor.
     *
     * @param messageHandler the adapted message handler
     * @param executionDirection the direction of execution
     */
    public WebFlowMessageHandlerAdaptor(
            @Nonnull @ParameterName(name="messageHandler") final MessageHandler messageHandler,
            @Nonnull @ParameterName(name="executionDirection") final Direction executionDirection) {
        this(executionDirection);
        
        handler = Constraint.isNotNull(messageHandler, "MessageHandler cannot be null");
    }

    /**
     * Constructor.
     *
     * @param lookupStrategy lookup function for message handler to run 
     * @param executionDirection the direction of execution
     */
    public WebFlowMessageHandlerAdaptor(
            @Nonnull @ParameterName(name="lookupStrategy")
                final Function<ProfileRequestContext,MessageHandler> lookupStrategy,
            @Nonnull @ParameterName(name="executionDirection") final Direction executionDirection) {
        this(executionDirection);
        
        handlerLookupStrategy = Constraint.isNotNull(lookupStrategy, "MessageHandler lookup strategy cannot be null");
    }
    
    /**
     * Set the event to signal in the event of a handler exception.
     * 
     * @param event event to signal
     */
    public void setErrorEvent(@Nullable final String event) {
        errorEvent = StringSupport.trimOrNull(event);
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }

        if (handler == null) {
            if (handlerLookupStrategy == null) {
                log.error("{} Neither c:messageHandler not c:lookupStrategy specified", getLogPrefix());
                return false;
            }
            assert handlerLookupStrategy != null;
            handler = handlerLookupStrategy.apply(profileRequestContext);
            if (handler == null) {
                log.debug("{} No message handler returned by lookup function, nothing to do", getLogPrefix());
                return false;
            }
        }
        
        final MetricContext metricCtx = profileRequestContext.getSubcontext(MetricContext.class);
        if (metricCtx != null) {
            assert handler != null;
            final String className = handler.getClass().getSimpleName();
            assert className!=null; 
            metricCtx.start(className);
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
//CheckStyle: ReturnCount OFF
    @Override public void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        final MessageHandler msgHandler = handler;
        assert msgHandler != null;
        MessageContext target = null;
        switch (direction) {
            case INBOUND:
                target = profileRequestContext.getInboundMessageContext();
                log.debug("{} Invoking message handler of type '{}' on INBOUND message context", getLogPrefix(), 
                        msgHandler.getClass().getName());
                break;
            case OUTBOUND:
                target = profileRequestContext.getOutboundMessageContext();
                log.debug("{} Invoking message handler of type '{}' on OUTBOUND message context", getLogPrefix(), 
                        msgHandler.getClass().getName());
                break;
            default:
                log.warn("{} Specified direction '{}' was unknown, skipping handler invocation", getLogPrefix(),
                        direction);
                return;
        } 
        
        if (target == null) {
            log.warn("{} Target message context was null, cannot invoke handler", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return;
        }

        final Object message = target.getMessage();
        if (message != null) {
            log.debug("{} Invoking message handler on message context containing a message of type '{}'",
                    getLogPrefix(), message.getClass().getName());
        }
        
        try {
            msgHandler.invoke(target);
        } catch (final MessageHandlerException e) {
            log.warn("{} Exception handling message", getLogPrefix(), e);
            if (errorEvent != null) {
                ActionSupport.buildEvent(profileRequestContext, errorEvent);
            } else {
                ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            }
        }
    }
  //CheckStyle: ReturnCount ON

    /** {@inheritDoc} */
    @Override
    protected void doPostExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        final MetricContext metricCtx = profileRequestContext.getSubcontext(MetricContext.class);
        if (metricCtx != null) {
            assert handler != null;
            final String name = handler.getClass().getSimpleName();
            assert name != null;
            metricCtx.stop(name);
            metricCtx.inc(name);
        }
        
        super.doPostExecute(profileRequestContext);
    }

 }