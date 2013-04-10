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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.profile.navigate.WebflowRequestContextHttpServletRequestLookup;
import net.shibboleth.idp.profile.navigate.WebflowRequestContextHttpServletResponseLookup;
import net.shibboleth.idp.profile.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    AbstractIdentifiableInitializableComponent implements ValidatableComponent, Action {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractProfileAction.class);

    /** Strategy used to lookup the {@link HttpServletRequest} from a given WebFlow {@link RequestContext}. */
    private Function<RequestContext, HttpServletRequest> httpRequestLookupStrategy;

    /** Strategy used to lookup the {@link HttpServletResponse} from a given WebFlow {@link RequestContext}. */
    private Function<RequestContext, HttpServletResponse> httpResponseLookupStrategy;

    /** Strategy used to lookup the {@link ProfileRequestContext} from a given WebFlow {@link RequestContext}. */
    private Function<RequestContext, ProfileRequestContext> profileContextLookupStrategy;

    /**
     * Constructor.
     * 
     * Initializes the ID of this action to the class name. Initializes {@link #httpRequestLookupStrategy} to
     * {@link WebflowRequestContextHttpServletRequestLookup}. Initializes {@link #httpResponseLookupStrategy} to
     * {@link WebflowRequestContextHttpServletResponseLookup}. Initializes {@link #profileContextLookupStrategy} to
     * {@link WebflowRequestContextProfileRequestContextLookup}.
     */
    public AbstractProfileAction() {
        super();

        setId(getClass().getName());

        httpRequestLookupStrategy = new WebflowRequestContextHttpServletRequestLookup();
        httpResponseLookupStrategy = new WebflowRequestContextHttpServletResponseLookup();
        profileContextLookupStrategy = new WebflowRequestContextProfileRequestContextLookup();
    }

    /** {@inheritDoc} */
    public synchronized void setId(String componentId) {
        super.setId(componentId);
    }
    
    /**
     * Gets the strategy used to lookup the {@link HttpServletRequest} from a given WebFlow {@link RequestContext}.
     * 
     * @return strategy used to lookup the {@link HttpServletRequest} from a given WebFlow {@link RequestContext}
     */
    @Nonnull public Function<RequestContext, HttpServletRequest> getHttpRequestLookupStrategy() {
        return httpRequestLookupStrategy;
    }

    /**
     * Sets the strategy used to lookup the {@link HttpServletRequest} from a given WebFlow {@link RequestContext}.
     * 
     * @param strategy strategy used to lookup the {@link HttpServletRequest} from a given WebFlow
     *            {@link RequestContext}
     */
    public synchronized void setHttpRequestLookupStrategy(
            @Nonnull final Function<RequestContext, HttpServletRequest> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        httpRequestLookupStrategy =
                Constraint.isNotNull(strategy, "HttpServletRequest lookup strategy cannot be null");
    }

    /**
     * Gets the strategy used to lookup the {@link HttpServletResponse} from a given WebFlow {@link RequestContext}.
     * 
     * @return strategy used to lookup the {@link HttpServletResponse} from a given WebFlow {@link RequestContext}
     */
    @Nonnull public Function<RequestContext, HttpServletResponse> getHttpResponseLookupStrategy() {
        return httpResponseLookupStrategy;
    }

    /**
     * Sets the strategy used to lookup the {@link HttpServletResponse} from a given WebFlow {@link RequestContext}.
     * 
     * @param strategy strategy used to lookup the {@link HttpServletResponse} from a given WebFlow
     *            {@link RequestContext}
     */
    public synchronized void setHttpResponseLookupStrategy(
            @Nonnull final Function<RequestContext, HttpServletResponse> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        this.httpResponseLookupStrategy =
                Constraint.isNotNull(strategy, "HttpServletResponse lookup strategy cannot be null");
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

        // we assume Spring set up its request context properly, if we needed to check this we would put a
        // checking action anywhere in a flow where a request would be (re-)entering a flow

        final HttpServletRequest httpRequest = httpRequestLookupStrategy.apply(springRequestContext);
        final HttpServletResponse httpResponse = httpResponseLookupStrategy.apply(springRequestContext);

        final ProfileRequestContext<InboundMessageType, OutboundMessageType> profileRequestContext =
                profileContextLookupStrategy.apply(springRequestContext);
        if (profileRequestContext == null) {
            log.error("Action {}: IdP profile request context is not available", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_PROFILE_CTX);
        }

        profileRequestContext.setHttpRequest(httpRequest);
        profileRequestContext.setHttpResponse(httpResponse);

        return doExecute(springRequestContext, profileRequestContext);
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        // nothing to do here
    }

    /**
     * Performs this action. Default implementation returns a "proceed" event.
     * 
     * @param springRequestContext current WebFlow request context
     * @param profileRequestContext the current IdP profile request context
     * 
     * @return the result of this action
     * 
     * @throws ProfileException thrown if there is a problem executing the profile action
     */
    @Nonnull protected Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext<InboundMessageType, OutboundMessageType> profileRequestContext)
            throws ProfileException {
        return ActionSupport.buildProceedEvent(this);
    }
}