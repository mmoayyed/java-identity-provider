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

import net.shibboleth.idp.relyingparty.RelyingPartySubcontext;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.Subcontext;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.opensaml.util.component.IdentifiableComponent;
import org.opensaml.util.constraint.documented.NotNull;
import org.opensaml.util.constraint.documented.Null;
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
     * @param action action attempting to retrieve the message context
     * @param profileRequestContext current profile request context
     * 
     * @return the inbound message context,
     * 
     * @throws InvalidInboundMessageContextException thrown if no inbound message context is available
     */
    @NotNull
    public static <T> MessageContext<T> getRequiredInboundMessageContext(
            @NotNull final AbstractIdentityProviderAction action,
            @NotNull final ProfileRequestContext<T, Object> profileRequestContext)
            throws InvalidInboundMessageContextException {
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
     * @param action action attempting to retrieve the message
     * @param messageContext the message context
     * 
     * @return the message
     * 
     * @throws InvalidInboundMessageContextException thrown if the message context does not contain a message
     */
    @NotNull
    public static <T> T getRequiredInboundMessage(@NotNull final AbstractIdentityProviderAction action,
            @NotNull final MessageContext<T> messageContext) throws InvalidInboundMessageContextException {
        final T message = messageContext.getMessage();
        if (message == null) {
            throw new InvalidInboundMessageContextException("Action " + action.getId()
                    + ": Inbound message context does not contain a message");
        }

        return message;
    }

    /**
     * A convenience method that combines
     * {@link #getRequiredInboundMessageContext(AbstractIdentityProviderAction, ProfileRequestContext)} and
     * {@link #getRequiredInboundMessage(AbstractIdentityProviderAction, MessageContext)}.
     * 
     * @param <T> the message type
     * @param action action attempting to retrieve the message
     * @param profileRequestContext current profile request context
     * 
     * @return the message
     * 
     * @throws InvalidInboundMessageContextException thrown if no inbound message context is available or the message
     *             context does not contain a message
     */
    @NotNull
    public static <T> T getRequiredInboundMessage(@NotNull final AbstractIdentityProviderAction action,
            @NotNull final ProfileRequestContext<T, Object> profileRequestContext)
            throws InvalidInboundMessageContextException {
        return getRequiredInboundMessage(action, getRequiredInboundMessageContext(action, profileRequestContext));
    }

    /**
     * Gets the outbound message context.
     * 
     * @param <T> the outbound message type
     * @param action action attempting to retrieve the message context
     * @param profileRequestContext current profile request context
     * 
     * @return the outbound message context
     * 
     * @throws InvalidOutboundMessageContextException thrown if no outbound message context is available
     */
    @NotNull
    public static <T> MessageContext<T> getRequiredOutboundMessageContext(
            @NotNull final AbstractIdentityProviderAction action,
            @NotNull final ProfileRequestContext<Object, T> profileRequestContext)
            throws InvalidOutboundMessageContextException {
        final MessageContext<T> messageContext = profileRequestContext.getOutboundMessageContext();
        if (messageContext == null) {
            throw new InvalidOutboundMessageContextException("Action " + action.getId()
                    + ": Outbound message context does not exist");
        }

        return messageContext;
    }

    /**
     * Gets the outbound message from the outbound message context.
     * 
     * @param <T> the message type
     * @param action action attempting to retrieve the message
     * @param messageContext the message context
     * 
     * @return the message
     * 
     * @throws InvalidOutboundMessageContextException thrown if the message context does not contain a message
     */
    @NotNull
    public static <T> T getRequiredOutboundMessage(@NotNull final AbstractIdentityProviderAction action,
            @NotNull final MessageContext<T> messageContext) throws InvalidOutboundMessageContextException {
        final T message = messageContext.getMessage();
        if (message == null) {
            throw new InvalidOutboundMessageContextException("Action " + action.getId()
                    + ": Outbound message context does not contain a message");
        }

        return message;
    }

    /**
     * A convenience method that combines
     * {@link #getRequiredOutboundMessageContext(AbstractIdentityProviderAction, ProfileRequestContext)} and
     * {@link #getRequiredOutboundMessage(AbstractIdentityProviderAction, MessageContext)}.
     * 
     * @param <T> the message type
     * @param action action attempting to retrieve the message
     * @param profileRequestContext current profile request context
     * 
     * @return the message
     * 
     * @throws InvalidOutboundMessageContextException thrown if no outbound message context is available or the message
     *             context does not contain a message
     */
    @NotNull
    public static <T> T getRequiredOutboundMessage(@NotNull final AbstractIdentityProviderAction action,
            @NotNull final ProfileRequestContext<Object, T> profileRequestContext)
            throws InvalidOutboundMessageContextException {
        return getRequiredOutboundMessage(action, getRequiredOutboundMessageContext(action, profileRequestContext));
    }

    /**
     * Gets the {@link RelyingPartySubcontext} from the {@link ProfileRequestContext}.
     * 
     * @param action action attempting to retrieve the {@link RelyingPartySubcontext}
     * @param profileRequestContext current profile request context
     * 
     * @return the retrieved {@link RelyingPartySubcontext}
     * 
     * @throws InvalidSubcontextException thrown if the {@link ProfileRequestContext} does not contain a
     *             {@link RelyingPartySubcontext}
     */
    @Null
    public static RelyingPartySubcontext getRequiredRelyingPartyContext(
            @NotNull final AbstractIdentityProviderAction action,
            @NotNull final ProfileRequestContext profileRequestContext) throws InvalidSubcontextException {
        return getRequiredSubcontext(action, profileRequestContext, RelyingPartySubcontext.class);
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
    @NotNull
    public static <T extends Subcontext> T getRequiredSubcontext(@NotNull final AbstractIdentityProviderAction action,
            @NotNull final SubcontextContainer container, @NotNull final Class<T> subcontextType)
            throws InvalidSubcontextException {
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
    @NotNull
    public static Event buildProceedEvent(@NotNull final IdentifiableComponent source) {
        return buildEvent(source, PROCEED_EVENT_ID, null);
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
    @NotNull
    public static Event buildErrorEvent(@NotNull final IdentifiableComponent source, @NotNull final Throwable error) {
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
    @NotNull
    public static Event buildErrorEvent(@NotNull final IdentifiableComponent source, @NotNull final Throwable error,
            @NotNull final String message) {
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

    /**
     * Builds an event, to be returned by the given component.
     * 
     * @param source IdP component that will return the constructed event, never null
     * @param eventId ID of the event, never null or empty
     * @param eventAttributes attributes associated with the event, may be null
     * 
     * @return the constructed {@link Event}
     */
    @NotNull
    public static Event buildEvent(@NotNull final IdentifiableComponent source, @NotNull final String eventId,
            @NotNull final AttributeMap eventAttributes) {
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
     * Get the {@link Throwable} associated with an error event.
     * 
     * @param event the event
     * 
     * @return the {@link Throwable} that resulted in the error
     */
    @Null
    public static Throwable getEventError(@NotNull final Event event) {
        return (Throwable) event.getAttributes().get(ERROR_THROWABLE_ID);
    }

    /**
     * Gets the error message associated with an error event. This method will first check to see if an explicit error
     * message was associated with the event if not it will return the message from the {@link Throwable} associated
     * with the event, if there is one.
     * 
     * @param event the event
     * 
     * @return the error message associated with an error event
     */
    @Null
    public static String getEventErrorMessage(@NotNull final Event event) {
        AttributeMap attributes = event.getAttributes();
        if (attributes.contains(ERROR_MESSAGE_ID)) {
            return (String) attributes.get(ERROR_MESSAGE_ID);
        } else if (attributes.contains(ERROR_THROWABLE_ID)) {
            return ((Throwable) attributes.get(ERROR_THROWABLE_ID)).getMessage();
        }

        return null;
    }
}