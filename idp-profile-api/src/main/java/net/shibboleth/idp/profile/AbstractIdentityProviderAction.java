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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.util.component.AbstractIdentifiableInitializableComponent;
import org.opensaml.util.component.ComponentValidationException;
import org.opensaml.util.component.ValidatableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.execution.Action;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

//TODO perf metrics

/**
 * Base class for IdP profile processing steps.
 * 
 * This base class takes care of the following things:
 * <ul>
 * <li>retrieving the {@link ProfileRequestContext} from the current request environment</li>
 * <li>ensure current {@link javax.servlet.http.HttpServletRequest} and {@link javax.servlet.http.HttpServletResponse}
 * is available on the {@link ProfileRequestContext}</li>
 * <li>tracking performance metrics for the action</li>
 * </ul>
 * 
 * @param <InboundMessageType> type of in-bound message
 * @param <OutboundMessageType> type of out-bound message
 */
@ThreadSafe
public abstract class AbstractIdentityProviderAction<InboundMessageType, OutboundMessageType> extends
        AbstractIdentifiableInitializableComponent implements ValidatableComponent, Action {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractIdentityProviderAction.class);

    /** {@inheritDoc} */
    public synchronized void setId(String componentId) {
        super.setId(componentId);
    }

    /** {@inheritDoc} */
    public Event execute(final RequestContext springRequestContext) throws ProfileException {
        if (!isInitialized()) {
            throw new UninitializedActionException(this);
        }

        // we assume Spring set up its request context properly, if we needed to check this we would put a
        // checking action anywhere in a flow where a request would be (re-)entering a flow
        final ExternalContext externalContext = springRequestContext.getExternalContext();
        final HttpServletRequest httpRequest = (HttpServletRequest) externalContext.getNativeRequest();
        final HttpServletResponse httpResponse = (HttpServletResponse) externalContext.getNativeResponse();

        final ProfileRequestContext<InboundMessageType, OutboundMessageType> profileRequestContext =
                (ProfileRequestContext<InboundMessageType, OutboundMessageType>) springRequestContext
                        .getConversationScope().get(ProfileRequestContext.BINDING_KEY);
        if (profileRequestContext == null) {
            log.error("Action {}: IdP profile request context is not available", getId());
            throw new InvalidProfileRequestContextStateException("IdP profile request context is not available");
        }

        profileRequestContext.setHttpRequest(httpRequest);
        profileRequestContext.setHttpResponse(httpResponse);

        return doExecute(httpRequest, httpResponse, springRequestContext, profileRequestContext);
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        // nothing to do here
    }

    /**
     * Performs this action.
     * 
     * @param httpRequest current HTTP request
     * @param httpResponse current HTTP response
     * @param springRequestContext current WebFlow request context, never null
     * @param profileRequestContext the current IdP profile request context, never null
     * 
     * @return the result of this action, never null
     * 
     * @throws ProfileException thrown if there is a problem executing the profile action
     */
    protected abstract Event doExecute(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final RequestContext springRequestContext,
            final ProfileRequestContext<InboundMessageType, OutboundMessageType> profileRequestContext)
            throws ProfileException;

    /** Thrown if an action has not been initialized before its execute method is called. */
    public static class UninitializedActionException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = -4147656158860902409L;

        /**
         * Constructor.
         * 
         * @param action the action that was not initialized before use
         */
        public UninitializedActionException(AbstractIdentityProviderAction action) {
            super("Profile action " + action.getId() + " was not initialized prior to use");
        }
    }
}