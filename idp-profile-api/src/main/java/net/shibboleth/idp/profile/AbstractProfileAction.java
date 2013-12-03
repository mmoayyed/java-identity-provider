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

package net.shibboleth.idp.profile;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.idp.profile.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.action.ProfileAction;
import org.opensaml.profile.context.EventContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.execution.Action;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

//TODO perf metrics

/**
 * Base class for Spring-aware profile actions.
 * 
 * This base class takes care of the following:
 * <ul>
 * <li>retrieving the {@link ProfileRequestContext} from the current request environment</li>
 * <li>ensuring the {@link javax.servlet.http.HttpServletRequest} and {@link javax.servlet.http.HttpServletResponse} are
 * available on the {@link ProfileRequestContext}, if they exist</li>
 * <li>tracking performance metrics for the action</li>
 * </ul>
 * 
 * Action implementations should override {@link #doExecute(RequestContext, ProfileRequestContext)}.
 * 
 * @param <InboundMessageType> type of in-bound message
 * @param <OutboundMessageType> type of out-bound message
 */
@ThreadSafe
public abstract class AbstractProfileAction<InboundMessageType, OutboundMessageType> extends
    org.opensaml.profile.action.AbstractProfileAction<InboundMessageType, OutboundMessageType> implements Action {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractProfileAction.class);

    /** Strategy used to lookup the {@link ProfileRequestContext} from a given WebFlow {@link RequestContext}. */
    private Function<RequestContext, ProfileRequestContext> profileContextLookupStrategy;

    /**
     * Constructor.
     * 
     * Initializes the ID of this action to the class name. Initializes {@link #profileContextLookupStrategy} to
     * {@link WebflowRequestContextProfileRequestContextLookup}.
     */
    public AbstractProfileAction() {
        super();

        setId(getClass().getName());

        profileContextLookupStrategy = new WebflowRequestContextProfileRequestContextLookup();
    }

    /**
     * Gets the strategy used to lookup the {@link ProfileRequestContext} from a given WebFlow {@link RequestContext}.
     * 
     * @return strategy used to lookup the {@link ProfileRequestContext} from a given WebFlow {@link RequestContext}
     */
    @Nonnull public Function<RequestContext, ProfileRequestContext> getProfileContextLookupStrategy() {
        return profileContextLookupStrategy;
    }

    /**
     * Sets the strategy used to lookup the {@link ProfileRequestContext} from a given WebFlow {@link RequestContext}.
     * 
     * @param strategy strategy used to lookup the {@link ProfileRequestContext} from a given WebFlow
     *            {@link RequestContext}
     */
    public synchronized void setProfileContextLookupStrategy(
            @Nonnull final Function<RequestContext, ProfileRequestContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        profileContextLookupStrategy =
                Constraint.isNotNull(strategy, "ProfileRequestContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull public Event execute(@Nonnull final RequestContext springRequestContext) throws ProfileException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        final ProfileRequestContext<InboundMessageType, OutboundMessageType> profileRequestContext =
                profileContextLookupStrategy.apply(springRequestContext);
        if (profileRequestContext == null) {
            log.error("Action {}: IdP profile request context is not available", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_PROFILE_CTX);
        }

        return doExecute(springRequestContext, profileRequestContext);
    }

    /**
     * Spring-aware actions can override this method to fully control the execution of an Action
     * by the Web Flow engine.
     * 
     * <p>Alternatively they may override {@link #doExecute(ProfileRequestContext)} and access
     * Spring information via a {@link SpringRequestContext} attached to the profile request context.</p>
     * 
     * <p>The default implementation attaches the Spring Web Flow request context to the profile
     * request context tree to "narrow" the execution signature to the basic OpenSAML {@link ProfileAction}
     * interface. After execution, an {@link EventContext} is sought, and used to return a result back to
     * the Web Flow engine. If no context exists, a "proceed" event is signaled.</p>
     * 
     * @param springRequestContext the Spring request context
     * @param profileRequestContext a profile request context
     * @return a Web Flow event produced by the action
     * @throws ProfileException if an error occurs during execution
     */
    @Nonnull protected Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext<InboundMessageType, OutboundMessageType> profileRequestContext)
                    throws ProfileException {
        
        // Attach the Spring context to the context tree.
        SpringRequestContext springSubcontext =
                profileRequestContext.getSubcontext(SpringRequestContext.class, true);      
        springSubcontext.setRequestContext(springRequestContext);

        try {
            execute(profileRequestContext);
        } finally {     
            // Remove the Spring context from the context tree.     
            profileRequestContext.removeSubcontext(springSubcontext);
        }
        
        return getResult(this, profileRequestContext);
    }
    
    /**
     * Examines the profile context for an event to return, or signals a "proceed" event if
     * no {@link EventContext} is located; the EventContext will be removed upon completion.
     * 
     * <p>The EventContext must contain a Spring Web Flow {@link Event} or a {@link String}.
     * Any other type of context data will be ignored.</p>
     * 
     * @param action    the action signaling the event
     * @param profileRequestContext the profile request context to examine
     * @return  an event based on the profile request context, or "proceed"
     */
    @Nonnull protected Event getResult(@Nonnull final ProfileAction action,
            @Nonnull final ProfileRequestContext<InboundMessageType, OutboundMessageType> profileRequestContext) {
        
        // Check for an EventContext on output. Do not autocreate it.
        EventContext eventCtx = profileRequestContext.getSubcontext(EventContext.class, false);
        if (eventCtx != null) {
            profileRequestContext.removeSubcontext(eventCtx);
            if (eventCtx.getEvent() instanceof Event) {
                return (Event) eventCtx.getEvent();
            } else if (eventCtx.getEvent() instanceof String) {
                return ActionSupport.buildEvent(action, (String) eventCtx.getEvent());
            } else if (eventCtx.getEvent() instanceof AttributeMap) {
                AttributeMap map = (AttributeMap) eventCtx.getEvent();
                return ActionSupport.buildEvent(action, map.getString("eventId", EventIds.PROCEED_EVENT_ID), map); 
            } else {
                return null;
            }
        } else {
            // Assume the result is to proceed.
            return ActionSupport.buildProceedEvent(action);
        }
    }
}