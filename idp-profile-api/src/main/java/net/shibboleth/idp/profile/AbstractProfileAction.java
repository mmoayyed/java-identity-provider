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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.opensaml.profile.action.AbstractConditionalProfileAction;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.action.ProfileAction;
import org.opensaml.profile.context.EventContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.execution.Action;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Base class for Spring-aware profile actions.
 * 
 * <p>This base class takes care of the following:</p>
 * <ul>
 * <li>retrieving the {@link ProfileRequestContext} from the current request environment</li>
 * <li>populating the SWF {@link RequestContext} into the profile context tree</li>
 * <li>starting or stopping any timers as instructed by a
 *     {@link org.opensaml.profile.context.MetricContext} in the tree</li>
 * </ul>
 * 
 * <p>Action implementations may override {@link #doExecute(RequestContext, ProfileRequestContext)}
 * if they require SWF functionality, but most should override {@link #doExecute(ProfileRequestContext)}
 * instead.</p>
 */
@ThreadSafe
public abstract class AbstractProfileAction extends AbstractConditionalProfileAction
        implements Action, MessageSource, MessageSourceAware {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractProfileAction.class);

    /** Strategy used to lookup the {@link ProfileRequestContext} from a given WebFlow {@link RequestContext}. */
    @Nonnull private Function<RequestContext,ProfileRequestContext> profileContextLookupStrategy;

    /** MessageSource injected by Spring, typically the parent ApplicationContext itself. */
    @Nullable private MessageSource messageSource;
    
    /**
     * Constructor.
     * 
     * Initializes the ID of this action to the class name and calls {@link #setProfileContextLookupStrategy(Function)}
     * with {@link WebflowRequestContextProfileRequestContextLookup}.
     */
    public AbstractProfileAction() {
        profileContextLookupStrategy = new WebflowRequestContextProfileRequestContextLookup();
    }

    /**
     * Get the strategy used to lookup the {@link ProfileRequestContext} from a given WebFlow {@link RequestContext}.
     * 
     * @return lookup strategy
     */
    @Nonnull public Function<RequestContext,ProfileRequestContext> getProfileContextLookupStrategy() {
        return profileContextLookupStrategy;
    }

    /**
     * Set the strategy used to lookup the {@link ProfileRequestContext} from a given WebFlow {@link RequestContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setProfileContextLookupStrategy(
            @Nonnull final Function<RequestContext,ProfileRequestContext> strategy) {
        checkSetterPreconditions();

        profileContextLookupStrategy =
                Constraint.isNotNull(strategy, "ProfileRequestContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public Event execute(@Nullable final RequestContext springRequestContext) {
        checkComponentActive();

        if (springRequestContext == null) {
            // Suspect this is impossible but SWF is not annotated as such.
            log.error("{} Spring request context is not available", getLogPrefix());
            return ActionSupport.buildEvent(this, EventIds.INVALID_PROFILE_CTX);
        }
        
        final ProfileRequestContext profileRequestContext =
                profileContextLookupStrategy.apply(springRequestContext);
        if (profileRequestContext == null) {
            log.error("{} IdP profile request context is not available", getLogPrefix());
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
     */
    @Nullable protected Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) {
        
        // Attach the Spring context to the context tree.
        final SpringRequestContext springSubcontext =
                profileRequestContext.ensureSubcontext(SpringRequestContext.class);
        assert springSubcontext != null;
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
     * Examines the profile context for an event to return, or signals a successful outcome if
     * no {@link EventContext} is located; the EventContext will be removed upon completion.
     * 
     * <p>The EventContext must contain a Spring Web Flow {@link Event} or a {@link String}.
     * Any other type of context data will be ignored.</p>
     * 
     * @param action    the action signaling the event
     * @param profileRequestContext the profile request context to examine
     * @return  an event based on the profile request context, or "proceed"
     */
    @Nullable protected Event getResult(@Nonnull final ProfileAction action,
            @Nonnull final ProfileRequestContext profileRequestContext) {
        
        // Check for an EventContext on output.
        final EventContext eventCtx = profileRequestContext.getSubcontext(EventContext.class);
        if (eventCtx != null) {
            final Object event = eventCtx.getEvent();
            
            if (event instanceof Event e) {
                return e;
            } else if (event instanceof String e) {
                return ActionSupport.buildEvent(action, e);
            } else if (event instanceof AttributeMap) {
                @SuppressWarnings("unchecked")
                final AttributeMap<Object> map = (AttributeMap<Object>) event;
                return ActionSupport.buildEvent(action, map.getString("eventId", EventIds.PROCEED_EVENT_ID), map);
            }
        }
        
        // A null value can be used to implicitly continue evaluating an action-state until the last step.
        return null;
    }
    
    /**
     * Utilizes the active flow's {@link ApplicationContext} to obtain a bean of a given name and class.
     * 
     * @param <T> the bean type
     * 
     * @param profileRequestContext the profile request context
     * @param name bean name
     * @param claz bean type
     * 
     * @return the bean or null
     * 
     * @since 4.3.0
     */
    @Nullable protected <T> T getBean(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull @NotEmpty final String name, @Nonnull final Class<T> claz) {
        
        final SpringRequestContext springRequestContext =
                profileRequestContext.getSubcontext(SpringRequestContext.class);
        if (springRequestContext == null) {
            log.warn("{} Spring request context not found in profile request context", getLogPrefix());
            return null;
        }

        final RequestContext requestContext = springRequestContext.getRequestContext();
        if (requestContext == null) {
            log.warn("{} Web Flow request context not found in Spring request context", getLogPrefix());
            return null;
        }

        return getBean(requestContext, name, claz);
    }

    /**
     * Utilizes the active flow's {@link ApplicationContext} to obtain a bean of a given name and class. 
     * 
     * @param <T> the bean type
     * 
     * @param flowRequestContext the active flow's request context
     * @param name bean name
     * @param claz bean type
     * 
     * @return the bean or null
     * 
     * @since 4.3.0
     */
    @Nullable protected <T> T getBean(@Nonnull final RequestContext flowRequestContext,
            @Nonnull @NotEmpty final String name, @Nonnull final Class<T> claz) {
        
        try {
            final Object bean = flowRequestContext.getActiveFlow().getApplicationContext().getBean(name);
            if (bean != null && claz.isInstance(bean)) {
                return claz.cast(bean);
            }
        } catch (final BeansException e) {
            
        }
        
        log.warn("{} No bean of the correct type found named {}", getLogPrefix(), name);
        return null;
    }

    /**
     * Return a casted parameter of a given name from the flow's flow or conversation scope (in that order). 
     * 
     * @param <T> the parameter type
     * 
     * @param profileRequestContext profile request context
     * @param name parameter name
     * 
     * @return the parameter or null
     * 
     * @since 4.3.0
     */
    @Nullable protected <T> T getParameter(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull @NotEmpty final String name) {
        
        final SpringRequestContext springRequestContext =
                profileRequestContext.getSubcontext(SpringRequestContext.class);
        if (springRequestContext == null) {
            log.warn("{} Spring request context not found in profile request context", getLogPrefix());
            return null;
        }

        final RequestContext requestContext = springRequestContext.getRequestContext();
        if (requestContext == null) {
            log.warn("{} Web Flow request context not found in Spring request context", getLogPrefix());
            return null;
        }

        return getParameter(requestContext, name);
    }

    /**
     * Return a casted parameter of a given name from the flow's flow or conversation scope (in that order). 
     * 
     * @param <T> the parameter type
     * 
     * @param flowRequestContext the active flow's request context
     * @param name parameter name
     * 
     * @return the parameter or null
     * 
     * @since 4.3.0
     */
    @Nullable protected <T> T getParameter(@Nonnull final RequestContext flowRequestContext,
            @Nonnull @NotEmpty final String name) {
        
        MutableAttributeMap<Object> scope = flowRequestContext.getFlowScope();
        if (scope != null && scope.contains(name)) {
            return (T) scope.get(name);
        }
        
        scope = flowRequestContext.getConversationScope();
        if (scope != null && scope.contains(name)) {
            return (T) scope.get(name);
        }
        
        return null;
    }
    
    /** {@inheritDoc} */
    @Override
    public void setMessageSource(@Nullable final MessageSource source) {
        messageSource = source;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public String getMessage(@Nonnull final String code, @Nullable final Object[] args,
            @Nullable final String defaultMessage, @Nonnull final Locale locale) {
        if (messageSource != null) {
            return messageSource.getMessage(code, args, defaultMessage, locale);
        }
        return MessageFormat.format(defaultMessage, args);
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public String getMessage(@Nonnull final String code, @Nullable final Object[] args,
            @Nonnull final Locale locale) {
        if (messageSource != null) {
            return messageSource.getMessage(code, args, locale);
        }
        throw new NoSuchMessageException("MessageSource was not set");
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public String getMessage(@Nonnull final MessageSourceResolvable resolvable, @Nonnull final Locale locale) {
        if (messageSource != null) {
            return messageSource.getMessage(resolvable, locale);
        }
        throw new NoSuchMessageException("MessageSource was not set");
    }

    /**
     * Gets the Spring {@link RequestContext} from a {@link SpringRequestContext} stored in the context tree.
     *
     * @param profileRequestContext Profile request context.
     *
     * @return Spring request context.
     */
    @Nullable protected RequestContext getRequestContext(@Nonnull final ProfileRequestContext profileRequestContext) {
        final SpringRequestContext springRequestCtx = profileRequestContext.getSubcontext(SpringRequestContext.class);
        if (springRequestCtx == null) {
            return null;
        }
        return springRequestCtx.getRequestContext();
    }

}