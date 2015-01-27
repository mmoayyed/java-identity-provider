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

package net.shibboleth.idp.saml.profile.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.google.common.base.Function;

/**
 * Action that logs a SAML message.
 * 
 * The SAML message is logged only if the DEBUG level is enabled.
 * 
 * The logger used to log the SAML message is configurable, but defaults to {@link #DEFAULT_LOGGER_NAME}.
 * 
 * The SAML message is retrieved from the {@link MessageContext} which is itself retrieved from the
 * {@link ProfileRequestContext} by a lookup strategy, either
 * {@link org.opensaml.profile.context.navigate.InboundMessageContextLookup} or
 * {@link org.opensaml.profile.context.navigate.OutboundMessageContextLookup}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 */
public class LogSAMLMessage extends AbstractProfileAction {

    /** Name of logger used by default. */
    @Nonnull private static final String DEFAULT_LOGGER_NAME = "PROTOCOL_MESSAGE";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LogSAMLMessage.class);

    /** SAML message to be logged. */
    @Nullable private SAMLObject message;

    /** Message context lookup strategy. */
    @Nonnull private final Function<ProfileRequestContext, MessageContext> messageContextLookupStrategy;

    /** Message lookup strategy. */
    @Nonnull private final Function<MessageContext, SAMLObject> messageLookupStrategy;

    /** Used to log protocol messages. */
    @Nonnull private Logger messageLogger;

    /**
     * Constructor.
     *
     * @param strategy message context lookup strategy
     */
    public LogSAMLMessage(@Nonnull final Function<ProfileRequestContext, MessageContext> strategy) {
        this(strategy, DEFAULT_LOGGER_NAME);
    }

    /**
     * Constructor.
     *
     * @param strategy message context lookup strategy
     * @param logger name of logger
     */
    public LogSAMLMessage(@Nonnull final Function<ProfileRequestContext, MessageContext> strategy,
            @Nonnull final String logger) {
        final String name = Constraint.isNotNull(StringSupport.trimOrNull(logger), "Logger can not be null nor empty");
        messageLogger = LoggerFactory.getLogger(name);
        messageContextLookupStrategy = Constraint.isNotNull(strategy, "Strategy can not be null");
        messageLookupStrategy = new MessageLookup<>(SAMLObject.class);
    }

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!messageLogger.isDebugEnabled()) {
            return false;
        }

        final MessageContext messageContext = messageContextLookupStrategy.apply(profileRequestContext);
        if (messageContext == null) {
            log.debug("{} No message context, nothing to do", getLogPrefix());
            return false;
        }

        message = messageLookupStrategy.apply(messageContext);
        if (message == null) {
            log.debug("{} No message, nothing to do", getLogPrefix());
            return false;
        }

        return super.doPreExecute(profileRequestContext);
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        try {
            final Element dom = XMLObjectSupport.marshall(message);
            messageLogger.debug("\n" + SerializeSupport.prettyPrintXML(dom));
        } catch (MarshallingException e) {
            log.error("Unable to marshall message for logging purposes", e);
        }
    }
}
