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

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.Subcontext;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.opensaml.util.component.IdentifiableComponent;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.execution.Event;

/** Helper class for {@link org.springframework.webflow.execution.Action} operations. */
public final class ActionSupport {

    /**
     * ID of an Event indicating that the action completed successfully and processing should move on to the next step.
     */
    public static final String PROCEED_EVENT_ID = "proceed";

    /**
     * ID of an Event indicating that the action encountered an error and that the profile request processing is
     * complete and an error should be sent back to the requester.
     */
    public static final String ERROR_EVENT_ID = "error";

    /**
     * Event attribute name under which the {@link Throwable} thrown by the {@link #doExecute(ProfileRequestContext)}
     * method is stored.
     */
    public static final String ERROR_THROWABLE_ID = "errorException";

    /** Event attribute name under a message describing the error is stored. */
    public static final String ERROR_MESSAGE_ID = "errorMessage";

    /** Constructor. */
    private ActionSupport() {
    }

    /**
     * Gets the inbound message context.
     * 
     * @param <T> the inbound message type
     * @param action action attempting to retrieve the message context, never null
     * @param profileRequestContext current profile request context, never null
     * 
     * @return the inbound message context, never null
     * 
     * @throws InvalidInboundMessageContextException thrown if no inbound message context is available
     */
    public static <T> MessageContext<T> getInboundMessageContext(final AbstractIdentityProviderAction action,
            final ProfileRequestContext<T, Object> profileRequestContext) throws InvalidInboundMessageContextException {
        final MessageContext<T> messageContext = profileRequestContext.getInboundMessageContext();
        if (messageContext == null) {
            throw new InvalidInboundMessageContextException("Action " + action.getId()
                    + ": Inbound message context does not exist");
        }

        return messageContext;
    }

    /**
     * Gets the inbound message from the inbound message context.
     * 
     * @param <T> the message type
     * @param action action attempting to retrieve the message context
     * @param messageContext the message context, never null
     * 
     * @return the message, never null
     * 
     * @throws InvalidInboundMessageContextException thrown if the message context does not contain a message
     */
    public static <T> T getInboundMessage(final AbstractIdentityProviderAction action,
            final MessageContext<T> messageContext) throws InvalidInboundMessageContextException {
        final T message = messageContext.getMessage();
        if (message == null) {
            throw new InvalidInboundMessageContextException("Action " + action.getId()
                    + ": Inbound message context does not contain a message");
        }

        return message;
    }

    /**
     * Gets the outbound message context.
     * 
     * @param <T> the outbound message type
     * @param action action attempting to retrieve the message context
     * @param profileRequestContext current profile request context, never null
     * 
     * @return the outbound message context, never null
     * 
     * @throws InvalidInboundMessageContextException thrown if no outbound message context is available
     */
    public static <T> MessageContext<T> getOutboundMessageContext(final AbstractIdentityProviderAction action,
            final ProfileRequestContext<Object, T> profileRequestContext) throws InvalidInboundMessageContextException {
        final MessageContext<T> messageContext = profileRequestContext.getOutboundMessageContext();
        if (messageContext == null) {
            throw new InvalidInboundMessageContextException("Action " + action.getId()
                    + ": Outbound message context does not exist");
        }

        return messageContext;
    }

    /**
     * Gets the outbound message from the outbound message context.
     * 
     * @param <T> the message type
     * @param action action attempting to retrieve the message context
     * @param messageContext the message context, never null
     * 
     * @return the message, never null
     * 
     * @throws InvalidOutboundMessageContextException thrown if the message context does not contain a message
     */
    public static <T> T getOutboundMessage(final AbstractIdentityProviderAction action,
            final MessageContext<T> messageContext) throws InvalidOutboundMessageContextException {
        final T message = messageContext.getMessage();
        if (message == null) {
            throw new InvalidOutboundMessageContextException("Action " + action.getId()
                    + ": Outbound message context does not contain a message");
        }

        return message;
    }

    /**
     * Gets a subcontext from the container. The subcontext is not auto-created if it does not exist.
     * 
     * @param <T> the subcontext type
     * @param action action attempting to retrieve the message context
     * @param container container for the subcontext
     * @param subcontextType the type of the subcontext
     * 
     * @return the subcontext
     * 
     * @throws InvalidSubcontextException thrown if the required subcontext does not exist.
     */
    public static <T extends Subcontext> T getSubcontext(final AbstractIdentityProviderAction action,
            SubcontextContainer container, Class<T> subcontextType) throws InvalidSubcontextException {
        final T subcontext = container.getSubcontext(subcontextType, false);
        if (subcontext == null) {
            throw new InvalidSubcontextException("Action " + action.getId() + ": " + container.getClass().getName()
                    + " does not contain a subcontext of type " + subcontextType.getName());
        }

        return subcontext;
    }

    /**
     * Builds a {@link #PROCEED_EVENT_ID} event with no related attributes.
     * 
     * @param source the source of the event
     * 
     * @return the proceed event
     */
    public static Event buildProceedEvent(final IdentifiableComponent source) {
        return buildEvent(source, PROCEED_EVENT_ID, null);
    }

    /**
     * Builds an event, to be returned by the given component.
     * 
     * @param source IdP component that will return the constructed event, never null
     * @param eventId ID of the event, never null or empty
     * @param eventAttributes attributes associated with the event, may be null
     * 
     * @return the constructed {@link Event}
     */
    public static Event buildEvent(final IdentifiableComponent source, final String eventId,
            final AttributeMap eventAttributes) {
        Assert.isNotNull(source, "Component may not be null");

        final String trimmedEventId =
                Assert.isNotNull(StringSupport.trimOrNull(eventId), "ID of event for action " + source.getId()
                        + " may not be null");

        if (eventAttributes == null || eventAttributes.isEmpty()) {
            return new Event(source.getId(), trimmedEventId);
        } else {
            return new Event(source.getId(), trimmedEventId, eventAttributes);
        }
    }

    /**
     * Builds an error event. The event ID is {@link #ERROR_EVENT_ID} and includes in its attribute map the given
     * exception, bound under {@link #ERROR_THROWABLE_ID}. If the given exception has a message it is bound under
     * {@link #ERROR_MESSAGE_ID}.
     * 
     * @param source component that produced the error event
     * @param error exception that represents the error, may be null
     * 
     * @return the constructed event
     */
    public static Event buildErrorEvent(final IdentifiableComponent source, final Throwable error) {
        return buildErrorEvent(source, error, error.getMessage());
    }

    /**
     * Builds an error event. The event ID is {@link #ERROR_EVENT_ID} and includes in its attribute map the given
     * exception, bound under {@link #ERROR_THROWABLE_ID} and the textual error message, bound under
     * {@link #ERROR_MESSAGE_ID}.
     * 
     * @param source component that produced the error event
     * @param error exception that represents the error, may be null
     * @param message textual error message, may be null
     * 
     * @return the constructed event
     */
    public static Event
            buildErrorEvent(final IdentifiableComponent source, final Throwable error, final String message) {
        LocalAttributeMap eventAttributes = new LocalAttributeMap();

        if (error != null) {
            eventAttributes.put(ERROR_THROWABLE_ID, error);
        }

        final String trimmedMessage = StringSupport.trimOrNull(message);
        if (trimmedMessage != null) {
            eventAttributes.put(ERROR_MESSAGE_ID, trimmedMessage);
        }

        return buildEvent(source, ERROR_EVENT_ID, eventAttributes);
    }
}