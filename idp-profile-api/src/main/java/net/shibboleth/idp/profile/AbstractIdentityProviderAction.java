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

import net.jcip.annotations.ThreadSafe;

import org.opensaml.util.component.AbstractIdentifiedInitializableComponent;
import org.opensaml.util.component.ComponentValidationException;
import org.opensaml.util.component.ValidatableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.execution.Action;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Base class for IdP profile processing steps.
 * 
 * This base class takes care of the following things:
 * <ul>
 * <li>retrieving the {@link ProfileRequestContext} from the current request environment</li>
 * <li>ensure current {@link javax.servlet.http.HttpServletRequest} and {@link javax.servlet.http.HttpServletResponse}
 * is available on the {@link ProfileRequestContext}</li>
 * <li>tracking performance metrics for the action</li>
 * <li>providing convenience method for building the {@link Event} objects returned by this action</li>
 * <li>catching any {@link Throwable} thrown by {@link #doExecute(ProfileRequestContext)} and constructing an error
 * {@link Event} from it</li>
 * </ul>
 * 
 * @param <InboundMessageType> type of in-bound message
 * @param <OutboundMessageType> type of out-bound message
 */

// TODO perf metrics
@ThreadSafe
public abstract class AbstractIdentityProviderAction<InboundMessageType, OutboundMessageType> extends
        AbstractIdentifiedInitializableComponent implements ValidatableComponent, Action {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractIdentityProviderAction.class);

    /** {@inheritDoc} */
    public synchronized void setId(String componentId) {
        super.setId(componentId);
    }

    /** {@inheritDoc} */
    public Event execute(final RequestContext springRequestContext) {
        final ProfileRequestContext<InboundMessageType, OutboundMessageType> profileRequestContext =
                (ProfileRequestContext<InboundMessageType, OutboundMessageType>) springRequestContext
                        .getConversationScope().get(ProfileRequestContext.BINDING_KEY);
        if (profileRequestContext != null) {
            profileRequestContext.setHttpRequest(ActionSupport.getHttpServletRequest(springRequestContext));
            profileRequestContext.setHttpResponse(ActionSupport.getHttpServletResponse(springRequestContext));
        }

        Event result;
        try {
            result = doExecute(springRequestContext, profileRequestContext);
        } catch (Throwable t) {
            result =
                    ActionSupport.buildEvent(this, ActionSupport.ERROR_EVENT_ID, new LocalAttributeMap(
                            ActionSupport.ERROR_THROWABLE_ID, t));
        }

        if (ActionSupport.ERROR_EVENT_ID.equals(result.getId())) {
            log.debug("Action {}: failed with error message {}", getId(),
                    result.getAttributes().get(ActionSupport.ERROR_MESSAGE_ID));
        }

        return result;
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        // nothing to do here
    }

    /**
     * Performs this action.
     * 
     * @param springRequestContext current WebFlow request context, never null
     * @param profileRequestContext the current IdP profile request context, never null
     * 
     * @return the result of this action, never null
     * 
     * @throws Throwable thrown if there is some problem executing this action
     */
    public abstract Event doExecute(final RequestContext springRequestContext,
            final ProfileRequestContext<InboundMessageType, OutboundMessageType> profileRequestContext)
            throws Throwable;

}