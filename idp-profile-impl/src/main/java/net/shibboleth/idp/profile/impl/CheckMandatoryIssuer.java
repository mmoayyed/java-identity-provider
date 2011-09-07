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

package net.shibboleth.idp.profile.impl;

import net.shibboleth.idp.profile.AbstractInboundMessageSubcontextAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.opensaml.messaging.context.BasicMessageMetadataSubcontext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/** Checks that the incoming message has an issuer. */
public final class CheckMandatoryIssuer extends AbstractInboundMessageSubcontextAction<BasicMessageMetadataSubcontext> {

    /** Constructor. The ID of this component is set to the name of this class. */
    public CheckMandatoryIssuer() {
        setId(CheckMandatoryIssuer.class.getName());
    }

    /** {@inheritDoc} */
    public Class<BasicMessageMetadataSubcontext> getSubcontextType() {
        return BasicMessageMetadataSubcontext.class;
    }

    /** {@inheritDoc} */
    public Event doExecute(final RequestContext springRequestContext,
            final ProfileRequestContext profileRequestContext, final BasicMessageMetadataSubcontext messageSubcontext) {

        if (messageSubcontext.getMessageIssuer() == null) {
            return ActionSupport.buildErrorEvent(this, new NoMessageIssuerException(),
                    "Basic message metadata subcontext does not a message issuer");
        }

        return ActionSupport.buildProceedEvent(this);
    }

    /** A profile processing exception that occurs when the inbound message has no identified message issuer. */
    public static class NoMessageIssuerException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = 8451917927885322986L;

        /** Constructor. */
        public NoMessageIssuerException() {
            super();
        }

        /**
         * Constructor.
         * 
         * @param message exception message
         */
        public NoMessageIssuerException(String message) {
            super(message);
        }

        /**
         * Constructor.
         * 
         * @param wrappedException exception to be wrapped by this one
         */
        public NoMessageIssuerException(Exception wrappedException) {
            super(wrappedException);
        }

        /**
         * Constructor.
         * 
         * @param message exception message
         * @param wrappedException exception to be wrapped by this one
         */
        public NoMessageIssuerException(String message, Exception wrappedException) {
            super(message, wrappedException);
        }
    }
}