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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.decoder.MessageDecoder;
import org.opensaml.messaging.handler.MessageHandler;
import org.opensaml.profile.action.ProfileAction;

import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Manages state for the webflow -> external -> webflow transitions
 * during proxied SAML authentication.
 * 
 * @since 4.0.0
 */
public class SAMLAuthnContext extends BaseContext {

    /** Outbound message handler to run prior to encoding. */
    @Nullable private MessageHandler outboundMessageHandler;

    /** Profile action to execute to produce outbound message response. */
    @Nonnull private ProfileAction encodeMessageAction;

    /** The function to use to obtain a decoder. */
    @Nonnull private Function<String,MessageDecoder> decoderFactory;
    
    /**
     * Constructor.
     *
     * @param action message-encoding profile action
     * @param factory factory function to obtain decoders
     */
    public SAMLAuthnContext(@Nonnull final ProfileAction action,
            @Nonnull final Function<String,MessageDecoder> factory) {
        encodeMessageAction = Constraint.isNotNull(action, "Profile action cannot be null");
        decoderFactory = Constraint.isNotNull(factory, "MessageDecoder factory cannot be null");
    }
    
    /**
     * Get the message-encoding profile action.
     * 
     * @return profile action
     */
    @Nonnull public ProfileAction getEncodeMessageAction() {
        return encodeMessageAction;
    }
    
    /**
     * Get the factory function to obtain message decoders.
     * 
     * @return factory function
     */
    @Nonnull public Function<String,MessageDecoder> getMessageDecoderFactory() {
        return decoderFactory;
    }
    
    /**
     * Get the outbound {@link MessageHandler} to run prior to encoding.
     * 
     * @return the outbound {@link MessageHandler}
     */
    @Nullable public MessageHandler getOutboundMessageHandler() {
        return outboundMessageHandler;
    }

    /**
     * Set the outbound {@link MessageHandler} to run prior to encoding.
     * 
     * @param handler outbound {@link MessageHandler} to set
     * 
     * @return this context
     */
    @Nonnull public SAMLAuthnContext setOutboundMessageHandler(@Nullable final MessageHandler handler) {
        outboundMessageHandler = handler;
        
        return this;
    }
    
}