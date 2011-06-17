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

import net.shibboleth.idp.AbstractComponent;

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.execution.Action;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Base class for IdP profile steps.
 * 
 * This base class takes care of the following things:
 * <ul>
 * <li>retrieving the {@link ProfileRequestContext} from the current request environment</li>
 * <li>ensure current {@link HttpServletRequest} and {@link HttpServletResponse} is available on the
 * {@link ProfileRequestContext}</li>
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

public abstract class AbstractShibbolehtAction<InboundMessageType, OutboundMessageType> extends AbstractComponent
        implements Action {

    /** ID of an Event representing an error with this action. */
    public static final String ERROR_EVENT_ID = "error";

    /**
     * Event attribute name under which the {@link Throwable} thrown by the {@link #doExecute(ProfileRequestContext)}
     * method is stored.
     */
    public static final String ERROR_THROWABLE_ID = "exception";

    /**
     * Constructor.
     * 
     * @param componentId unique ID for this action
     */
    public AbstractShibbolehtAction(final String componentId) {
        super(componentId);
    }

    /** {@inheritDoc} */
    public Event execute(final RequestContext context) {
        final HttpServletRequest httpRequest = (HttpServletRequest) context.getExternalContext().getNativeRequest();
        final HttpServletResponse httpResponse = (HttpServletResponse) context.getExternalContext().getNativeResponse();

        final ProfileRequestContext<InboundMessageType, OutboundMessageType> profileRequestContext =
                (ProfileRequestContext<InboundMessageType, OutboundMessageType>) context.getConversationScope().get(
                        ProfileRequestContext.BINDING_KEY);
        profileRequestContext.setHttpRequest(httpRequest);
        profileRequestContext.setHttpResponse(httpResponse);

        Event result;
        try {
            result = doExecute(profileRequestContext);
        } catch (Throwable t) {
            result = buildEvent(ERROR_EVENT_ID, new LocalAttributeMap(ERROR_THROWABLE_ID, t));
        }

        return result;
    }

    /**
     * Performs this action.
     * 
     * @param requestContext the current profile request context
     * 
     * @return the result of this action
     * 
     * @throws Throwable thrown if there is some problem executing this action
     */
    public abstract Event
            doExecute(final ProfileRequestContext<InboundMessageType, OutboundMessageType> requestContext)
                    throws Throwable;

    /**
     * Creates an {@link Event} to be returned by this action. The source of the event is set to ID for this component
     * and contains no attributes.
     * 
     * @param eventId ID of the event, may not be null or empty
     * 
     * @return the event
     */
    protected Event buildEvent(final String eventId) {
        return buildEvent(eventId, null);
    }

    /**
     * Creates an {@link Event} to be returned by this action. The source of the event is set to ID for this component
     * and contains no attributes.
     * 
     * @param eventId ID of the event, may not be null or empty
     * @param attributes event attributes, may be null or empty
     * 
     * @return the event
     */
    protected Event buildEvent(final String eventId, final AttributeMap attributes) {
        final String trimmedEventId = StringSupport.trimOrNull(eventId);
        Assert.isNotNull(trimmedEventId, "ID of event for action " + getId() + " may not be null");

        if (attributes == null || attributes.isEmpty()) {
            return new Event(getId(), eventId);
        } else {
            return new Event(getId(), eventId, attributes);
        }
    }
}